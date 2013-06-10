/*******************************************************************************
 * Copyright (c) 2013 GoPivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.gettingstarted.guides.wizard;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.springframework.ide.eclipse.gettingstarted.GettingStartedActivator;
import org.springframework.ide.eclipse.gettingstarted.content.BuildType;
import org.springframework.ide.eclipse.gettingstarted.content.CodeSet;
import org.springframework.ide.eclipse.gettingstarted.dashboard.WebDashboardPage;
import org.springframework.ide.eclipse.gettingstarted.guides.GettingStartedGuide;
import org.springframework.ide.eclipse.gettingstarted.importing.ImportUtils;
import org.springframework.ide.eclipse.gettingstarted.util.UIThreadDownloadDisallowed;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveVariable;
import org.springsource.ide.eclipse.commons.livexp.core.ValidationResult;
import org.springsource.ide.eclipse.commons.livexp.core.Validator;
import org.springsource.ide.eclipse.gradle.core.util.ExceptionUtil;

/**
 * Core counterpart of GuideImportWizard (essentially this is a 'model' for the wizard
 * UI.
 * 
 * @author Kris De Volder
 */
public class GuideImportWizardModel {
	
	//TODO: Validate build system choice against installed tooling. (warn if m2e / gradle tooling is
	// required but not installed.
	
	//TODO: Validation: shouldn't allow importing if something already exists where codeset content
	// will be downloaded. This will overwrite what's there. At the very least a warning should
	// appear in the wizard.
	
	public static class CodeSetValidator extends LiveExpression<ValidationResult> {

		private LiveVariable<GettingStartedGuide> codesetProvider;
		private LiveSet<String> selectedNames;

		public CodeSetValidator(LiveVariable<GettingStartedGuide> guide, LiveSet<String> codesets) {
			this.codesetProvider = guide;
			this.selectedNames = codesets;
			this.dependsOn(guide);
			this.dependsOn(codesets);
		}

		@Override
		protected ValidationResult compute() {
//			GettingStartedGuide g = codesetProvider.getValue();
//			if (g!=null) { //Don't check or produce errors unless a content provider has been selected.
//				Set<String> names = selectedNames.getValue();
//				if (names == null || names.isEmpty()) {
//					return ValidationResult.error("No codeset selected");
//				}
//			}
			return ValidationResult.OK;
		}

	}

	/**
	 * The chosen guide to import stuff from.
	 */
	private LiveVariable<GettingStartedGuide> guide = new LiveVariable<GettingStartedGuide>();
	
	/**
	 * The names of the codesets selected for import.
	 */
	private LiveSet<String> codesets = new LiveSet<String>(new HashSet<String>());
	{
		codesets.addAll(GettingStartedGuide.defaultCodesetNames); //Select both codesets by default.
	}
	
	/**
	 * The valid codeset names w.r.t. the currently selected guide
	 */
	public final LiveExpression<String[]> validCodesetNames = new LiveExpression<String[]>(null) {

		@Override
		protected String[] compute() {
			try {
				GettingStartedGuide g = guide.getValue();
				if (g!=null) {
					List<CodeSet> validSets = g.getCodeSets();
					if (validSets!=null) {
						String[] names = new String[validSets.size()];
						for (int i = 0; i < names.length; i++) {
							names[i] = validSets.get(i).getName();
						}
						return names;
					}
				}
			} catch (UIThreadDownloadDisallowed e) {
				//Failed because content is not yet downloade but this is ok... 
				//just schedule download to happen later and in the mean time return something sensible
				scheduleDownloadJob();
			} catch (Throwable e) {
				GettingStartedActivator.log(e);
			}
			return GettingStartedGuide.defaultCodesetNames;
		}
	};
	
	/**
	 * The build type chosen by user
	 */
	private LiveVariable<BuildType> buildType = new LiveVariable<BuildType>(BuildType.DEFAULT);
	
	private LiveExpression<ValidationResult> guideValidator = Validator.notNull(guide, "A Guide must be selected");
	private LiveExpression<ValidationResult> codesetValidator = new CodeSetValidator(guide, codesets);
	private LiveExpression<ValidationResult> buildTypeValidator = new Validator() {
		@Override
		protected ValidationResult compute() {
			try {
				GettingStartedGuide g = guide.getValue();
				if (g!=null) {
					try {
						BuildType bt = buildType.getValue();
						if (bt==null) {
							return ValidationResult.error("No build type selected");
						} else {
							List<String> codesetNames = codesets.getValues();
							if (codesetNames!=null) {
								for (String csname : codesetNames) {
									CodeSet cs = g.getCodeSet(csname);
									if (cs==null) {
										//Can happen if widgets / model is in process of propagating changes
										// codeset names may still be those selected for another guide. May not
										// be valid yet for current guide.
										System.out.println("Ignore invalid codeset "+csname+" in "+g.getName());
									} else {
										ValidationResult result = cs.validateBuildType(bt);
										if (!result.isOk()) {
											return result.withMessage("CodeSet '"+csname+"': "+result.msg);
										}
									}
								}
							}
						}
					} catch (UIThreadDownloadDisallowed e) {
						//Careful... check some of the validation will trigger downloads. This is not allowed in UI thread.
						scheduleDownloadJob();
						return ValidationResult.info(g.getName()+" is downloading...");
					}
				}
				return ValidationResult.OK;
			} catch (Throwable e) {
				GettingStartedActivator.log(e);
				return ValidationResult.error(ExceptionUtil.getMessage(e));
			}
		}

	};
	
	public LiveExpression<Boolean> isDownloaded = new LiveExpression<Boolean>(false) {
		@Override
		protected Boolean compute() {
			GettingStartedGuide g = guide.getValue();
			return g == null || g.isDownloaded(); 
		}
	};

	/**
	 * The description of the current guide.
	 */
	public final LiveExpression<String> description = new LiveExpression<String>("<no description>") {
		@Override
		protected String compute() {
			GettingStartedGuide g = guide.getValue();
			if (g!=null) {
				return g.getDescription();
			}
			return "<no guide selected>";
		}
	};
	
	public final LiveExpression<URL> homePage = new LiveExpression<URL>(null) {
		@Override
		protected URL compute() {
			GettingStartedGuide g = guide.getValue();
			if (g!=null) {
				return g.getHomePage();
			}
			return null;
		}
	};

	/**
	 * Indicates whether the user has selected the option to open the home page.
	 */
	private LiveVariable<Boolean> enableOpenHomePage = new LiveVariable<Boolean>(true);
	
	{
		buildTypeValidator.dependsOn(guide);
		buildTypeValidator.dependsOn(isDownloaded);
		buildTypeValidator.dependsOn(buildType);
		buildTypeValidator.dependsOn(codesets);
		
		isDownloaded.dependsOn(guide);
		
		description.dependsOn(guide);
		
		homePage.dependsOn(guide);
		
		validCodesetNames.dependsOn(guide);
		validCodesetNames.dependsOn(isDownloaded);
	}
	
	/**
	 * Downloads currently selected guide content (if it is not already cached locally.
	 */
	public void performDownload(IProgressMonitor mon) throws Exception {
		mon.beginTask("Downloading", 1);
		try {
			GettingStartedGuide g = guide.getValue();
			if (g!=null) {
				g.getZip().getFile(); //This forces download
			}
		} finally {
			isDownloaded.refresh();
			mon.done();
		}
	}
	
	private void scheduleDownloadJob() {
		Job job = new Job("Downloading guide content") {
			protected IStatus run(IProgressMonitor mon) {
				try {
					performDownload(mon);
				} catch (Throwable e) {
					return ExceptionUtil.status(e);
				}
				return Status.OK_STATUS;
			}
			
		};
		job.schedule();
	}
	
	
	/**
	 * Performs the final step of the wizard when user clicks on Finish button.
	 * @throws InterruptedException 
	 * @throws InvocationTargetException 
	 */
	public boolean performFinish(IProgressMonitor mon) throws InvocationTargetException, InterruptedException {
		//The import will be carried out with whatever the currently selected values are
		// in all the input fields / variables / widgets.
		GettingStartedGuide g = guide.getValue();
		BuildType bt = buildType.getValue();
		Set<String> codesetNames = codesets.getValue();
		
		mon.beginTask("Import guide content", codesetNames.size()+1);
		try {
			for (String name : codesetNames) {
				IRunnableWithProgress oper = bt.getImportStrategy().createOperation(ImportUtils.importConfig(
						g, 
						g.getCodeSet(name)
				));
				oper.run(new SubProgressMonitor(mon, 1));
			}
			if (enableOpenHomePage.getValue()) {
				URL url = homePage.getValue();
				if (url!=null) {
					WebDashboardPage.openUrl(url.toString());
				}
			}
			return true;
		} catch (UIThreadDownloadDisallowed e) {
			//This shouldn't be possible... Finish button won't be enabled unless all is validated which implies
			// the content has to be downloaded.
			GettingStartedActivator.log(e);
			return false;
		} finally {
			mon.done();
		}
	}
	
	public void setGuide(GettingStartedGuide guide) {
		this.guide.setValue(guide);
	}
	
	public GettingStartedGuide getGuide() {
		return guide.getValue();
	}

	public SelectionModel<BuildType> getBuildTypeModel() {
		return new SelectionModel<BuildType>(buildType, buildTypeValidator);
	}

	public SelectionModel<GettingStartedGuide> getGuideSelectionModel() {
		return new SelectionModel<GettingStartedGuide>(guide, guideValidator);
	}
	
	public MultiSelectionModel<String> getCodeSetModel() {
		return new MultiSelectionModel<String>(codesets, codesetValidator);
	}
	
	public LiveExpression<Boolean> isDownloaded() {
		return isDownloaded;
	}

	public LiveVariable<Boolean> getEnableOpenHomePage() {
		return enableOpenHomePage;
	}

}
