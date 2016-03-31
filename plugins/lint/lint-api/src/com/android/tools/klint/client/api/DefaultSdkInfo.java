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

package com.android.tools.lint.client.api;

import static com.android.SdkConstants.ABSOLUTE_LAYOUT;
import static com.android.SdkConstants.ABS_LIST_VIEW;
import static com.android.SdkConstants.ABS_SEEK_BAR;
import static com.android.SdkConstants.ABS_SPINNER;
import static com.android.SdkConstants.ADAPTER_VIEW;
import static com.android.SdkConstants.AUTO_COMPLETE_TEXT_VIEW;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.CHECKABLE;
import static com.android.SdkConstants.CHECKED_TEXT_VIEW;
import static com.android.SdkConstants.CHECK_BOX;
import static com.android.SdkConstants.COMPOUND_BUTTON;
import static com.android.SdkConstants.EDIT_TEXT;
import static com.android.SdkConstants.EXPANDABLE_LIST_VIEW;
import static com.android.SdkConstants.FRAME_LAYOUT;
import static com.android.SdkConstants.GALLERY;
import static com.android.SdkConstants.GRID_VIEW;
import static com.android.SdkConstants.HORIZONTAL_SCROLL_VIEW;
import static com.android.SdkConstants.IMAGE_BUTTON;
import static com.android.SdkConstants.IMAGE_VIEW;
import static com.android.SdkConstants.LINEAR_LAYOUT;
import static com.android.SdkConstants.LIST_VIEW;
import static com.android.SdkConstants.MULTI_AUTO_COMPLETE_TEXT_VIEW;
import static com.android.SdkConstants.PROGRESS_BAR;
import static com.android.SdkConstants.RADIO_BUTTON;
import static com.android.SdkConstants.RADIO_GROUP;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.SCROLL_VIEW;
import static com.android.SdkConstants.SEEK_BAR;
import static com.android.SdkConstants.SPINNER;
import static com.android.SdkConstants.SURFACE_VIEW;
import static com.android.SdkConstants.SWITCH;
import static com.android.SdkConstants.TABLE_LAYOUT;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.TAB_HOST;
import static com.android.SdkConstants.TAB_WIDGET;
import static com.android.SdkConstants.TEXT_VIEW;
import static com.android.SdkConstants.TOGGLE_BUTTON;
import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_ANIMATOR;
import static com.android.SdkConstants.VIEW_GROUP;
import static com.android.SdkConstants.VIEW_PKG_PREFIX;
import static com.android.SdkConstants.VIEW_STUB;
import static com.android.SdkConstants.VIEW_SWITCHER;
import static com.android.SdkConstants.WEB_VIEW;
import static com.android.SdkConstants.WIDGET_PKG_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default simple implementation of an {@link SdkInfo}
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
class DefaultSdkInfo extends SdkInfo {
    @Override
    @Nullable
    public String getParentViewName(@NonNull String name) {
        name = getRawType(name);

        return PARENTS.get(name);
    }

    @Override
    @Nullable
    public String getParentViewClass(@NonNull String fqcn) {
        int index = fqcn.lastIndexOf('.');
        if (index != -1) {
            fqcn = fqcn.substring(index + 1);
        }

        String parent = PARENTS.get(fqcn);
        if (parent == null) {
            return null;
        }
        // The map only stores class names internally; correct for full package paths
        if (parent.equals(VIEW) || parent.equals(VIEW_GROUP) || parent.equals(SURFACE_VIEW)) {
            return VIEW_PKG_PREFIX + parent;
        } else {
            return WIDGET_PKG_PREFIX + parent;
        }
    }

    @Override
    public boolean isSubViewOf(@NonNull String parentType, @NonNull String childType) {
        String parent = getRawType(parentType);
        String child = getRawType(childType);

        // Do analysis just on non-fqcn paths
        if (parent.indexOf('.') != -1) {
            parent = parent.substring(parent.lastIndexOf('.') + 1);
        }
        if (child.indexOf('.') != -1) {
            child = child.substring(child.lastIndexOf('.') + 1);
        }

        if (parent.equals(VIEW)) {
            return true;
        }

        while (!child.equals(VIEW)) {
            if (parent.equals(child)) {
                return true;
            }
            if (implementsInterface(child, parentType)) {
                return true;
            }
            child = PARENTS.get(child);
            if (child == null) {
                // Unknown view - err on the side of caution
                return true;
            }
        }

        return false;
    }

    private static boolean implementsInterface(String className, String interfaceName) {
        return interfaceName.equals(INTERFACES.get(className));
    }

    // Strip off type parameters, e.g. AdapterView<?> => AdapterView
    private static String getRawType(String type) {
        if (type != null) {
            int index = type.indexOf('<');
            if (index != -1) {
                type = type.substring(0, index);
            }
        }

        return type;
    }

    @Override
    public boolean isLayout(@NonNull String tag) {
        // TODO: Read in widgets.txt from the platform install area to look up this information
        // dynamically instead!

        if (super.isLayout(tag)) {
            return true;
        }

        return LAYOUTS.contains(tag);
    }

    private static final int CLASS_COUNT = 59;
    private static final int LAYOUT_COUNT = 20;

    private static final Map<String,String> PARENTS = Maps.newHashMapWithExpectedSize(CLASS_COUNT);
    private static final Set<String> LAYOUTS =  Sets.newHashSetWithExpectedSize(CLASS_COUNT);

    static {
        PARENTS.put(COMPOUND_BUTTON, BUTTON);
        PARENTS.put(ABS_SPINNER, ADAPTER_VIEW);
        PARENTS.put(ABS_LIST_VIEW, ADAPTER_VIEW);
        PARENTS.put(ABS_SEEK_BAR, ADAPTER_VIEW);
        PARENTS.put(ADAPTER_VIEW, VIEW_GROUP);
        PARENTS.put(VIEW_GROUP, VIEW);

        PARENTS.put(TEXT_VIEW, VIEW);
        PARENTS.put(CHECKED_TEXT_VIEW, TEXT_VIEW);
        PARENTS.put(RADIO_BUTTON, COMPOUND_BUTTON);
        PARENTS.put(SPINNER, ABS_SPINNER);
        PARENTS.put(IMAGE_BUTTON, IMAGE_VIEW);
        PARENTS.put(IMAGE_VIEW, VIEW);
        PARENTS.put(EDIT_TEXT, TEXT_VIEW);
        PARENTS.put(PROGRESS_BAR, VIEW);
        PARENTS.put(TOGGLE_BUTTON, COMPOUND_BUTTON);
        PARENTS.put(VIEW_STUB, VIEW);
        PARENTS.put(BUTTON, TEXT_VIEW);
        PARENTS.put(SEEK_BAR, ABS_SEEK_BAR);
        PARENTS.put(CHECK_BOX, COMPOUND_BUTTON);
        PARENTS.put(SWITCH, COMPOUND_BUTTON);
        PARENTS.put(GALLERY, ABS_SPINNER);
        PARENTS.put(SURFACE_VIEW, VIEW);
        PARENTS.put(ABSOLUTE_LAYOUT, VIEW_GROUP);
        PARENTS.put(LINEAR_LAYOUT, VIEW_GROUP);
        PARENTS.put(RELATIVE_LAYOUT, VIEW_GROUP);
        PARENTS.put(LIST_VIEW, ABS_LIST_VIEW);
        PARENTS.put(VIEW_SWITCHER, VIEW_ANIMATOR);
        PARENTS.put(FRAME_LAYOUT, VIEW_GROUP);
        PARENTS.put(HORIZONTAL_SCROLL_VIEW, FRAME_LAYOUT);
        PARENTS.put(VIEW_ANIMATOR, FRAME_LAYOUT);
        PARENTS.put(TAB_HOST, FRAME_LAYOUT);
        PARENTS.put(TABLE_ROW, LINEAR_LAYOUT);
        PARENTS.put(RADIO_GROUP, LINEAR_LAYOUT);
        PARENTS.put(TAB_WIDGET, LINEAR_LAYOUT);
        PARENTS.put(EXPANDABLE_LIST_VIEW, LIST_VIEW);
        PARENTS.put(TABLE_LAYOUT, LINEAR_LAYOUT);
        PARENTS.put(SCROLL_VIEW, FRAME_LAYOUT);
        PARENTS.put(GRID_VIEW, ABS_LIST_VIEW);
        PARENTS.put(WEB_VIEW, ABSOLUTE_LAYOUT);
        PARENTS.put(AUTO_COMPLETE_TEXT_VIEW, EDIT_TEXT);
        PARENTS.put(MULTI_AUTO_COMPLETE_TEXT_VIEW, AUTO_COMPLETE_TEXT_VIEW);
        PARENTS.put(CHECKED_TEXT_VIEW, TEXT_VIEW);

        PARENTS.put("MediaController", FRAME_LAYOUT);     //$NON-NLS-1$
        PARENTS.put("SlidingDrawer", VIEW_GROUP);         //$NON-NLS-1$
        PARENTS.put("DialerFilter", RELATIVE_LAYOUT);     //$NON-NLS-1$
        PARENTS.put("DigitalClock", TEXT_VIEW);           //$NON-NLS-1$
        PARENTS.put("Chronometer", TEXT_VIEW);            //$NON-NLS-1$
        PARENTS.put("ImageSwitcher", VIEW_SWITCHER);      //$NON-NLS-1$
        PARENTS.put("TextSwitcher", VIEW_SWITCHER);       //$NON-NLS-1$
        PARENTS.put("AnalogClock", VIEW);                 //$NON-NLS-1$
        PARENTS.put("TwoLineListItem", RELATIVE_LAYOUT);  //$NON-NLS-1$
        PARENTS.put("ZoomControls", LINEAR_LAYOUT);       //$NON-NLS-1$
        PARENTS.put("DatePicker", FRAME_LAYOUT);          //$NON-NLS-1$
        PARENTS.put("TimePicker", FRAME_LAYOUT);          //$NON-NLS-1$
        PARENTS.put("VideoView", SURFACE_VIEW);           //$NON-NLS-1$
        PARENTS.put("ZoomButton", IMAGE_BUTTON);          //$NON-NLS-1$
        PARENTS.put("RatingBar", ABS_SEEK_BAR);           //$NON-NLS-1$
        PARENTS.put("ViewFlipper", VIEW_ANIMATOR);        //$NON-NLS-1$
        PARENTS.put("NumberPicker", LINEAR_LAYOUT);       //$NON-NLS-1$

        assert PARENTS.size() <= CLASS_COUNT : PARENTS.size();

        /*
        // Check that all widgets lead to the root view
        if (LintUtils.assertionsEnabled()) {
            for (String key : PARENTS.keySet()) {
                String parent = PARENTS.get(key);
                if (!parent.equals(VIEW)) {
                    String grandParent = PARENTS.get(parent);
                    assert grandParent != null : parent;
                }
            }
        }
        */

        LAYOUTS.add(TAB_HOST);
        LAYOUTS.add(HORIZONTAL_SCROLL_VIEW);
        LAYOUTS.add(VIEW_SWITCHER);
        LAYOUTS.add(TAB_WIDGET);
        LAYOUTS.add(VIEW_ANIMATOR);
        LAYOUTS.add(SCROLL_VIEW);
        LAYOUTS.add(GRID_VIEW);
        LAYOUTS.add(TABLE_ROW);
        LAYOUTS.add(RADIO_GROUP);
        LAYOUTS.add(LIST_VIEW);
        LAYOUTS.add(EXPANDABLE_LIST_VIEW);
        LAYOUTS.add("MediaController");        //$NON-NLS-1$
        LAYOUTS.add("DialerFilter");           //$NON-NLS-1$
        LAYOUTS.add("ViewFlipper");            //$NON-NLS-1$
        LAYOUTS.add("SlidingDrawer");          //$NON-NLS-1$
        LAYOUTS.add("StackView");              //$NON-NLS-1$
        LAYOUTS.add("SearchView");             //$NON-NLS-1$
        LAYOUTS.add("TextSwitcher");           //$NON-NLS-1$
        LAYOUTS.add("AdapterViewFlipper");     //$NON-NLS-1$
        LAYOUTS.add("ImageSwitcher");          //$NON-NLS-1$
        assert LAYOUTS.size() <= LAYOUT_COUNT : LAYOUTS.size();
    }

    // Currently using a map; this should really be a list, but using a map until we actually
    // start adding more than one item
    @NonNull
    private static final Map<String, String> INTERFACES = new HashMap<String, String>(2);
    static {
        INTERFACES.put(CHECKED_TEXT_VIEW, CHECKABLE);
        INTERFACES.put(COMPOUND_BUTTON, CHECKABLE);
    }
}
