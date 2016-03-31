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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CONTENT_DESCRIPTION;
import static com.android.SdkConstants.ATTR_HINT;
import static com.android.SdkConstants.ATTR_LABEL;
import static com.android.SdkConstants.ATTR_PROMPT;
import static com.android.SdkConstants.ATTR_TEXT;
import static com.android.SdkConstants.ATTR_TITLE;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;

import java.util.Arrays;
import java.util.Collection;

/**
 * Check which looks at the children of ScrollViews and ensures that they fill/match
 * the parent width instead of setting wrap_content.
 */
public class HardcodedValuesDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "HardcodedText", //$NON-NLS-1$
            "Hardcoded text",

            "Hardcoding text attributes directly in layout files is bad for several reasons:\n" +
            "\n" +
            "* When creating configuration variations (for example for landscape or portrait)" +
            "you have to repeat the actual text (and keep it up to date when making changes)\n" +
            "\n" +
            "* The application cannot be translated to other languages by just adding new " +
            "translations for existing string resources.\n" +
            "\n" +
            "In Android Studio and Eclipse there are quickfixes to automatically extract this " +
            "hardcoded string into a resource lookup.",

            Category.I18N,
            5,
            Severity.WARNING,
            new Implementation(
                    HardcodedValuesDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    // TODO: Add additional issues here, such as hardcoded colors, hardcoded sizes, etc

    /** Constructs a new {@link HardcodedValuesDetector} */
    public HardcodedValuesDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Arrays.asList(
                // Layouts
                ATTR_TEXT,
                ATTR_CONTENT_DESCRIPTION,
                ATTR_HINT,
                ATTR_LABEL,
                ATTR_PROMPT,

                // Menus
                ATTR_TITLE
        );
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.MENU;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String value = attribute.getValue();
        if (!value.isEmpty() && (value.charAt(0) != '@' && value.charAt(0) != '?')) {
            // Make sure this is really one of the android: attributes
            if (!ANDROID_URI.equals(attribute.getNamespaceURI())) {
                return;
            }

            context.report(ISSUE, attribute, context.getLocation(attribute),
                String.format("[I18N] Hardcoded string \"%1$s\", should use `@string` resource",
                              value));
        }
    }
}
