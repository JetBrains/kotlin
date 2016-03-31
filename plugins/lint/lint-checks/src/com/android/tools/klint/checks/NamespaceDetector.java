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
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.URI_PREFIX;
import static com.android.SdkConstants.XMLNS_PREFIX;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Map;

/**
 * Checks for various issues related to XML namespaces
 */
public class NamespaceDetector extends LayoutDetector {

    @SuppressWarnings("unchecked")
    private static final Implementation IMPLEMENTATION = new Implementation(
            NamespaceDetector.class,
            Scope.MANIFEST_AND_RESOURCE_SCOPE,
            Scope.RESOURCE_FILE_SCOPE, Scope.MANIFEST_SCOPE);

    /** Typos in the namespace */
    public static final Issue TYPO = Issue.create(
            "NamespaceTypo", //$NON-NLS-1$
            "Misspelled namespace declaration",

            "Accidental misspellings in namespace declarations can lead to some very " +
            "obscure error messages. This check looks for potential misspellings to " +
            "help track these down.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Unused namespace declarations */
    public static final Issue UNUSED = Issue.create(
            "UnusedNamespace", //$NON-NLS-1$
            "Unused namespace",

            "Unused namespace declarations take up space and require processing that is not " +
            "necessary",

            Category.PERFORMANCE,
            1,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Using custom namespace attributes in a library project */
    public static final Issue CUSTOM_VIEW = Issue.create(
            "LibraryCustomView", //$NON-NLS-1$
            "Custom views in libraries should use res-auto-namespace",

            "When using a custom view with custom attributes in a library project, the layout " +
            "must use the special namespace " + AUTO_URI + " instead of a URI which includes " +
            "the library project's own package. This will be used to automatically adjust the " +
            "namespace of the attributes when the library resources are merged into the " +
            "application project.",
            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Unused namespace declarations */
    public static final Issue RES_AUTO = Issue.create(
            "ResAuto", //$NON-NLS-1$
            "Hardcoded Package in Namespace",

            "In Gradle projects, the actual package used in the final APK can vary; for example," +
            "you can add a `.debug` package suffix in one version and not the other. " +
            "Therefore, you should *not* hardcode the application package in the resource; " +
            "instead, use the special namespace `http://schemas.android.com/apk/res-auto` " +
            "which will cause the tools to figure out the right namespace for the resource " +
            "regardless of the actual package used during the build.",

            Category.CORRECTNESS,
            9,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Prefix relevant for custom namespaces */
    private static final String XMLNS_ANDROID = "xmlns:android";                    //$NON-NLS-1$
    private static final String XMLNS_A = "xmlns:a";                                //$NON-NLS-1$

    private Map<String, Attr> mUnusedNamespaces;
    private boolean mCheckUnused;

    /** Constructs a new {@link NamespaceDetector} */
    public NamespaceDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        boolean haveCustomNamespace = false;
        Element root = document.getDocumentElement();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node item = attributes.item(i);
            if (item.getNodeName().startsWith(XMLNS_PREFIX)) {
                String value = item.getNodeValue();

                if (!value.equals(ANDROID_URI)) {
                    Attr attribute = (Attr) item;

                    if (value.startsWith(URI_PREFIX)) {
                        haveCustomNamespace = true;
                        if (mUnusedNamespaces == null) {
                            mUnusedNamespaces = new HashMap<String, Attr>();
                        }
                        mUnusedNamespaces.put(item.getNodeName().substring(XMLNS_PREFIX.length()),
                                attribute);
                    } else if (value.startsWith("urn:")) { //$NON-NLS-1$
                        continue;
                    } else if (!value.startsWith("http://")) { //$NON-NLS-1$
                        if (context.isEnabled(TYPO)) {
                            context.report(TYPO, attribute, context.getValueLocation(attribute),
                                    "Suspicious namespace: should start with `http://`");
                        }

                        continue;
                    } else if (!value.equals(AUTO_URI) && value.contains("auto") && //$NON-NLS-1$
                            value.startsWith("http://schemas.android.com/")) { //$NON-NLS-1$
                        context.report(RES_AUTO, attribute, context.getValueLocation(attribute),
                                "Suspicious namespace: Did you mean `" + AUTO_URI + "`?");
                    }

                    if (!context.isEnabled(TYPO)) {
                        continue;
                    }

                    String name = attribute.getName();
                    if (!name.equals(XMLNS_ANDROID) && !name.equals(XMLNS_A)) {
                        // See if it looks like a typo
                        int resIndex = value.indexOf("/res/"); //$NON-NLS-1$
                        if (resIndex != -1 && value.length() + 5 > URI_PREFIX.length()) {
                            String urlPrefix = value.substring(0, resIndex + 5);
                            if (!urlPrefix.equals(URI_PREFIX) &&
                                    LintUtils.editDistance(URI_PREFIX, urlPrefix) <= 3) {
                                String correctUri = URI_PREFIX + value.substring(resIndex + 5);
                                context.report(TYPO, attribute,
                                        context.getValueLocation(attribute),
                                        String.format(
                                            "Possible typo in URL: was `\"%1$s\"`, should " +
                                            "probably be `\"%2$s\"`",
                                            value, correctUri));
                            }
                        }
                        continue;
                    }

                    if (name.equals(XMLNS_A)) {
                        // For the "android" prefix we always assume that the namespace prefix
                        // should be our expected prefix, but for the "a" prefix we make sure
                        // that it's at least "close"; if you're bound it to something completely
                        // different, don't complain.
                        if (LintUtils.editDistance(ANDROID_URI, value) > 4) {
                            continue;
                        }
                    }

                    if (value.equalsIgnoreCase(ANDROID_URI)) {
                        context.report(TYPO, attribute, context.getValueLocation(attribute),
                                String.format(
                                    "URI is case sensitive: was `\"%1$s\"`, expected `\"%2$s\"`",
                                    value, ANDROID_URI));
                    } else {
                        context.report(TYPO, attribute, context.getValueLocation(attribute),
                                String.format(
                                    "Unexpected namespace URI bound to the `\"android\"` " +
                                    "prefix, was `%1$s`, expected `%2$s`", value, ANDROID_URI));
                    }
                }
            }
        }

        if (haveCustomNamespace) {
            Project project = context.getProject();
            boolean checkCustomAttrs =
                    context.isEnabled(CUSTOM_VIEW) && project.isLibrary()
                    || context.isEnabled(RES_AUTO) && project.isGradleProject();

            mCheckUnused = context.isEnabled(UNUSED);

            if (checkCustomAttrs) {
                checkCustomNamespace(context, root);
            }
            checkElement(root);

            if (mCheckUnused && !mUnusedNamespaces.isEmpty()) {
                for (Map.Entry<String, Attr> entry : mUnusedNamespaces.entrySet()) {
                    String prefix = entry.getKey();
                    Attr attribute = entry.getValue();
                    context.report(UNUSED, attribute, context.getLocation(attribute),
                            String.format("Unused namespace `%1$s`", prefix));
                }
            }
        }
    }

    private static void checkCustomNamespace(XmlContext context, Element element) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (attribute.getName().startsWith(XMLNS_PREFIX)) {
                String uri = attribute.getValue();
                if (uri != null && !uri.isEmpty() && uri.startsWith(URI_PREFIX)
                        && !uri.equals(ANDROID_URI)) {
                    if (context.getProject().isGradleProject()) {
                        context.report(RES_AUTO, attribute, context.getValueLocation(attribute),
                            "In Gradle projects, always use `" + AUTO_URI + "` for custom " +
                            "attributes");
                    } else {
                        context.report(CUSTOM_VIEW, attribute, context.getValueLocation(attribute),
                            "When using a custom namespace attribute in a library project, " +
                            "use the namespace `\"" + AUTO_URI + "\"` instead.");
                    }
                }
            }
        }
    }

    private void checkElement(Node node) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            if (mCheckUnused) {
                NamedNodeMap attributes = node.getAttributes();
                for (int i = 0, n = attributes.getLength(); i < n; i++) {
                    Attr attribute = (Attr) attributes.item(i);
                    String prefix = attribute.getPrefix();
                    if (prefix != null) {
                        mUnusedNamespaces.remove(prefix);
                    }
                }
            }

            NodeList childNodes = node.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                checkElement(childNodes.item(i));
            }
        }
    }
}
