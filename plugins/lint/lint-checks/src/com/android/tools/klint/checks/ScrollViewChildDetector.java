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
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.SCROLL_VIEW;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Check which looks at the children of ScrollViews and ensures that they fill/match
 * the parent width instead of setting wrap_content.
 */
public class ScrollViewChildDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ScrollViewSize", //$NON-NLS-1$
            "ScrollView size validation",
            // TODO add a better explanation here!
            "ScrollView children must set their `layout_width` or `layout_height` attributes " +
            "to `wrap_content` rather than `fill_parent` or `match_parent` in the scrolling " +
            "dimension",
            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            new Implementation(
                    ScrollViewChildDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new {@link ScrollViewChildDetector} */
    public ScrollViewChildDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                SCROLL_VIEW,
                HORIZONTAL_SCROLL_VIEW
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        List<Element> children = LintUtils.getChildren(element);
        boolean isHorizontal = HORIZONTAL_SCROLL_VIEW.equals(element.getTagName());
        String attributeName = isHorizontal ? ATTR_LAYOUT_WIDTH : ATTR_LAYOUT_HEIGHT;
        for (Element child : children) {
            Attr sizeNode = child.getAttributeNodeNS(ANDROID_URI, attributeName);
            if (sizeNode == null) {
                return;
            }
            String value = sizeNode.getValue();
            if (VALUE_FILL_PARENT.equals(value) || VALUE_MATCH_PARENT.equals(value)) {
                String msg = String.format("This %1$s should use `android:%2$s=\"wrap_content\"`",
                        child.getTagName(), attributeName);
                context.report(ISSUE, sizeNode, context.getLocation(sizeNode), msg);
            }
        }
    }
}
