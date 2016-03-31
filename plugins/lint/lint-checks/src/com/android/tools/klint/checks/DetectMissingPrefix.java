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

import static com.android.SdkConstants.ANDROID_PKG_PREFIX;
import static com.android.SdkConstants.ANDROID_SUPPORT_PKG_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_CORE_APP;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_PACKAGE;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.TAG_LAYOUT;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.resources.ResourceFolderType.ANIM;
import static com.android.resources.ResourceFolderType.ANIMATOR;
import static com.android.resources.ResourceFolderType.COLOR;
import static com.android.resources.ResourceFolderType.DRAWABLE;
import static com.android.resources.ResourceFolderType.INTERPOLATOR;
import static com.android.resources.ResourceFolderType.LAYOUT;
import static com.android.resources.ResourceFolderType.MENU;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
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
import org.w3c.dom.Node;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects layout attributes on builtin Android widgets that do not specify
 * a prefix but probably should.
 */
public class DetectMissingPrefix extends LayoutDetector {

    /** Attributes missing the android: prefix */
    @SuppressWarnings("unchecked")
    public static final Issue MISSING_NAMESPACE = Issue.create(
            "MissingPrefix", //$NON-NLS-1$
            "Missing Android XML namespace",
            "Most Android views have attributes in the Android namespace. When referencing " +
            "these attributes you *must* include the namespace prefix, or your attribute will " +
            "be interpreted by `aapt` as just a custom attribute.\n" +
            "\n" +
            "Similarly, in manifest files, nearly all attributes should be in the `android:` " +
            "namespace.",

            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    DetectMissingPrefix.class,
                    Scope.MANIFEST_AND_RESOURCE_SCOPE,
                    Scope.MANIFEST_SCOPE, Scope.RESOURCE_FILE_SCOPE));

    private static final Set<String> NO_PREFIX_ATTRS = new HashSet<String>();
    static {
        NO_PREFIX_ATTRS.add(ATTR_CLASS);
        NO_PREFIX_ATTRS.add(ATTR_STYLE);
        NO_PREFIX_ATTRS.add(ATTR_LAYOUT);
        NO_PREFIX_ATTRS.add(ATTR_PACKAGE);
        NO_PREFIX_ATTRS.add(ATTR_CORE_APP);
    }

    /** Constructs a new {@link DetectMissingPrefix} */
    public DetectMissingPrefix() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == LAYOUT
                || folderType == MENU
                || folderType == DRAWABLE
                || folderType == ANIM
                || folderType == ANIMATOR
                || folderType == COLOR
                || folderType == INTERPOLATOR;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        String uri = attribute.getNamespaceURI();
        if (uri == null || uri.isEmpty()) {
            String name = attribute.getName();
            if (name == null) {
                return;
            }
            if (NO_PREFIX_ATTRS.contains(name)) {
                return;
            }

            Element element = attribute.getOwnerElement();
            if (isCustomView(element) && context.getResourceFolderType() != null) {
                return;
            } else if (context.getResourceFolderType() == ResourceFolderType.LAYOUT) {
                // Data binding: These look like Android framework views but
                // are data binding directives not in the Android namespace
                Element root = element.getOwnerDocument().getDocumentElement();
                if (TAG_LAYOUT.equals(root.getTagName())) {
                    return;
                }
            }

            if (name.indexOf(':') != -1) {
                // Don't flag warnings for attributes that already have a different
                // namespace! This doesn't usually happen when lint is run from the
                // command line, since (with the exception of xmlns: declaration attributes)
                // an attribute shouldn't have a prefix *and* have no namespace, but
                // when lint is run in the IDE (with a more fault-tolerant XML parser)
                // this can happen, and we don't want to flag erroneous/misleading lint
                // errors in this case.
                return;
            }

            context.report(MISSING_NAMESPACE, attribute,
                    context.getLocation(attribute),
                    "Attribute is missing the Android namespace prefix");
        } else if (!ANDROID_URI.equals(uri)
                && !TOOLS_URI.equals(uri)
                && context.getResourceFolderType() == ResourceFolderType.LAYOUT
                && !isCustomView(attribute.getOwnerElement())
                && !attribute.getLocalName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                // TODO: Consider not enforcing that the parent is a custom view
                // too, though in that case we should filter out views that are
                // layout params for the custom view parent:
                // ....&& !attribute.getLocalName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)
                && attribute.getOwnerElement().getParentNode().getNodeType() == Node.ELEMENT_NODE
                && !isCustomView((Element) attribute.getOwnerElement().getParentNode())) {
            if (context.getResourceFolderType() == ResourceFolderType.LAYOUT
                    && AUTO_URI.equals(uri)) {
                // Data binding: Can add attributes like onClickListener to buttons etc.
                Element root = attribute.getOwnerDocument().getDocumentElement();
                if (TAG_LAYOUT.equals(root.getTagName())) {
                    return;
                }
            }

            context.report(MISSING_NAMESPACE, attribute,
                    context.getLocation(attribute),
                    String.format("Unexpected namespace prefix \"%1$s\" found for tag `%2$s`",
                            attribute.getPrefix(), attribute.getOwnerElement().getTagName()));
        }
    }

    private static boolean isCustomView(Element element) {
        // If this is a custom view, the usage of custom attributes can be legitimate
        String tag = element.getTagName();
        if (tag.equals(VIEW_TAG)) {
            // <view class="my.custom.view" ...>
            return true;
        }

        return tag.indexOf('.') != -1 && (!tag.startsWith(ANDROID_PKG_PREFIX)
                || tag.startsWith(ANDROID_SUPPORT_PKG_PREFIX));
    }
}
