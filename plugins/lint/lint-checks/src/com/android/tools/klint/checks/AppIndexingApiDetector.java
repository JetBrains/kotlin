/*
 * Copyright (C) 2015 The Android Open Source Project
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
import static com.android.SdkConstants.ATTR_HOST;
import static com.android.SdkConstants.ATTR_PATH;
import static com.android.SdkConstants.ATTR_PATH_PATTERN;
import static com.android.SdkConstants.ATTR_PATH_PREFIX;
import static com.android.SdkConstants.ATTR_SCHEME;

import static com.android.xml.AndroidManifest.ATTRIBUTE_MIME_TYPE;
import static com.android.xml.AndroidManifest.ATTRIBUTE_NAME;
import static com.android.xml.AndroidManifest.ATTRIBUTE_PORT;
import static com.android.xml.AndroidManifest.NODE_ACTION;
import static com.android.xml.AndroidManifest.NODE_CATEGORY;
import static com.android.xml.AndroidManifest.NODE_DATA;
import static com.android.xml.AndroidManifest.NODE_INTENT;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 * Check if the usage of App Indexing is correct.
 */
public class AppIndexingApiDetector extends Detector implements Detector.XmlScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            AppIndexingApiDetector.class, Scope.MANIFEST_SCOPE);

    public static final Issue ISSUE_ERROR = Issue.create("AppIndexingError", //$NON-NLS-1$
            "Wrong Usage of App Indexing",
            "Ensures the app can correctly handle deep links and integrate with " +
                    "App Indexing for Google search.",
            Category.USABILITY, 5, Severity.ERROR, IMPLEMENTATION)
            .addMoreInfo("https://g.co/AppIndexing");

    public static final Issue ISSUE_WARNING = Issue.create("AppIndexingWarning", //$NON-NLS-1$
            "Missing App Indexing Support",
            "Ensures the app can correctly handle deep links and integrate with " +
                    "App Indexing for Google search.",
            Category.USABILITY, 5, Severity.WARNING, IMPLEMENTATION)
            .addMoreInfo("https://g.co/AppIndexing");

    private static final String[] PATH_ATTR_LIST = new String[]{ATTR_PATH_PREFIX, ATTR_PATH,
            ATTR_PATH_PATTERN};

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList(NODE_INTENT);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element intent) {
        boolean actionView = hasActionView(intent);
        boolean browsable = isBrowsable(intent);
        boolean isHttp = false;
        boolean hasScheme = false;
        boolean hasHost = false;
        boolean hasPort = false;
        boolean hasPath = false;
        boolean hasMimeType = false;
        Element firstData = null;
        NodeList children = intent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(NODE_DATA)) {
                Element data = (Element) child;
                if (firstData == null) {
                    firstData = data;
                }
                if (isHttpSchema(data)) {
                    isHttp = true;
                }
                checkSingleData(context, data);

                for (String name : PATH_ATTR_LIST) {
                    if (data.hasAttributeNS(ANDROID_URI, name)) {
                        hasPath = true;
                    }
                }

                if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
                    hasScheme = true;
                }

                if (data.hasAttributeNS(ANDROID_URI, ATTR_HOST)) {
                    hasHost = true;
                }

                if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
                    hasPort = true;
                }

                if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_MIME_TYPE)) {
                    hasMimeType = true;
                }
            }
        }

        // In data field, a URL is consisted by
        // <scheme>://<host>:<port>[<path>|<pathPrefix>|<pathPattern>]
        // Each part of the URL should not have illegal character.
        if ((hasPath || hasHost || hasPort) && !hasScheme) {
            context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                    "android:scheme missing");
        }

        if ((hasPath || hasPort) && !hasHost) {
            context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                    "android:host missing");
        }

        if (actionView && browsable) {
            if (firstData == null) {
                // If this activity is an ACTION_VIEW action with category BROWSABLE, but doesn't
                // have data node, it may be a mistake and we will report error.
                context.report(ISSUE_ERROR, intent, context.getLocation(intent),
                        "Missing data node?");
            } else if (!hasScheme && !hasMimeType) {
                // If this activity is an action view, is browsable, but has neither a
                // URL nor mimeType, it may be a mistake and we will report error.
                context.report(ISSUE_ERROR, firstData, context.getLocation(firstData),
                        "Missing URL for the intent filter?");
            }
        }

        // If this activity is an ACTION_VIEW action, has a http URL but doesn't have
        // BROWSABLE, it may be a mistake and and we will report warning.
        if (actionView && isHttp && !browsable) {
            context.report(ISSUE_WARNING, intent, context.getLocation(intent),
                    "Activity supporting ACTION_VIEW is not set as BROWSABLE");
        }
    }

    private static boolean hasActionView(Element intent) {
        NodeList children = intent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(NODE_ACTION)) {
                Element action = (Element) child;
                if (action.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                    Attr attr = action.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                    if (attr.getValue().equals("android.intent.action.VIEW")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBrowsable(Element intent) {
        NodeList children = intent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    child.getNodeName().equals(NODE_CATEGORY)) {
                Element e = (Element) child;
                if (e.hasAttributeNS(ANDROID_URI, ATTRIBUTE_NAME)) {
                    Attr attr = e.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_NAME);
                    if (attr.getNodeValue().equals("android.intent.category.BROWSABLE")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isHttpSchema(Element data) {
        if (data.hasAttributeNS(ANDROID_URI, ATTR_SCHEME)) {
            String value = data.getAttributeNodeNS(ANDROID_URI, ATTR_SCHEME).getValue();
            if (value.equalsIgnoreCase("http") || value.equalsIgnoreCase("https")) {
                return true;
            }
        }
        return false;
    }

    private static void checkSingleData(XmlContext context, Element data) {
        // path, pathPrefix and pathPattern should starts with /.
        for (String name : PATH_ATTR_LIST) {
            if (data.hasAttributeNS(ANDROID_URI, name)) {
                Attr attr = data.getAttributeNodeNS(ANDROID_URI, name);
                String path = replaceUrlWithValue(context, attr.getValue());
                if (!path.startsWith("/") && !path.startsWith(SdkConstants.PREFIX_RESOURCE_REF)) {
                    context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                            "android:" + name + " attribute should start with '/', but it is : "
                                    + path);
                }
            }
        }

        // port should be a legal number.
        if (data.hasAttributeNS(ANDROID_URI, ATTRIBUTE_PORT)) {
            Attr attr = data.getAttributeNodeNS(ANDROID_URI, ATTRIBUTE_PORT);
            try {
                String port = replaceUrlWithValue(context, attr.getValue());
                Integer.parseInt(port);
            } catch (NumberFormatException e) {
                context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                        "android:port is not a legal number");
            }
        }

        // Each field should be non empty.
        NamedNodeMap attrs = data.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node item = attrs.item(i);
            if (item.getNodeType() == Node.ATTRIBUTE_NODE) {
                Attr attr = (Attr) attrs.item(i);
                if (attr.getValue().isEmpty()) {
                    context.report(ISSUE_ERROR, attr, context.getLocation(attr),
                            attr.getName() + " cannot be empty");
                }
            }
        }
    }

    private static String replaceUrlWithValue(@NonNull XmlContext context,
            @NonNull String str) {
        Project project = context.getProject();
        LintClient client = context.getClient();
        if (!client.supportsProjectResources()) {
            return str;
        }
        ResourceUrl style = ResourceUrl.parse(str);
        if (style == null || style.type != ResourceType.STRING || style.framework) {
            return str;
        }
        AbstractResourceRepository resources = client.getProjectResources(project, true);
        if (resources == null) {
            return str;
        }
        List<ResourceItem> items = resources.getResourceItem(ResourceType.STRING, style.name);
        if (items == null || items.isEmpty()) {
            return str;
        }
        ResourceValue resourceValue = items.get(0).getResourceValue(false);
        if (resourceValue == null) {
            return str;
        }
        return resourceValue.getValue() == null ? str : resourceValue.getValue();
    }
}
