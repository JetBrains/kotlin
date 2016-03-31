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

import static com.android.SdkConstants.ABSOLUTE_LAYOUT;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_BACKGROUND;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_PADDING;
import static com.android.SdkConstants.ATTR_PADDING_BOTTOM;
import static com.android.SdkConstants.ATTR_PADDING_END;
import static com.android.SdkConstants.ATTR_PADDING_LEFT;
import static com.android.SdkConstants.ATTR_PADDING_RIGHT;
import static com.android.SdkConstants.ATTR_PADDING_START;
import static com.android.SdkConstants.ATTR_PADDING_TOP;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.FRAME_LAYOUT;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.RADIO_GROUP;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.SCROLL_VIEW;
import static com.android.SdkConstants.TABLE_LAYOUT;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.VIEW_MERGE;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Checks whether the current node can be removed without affecting the layout.
 */
public class UselessViewDetector extends LayoutDetector {

    private static final Implementation IMPLEMENTATION = new Implementation(
            UselessViewDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Issue of including a parent that has no value on its own */
    public static final Issue USELESS_PARENT = Issue.create(
            "UselessParent", //$NON-NLS-1$
            "Useless parent layout",
            "A layout with children that has no siblings, is not a scrollview or " +
            "a root layout, and does not have a background, can be removed and have " +
            "its children moved directly into the parent for a flatter and more " +
            "efficient layout hierarchy.",
            Category.PERFORMANCE,
            2,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Issue of including a leaf that isn't shown */
    public static final Issue USELESS_LEAF = Issue.create(
            "UselessLeaf", //$NON-NLS-1$
            "Useless leaf layout",
            "A layout that has no children or no background can often be removed (since it " +
            "is invisible) for a flatter and more efficient layout hierarchy.",
            Category.PERFORMANCE,
            2,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs a new {@link UselessViewDetector} */
    public UselessViewDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    private static final List<String> CONTAINERS = new ArrayList<String>(18);
    static {
        CONTAINERS.add(ABSOLUTE_LAYOUT);
        CONTAINERS.add(FRAME_LAYOUT);
        CONTAINERS.add(GRID_LAYOUT);
        CONTAINERS.add(GRID_VIEW);
        CONTAINERS.add(HORIZONTAL_SCROLL_VIEW);
        CONTAINERS.add("ImageSwitcher");                      //$NON-NLS-1$
        CONTAINERS.add(LINEAR_LAYOUT);
        CONTAINERS.add(RADIO_GROUP);
        CONTAINERS.add(RELATIVE_LAYOUT);
        CONTAINERS.add(SCROLL_VIEW);
        CONTAINERS.add("SlidingDrawer");                      //$NON-NLS-1$
        CONTAINERS.add("StackView");                          //$NON-NLS-1$
        CONTAINERS.add(TABLE_LAYOUT);
        CONTAINERS.add(TABLE_ROW);
        CONTAINERS.add("TextSwitcher");                       //$NON-NLS-1$
        CONTAINERS.add("ViewAnimator");                       //$NON-NLS-1$
        CONTAINERS.add("ViewFlipper");                        //$NON-NLS-1$
        CONTAINERS.add("ViewSwitcher");                       //$NON-NLS-1$
        // Available ViewGroups that are not included by this check:
        //  CONTAINERS.add("android.gesture.GestureOverlayView");
        //  CONTAINERS.add("AdapterViewFlipper");
        //  CONTAINERS.add("DialerFilter");
        //  CONTAINERS.add("ExpandableListView");
        //  CONTAINERS.add("ListView");
        //  CONTAINERS.add("MediaController");
        //  CONTAINERS.add("merge");
        //  CONTAINERS.add("SearchView");
        //  CONTAINERS.add("TabWidget");
        //  CONTAINERS.add("TabHost");
    }
    @Override
    public Collection<String> getApplicableElements() {
        return CONTAINERS;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        int childCount = LintUtils.getChildCount(element);
        if (childCount == 0) {
            // Check to see if this is a leaf layout that can be removed
            checkUselessLeaf(context, element);
        } else {
            // Check to see if this is a middle-man layout which can be removed
            checkUselessMiddleLayout(context, element);
        }
    }

    // This is the old UselessLayoutCheck from layoutopt
    private static void checkUselessMiddleLayout(XmlContext context, Element element) {
        // Conditions:
        // - The node has children
        // - The node does not have siblings
        // - The node's parent is not a scroll view (horizontal or vertical)
        // - The node does not have a background or its parent does not have a
        //   background or neither the node and its parent have a background
        // - The parent is not a <merge/>

        Node parentNode = element.getParentNode();
        if (parentNode.getNodeType() != Node.ELEMENT_NODE) {
            // Can't remove root
            return;
        }

        Element parent = (Element) parentNode;
        String parentTag = parent.getTagName();
        if (parentTag.equals(SCROLL_VIEW) || parentTag.equals(HORIZONTAL_SCROLL_VIEW) ||
                parentTag.equals(VIEW_MERGE)) {
            // Can't remove if the parent is a scroll view or a merge
            return;
        }

        // This method is only called when we've already ensured that it has children
        assert LintUtils.getChildCount(element) > 0;

        int parentChildCount = LintUtils.getChildCount(parent);
        if (parentChildCount != 1) {
            // Don't remove if the node has siblings
            return;
        }

        // - A parent can be removed if it doesn't have a background
        // - A parent can be removed if has a background *and* the child does not have a
        //   background (in which case, just move the background over to the child, remove
        //   the parent)
        // - If both child and parent have a background, the parent cannot be removed (a
        //   background can be translucent, have transparent padding, etc.)
        boolean nodeHasBackground = element.hasAttributeNS(ANDROID_URI, ATTR_BACKGROUND);
        boolean parentHasBackground = parent.hasAttributeNS(ANDROID_URI, ATTR_BACKGROUND);
        if (nodeHasBackground && parentHasBackground) {
            // Can't remove because both define a background, and they might both be
            // visible (e.g. through transparency or padding).
            return;
        }

        // Certain parents are special - such as the TabHost and the GestureOverlayView -
        // where we want to leave things alone.
        if (!CONTAINERS.contains(parentTag)) {
            return;
        }

        // If we define a padding, and the parent provides a background, then
        // this view is not *necessarily* useless.
        if (parentHasBackground && element.hasAttributeNS(ANDROID_URI, ATTR_PADDING)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_LEFT)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_RIGHT)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_TOP)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_BOTTOM)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_START)
                || element.hasAttributeNS(ANDROID_URI, ATTR_PADDING_END)) {
            return;
        }

        boolean hasId = element.hasAttributeNS(ANDROID_URI, ATTR_ID);
        Location location = context.getLocation(element);
        String tag = element.getTagName();
        String format;
        if (hasId) {
            format = "This `%1$s` layout or its `%2$s` parent is possibly useless";
        } else {
            format = "This `%1$s` layout or its `%2$s` parent is useless";
        }
        if (nodeHasBackground || parentHasBackground) {
            format += "; transfer the `background` attribute to the other view";
        }
        String message = String.format(format, tag, parentTag);
        context.report(USELESS_PARENT, element, location, message);
    }

    // This is the old UselessView check from layoutopt
    private static void checkUselessLeaf(XmlContext context, Element element) {
        assert LintUtils.getChildCount(element) == 0;

        // Conditions:
        // - The node is a container view (LinearLayout, etc.)
        // - The node has no id
        // - The node has no background
        // - The node has no children
        // - The node has no style
        // - The node is not a root

        if (element.hasAttributeNS(ANDROID_URI, ATTR_ID)) {
            return;
        }

        if (element.hasAttributeNS(ANDROID_URI, ATTR_BACKGROUND)) {
            return;
        }

        if (element.hasAttribute(ATTR_STYLE)) {
            return;
        }

        if (element == context.document.getDocumentElement()) {
            return;
        }

        Location location = context.getLocation(element);
        String tag = element.getTagName();
        String message = String.format(
                "This `%1$s` view is useless (no children, no `background`, no `id`, no `style`)", tag);
        context.report(USELESS_LEAF, element, location, message);
    }
}
