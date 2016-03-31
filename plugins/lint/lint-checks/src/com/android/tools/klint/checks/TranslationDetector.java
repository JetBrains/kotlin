/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ATTR_LOCALE;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TRANSLATABLE;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.STRING_PREFIX;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STRING;
import static com.android.SdkConstants.TAG_STRING_ARRAY;
import static com.android.SdkConstants.TOOLS_URI;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.builder.model.AndroidProject;
import com.android.builder.model.ProductFlavor;
import com.android.builder.model.ProductFlavorContainer;
import com.android.builder.model.Variant;
import com.android.ide.common.resources.LocaleManager;
import com.android.ide.common.resources.configuration.FolderConfiguration;
import com.android.ide.common.resources.configuration.LocaleQualifier;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Checks for incomplete translations - e.g. keys that are only present in some
 * locales but not all.
 */
public class TranslationDetector extends ResourceXmlDetector {
    @VisibleForTesting
    static boolean sCompleteRegions =
            System.getenv("ANDROID_LINT_COMPLETE_REGIONS") != null; //$NON-NLS-1$

    private static final Implementation IMPLEMENTATION = new Implementation(
            TranslationDetector.class,
            Scope.ALL_RESOURCES_SCOPE);

    /** Are all translations complete? */
    public static final Issue MISSING = Issue.create(
            "MissingTranslation", //$NON-NLS-1$
            "Incomplete translation",
            "If an application has more than one locale, then all the strings declared in " +
            "one language should also be translated in all other languages.\n" +
            "\n" +
            "If the string should *not* be translated, you can add the attribute " +
            "`translatable=\"false\"` on the `<string>` element, or you can define all " +
            "your non-translatable strings in a resource file called `donottranslate.xml`. " +
            "Or, you can ignore the issue with a `tools:ignore=\"MissingTranslation\"` " +
            "attribute.\n" +
            "\n" +
            "By default this detector allows regions of a language to just provide a " +
            "subset of the strings and fall back to the standard language strings. " +
            "You can require all regions to provide a full translation by setting the " +
            "environment variable `ANDROID_LINT_COMPLETE_REGIONS`.\n" +
            "\n" +
            "You can tell lint (and other tools) which language is the default language " +
            "in your `res/values/` folder by specifying `tools:locale=\"languageCode\"` for " +
            "the root `<resources>` element in your resource file. (The `tools` prefix refers " +
            "to the namespace declaration `http://schemas.android.com/tools`.)",
            Category.MESSAGES,
            8,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Are there extra translations that are "unused" (appear only in specific languages) ? */
    public static final Issue EXTRA = Issue.create(
            "ExtraTranslation", //$NON-NLS-1$
            "Extra translation",
            "If a string appears in a specific language translation file, but there is " +
            "no corresponding string in the default locale, then this string is probably " +
            "unused. (It's technically possible that your application is only intended to " +
            "run in a specific locale, but it's still a good idea to provide a fallback.).\n" +
            "\n" +
            "Note that these strings can lead to crashes if the string is looked up on any " +
            "locale not providing a translation, so it's important to clean them up.",
            Category.MESSAGES,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    private Set<String> mNames;
    private Set<String> mTranslatedArrays;
    private Set<String> mNonTranslatable;
    private boolean mIgnoreFile;
    private Map<File, Set<String>> mFileToNames;
    private Map<File, String> mFileToLocale;

    /** Locations for each untranslated string name. Populated during phase 2, if necessary */
    private Map<String, Location> mMissingLocations;

    /** Locations for each extra translated string name. Populated during phase 2, if necessary */
    private Map<String, Location> mExtraLocations;

    /** Error messages for each untranslated string name. Populated during phase 2, if necessary */
    private Map<String, String> mDescriptions;

    /** Constructs a new {@link TranslationDetector} */
    public TranslationDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_STRING,
                TAG_STRING_ARRAY
        );
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (context.getDriver().getPhase() == 1) {
            mFileToNames = new HashMap<File, Set<String>>();
        }
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        if (context.getPhase() == 1) {
            mNames = new HashSet<String>();
        }

        // Convention seen in various projects
        mIgnoreFile = context.file.getName().startsWith("donottranslate") //$NON-NLS-1$
                        || UnusedResourceDetector.isAnalyticsFile(context);

        if (!context.getProject().getReportIssues()) {
            mIgnoreFile = true;
        }
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (context.getPhase() == 1) {
            // Store this layout's set of ids for full project analysis in afterCheckProject
            if (context.getProject().getReportIssues() && mNames != null && !mNames.isEmpty()) {
                mFileToNames.put(context.file, mNames);

                Element root = ((XmlContext) context).document.getDocumentElement();
                if (root != null) {
                    String locale = root.getAttributeNS(TOOLS_URI, ATTR_LOCALE);
                    if (locale != null && !locale.isEmpty()) {
                        if (mFileToLocale == null) {
                            mFileToLocale = Maps.newHashMap();
                        }
                        mFileToLocale.put(context.file, locale);
                    }
                    // Add in English here if not specified? Worry about false positives listing "en" explicitly
                }
            }

            mNames = null;
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            // NOTE - this will look for the presence of translation strings.
            // If you create a resource folder but don't actually place a file in it
            // we won't detect that, but it seems like a smaller problem.

            checkTranslations(context);

            mFileToNames = null;

            if (mMissingLocations != null || mExtraLocations != null) {
                context.getDriver().requestRepeat(this, Scope.ALL_RESOURCES_SCOPE);
            }
        } else {
            assert context.getPhase() == 2;

            reportMap(context, MISSING, mMissingLocations);
            reportMap(context, EXTRA, mExtraLocations);
            mMissingLocations = null;
            mExtraLocations = null;
            mDescriptions = null;
        }
    }

    private void reportMap(Context context, Issue issue, Map<String, Location> map) {
        if (map != null) {
            for (Map.Entry<String, Location> entry : map.entrySet()) {
                Location location = entry.getValue();
                String name = entry.getKey();
                String message = mDescriptions.get(name);

                if (location == null) {
                    location = Location.create(context.getProject().getDir());
                }

                // We were prepending locations, but we want to prefer the base folders
                location = Location.reverse(location);

                context.report(issue, location, message);
            }
        }
    }

    private void checkTranslations(Context context) {
        // Only one file defining strings? If so, no problems.
        Set<File> files = mFileToNames.keySet();
        Set<File> parentFolders = new HashSet<File>();
        for (File file : files) {
            parentFolders.add(file.getParentFile());
        }
        if (parentFolders.size() == 1
                && FD_RES_VALUES.equals(parentFolders.iterator().next().getName())) {
            // Only one language - no problems.
            return;
        }

        boolean reportMissing = context.isEnabled(MISSING);
        boolean reportExtra = context.isEnabled(EXTRA);

        // res/strings.xml etc
        String defaultLanguage = "Default";

        Map<File, String> parentFolderToLanguage = new HashMap<File, String>();
        for (File parent : parentFolders) {
            String name = parent.getName();

            // Look up the language for this folder.
            String language = getLanguageTag(name);
            if (language == null) {
                language = defaultLanguage;
            }

            parentFolderToLanguage.put(parent, language);
        }

        int languageCount = parentFolderToLanguage.values().size();
        if (languageCount == 0 || languageCount == 1 && defaultLanguage.equals(
                parentFolderToLanguage.values().iterator().next())) {
            // At most one language -- no problems.
            return;
        }

        // Merge together the various files building up the translations for each language
        Map<String, Set<String>> languageToStrings =
                new HashMap<String, Set<String>>(languageCount);
        Set<String> allStrings = new HashSet<String>(200);
        for (File file : files) {
            String language = null;
            if (mFileToLocale != null) {
                String locale = mFileToLocale.get(file);
                if (locale != null) {
                    int index = locale.indexOf('-');
                    if (index != -1) {
                        locale = locale.substring(0, index);
                    }
                    language = locale;
                }
            }
            if (language == null) {
                language = parentFolderToLanguage.get(file.getParentFile());
            }
            assert language != null : file.getParent();
            Set<String> fileStrings = mFileToNames.get(file);

            Set<String> languageStrings = languageToStrings.get(language);
            if (languageStrings == null) {
                // We don't need a copy; we're done with the string tables now so we
                // can modify them
                languageToStrings.put(language, fileStrings);
            } else {
                languageStrings.addAll(fileStrings);
            }
            allStrings.addAll(fileStrings);
        }

        Set<String> defaultStrings = languageToStrings.get(defaultLanguage);
        if (defaultStrings == null) {
            defaultStrings = new HashSet<String>();
        }

        // See if it looks like the user has named a specific locale as the base language
        // (this impacts whether we report strings as "extra" or "missing")
        if (mFileToLocale != null) {
            Set<String> specifiedLocales = Sets.newHashSet();
            for (Map.Entry<File, String> entry : mFileToLocale.entrySet()) {
                String locale = entry.getValue();
                int index = locale.indexOf('-');
                if (index != -1) {
                    locale = locale.substring(0, index);
                }
                specifiedLocales.add(locale);
            }
            if (specifiedLocales.size() == 1) {
                String first = specifiedLocales.iterator().next();
                Set<String> languageStrings = languageToStrings.get(first);
                assert languageStrings != null;
                defaultStrings.addAll(languageStrings);
            }
        }

        int stringCount = allStrings.size();

        // Treat English is the default language if not explicitly specified
        if (!sCompleteRegions && !languageToStrings.containsKey("en")
                && mFileToLocale == null) {  //$NON-NLS-1$
            // But only if we have an actual region
            for (String l : languageToStrings.keySet()) {
                if (l.startsWith("en-")) {  //$NON-NLS-1$
                    languageToStrings.put("en", defaultStrings); //$NON-NLS-1$
                    break;
                }
            }
        }

        List<String> resConfigLanguages = getResConfigLanguages(context.getMainProject());
        if (resConfigLanguages != null) {
            List<String> keys = Lists.newArrayList(languageToStrings.keySet());
            for (String locale : keys) {
                if (defaultLanguage.equals(locale)) {
                    continue;
                }
                String language = locale;
                int index = language.indexOf('-');
                if (index != -1) {
                    // Strip off region
                    language = language.substring(0, index);
                }
                if (!resConfigLanguages.contains(language)) {
                    languageToStrings.remove(locale);
                }
            }
        }

        // Do we need to resolve fallback strings for regions that only define a subset
        // of the strings in the language and fall back on the main language for the rest?
        if (!sCompleteRegions) {
            for (String l : languageToStrings.keySet()) {
                if (l.indexOf('-') != -1) {
                    // Yes, we have regions. Merge all base language string names into each region.
                    for (Map.Entry<String, Set<String>> entry : languageToStrings.entrySet()) {
                        Set<String> strings = entry.getValue();
                        if (stringCount != strings.size()) {
                            String languageRegion = entry.getKey();
                            int regionIndex = languageRegion.indexOf('-');
                            if (regionIndex != -1) {
                                String language = languageRegion.substring(0, regionIndex);
                                Set<String> fallback = languageToStrings.get(language);
                                if (fallback != null) {
                                    strings.addAll(fallback);
                                }
                            }
                        }
                    }
                    // We only need to do this once; when we see the first region we know
                    // we need to do it; once merged we can bail
                    break;
                }
            }
        }

        // Fast check to see if there's no problem: if the default locale set is the
        // same as the all set (meaning there are no extra strings in the other languages)
        // then we can quickly determine if everything is okay by just making sure that
        // each language defines everything. If that's the case they will all have the same
        // string count.
        if (stringCount == defaultStrings.size()) {
            boolean haveError = false;
            for (Map.Entry<String, Set<String>> entry : languageToStrings.entrySet()) {
                Set<String> strings = entry.getValue();
                if (stringCount != strings.size()) {
                    haveError = true;
                    break;
                }
            }
            if (!haveError) {
                return;
            }
        }

        List<String> languages = new ArrayList<String>(languageToStrings.keySet());
        Collections.sort(languages);
        for (String language : languages) {
            Set<String> strings = languageToStrings.get(language);
            if (defaultLanguage.equals(language)) {
                continue;
            }

            // if strings.size() == stringCount, then this language is defining everything,
            // both all the default language strings and the union of all extra strings
            // defined in other languages, so there's no problem.
            if (stringCount != strings.size()) {
                if (reportMissing) {
                    Set<String> difference = Sets.difference(defaultStrings, strings);
                    if (!difference.isEmpty()) {
                        if (mMissingLocations == null) {
                            mMissingLocations = new HashMap<String, Location>();
                        }
                        if (mDescriptions == null) {
                            mDescriptions = new HashMap<String, String>();
                        }

                        for (String s : difference) {
                            mMissingLocations.put(s, null);
                            String message = mDescriptions.get(s);
                            if (message == null) {
                                message = String.format("\"`%1$s`\" is not translated in %2$s",
                                        s, getLanguageDescription(language));
                            } else {
                                message = message + ", " + getLanguageDescription(language);
                            }
                            mDescriptions.put(s, message);
                        }
                    }
                }
            }

            if (stringCount != defaultStrings.size()) {
                if (reportExtra) {
                    Set<String> difference = Sets.difference(strings, defaultStrings);
                    if (!difference.isEmpty()) {
                        if (mExtraLocations == null) {
                            mExtraLocations = new HashMap<String, Location>();
                        }
                        if (mDescriptions == null) {
                            mDescriptions = new HashMap<String, String>();
                        }

                        for (String s : difference) {
                            if (mTranslatedArrays != null && mTranslatedArrays.contains(s)) {
                                continue;
                            }
                            if (mNonTranslatable != null && mNonTranslatable.contains(s)) {
                                continue;
                            }

                            mExtraLocations.put(s, null);
                            String message = String.format(
                                "\"`%1$s`\" is translated here but not found in default locale", s);
                            mDescriptions.put(s, message);
                        }
                    }
                }
            }
        }
    }

    public static String getLanguageDescription(@NonNull String locale) {
        int index = locale.indexOf('-');
        String regionCode = null;
        String languageCode = locale;
        if (index != -1) {
            regionCode = locale.substring(index + 1).toUpperCase(Locale.US);
            languageCode = locale.substring(0, index).toLowerCase(Locale.US);
        }

        String languageName = LocaleManager.getLanguageName(languageCode);
        if (languageName != null) {
            if (regionCode != null) {
                String regionName = LocaleManager.getRegionName(regionCode);
                if (regionName != null) {
                    languageName = languageName + ": " + regionName;
                }
            }

            return String.format("\"%1$s\" (%2$s)", locale, languageName);
        } else {
            return '"' + locale + '"';
        }
    }


    /** Look up the language for the given folder name */
    private static String getLanguageTag(String name) {
        if (FD_RES_VALUES.equals(name)) {
            return null;
        }

        FolderConfiguration configuration = FolderConfiguration.getConfigForFolder(name);
        if (configuration != null) {
          LocaleQualifier locale = configuration.getLocaleQualifier();
          if (locale != null && !locale.hasFakeValue()) {
              return locale.getTag();
          }
        }

        return null;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (mIgnoreFile) {
            return;
        }

        Attr attribute = element.getAttributeNode(ATTR_NAME);

        if (context.getPhase() == 2) {
            // Just locating names requested in the {@link #mLocations} map
            if (attribute == null) {
                return;
            }
            String name = attribute.getValue();
            if (mMissingLocations != null && mMissingLocations.containsKey(name)) {
                String language = getLanguageTag(context.file.getParentFile().getName());
                if (language == null) {
                    if (context.getDriver().isSuppressed(context, MISSING, element)) {
                        mMissingLocations.remove(name);
                        return;
                    }

                    Location location = context.getLocation(attribute);
                    location.setClientData(element);
                    location.setSecondary(mMissingLocations.get(name));
                    mMissingLocations.put(name, location);
                }
            }
            if (mExtraLocations != null && mExtraLocations.containsKey(name)) {
                String language = getLanguageTag(context.file.getParentFile().getName());
                if (language != null) {
                    if (context.getDriver().isSuppressed(context, EXTRA, element)) {
                        mExtraLocations.remove(name);
                        return;
                    }
                    Location location = context.getLocation(attribute);
                    location.setClientData(element);
                    location.setMessage("Also translated here");
                    location.setSecondary(mExtraLocations.get(name));
                    mExtraLocations.put(name, location);
                }
            }
            return;
        }

        assert context.getPhase() == 1;
        if (attribute == null || attribute.getValue().isEmpty()) {
            context.report(MISSING, element, context.getLocation(element),
                    "Missing `name` attribute in `<string>` declaration");
        } else {
            String name = attribute.getValue();

            Attr translatable = element.getAttributeNode(ATTR_TRANSLATABLE);
            if (translatable != null && !Boolean.valueOf(translatable.getValue())) {
                String l = LintUtils.getLocaleAndRegion(context.file.getParentFile().getName());
                //noinspection VariableNotUsedInsideIf
                if (l != null) {
                    context.report(EXTRA, translatable, context.getLocation(translatable),
                        "Non-translatable resources should only be defined in the base " +
                        "`values/` folder");
                } else {
                    if (mNonTranslatable == null) {
                        mNonTranslatable = new HashSet<String>();
                    }
                    mNonTranslatable.add(name);
                }
                return;
            } else if (name.equals("google_maps_key")                  //$NON-NLS-1$
                    || name.equals("google_maps_key_instructions")) {  //$NON-NLS-1$
                // Older versions of the templates shipped with these not marked as
                // non-translatable; don't flag them
                if (mNonTranslatable == null) {
                    mNonTranslatable = new HashSet<String>();
                }
                mNonTranslatable.add(name);
                return;
            }

            if (element.getTagName().equals(TAG_STRING_ARRAY) &&
                    allItemsAreReferences(element)) {
                // No need to provide translations for string arrays where all
                // the children items are defined as translated string resources,
                // e.g.
                //    <string-array name="foo">
                //       <item>@string/item1</item>
                //       <item>@string/item2</item>
                //    </string-array>
                // However, we need to remember these names such that we don't consider
                // these arrays "extra" if one of the *translated* versions of the array
                // perform an inline translation of an array item
                if (mTranslatedArrays == null) {
                    mTranslatedArrays = new HashSet<String>();
                }
                mTranslatedArrays.add(name);
                return;
            }

            // Check for duplicate name definitions? No, because there can be
            // additional customizations like product=
            //if (mNames.contains(name)) {
            //    context.mClient.report(ISSUE, context.getLocation(attribute),
            //        String.format("Duplicate name %1$s, already defined earlier in this file",
            //            name));
            //}

            mNames.add(name);

            if (mNonTranslatable != null && mNonTranslatable.contains(name)) {
                String message = String.format("The resource string \"`%1$s`\" has been marked as " +
                        "`translatable=\"false\"`", name);
                context.report(EXTRA, attribute, context.getLocation(attribute), message);
            }

            // TBD: Also make sure that the strings are not empty or placeholders?
        }
    }

    private static boolean allItemsAreReferences(Element element) {
        assert element.getTagName().equals(TAG_STRING_ARRAY);
        NodeList childNodes = element.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node item = childNodes.item(i);
            if (item.getNodeType() == Node.ELEMENT_NODE &&
                    TAG_ITEM.equals(item.getNodeName())) {
                NodeList itemChildren = item.getChildNodes();
                for (int j = 0, m = itemChildren.getLength(); j < m; j++) {
                    Node valueNode = itemChildren.item(j);
                    if (valueNode.getNodeType() == Node.TEXT_NODE) {
                        String value = valueNode.getNodeValue().trim();
                        if (!value.startsWith(ANDROID_PREFIX)
                                && !value.startsWith(STRING_PREFIX)) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    @Nullable
    private static List<String> getResConfigLanguages(@NonNull Project project) {
        if (project.isGradleProject() && project.getGradleProjectModel() != null &&
                project.getCurrentVariant() != null) {
            Set<String> relevantDensities = Sets.newHashSet();
            Variant variant = project.getCurrentVariant();
            List<String> variantFlavors = variant.getProductFlavors();
            AndroidProject gradleProjectModel = project.getGradleProjectModel();

            addResConfigsFromFlavor(relevantDensities, null,
                    project.getGradleProjectModel().getDefaultConfig());
            for (ProductFlavorContainer container : gradleProjectModel.getProductFlavors()) {
                addResConfigsFromFlavor(relevantDensities, variantFlavors, container);
            }
            if (!relevantDensities.isEmpty()) {
                ArrayList<String> strings = Lists.newArrayList(relevantDensities);
                Collections.sort(strings);
                return strings;
            }
        }

        return null;
    }

    /**
     * Adds in the resConfig values specified by the given flavor container, assuming
     * it's in one of the relevant variantFlavors, into the given set
     */
    private static void addResConfigsFromFlavor(@NonNull Set<String> relevantLanguages,
            @Nullable List<String> variantFlavors,
            @NonNull ProductFlavorContainer container) {
        ProductFlavor flavor = container.getProductFlavor();
        if (variantFlavors == null || variantFlavors.contains(flavor.getName())) {
            if (!flavor.getResourceConfigurations().isEmpty()) {
                for (String resConfig : flavor.getResourceConfigurations()) {
                    // Look for languages; these are of length 2. (ResConfigs
                    // can also refer to densities, etc.)
                    if (resConfig.length() == 2) {
                        relevantLanguages.add(resConfig);
                    }
                }
            }
        }
    }
}
