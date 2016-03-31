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
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.VALUE_WRAP_CONTENT;
import static com.android.SdkConstants.WEB_VIEW;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.Collections;

public class WebViewDetector extends LayoutDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            WebViewDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "WebViewLayout", //$NON-NLS-1$
            "WebViews in wrap_content parents",

            "The WebView implementation has certain performance optimizations which will not " +
            "work correctly if the parent view is using `wrap_content` rather than " +
            "`match_parent`. This can lead to subtle UI bugs.",

            Category.CORRECTNESS,
            7,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Constructs a new {@link WebViewDetector} */
    public WebViewDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(WEB_VIEW);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        Node parentNode = element.getParentNode();
        if (parentNode != null && parentNode.getNodeType() == Node.ELEMENT_NODE) {
            Element parent = (Element)parentNode;
            Attr width = parent.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
            Attr height = parent.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);
            Attr attr = null;
            if (width != null && VALUE_WRAP_CONTENT.equals(width.getValue())) {
                attr = width;
            }
            if (height != null && VALUE_WRAP_CONTENT.equals(height.getValue())) {
                attr = height;
            }
            if (attr != null) {
                String message = String.format("Placing a `<WebView>` in a parent element that "
                        + "uses a `wrap_content %1$s` can lead to subtle bugs; use `match_parent` "
                        + "instead", attr.getLocalName());
                Location location = context.getLocation(element);
                Location secondary = context.getLocation(attr);
                secondary.setMessage("`wrap_content` here may not work well with WebView below");
                location.setSecondary(secondary);
                context.report(ISSUE, element, location, message);
            }
        }
    }
}
