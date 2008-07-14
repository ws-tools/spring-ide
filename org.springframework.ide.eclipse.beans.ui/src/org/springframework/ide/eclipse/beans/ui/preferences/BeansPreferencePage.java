/*******************************************************************************
 * Copyright (c) 2005, 2007 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.beans.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.springframework.ide.eclipse.beans.core.model.IBeansModel;
import org.springframework.ide.eclipse.beans.ui.BeansUIPlugin;

/**
 * {@link IWorkbenchPreferencePage} that allows to change the persistence property for the
 * {@link IBeansModel}.
 * @author Christian Dupuis
 * @since 2.0
 */
public class BeansPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private RadioGroupFieldEditor radioEditor;

	private BooleanFieldEditor graphEditorInnerBeans;

	private FieldEditor graphEditorInfrastructureBeans;

	protected Control createContents(Composite parent) {

		Composite entryTable = new Composite(parent, SWT.NULL);

		// Create a data that takes up the extra space in the dialog .
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		entryTable.setLayoutData(data);

		GridLayout layout = new GridLayout();
		entryTable.setLayout(layout);

		Label label = new Label(entryTable, SWT.NONE);
		label.setText("Use this preference page to specify the default Double Click Action\n"
				+ "on the Spring Explorer and content restrictions for the Beans Graph.");

		Composite radioComposite = new Composite(entryTable, SWT.NONE);
		radioComposite.setLayout(new GridLayout());

		// Create a data that takes up the extra space in the dialog.
		radioComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite radioComposite2 = new Composite(radioComposite, SWT.NONE);
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		radioComposite2.setLayout(layout);
		radioComposite2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		radioEditor = new RadioGroupFieldEditor(
				BeansUIPlugin.DEFAULT_DOUBLE_CLICK_ACTION_PREFERENCE_ID,
				"Default Double Click Action", 1, new String[][] {
						{ "Open Configuration File", "true" }, { "Open Java Element", "false" } },
				radioComposite2, true);
		radioEditor.setPage(this);
		radioEditor.setPreferenceStore(getPreferenceStore());
		radioEditor.load();

		Composite graphComposite = new Composite(entryTable, SWT.NONE);
		graphComposite.setLayout(new GridLayout());

		graphComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Group graphEditorGroup = new Group(graphComposite, SWT.NONE);
		graphEditorGroup.setText("Beans Graph");
		layout = new GridLayout();
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		graphEditorGroup.setLayout(layout);
		graphEditorGroup.setLayoutData(gd);
		Composite graphComposite2 = new Composite(graphEditorGroup, SWT.NONE);
		layout.marginWidth = 3;
		layout.marginHeight = 3;
		graphComposite2.setLayout(layout);
		graphComposite2.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		graphEditorInnerBeans = new BooleanFieldEditor(
				BeansUIPlugin.SHOULD_SHOW_INNER_BEANS_PREFERENCE_ID, "Display inner beans",
				SWT.NONE, graphComposite2);
		graphEditorInnerBeans.setPage(this);
		graphEditorInnerBeans.setPreferenceStore(getPreferenceStore());
		graphEditorInnerBeans.load();
		graphEditorInfrastructureBeans = new BooleanFieldEditor(
				BeansUIPlugin.SHOULD_SHOW_INFRASTRUCTURE_BEANS_PREFERENCE_ID,
				"Display infrastructure beans", SWT.NONE, graphComposite2);
		graphEditorInfrastructureBeans.setPage(this);
		graphEditorInfrastructureBeans.setPreferenceStore(getPreferenceStore());
		graphEditorInfrastructureBeans.load();

		return entryTable;
	}

	public void init(IWorkbench workbench) {
		// Initialize the preference store we wish to use
		setPreferenceStore(BeansUIPlugin.getDefault().getPreferenceStore());
	}

	protected void performDefaults() {
		radioEditor.loadDefault();
		graphEditorInnerBeans.loadDefault();
		graphEditorInfrastructureBeans.loadDefault();
	}

	public boolean performOk() {
		radioEditor.store();
		graphEditorInnerBeans.store();
		graphEditorInfrastructureBeans.store();

		return super.performOk();
	}

}