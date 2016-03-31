/*
 * Copyright (C) 2014 The Android Open Source Project
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
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.Collection;
import java.util.Collections;

/**
 * Checks for problems with include tags, such as providing layout parameters
 * without specifying both layout_width and layout_height
 */
public class IncludeDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "IncludeLayoutParam", //$NON-NLS-1$
            "Ignored layout params on include",

            "Layout parameters specified on an `<include>` tag will only be used if you " +
            "also override `layout_width` and `layout_height` on the `<include>` tag; " +
            "otherwise they will be ignored.",

            Category.CORRECTNESS,
            5,
            Severity.ERROR,
            new Implementation(
                    IncludeDetector.class,
                    Scope.RESOURCE_FILE_SCOPE)).addMoreInfo(
    "http://stackoverflow.com/questions/2631614/does-android-xml-layouts-include-tag-really-work");

    @Nullable
    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(VIEW_INCLUDE);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        NamedNodeMap attributes = element.getAttributes();
        int length = attributes.getLength();
        boolean hasWidth = false;
        boolean hasHeight = false;
        boolean hasOtherLayoutParam = false;
        for (int i = 0; i < length; i++) {
            Attr attribute = (Attr) attributes.item(i);
            String name = attribute.getLocalName();
            if (name != null && name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                if (ATTR_LAYOUT_WIDTH.equals(name)) {
                    hasWidth = true;
                } else if (ATTR_LAYOUT_HEIGHT.equals(name)) {
                    hasHeight = true;
                } else if (ANDROID_URI.equals(attribute.getNamespaceURI())) {
                    hasOtherLayoutParam = true;
                }
            }
        }

        boolean flagWidth = !hasOtherLayoutParam && hasWidth && !hasHeight;
        boolean flagHeight = !hasOtherLayoutParam && !hasWidth && hasHeight;

        if (hasOtherLayoutParam && (!hasWidth || !hasHeight) || flagWidth || flagHeight) {
            for (int i = 0; i < length; i++) {
                Attr attribute = (Attr) attributes.item(i);
                String name = attribute.getLocalName();
                if (name != null && name.startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                        && (!ATTR_LAYOUT_WIDTH.equals(name) || flagWidth)
                        && (!ATTR_LAYOUT_HEIGHT.equals(name) || flagHeight)
                        && ANDROID_URI.equals(attribute.getNamespaceURI())) {
                    String condition = !hasWidth && !hasHeight ?
                            "both `layout_width` and `layout_height` are also specified"
                            : !hasWidth ? "`layout_width` is also specified"
                                        : "`layout_height` is also specified";
                    String message = String.format(
                            "Layout parameter `%1$s` ignored unless %2$s on `<include>` tag",
                            name, condition);
                    context.report(ISSUE, element, context.getLocation(attribute),
                            message);
                }
            }
        }
    }

    /**
     * Returns true if the error message (earlier reported by this lint detector) requests
     * for the layout_width to be defined.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message computed by lint
     * @return true if the layout_width needs to be defined
     */
    public static boolean requestsWidth(@NonNull String errorMessage) {
        int index = errorMessage.indexOf(" unless ");
        if (index != -1) {
            return errorMessage.contains(ATTR_LAYOUT_WIDTH);
        }
        return false;
    }

    /**
     * Returns true if the error message (earlier reported by this lint detector) requests
     * for the layout_height to be defined.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message computed by lint
     * @return true if the layout_height needs to be defined
     */
    public static boolean requestsHeight(@NonNull String errorMessage) {
        int index = errorMessage.indexOf(" unless ");
        if (index != -1) {
            return errorMessage.contains(ATTR_LAYOUT_HEIGHT);
        }
        return false;
    }
}
