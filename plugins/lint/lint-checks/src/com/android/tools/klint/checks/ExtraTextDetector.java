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

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Check which looks for invalid resources. Aapt already performs some validation,
 * such as making sure that resource references point to resources that exist, but this
 * detector looks for additional issues.
 */
public class ExtraTextDetector extends ResourceXmlDetector {
    private boolean mFoundText;

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "ExtraText", //$NON-NLS-1$
            "Extraneous text in resource files",

            "Layout resource files should only contain elements and attributes. Any XML " +
            "text content found in the file is likely accidental (and potentially " +
            "dangerous if the text resembles XML and the developer believes the text " +
            "to be functional)",
            Category.CORRECTNESS,
            3,
            Severity.WARNING,
            new Implementation(
                    ExtraTextDetector.class,
                    Scope.RESOURCE_FILE_SCOPE)
            );

    /** Constructs a new detector */
    public ExtraTextDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT
                || folderType == ResourceFolderType.MENU
                || folderType == ResourceFolderType.ANIM
                || folderType == ResourceFolderType.ANIMATOR
                || folderType == ResourceFolderType.DRAWABLE
                || folderType == ResourceFolderType.COLOR;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        mFoundText = false;
        visitNode(context, document);
    }

    private void visitNode(XmlContext context, Node node) {
        short nodeType = node.getNodeType();
        if (nodeType == Node.TEXT_NODE && !mFoundText) {
            String text = node.getNodeValue();
            for (int i = 0, n = text.length(); i < n; i++) {
                char c = text.charAt(i);
                if (!Character.isWhitespace(c)) {
                    String snippet = text.trim();
                    int maxLength = 100;
                    if (snippet.length() > maxLength) {
                        snippet = snippet.substring(0, maxLength) + "...";
                    }
                    Location location = context.getLocation(node);
                    if (i > 0) {
                        // Adjust the error position to point to the beginning of
                        // the text rather than the beginning of the text node
                        // (which is often the newline at the end of the previous
                        // line and the indentation)
                        Position start = location.getStart();
                        if (start != null) {
                            int line = start.getLine();
                            int column = start.getColumn();
                            int offset = start.getOffset();

                            for (int j = 0; j < i; j++) {
                                offset++;

                                if (text.charAt(j) == '\n') {
                                    if (line != -1) {
                                        line++;
                                    }
                                    if (column != -1) {
                                        column = 0;
                                    }
                                } else if (column != -1) {
                                    column++;
                                }
                            }

                            start = new DefaultPosition(line, column, offset);
                            location = Location.create(context.file, start, location.getEnd());
                        }
                    }
                    context.report(ISSUE, node, location,
                            String.format("Unexpected text found in layout file: \"%1$s\"",
                                    snippet));
                    mFoundText = true;
                    break;
                }
            }
        }

        // Visit children
        NodeList childNodes = node.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            visitNode(context, child);
        }
    }
}
