/*
 * Copyright (C) 2012 The Android Open Source Project
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
import static com.android.SdkConstants.ATTR_TITLE;
import static com.android.SdkConstants.ATTR_VISIBLE;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.VALUE_FALSE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;
import java.util.Collections;

/**
 * Check which makes sure menu items specify a title
 */
public class TitleDetector extends ResourceXmlDetector implements JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "MenuTitle", //$NON-NLS-1$
            "Missing menu title",

            "From the action bar documentation:\n" +
            // u2014: em dash
            "\"It's important that you always define android:title for each menu item \u2014 " +
            "even if you don't declare that the title appear with the action item \u2014 for " +
            "three reasons:\n" +
            "\n" +
            "* If there's not enough room in the action bar for the action item, the menu " +
            "item appears in the overflow menu and only the title appears.\n" +
            "* Screen readers for sight-impaired users read the menu item's title.\n" +
            "* If the action item appears with only the icon, a user can long-press the item " +
            "to reveal a tool-tip that displays the action item's title.\n" +
            "The android:icon is always optional, but recommended.",

            Category.USABILITY,
            5,
            Severity.ERROR,
            new Implementation(
                    TitleDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo(
            "http://developer.android.com/guide/topics/ui/actionbar.html"); //$NON-NLS-1$

    /** Constructs a new {@link TitleDetector} */
    public TitleDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.MENU;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(TAG_ITEM);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (element.hasAttributeNS(ANDROID_URI, ATTR_TITLE)) {
            return;
        }

        // TODO: Find out if this is necessary on older versions too.
        // I swear I saw it mentioned.
        if (context.getMainProject().getTargetSdk() < 11) {
            return;
        }

        if (VALUE_FALSE.equals(element.getAttributeNS(ANDROID_URI, ATTR_VISIBLE))) {
            return;
        }

        String message = "Menu items should specify a `title`";
        context.report(ISSUE, element, context.getLocation(element), message);
    }
}
