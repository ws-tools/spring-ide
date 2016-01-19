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
package org.springframework.ide.eclipse.boot.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.Assert;
import org.springframework.ide.eclipse.boot.util.StringUtil;
import org.springframework.ide.eclipse.core.StringUtils;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.CheckBoxesSection.CheckBoxModel;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.HierarchicalMultiSelectionFieldModel;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.MultiSelectionFieldModel;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.NewSpringBootWizardModel;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.PopularityTracker;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.RadioGroup;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.RadioGroups;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.RadioInfo;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.json.InitializrServiceSpec;
import org.springframework.ide.eclipse.wizard.gettingstarted.boot.json.InitializrServiceSpec.Dependency;
import org.springframework.ide.eclipse.wizard.gettingstarted.content.BuildType;
import org.springframework.ide.eclipse.wizard.gettingstarted.importing.ImportStrategy;
import org.springsource.ide.eclipse.commons.frameworks.core.downloadmanager.URLConnectionFactory;
import org.springsource.ide.eclipse.commons.livexp.core.FieldModel;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveVariable;
import org.springsource.ide.eclipse.commons.livexp.util.Filter;
import org.springsource.ide.eclipse.commons.livexp.util.Filters;

import junit.framework.TestCase;

/**
 * Tests whether NewSpringBootWizardModel adequately parses initializer form data.
 *
 * @author Kris De Volder
 */
public class NewSpringBootWizardModelTest extends TestCase {

	//private static final String INITIALIZR_JSON = "initializr.json";
	private static final String INITIALIZR_JSON = "initializr-v2.1.json";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	public static NewSpringBootWizardModel parseFrom(String resourcePath, IPreferenceStore store) throws Exception {
		URL formUrl = resourceUrl(resourcePath);
		return new NewSpringBootWizardModel(new URLConnectionFactory(), formUrl.toString(), store);
	}

	public static NewSpringBootWizardModel parseFrom(String resourcePath) throws Exception {
		return parseFrom(resourcePath, new MockPrefsStore());
	}

	public static URL resourceUrl(String resourcePath) {
		URL formUrl = NewSpringBootWizardModelTest.class.getResource(resourcePath);
		return formUrl;
	}

	public void testParsedRadios() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		RadioGroups radioGroups = model.getRadioGroups();
		assertGroupNames(radioGroups, "type", "packaging", "javaVersion", "language", "bootVersion");
	}

	public void testPackagingRadios() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		RadioGroup packagingTypes = model.getRadioGroups().getGroup("packaging");
		assertNotNull(packagingTypes);
		assertGroupValues(packagingTypes, "jar", "war");
		assertEquals("jar", packagingTypes.getDefaultValue().getValue());
	}

	public void testJavaVersionRadios() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		RadioGroup group = model.getRadioGroups().getGroup("javaVersion");
		assertNotNull(group);
		assertGroupValues(group, "1.6", "1.7", "1.8");
		assertEquals("1.8", group.getDefaultValue().getValue());
	}

	public void testBuildTypeRadios() throws Exception {
		String jsonFile = INITIALIZR_JSON;
		NewSpringBootWizardModel model = parseFrom(jsonFile);
		String starterZipUrl = resourceUrl(jsonFile).toURI().resolve("/starter.zip").toString();
		assertEquals(starterZipUrl, model.baseUrl.getValue());

		RadioGroup group = model.getRadioGroups().getGroup("type");
		assertNotNull(group);
		assertGroupValues(group, "MAVEN", "GRADLE-STS", "GRADLE-Buildship");
		assertEquals("MAVEN", group.getDefaultValue().getValue());

		group.getSelection().selection.setValue(group.getRadio("MAVEN"));
		assertEquals(BuildType.MAVEN, model.getBuildType());
		assertEquals(starterZipUrl, model.baseUrl.getValue());

		for (ImportStrategy gradleStrategy : BuildType.GRADLE.getImportStrategies()) {
			group.getSelection().selection.setValue(group.getRadio(gradleStrategy.getId()));
			assertEquals(BuildType.GRADLE, model.getBuildType());
			assertEquals(gradleStrategy, model.getImportStrategy());
			assertEquals(starterZipUrl, model.baseUrl.getValue());
		}
	}

	public void testBuildTypeRadiosVariant() throws Exception {
		//Hypothetical variant where the json "types" lists different actions for maven and gradle zip

		String jsonFile = "initializr-variant.json";

		String mavenZipUrl = resourceUrl(jsonFile).toURI().resolve("/maven.zip").toString();
		String gradleZipUrl = resourceUrl(jsonFile).toURI().resolve("/gradle.zip").toString();

		NewSpringBootWizardModel model = parseFrom(jsonFile);

		RadioGroup group = model.getRadioGroups().getGroup("type");
		assertNotNull(group);
		assertGroupValues(group, "MAVEN", "GRADLE-STS", "GRADLE-Buildship");
		assertEquals("MAVEN", group.getDefaultValue().getValue());
		assertEquals(mavenZipUrl, model.baseUrl.getValue());

		for (ImportStrategy gradleStrat : BuildType.GRADLE.getImportStrategies()) {
			group.getSelection().selection.setValue(group.getRadio(gradleStrat.getId()));
			assertEquals(BuildType.GRADLE, model.getBuildType());
			assertEquals(gradleStrat, model.getImportStrategy());
			assertEquals(gradleZipUrl, model.baseUrl.getValue());
		}

		group.getSelection().selection.setValue(group.getRadio("MAVEN"));
		assertEquals(BuildType.MAVEN, model.getBuildType());
		assertEquals(mavenZipUrl, model.baseUrl.getValue());
	}

	public void testStarters() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);

		Collection<Dependency> styles = getAllChoices(model.dependencies);
		assertNotNull(styles);
		assertTrue(styles.size()>10);

		for (String catName : model.dependencies.getCategories()) {
			String lastLabel = null; //check that style labels are sorted within each category
			MultiSelectionFieldModel<Dependency> cat = model.dependencies.getContents(catName);
			for (Dependency choice : cat.getChoices()) {
				String label = cat.getLabel(choice);
				if (lastLabel!=null) {
					assertTrue("Labels not sorted: '"+lastLabel+"' > '"+label+"'", lastLabel.compareTo(label)<0);
				}
				lastLabel = label;
				assertNotNull("No tooltip for: "+choice+" ["+label+"]", cat.getTooltip(choice));
			}
		}
	}

	private <T> Collection<T> getAllChoices(
			HierarchicalMultiSelectionFieldModel<T> dependencies) {
		ArrayList<T> choices = new ArrayList<T>();
		for (String catName : dependencies.getCategories()) {
			MultiSelectionFieldModel<T> cat = dependencies.getContents(catName);
			choices.addAll(Arrays.asList(cat.getChoices()));
		}
		return choices;
	}

	public void testVersionRangesBuildSnaphotBug() throws Exception {
		//See https://www.pivotaltracker.com/story/show/100963226
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		Dependency devtools = getDependencyById(model, "devtools");

		RadioGroup bootVersion = model.getBootVersion();
		RadioInfo newer1 = bootVersion.getRadio("1.3.0.M2");
		RadioInfo newer2 = bootVersion.getRadio("1.3.0.BUILD-SNAPSHOT");
		RadioInfo older = bootVersion.getRadio("1.2.6.BUILD-SNAPSHOT");

		LiveExpression<Boolean> devtoolsEnabled  = getEnablement(model.dependencies, devtools);

		bootVersion.setValue(older);
		assertFalse(devtoolsEnabled.getValue());

		bootVersion.setValue(newer1);
		assertTrue(devtoolsEnabled.getValue());

		bootVersion.setValue(newer2);
		assertTrue(devtoolsEnabled.getValue());
	}

	public void testVersionRanges() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		Dependency bitronix = getDependencyById(model, "jta-bitronix");
		Dependency thymeleaf = getDependencyById(model, "thymeleaf");
		assertEquals("1.2.0.M1", bitronix.getVersionRange());
		assertFalse(StringUtils.hasText(thymeleaf.getVersionRange()));

		RadioGroup bootVersion = model.getBootVersion();
		assertNotNull(bootVersion);
		assertGroupValues(bootVersion,
		      "1.3.0.M2",
		      "1.3.0.BUILD-SNAPSHOT",
		      "1.2.6.BUILD-SNAPSHOT",
		      "1.2.5.RELEASE",
		      "1.1.12.RELEASE"
		);

		RadioInfo older = bootVersion.getRadio("1.1.12.RELEASE");
		RadioInfo newer = bootVersion.getRadio("1.2.5.RELEASE");

		LiveExpression<Boolean> bitronixEnabled  = getEnablement(model.dependencies, bitronix);
		LiveExpression<Boolean> thymeleafEnabled = getEnablement(model.dependencies, thymeleaf);

		bootVersion.setValue(older);
		assertFalse(bitronixEnabled.getValue());
		assertTrue(thymeleafEnabled.getValue());

		bootVersion.setValue(newer);
		assertTrue(bitronixEnabled.getValue());
		assertTrue(thymeleafEnabled.getValue());

		bootVersion.setValue(older);

		Set<Dependency> selectedDepedencies = getSelecteds(model.dependencies);
		assertTrue(selectedDepedencies.isEmpty());
		select(model.dependencies, bitronix);
		select(model.dependencies, thymeleaf);

		selectedDepedencies = getSelecteds(model.dependencies);
		assertEquals(2, selectedDepedencies.size());

		String url = model.downloadUrl.getValue();
		assertContains("thymeleaf", url);
		assertFalse(url.contains("bitronix"));
	}

	public void testPopularityTracking() throws Exception {
		IPreferenceStore store = new MockPrefsStore();
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON, store);
		assertTrue(model.getMostPopular(10).isEmpty());
		Dependency web = getDependencyById(model, "web");
		Dependency actuator = getDependencyById(model, "actuator");
		Dependency thymeleaf = getDependencyById(model, "thymeleaf");

		select(model.dependencies, web);
		select(model.dependencies, thymeleaf);
		select(model.dependencies, actuator);

		model.updateUsageCounts();

		PopularityTracker tracker = new PopularityTracker(store);
		assertEquals(1, tracker.getUsageCount(web));
		assertEquals(1, tracker.getUsageCount(thymeleaf));
		assertEquals(1, tracker.getUsageCount(actuator));

		model.updateUsageCounts();
		assertEquals(2, tracker.getUsageCount(web));
		assertEquals(2, tracker.getUsageCount(thymeleaf));
		assertEquals(2, tracker.getUsageCount(actuator));

		unselect(model.dependencies, actuator);
		model.updateUsageCounts();

		unselect(model.dependencies, thymeleaf);
		model.updateUsageCounts();

		assertEquals(4, tracker.getUsageCount(web));
		assertEquals(3, tracker.getUsageCount(thymeleaf));
		assertEquals(2, tracker.getUsageCount(actuator));

		assertCheckboxes(model.getMostPopular(10),
				web, thymeleaf, actuator);

		/////////////////////////////////////////////////////////////////////////////
		// check that model counts are limitted according to 'howMany' argument

		assertCheckboxes(model.getMostPopular(3),
				web, thymeleaf, actuator);
		assertCheckboxes(model.getMostPopular(2),
				web, thymeleaf);
		assertCheckboxes(model.getMostPopular(1),
				web);
		assertCheckboxes(model.getMostPopular(0)
				/*nothing*/);

	}


	public void testPopularCheckboxSharesSelectionState() throws Exception {
		IPreferenceStore store = new MockPrefsStore();
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON, store);
		assertTrue(model.getMostPopular(10).isEmpty());
		Dependency web = getDependencyById(model, "web");

		PopularityTracker tracker = new PopularityTracker(store);
		tracker.incrementUsageCount(web);

		String webCat = getCategory(model.dependencies, web);
		MultiSelectionFieldModel<Dependency> webGroup = model.dependencies.getContents(webCat);
		List<CheckBoxModel<Dependency>> allWebBoxes = webGroup.getCheckBoxModels();

		CheckBoxModel<Dependency> normalBox = getCheckboxById(allWebBoxes, web.getId());
		CheckBoxModel<Dependency> popularBox = getCheckboxById(model.getMostPopular(10), web.getId());

		assertFalse(normalBox.getSelection().getValue());
		assertFalse(popularBox.getSelection().getValue());

		normalBox.getSelection().setValue(true);
		assertTrue(normalBox.getSelection().getValue());
		assertTrue(popularBox.getSelection().getValue());

		popularBox.getSelection().setValue(false);
		assertFalse(normalBox.getSelection().getValue());
		assertFalse(popularBox.getSelection().getValue());
	}

	public void testDependencySearchBox() throws Exception {
		// The trickies bit of implementing the search box is, unfortunately, making the
		// SWT gui widgetry apply the filter and hide / show corresponding ui elements.
		// This unfortunately not tested here. This test only verifies the filter's
		// matching logic.

		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);

		LiveVariable<String> searchBox = model.getDependencyFilterBoxText();
		LiveExpression<Filter<CheckBoxModel<Dependency>>> filter = model.getDependencyFilter();

		assertEquals("", searchBox.getValue());
		assertEquals(Filters.acceptAll(),filter.getValue());

		assertFilterAccepts(model, true, "web", "web", "Web", "Full stack yada yada");
		assertFilterAccepts(model, true, "WeB", "web", "Web", "Full stack yada yada");
		assertFilterAccepts(model, false, "ZZZZZZZZZZ", "web", "Web", "Full stack yada yada");

		assertFilterAccepts(model, true, "foo", "something", "label FoO label", "desc");
		assertFilterAccepts(model, true, "foo", "something", "label", "desc FOO desc");
		assertFilterAccepts(model, false, "foo", "foo", "label", "desc");

	}

	public void testRestoreFromOldBuildtypePreference() throws Exception {
		//We here test a weak requirement that the wizard model 'handles' it okay when
		// the preference saved in the workspace is from before the buildship refactoring.
		//The requirements are:
		// - it doesn't crash
		// - it just chooses the maven build type / strategy by default. We do not
		//   attempt to map the old type-ids to the new ones.
		MockPrefsStore prefs = new MockPrefsStore();
		prefs.putValue("org.springframework.ide.eclipse.wizard.gettingstarted.boot.PreferredSelections.type", "gradle-project");

		NewSpringBootWizardModel wizard = parseFrom(INITIALIZR_JSON, prefs);
		assertEquals(BuildType.MAVEN, wizard.getBuildType());
		assertEquals(BuildType.MAVEN.getDefaultStrategy(), wizard.getImportStrategy());
	}

	private CheckBoxModel<Dependency> getCheckboxById(NewSpringBootWizardModel model, String id) {
		CheckBoxModel<Dependency> found = null;
		for (String cat : model.dependencies.getCategories()) {
			for (CheckBoxModel<Dependency> cb : model.dependencies.getContents(cat).getCheckBoxModels()) {
				if (cb.getValue().getId().equals(id)) {
					Assert.assertNull(found);
					found = cb;
				}
			}
		}
		return found;
	}

	private void assertFilterAccepts(NewSpringBootWizardModel model, boolean expect, String pattern,
			String id, String label, String desc) {
		LiveVariable<String> searchBox = model.getDependencyFilterBoxText();
		searchBox.setValue(pattern);
		Filter<CheckBoxModel<Dependency>> filter = model.getDependencyFilter().getValue();

		LiveVariable<Boolean> selection = new LiveVariable<Boolean>(false);
		LiveVariable<Boolean> enablement = new LiveVariable<Boolean>(true);

		Dependency dep = new Dependency();
		dep.setId(id);
		dep.setName(null);
		dep.setDescription(desc);
		CheckBoxModel<Dependency> cb = new CheckBoxModel<Dependency>(label, dep, selection, enablement);

		assertEquals(expect, filter.accept(cb));

		// No matter what, filter should allways accept checbox that has already been selected:
		selection.setValue(true);
		assertEquals(true, filter.accept(cb));

		// Check that enablement is ignored by filter (i.e. setting enablement false does not change
		// whether filter accepts the cb
		enablement.setValue(true);
		assertEquals(true, filter.accept(cb));
		selection.setValue(false);
		assertEquals(expect, filter.accept(cb));
	}

	private CheckBoxModel<Dependency> getCheckboxById(List<CheckBoxModel<Dependency>> list, String id) {
		for (CheckBoxModel<Dependency> cb : list) {
			if (id.equals(cb.getValue().getId())) {
				return cb;
			}
		}
		return null;
	}

	private void assertCheckboxes(List<CheckBoxModel<Dependency>> actuals, Dependency... expecteds) {
		StringBuilder expectedIds = new StringBuilder();
		for (Dependency e : expecteds) {
			expectedIds.append(e.getId()+"\n");
		}
		StringBuilder actualIds = new StringBuilder();
		for (CheckBoxModel<Dependency> cb : actuals) {
			actualIds.append(cb.getValue().getId()+"\n");
		}
		assertEquals(expectedIds.toString(), actualIds.toString());
	}

	private void unselect(HierarchicalMultiSelectionFieldModel<Dependency> dependencies,
			Dependency dep) {
			String cat = getCategory(dependencies, dep);
			MultiSelectionFieldModel<Dependency> selecteds = dependencies.getContents(cat);
			selecteds.unselect(dep);
	}

	private void select(HierarchicalMultiSelectionFieldModel<Dependency> dependencies,
			Dependency dep) {
		String cat = getCategory(dependencies, dep);
		MultiSelectionFieldModel<Dependency> selecteds = dependencies.getContents(cat);
		selecteds.select(dep);
	}

	private Set<Dependency> getSelecteds(HierarchicalMultiSelectionFieldModel<Dependency> dependencies) {
		HashSet<Dependency> selecteds = new HashSet<InitializrServiceSpec.Dependency>();
		for (String catName : dependencies.getCategories()) {
			MultiSelectionFieldModel<Dependency> cat = dependencies.getContents(catName);
			selecteds.addAll(cat.getCurrentSelection());
		}
		return selecteds;
	}

	private LiveExpression<Boolean> getEnablement(HierarchicalMultiSelectionFieldModel<Dependency> dependencies, Dependency dep) {
		String cat = getCategory(dependencies, dep);
		return dependencies.getContents(cat).getEnablement(dep);
	}

	private String getCategory( HierarchicalMultiSelectionFieldModel<Dependency> dependencies, Dependency dep) {
		for (String cat : dependencies.getCategories()) {
			if (contains(dependencies.getContents(cat).getChoices(), dep)) {
				return cat;
			}
		}
		throw new Error("Shouldn't get here");
	}

	private boolean contains(Dependency[] deps, Dependency find) {
		for (Dependency dep : deps) {
			if (find.equals(dep)) {
				return true;
			}
		}
		return false;
	}

	public void assertContains(String needle, String haystack) {
		if (haystack==null || !haystack.contains(needle)) {
			fail("Not found: "+needle+"\n in \n"+haystack);
		}
 	}

	private Dependency getDependencyById(NewSpringBootWizardModel model, String depId) {
		for (Dependency dep : getAllChoices(model.dependencies)) {
			if (dep.getId().equals(depId)) {
				return dep;
			}
		}
		return null;
	}

	public void testLabels() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		Iterator<FieldModel<String>> stringInputs = model.stringInputs.iterator();
		while (stringInputs.hasNext()) {
			FieldModel<String> input = stringInputs.next();
			assertRealLabel(input.getLabel());
		}

		for (RadioGroup group : model.getRadioGroups().getGroups()) {
			String label = group.getLabel();
			assertRealLabel(label);

			for (RadioInfo radio : group.getRadios()) {
				label = radio.getLabel();
				assertRealLabel(label);
			}
		}
	}

	public void testPrintLabels() throws Exception {
		//print all radios in groups with lable for quick visual inspection.
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		for (RadioGroup group : model.getRadioGroups().getGroups()) {
			String label = group.getLabel();
			System.out.println(group + " -> "+label);

			for (RadioInfo radio : group.getRadios()) {
				label = radio.getLabel();
				System.out.println("  " + radio + " -> "+label);
			}
		}

	}

	/**
	 * Basic test of the 'spec parser'. Just check that it accepts a well-formed
	 * json spec document and doesn't crash on it.
	 */
	public void testInitializrSpecParser() throws Exception {
		doParseTest(INITIALIZR_JSON);
	}

	private void doParseTest(String resource) throws IOException, Exception {
		URL url = NewSpringBootWizardModelTest.class.getResource(resource);
		URLConnection conn = new URLConnectionFactory().createConnection(url);
		conn.connect();
		InputStream input = conn.getInputStream();
		try {
			InitializrServiceSpec spec = InitializrServiceSpec.parseFrom(input);
			assertNotNull(spec);
		} finally {
			input.close();
		}
	}

	/**
	 * Test that radio params are wired up in the model so that selecting them changes the downloadUrl.
	 */
	public void testRadioQueryParams() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		RadioGroup packaging = model.getRadioGroups().getGroup("packaging");
		LiveVariable<RadioInfo> selection = packaging.getSelection().selection;

		assertEquals("jar", selection.getValue().getValue());
		String urlParam = getUrlParam(model.downloadUrl.getValue(), "packaging");
		assertEquals("jar", urlParam);

		selection.setValue(packaging.getRadio("war"));
		urlParam = getUrlParam(model.downloadUrl.getValue(), "packaging");
		assertEquals("war", urlParam);
	}

	/**
	 * Test that radio params for 'type' properly use the typenames from start.spring.io spec
	 * and not the ids used internally to distinguish between grade+buildship versus gradle+sts
	 */
	public void testTypeRadioQueryParams() throws Exception {
		NewSpringBootWizardModel model = parseFrom(INITIALIZR_JSON);
		RadioGroup type = model.getRadioGroups().getGroup("type");
		LiveVariable<RadioInfo> selection = type.getSelection().selection;

		assertEquals("MAVEN", selection.getValue().getValue());
		String urlParam = getUrlParam(model.downloadUrl.getValue(), "type");
		assertEquals("maven-project", urlParam);

		selection.setValue(type.getRadio("GRADLE-STS"));
		urlParam = getUrlParam(model.downloadUrl.getValue(), "type");
		assertEquals("gradle-project", urlParam);

		selection.setValue(type.getRadio("GRADLE-Buildship"));
		urlParam = getUrlParam(model.downloadUrl.getValue(), "type");
		assertEquals("gradle-project", urlParam);
	}

	public static Map<String, List<String>> getQueryParams(String url) throws Exception {
        Map<String, List<String>> params = new HashMap<String, List<String>>();
        String[] urlParts = url.split("\\?");
        if (urlParts.length > 1) {
            String query = urlParts[1];
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                String key = URLDecoder.decode(pair[0], "UTF-8");
                String value = "";
                if (pair.length > 1) {
                    value = URLDecoder.decode(pair[1], "UTF-8");
                }

                List<String> values = params.get(key);
                if (values == null) {
                    values = new ArrayList<String>();
                    params.put(key, values);
                }
                values.add(value);
            }
        }

        return params;
	}

	private String getUrlParam(String url, String name) throws Exception {
		Map<String, List<String>> params = getQueryParams(url);
		List<String> values = params.get(name);
		if (values!=null && !values.isEmpty()) {
			assertEquals(1, values.size());
			return values.get(0);
		}
		return null;
	}

	private void assertRealLabel(String label) {
		assertNotNull("Label is null", label); //have a label
		assertFalse("Label is empty", "".equals(label.trim())); //label not empty
		if (Character.isDigit(label.charAt(0))) {
			//labels like '1.6.' are okay too.
			return;
		}
		assertTrue("Label doesn't start with uppercase: '"+label+"'", Character.isUpperCase(label.charAt(0))); //'real' label, not just the default taken from the name.
	}

	private void assertGroupValues(RadioGroup group, String... expecteds) {
		Set<String> expectedSet = new HashSet<String>(Arrays.asList(expecteds));
		RadioInfo[] radios = group.getRadios();
		for (int i = 0; i < radios.length; i++) {
			String actual = radios[i].getValue();
			if (!expectedSet.contains(actual)) {
				fail("Unexpected: "+actual);
			}
			expectedSet.remove(actual);
		}
		if (!expectedSet.isEmpty()) {
			StringBuilder notFound = new StringBuilder();
			for (String missing : expectedSet) {
				notFound.append(" "+missing);
			}
			fail("Missing: "+notFound);
		}
	}

	private void assertGroupNames(RadioGroups radioGroups, String... expectNames) {
		List<RadioGroup> groups = radioGroups.getGroups();
		assertEquals(expectNames.length, groups.size());
		for (int i = 0; i < expectNames.length; i++) {
			assertEquals(expectNames[i], groups.get(i).getName());
		}
	}
}
