/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY;
import static com.android.xml.AndroidManifest.NODE_ACTIVITY_ALIAS;
import static com.android.xml.AndroidManifest.NODE_APPLICATION;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_COMPATIBLE_SCREENS;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_GRANT_URI_PERMISSION;
import static com.android.xml.AndroidManifest.NODE_INSTRUMENTATION;
import static com.android.xml.AndroidManifest.NODE_INTENT;
import static com.android.xml.AndroidManifest.NODE_MANIFEST;
import static com.android.xml.AndroidManifest.NODE_METADATA;
import static com.android.xml.AndroidManifest.NODE_PATH_PERMISSION;
import static com.android.xml.AndroidManifest.NODE_PERMISSION;
import static com.android.xml.AndroidManifest.NODE_PERMISSION_GROUP;
import static com.android.xml.AndroidManifest.NODE_PERMISSION_TREE;
import static com.android.xml.AndroidManifest.NODE_PROVIDER;
import static com.android.xml.AndroidManifest.NODE_RECEIVER;
import static com.android.xml.AndroidManifest.NODE_SERVICE;
import static com.android.xml.AndroidManifest.NODE_SUPPORTS_GL_TEXTURE;
import static com.android.xml.AndroidManifest.NODE_SUPPORTS_SCREENS;
import static com.android.xml.AndroidManifest.NODE_USES_CONFIGURATION;
import static com.android.xml.AndroidManifest.NODE_USES_FEATURE;
import static com.android.xml.AndroidManifest.NODE_USES_LIBRARY;
import static com.android.xml.AndroidManifest.NODE_USES_PERMISSION;
import static com.android.xml.AndroidManifest.NODE_USES_SDK;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.w3c.dom.Element;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Checks for typos in manifest files
 */
public class ManifestTypoDetector extends Detector implements Detector.XmlScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ManifestTypo", //$NON-NLS-1$
            "Typos in manifest tags",

            "This check looks through the manifest, and if it finds any tags " +
            "that look like likely misspellings, they are flagged.",
            Category.CORRECTNESS,
            5,
            Severity.FATAL,
            new Implementation(
                    ManifestTypoDetector.class,
                    Scope.MANIFEST_SCOPE));

    private static final Set<String> sValidTags;
    static {
        int expectedSize = 30;
        sValidTags = Sets.newHashSetWithExpectedSize(expectedSize);
        sValidTags.add(NODE_MANIFEST);
        sValidTags.add(NODE_APPLICATION);
        sValidTags.add(NODE_ACTIVITY);
        sValidTags.add(NODE_SERVICE);
        sValidTags.add(NODE_PROVIDER);
        sValidTags.add(NODE_RECEIVER);
        sValidTags.add(NODE_USES_FEATURE);
        sValidTags.add(NODE_USES_LIBRARY);
        sValidTags.add(NODE_USES_SDK);
        sValidTags.add(NODE_INSTRUMENTATION);
        sValidTags.add(NODE_USES_PERMISSION);
        sValidTags.add(NODE_PERMISSION);
        sValidTags.add(NODE_PERMISSION_TREE);
        sValidTags.add(NODE_PERMISSION_GROUP);
        sValidTags.add(NODE_USES_CONFIGURATION);
        sValidTags.add(NODE_ACTIVITY_ALIAS);
        sValidTags.add(NODE_INTENT);
        sValidTags.add(NODE_METADATA);
        sValidTags.add(NODE_ACTION);
        sValidTags.add(NODE_CATEGORY);
        sValidTags.add(NODE_DATA);
        sValidTags.add(NODE_GRANT_URI_PERMISSION);
        sValidTags.add(NODE_PATH_PERMISSION);
        sValidTags.add(NODE_SUPPORTS_SCREENS);
        sValidTags.add(NODE_COMPATIBLE_SCREENS);
        sValidTags.add(NODE_SUPPORTS_GL_TEXTURE);

        // Private tags
        sValidTags.add("eat-comment");          //$NON-NLS-1$
        sValidTags.add("original-package");     //$NON-NLS-1$
        sValidTags.add("protected-broadcast");  //$NON-NLS-1$
        sValidTags.add("adopt-permissions");    //$NON-NLS-1$

        assert sValidTags.size() <= expectedSize : sValidTags.size();
    }

    /** Constructs a new {@link ManifestTypoDetector} check */
    public ManifestTypoDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getName().equals(ANDROID_MANIFEST_XML);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return XmlScanner.ALL;
    }

    private static final int MAX_EDIT_DISTANCE = 3;

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        if (!sValidTags.contains(tag)) {
            int tagLength = tag.length();
            // Try to find the corresponding match
            List<String> suggestions = null;
            for (String suggestion : sValidTags) {
                if (Math.abs(suggestion.length() - tagLength) > MAX_EDIT_DISTANCE) {
                    continue;
                }
                if (LintUtils.editDistance(suggestion, tag) <= MAX_EDIT_DISTANCE) {
                    if (suggestions == null) {
                        suggestions = Lists.newArrayList();
                    }
                    suggestions.add('<' + suggestion + '>');
                }
            }
            if (suggestions != null) {
                assert !suggestions.isEmpty();
                String suggestionString;
                if (suggestions.size() == 1) {
                    suggestionString = suggestions.get(0);
                } else if (suggestions.size() == 2) {
                    suggestionString = String.format("%1$s or %2$s",
                            suggestions.get(0), suggestions.get(1));
                } else {
                    suggestionString = LintUtils.formatList(suggestions, -1);
                }
                String message = String.format("Misspelled tag `<%1$s>`: Did you mean `%2$s` ?",
                        tag, suggestionString);
                context.report(ISSUE, element, context.getLocation(element),
                        message);
            }
        }
    }
}
