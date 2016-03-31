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

import static com.android.SdkConstants.GALLERY;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.LIST_VIEW;
import static com.android.SdkConstants.SCROLL_VIEW;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collection;

/**
 * Checks whether a scroll view contains a nested scrolling widget
 */
public class NestedScrollingWidgetDetector extends LayoutDetector {
    private int mVisitingHorizontalScroll;
    private int mVisitingVerticalScroll;

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "NestedScrolling", //$NON-NLS-1$
            "Nested scrolling widgets",
            // TODO: Better description!
            "A scrolling widget such as a `ScrollView` should not contain any nested " +
            "scrolling widgets since this has various usability issues",
            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            new Implementation(
                    NestedScrollingWidgetDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new {@link NestedScrollingWidgetDetector} */
    public NestedScrollingWidgetDetector() {
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mVisitingHorizontalScroll = 0;
        mVisitingVerticalScroll = 0;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    @NonNull
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                SCROLL_VIEW,
                LIST_VIEW,
                GRID_VIEW,
                // Horizontal
                GALLERY,
                HORIZONTAL_SCROLL_VIEW
        );
    }

    private Element findOuterScrollingWidget(Node node, boolean vertical) {
        Collection<String> applicableElements = getApplicableElements();
        while (node != null) {
            if (node instanceof Element) {
                Element element = (Element) node;
                String tagName = element.getTagName();
                if (applicableElements.contains(tagName)
                        && vertical == isVerticalScroll(element)) {
                    return element;
                }
            }

            node = node.getParentNode();
        }

        return null;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        boolean vertical = isVerticalScroll(element);
        if (vertical) {
            mVisitingVerticalScroll++;
        } else {
            mVisitingHorizontalScroll++;
        }

        if (mVisitingHorizontalScroll > 1 || mVisitingVerticalScroll > 1) {
            Element parent = findOuterScrollingWidget(element.getParentNode(), vertical);
            if (parent != null) {
                String format;
                if (mVisitingVerticalScroll > 1) {
                    format = "The vertically scrolling `%1$s` should not contain another " +
                            "vertically scrolling widget (`%2$s`)";
                } else {
                    format = "The horizontally scrolling `%1$s` should not contain another " +
                            "horizontally scrolling widget (`%2$s`)";
                }
                String msg = String.format(format, parent.getTagName(), element.getTagName());
                context.report(ISSUE, element, context.getLocation(element), msg);
            }
        }
    }

    @Override
    public void visitElementAfter(@NonNull XmlContext context, @NonNull Element element) {
        if (isVerticalScroll(element)) {
            mVisitingVerticalScroll--;
            assert mVisitingVerticalScroll >= 0;
        } else {
            mVisitingHorizontalScroll--;
            assert mVisitingHorizontalScroll >= 0;
        }
    }

    private static boolean isVerticalScroll(Element element) {
        String view = element.getTagName();
        if (view.equals(GALLERY) || view.equals(HORIZONTAL_SCROLL_VIEW)) {
            return false;
        } else {
            // This method should only be called with one of the 5 widget types
            // listed in getApplicableElements
            assert view.equals(SCROLL_VIEW) || view.equals(LIST_VIEW) || view.equals(GRID_VIEW);
            return true;
        }
    }
}
