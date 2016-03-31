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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.Collections;

/**
 * Check which makes sure NFC tech lists do not include spaces around {@code <tech>} values
 * since that's not handled correctly by the inflater
 */
public class NfcTechListDetector extends ResourceXmlDetector implements JavaScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "NfcTechWhitespace", //$NON-NLS-1$
            "Whitespace in NFC tech lists",

            "In a <tech-list>, there can be whitespace around the <tech> elements," +
            "but not inside them. This is because the code which reads in the tech " +
            "list is currently very strict and will include the whitespace as part " +
            "of the name.\n" +
            "\n" +
            "In other words, use <tech>name</tech>, not <tech> name </tech>.",

            Category.CORRECTNESS,
            5,
            Severity.FATAL,
            new Implementation(
                    NfcTechListDetector.class,
                    Scope.RESOURCE_FILE_SCOPE))
            .addMoreInfo(
            "https://code.google.com/p/android/issues/detail?id=65351"); //$NON-NLS-1$

    /** Constructs a new {@link NfcTechListDetector} */
    public NfcTechListDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.XML;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Collections.singletonList("tech");
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        Node parentNode = element.getParentNode();
        if (parentNode == null || parentNode.getNodeType() != Node.ELEMENT_NODE ||
                !"tech-list".equals(parentNode.getNodeName())) {
            return;
        }

        NodeList children = element.getChildNodes();
        if (children.getLength() != 1) {
            return;
        }
        Node child = children.item(0);
        if (child.getNodeType() != Node.TEXT_NODE) {
            // TODO: Warn if you have comment nodes etc too? Will probably also break inflater.
            return;
        }

        String text = child.getNodeValue();
        if (!text.equals(text.trim())) {
            String message = "There should not be any whitespace inside `<tech>` elements";
            context.report(ISSUE, element, context.getLocation(child), message);
        }
    }
}
