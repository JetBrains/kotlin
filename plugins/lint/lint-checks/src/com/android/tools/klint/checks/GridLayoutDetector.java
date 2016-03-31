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

import static com.android.SdkConstants.ANDROID_NS_NAME;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLUMN_COUNT;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN;
import static com.android.SdkConstants.ATTR_LAYOUT_COLUMN_SPAN;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW;
import static com.android.SdkConstants.ATTR_LAYOUT_ROW_SPAN;
import static com.android.SdkConstants.ATTR_ORIENTATION;
import static com.android.SdkConstants.ATTR_ROW_COUNT;
import static com.android.SdkConstants.ATTR_USE_DEFAULT_MARGINS;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.FQCN_GRID_LAYOUT_V7;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.XMLNS_PREFIX;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.Arrays;
import java.util.Collection;

/**
 * Check which looks for potential errors in declarations of GridLayouts, such as specifying
 * row/column numbers outside the declared dimensions of the grid.
 */
public class GridLayoutDetector extends LayoutDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "GridLayout", //$NON-NLS-1$
            "GridLayout validation",
            "Declaring a layout_row or layout_column that falls outside the declared size " +
            "of a GridLayout's `rowCount` or `columnCount` is usually an unintentional error.",
            Category.CORRECTNESS,
            4,
            Severity.FATAL,
            new Implementation(
                    GridLayoutDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a new {@link GridLayoutDetector} check */
    public GridLayoutDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                GRID_LAYOUT,
                FQCN_GRID_LAYOUT_V7
        );
    }

    private static int getInt(Element element, String attribute, int defaultValue) {
        String valueString = element.getAttributeNS(ANDROID_URI, attribute);
        if (valueString != null && !valueString.isEmpty()) {
            try {
                return Integer.decode(valueString);
            } catch (NumberFormatException nufe) {
                // Ignore - error in user's XML
            }
        }

        return defaultValue;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        int declaredRowCount = getInt(element, ATTR_ROW_COUNT, -1);
        int declaredColumnCount = getInt(element, ATTR_COLUMN_COUNT, -1);

        if (declaredColumnCount != -1 || declaredRowCount != -1) {
            for (Element child : LintUtils.getChildren(element)) {
                if (declaredColumnCount != -1) {
                    int column = getInt(child, ATTR_LAYOUT_COLUMN, -1);
                    if (column >= declaredColumnCount) {
                        Attr node = child.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_COLUMN);
                        context.report(ISSUE, node, context.getLocation(node),
                                String.format("Column attribute (%1$d) exceeds declared grid column count (%2$d)",
                                        column, declaredColumnCount));
                    }
                }
                if (declaredRowCount != -1) {
                    int row = getInt(child, ATTR_LAYOUT_ROW, -1);
                    if (row > declaredRowCount) {
                        Attr node = child.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_ROW);
                        context.report(ISSUE, node, context.getLocation(node),
                                String.format("Row attribute (%1$d) exceeds declared grid row count (%2$d)",
                                        row, declaredRowCount));
                    }
                }
            }
        }

        if (element.getTagName().equals(FQCN_GRID_LAYOUT_V7)) {
            // Make sure that we're not using android: namespace attributes where we should
            // be using app namespace attributes!
            ensureAppNamespace(context, element, ATTR_COLUMN_COUNT);
            ensureAppNamespace(context, element, ATTR_ORIENTATION);
            ensureAppNamespace(context, element, ATTR_ROW_COUNT);
            ensureAppNamespace(context, element, ATTR_USE_DEFAULT_MARGINS);
            ensureAppNamespace(context, element, "alignmentMode");
            ensureAppNamespace(context, element, "columnOrderPreserved");
            ensureAppNamespace(context, element, "rowOrderPreserved");

            for (Element child : LintUtils.getChildren(element)) {
                ensureAppNamespace(context, child, ATTR_LAYOUT_COLUMN);
                ensureAppNamespace(context, child, ATTR_LAYOUT_COLUMN_SPAN);
                ensureAppNamespace(context, child, ATTR_LAYOUT_GRAVITY);
                ensureAppNamespace(context, child, ATTR_LAYOUT_ROW);
                ensureAppNamespace(context, child, ATTR_LAYOUT_ROW_SPAN);
                ensureAppNamespace(context, child, "layout_rowWeight");
                ensureAppNamespace(context, child, "layout_columnWeight");
            }
        }
    }

    private static void ensureAppNamespace(XmlContext context, Element element, String name) {
        Attr attribute = element.getAttributeNodeNS(ANDROID_URI, name);
        if (attribute != null) {
            String prefix = getNamespacePrefix(element.getOwnerDocument(), AUTO_URI);
            boolean haveNamespace = prefix != null;
            if (!haveNamespace) {
                prefix = "app";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Wrong namespace; with v7 `GridLayout` you should use ").append(prefix)
                    .append(":").append(name);
            if (!haveNamespace) {
                sb.append(" (and add `xmlns:app=\"").append(AUTO_URI)
                        .append("\"` to your root element.)");
            }
            String message = sb.toString();

            context.report(ISSUE, attribute, context.getLocation(attribute), message);
        }
    }

    @Nullable
    private static String getNamespacePrefix(Document document, String uri) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return null;
        }
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0, n = attributes.getLength(); i < n; i++) {
            Node attribute = attributes.item(i);
            if (attribute.getNodeName().startsWith(XMLNS_PREFIX) &&
                    attribute.getNodeValue().equals(uri)) {
                return attribute.getNodeName().substring(XMLNS_PREFIX.length());
            }

        }

        return null;
    }

    /**
     * Given an error message produced by this lint detector,
     * returns the old value to be replaced in the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding old value, or null if not recognized
     */
    @Nullable
    public static String getOldValue(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);
        String attribute = LintUtils.findSubstring(errorMessage, " should use ", " ");
        if (attribute == null) {
            attribute = LintUtils.findSubstring(errorMessage, " should use ", null);
        }
        if (attribute != null) {
            int index = attribute.indexOf(':');
            if (index != -1) {
                return ANDROID_NS_NAME + attribute.substring(index);
            }
        }

        return null;
    }

    /**
     * Given an error message produced by this lint detector,
     * returns the new value to be put into the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding new value, or null if not recognized
     */
    @Nullable
    public static String getNewValue(@NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);
        String attribute = LintUtils.findSubstring(errorMessage, " should use ", " ");
        if (attribute == null) {
            attribute = LintUtils.findSubstring(errorMessage, " should use ", null);
        }
        return attribute;
    }
}
