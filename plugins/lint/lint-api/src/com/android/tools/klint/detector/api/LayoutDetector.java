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

package com.android.tools.lint.detector.api;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_PADDING;
import static com.android.SdkConstants.ATTR_PADDING_BOTTOM;
import static com.android.SdkConstants.ATTR_PADDING_LEFT;
import static com.android.SdkConstants.ATTR_PADDING_RIGHT;
import static com.android.SdkConstants.ATTR_PADDING_TOP;
import static com.android.SdkConstants.VALUE_FILL_PARENT;
import static com.android.SdkConstants.VALUE_MATCH_PARENT;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.google.common.annotations.Beta;

import org.w3c.dom.Element;

/**
 * Abstract class specifically intended for layout detectors which provides some
 * common utility methods shared by layout detectors.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class LayoutDetector extends ResourceXmlDetector {
    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    private static boolean isFillParent(@NonNull Element element, @NonNull String dimension) {
        String width = element.getAttributeNS(ANDROID_URI, dimension);
        return width.equals(VALUE_MATCH_PARENT) || width.equals(VALUE_FILL_PARENT);
    }

    protected static boolean isWidthFillParent(@NonNull Element element) {
        return isFillParent(element, ATTR_LAYOUT_WIDTH);
    }

    protected static boolean isHeightFillParent(@NonNull Element element) {
        return isFillParent(element, ATTR_LAYOUT_HEIGHT);
    }

    protected boolean hasPadding(@NonNull Element root) {
        return root.hasAttributeNS(ANDROID_URI, ATTR_PADDING)
                || root.hasAttributeNS(ANDROID_URI, ATTR_PADDING_LEFT)
                || root.hasAttributeNS(ANDROID_URI, ATTR_PADDING_RIGHT)
                || root.hasAttributeNS(ANDROID_URI, ATTR_PADDING_TOP)
                || root.hasAttributeNS(ANDROID_URI, ATTR_PADDING_BOTTOM);
    }
}
