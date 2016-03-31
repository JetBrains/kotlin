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

import static com.android.SdkConstants.ANDROID_URI;

import com.android.annotations.NonNull;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks for unreachable states in an Android state list definition
 */
public class StateListDetector extends ResourceXmlDetector {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "StateListReachable", //$NON-NLS-1$
            "Unreachable state in a `<selector>`",
            "In a selector, only the last child in the state list should omit a " +
            "state qualifier. If not, all subsequent items in the list will be ignored " +
            "since the given item will match all.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(
                    StateListDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    private static final String STATE_PREFIX = "state_"; //$NON-NLS-1$

    /** Constructs a new {@link StateListDetector} */
    public StateListDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.DRAWABLE;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        // TODO: Look for views that don't specify
        // Display the error token somewhere so it can be suppressed
        // Emit warning at the end "run with --help to learn how to suppress types of errors/checks";
        // ("...and this message.")

        Element root = document.getDocumentElement();
        if (root != null && root.getTagName().equals("selector")) { //$NON-NLS-1$
            List<Element> children = LintUtils.getChildren(root);
            Map<Element, Set<String>> states =
                    new HashMap<Element, Set<String>>(children.size());

            for (int i = 0; i < children.size(); i++) {
                Element child = children.get(i);
                NamedNodeMap attributes = child.getAttributes();
                Set<String> stateNames = new HashSet<String>(attributes.getLength());
                states.put(child, stateNames);

                for (int j = 0; j < attributes.getLength(); j++) {
                    Attr attribute = (Attr) attributes.item(j);
                    String name = attribute.getLocalName();
                    if (name == null) {
                        continue;
                    }
                    if (name.startsWith(STATE_PREFIX)) {
                        stateNames.add(name + '=' + attribute.getValue());
                    } else {
                        String namespaceUri = attribute.getNamespaceURI();
                        if (namespaceUri != null && !namespaceUri.isEmpty() &&
                                !ANDROID_URI.equals(namespaceUri)) {
                            // There is a custom attribute on this item.
                            // This could be a state, see
                            //   http://code.google.com/p/android/issues/detail?id=22339
                            // so don't flag this one.
                            stateNames.add(attribute.getName() + '=' + attribute.getValue());
                        }
                    }
                }
            }

            // See if for each state, any subsequent state fully contains all the same
            // state requirements

            for (int i = 0; i < children.size() - 1; i++) {
                Element prev = children.get(i);
                Set<String> prevStates = states.get(prev);
                assert prevStates != null : prev;
                for (int j = i + 1; j < children.size(); j++) {
                    Element current = children.get(j);
                    Set<String> currentStates = states.get(current);
                    assert currentStates != null : current;
                    if (currentStates.containsAll(prevStates)) {
                        Location location = context.getLocation(current);
                        Location secondary = context.getLocation(prev);
                        secondary.setMessage("Earlier item which masks item");
                        location.setSecondary(secondary);
                        context.report(ISSUE, current, location, String.format(
                                "This item is unreachable because a previous item (item #%1$d) is a more general match than this one",
                                i + 1));
                        // Don't keep reporting errors for all the remaining cases in this file
                        return;
                    }
                }
            }
        }
    }
}
