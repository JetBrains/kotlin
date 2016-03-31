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
import static com.android.SdkConstants.ATTR_IMPORTANT_FOR_ACCESSIBILITY;
import static com.android.SdkConstants.IMAGE_BUTTON;
import static com.android.SdkConstants.IMAGE_VIEW;
import static com.android.SdkConstants.VALUE_NO;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Check which looks for accessibility problems like missing content descriptions
 * <p>
 * TODO: Resolve styles and don't warn where styles are defining the content description
 * (though this seems unusual; content descriptions are not typically generic enough to
 * put in styles)
 */
public class AccessibilityDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ContentDescription", //$NON-NLS-1$
            "Image without `contentDescription`",
            "Non-textual widgets like ImageViews and ImageButtons should use the " +
            "`contentDescription` attribute to specify a textual description of " +
            "the widget such that screen readers and other accessibility tools " +
            "can adequately describe the user interface.\n" +
            "\n" +
            "Note that elements in application screens that are purely decorative " +
            "and do not provide any content or enable a user action should not " +
            "have accessibility content descriptions. In this case, just suppress the " +
            "lint warning with a tools:ignore=\"ContentDescription\" attribute.\n" +
            "\n" +
            "Note that for text fields, you should not set both the `hint` and the " +
            "`contentDescription` attributes since the hint will never be shown. Just " +
            "set the `hint`. See " +
            "http://developer.android.com/guide/topics/ui/accessibility/checklist.html#special-cases.",

            Category.A11Y,
            3,
            Severity.WARNING,
            new Implementation(
                    AccessibilityDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new {@link AccessibilityDetector} */
    public AccessibilityDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                IMAGE_BUTTON,
                IMAGE_VIEW
        );
    }

    @Override
    @Nullable
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_CONTENT_DESCRIPTION);
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        Element element = attribute.getOwnerElement();
        if (element.hasAttributeNS(ANDROID_URI, ATTR_HINT)) {
            context.report(ISSUE, element, context.getLocation(attribute),
                    "Do not set both `contentDescription` and `hint`: the `contentDescription` " +
                    "will mask the `hint`");
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (!element.hasAttributeNS(ANDROID_URI, ATTR_CONTENT_DESCRIPTION)) {
            // Ignore views that are explicitly not important for accessibility
            if (VALUE_NO.equals(element.getAttributeNS(ANDROID_URI,
                    ATTR_IMPORTANT_FOR_ACCESSIBILITY))) {
                return;
            }
            context.report(ISSUE, element, context.getLocation(element),
                    "[Accessibility] Missing `contentDescription` attribute on image");
        } else {
            Attr attributeNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_CONTENT_DESCRIPTION);
            String attribute = attributeNode.getValue();
            if (attribute.isEmpty() || attribute.equals("TODO")) { //$NON-NLS-1$
                context.report(ISSUE, attributeNode, context.getLocation(attributeNode),
                        "[Accessibility] Empty `contentDescription` attribute on image");
            }
        }
    }
}
