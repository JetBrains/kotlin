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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.List;

/**
 * Check which makes sure that a full-backup-content descriptor file is valid and logical
 */
public class FullBackupContentDetector extends ResourceXmlDetector implements JavaScanner {
    /**
     * Validation of {@code <full-backup-content>} XML elements
     */
    public static final Issue ISSUE = Issue.create(
            "FullBackupContent", //$NON-NLS-1$
            "Valid Full Backup Content File",

            "Ensures that a `<full-backup-content>` file, which is pointed to by a " +
            "`android:fullBackupContent attribute` in the manifest file, is valid",

            Category.CORRECTNESS,
            5,
            Severity.FATAL,
            new Implementation(
                    FullBackupContentDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    @SuppressWarnings("SpellCheckingInspection")
    private static final String DOMAIN_SHARED_PREF = "sharedpref";
    private static final String DOMAIN_ROOT = "root";
    private static final String DOMAIN_FILE = "file";
    private static final String DOMAIN_DATABASE = "database";
    private static final String DOMAIN_EXTERNAL = "external";
    private static final String TAG_EXCLUDE = "exclude";
    private static final String TAG_INCLUDE = "include";
    private static final String TAG_FULL_BACKUP_CONTENT = "full-backup-content";
    private static final String ATTR_PATH = "path";
    private static final String ATTR_DOMAIN = "domain";

    /**
     * Constructs a new {@link FullBackupContentDetector}
     */
    public FullBackupContentDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.XML;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }
        if (!TAG_FULL_BACKUP_CONTENT.equals(root.getTagName())) {
            return;
        }

        List<Element> includes = Lists.newArrayList();
        List<Element> excludes = Lists.newArrayList();
        NodeList children = root.getChildNodes();
        for (int i = 0, n = children.getLength(); i < n; i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) child;
                String tag = element.getTagName();
                if (TAG_INCLUDE.equals(tag)) {
                    includes.add(element);
                } else if (TAG_EXCLUDE.equals(tag)) {
                    excludes.add(element);
                } else {
                    // See FullBackup#validateInnerTagContents
                    context.report(ISSUE, element, context.getNameLocation(element),
                            String.format("Unexpected element `<%1$s>`", tag));
                }
            }
        }

        Multimap<String, String> includePaths = ArrayListMultimap.create(includes.size(), 4);
        for (Element include : includes) {
            String domain = validateDomain(context, include);
            String path = validatePath(context, include);
            if (domain == null) {
                continue;
            }
            includePaths.put(domain, path);
        }

        for (Element exclude : excludes) {
            String excludePath = validatePath(context, exclude);
            if (excludePath.isEmpty()) {
                continue;
            }
            String domain = validateDomain(context, exclude);
            if (domain == null) {
                continue;
            }
            if (includePaths.isEmpty()) {
                // There is no <include> anywhere: that means that everything
                // is considered included and there's no potential prefix mismatch
                continue;
            }

            boolean hasPrefix = false;
            Collection<String> included = includePaths.get(domain);
            if (included == null) {
                continue;
            }
            for (String includePath : included) {
                if (excludePath.startsWith(includePath)) {
                    if (excludePath.equals(includePath)) {
                        Attr pathNode = exclude.getAttributeNode(ATTR_PATH);
                        assert pathNode != null;
                        Location location = context.getValueLocation(pathNode);
                        // Find corresponding include path so we can link to it in the
                        // chained location list
                        for (Element include : includes) {
                            Attr includePathNode = include.getAttributeNode(ATTR_PATH);
                            String includeDomain = include.getAttribute(ATTR_DOMAIN);
                            if (includePathNode != null
                                    && excludePath.equals(includePathNode.getValue())
                                    && domain.equals(includeDomain)) {
                                Location earlier = context.getLocation(includePathNode);
                                earlier.setMessage("Unnecessary/conflicting <include>");
                                location.setSecondary(earlier);
                            }
                        }
                        context.report(ISSUE, exclude, location,
                                String.format("Include `%1$s` is also excluded", excludePath));
                    }
                    hasPrefix = true;
                    break;
                }
            }
            if (!hasPrefix) {
                Attr pathNode = exclude.getAttributeNode(ATTR_PATH);
                assert pathNode != null;
                context.report(ISSUE, exclude, context.getValueLocation(pathNode),
                        String.format("`%1$s` is not in an included path", excludePath));
            }
        }
    }

    @NonNull
    private static String validatePath(@NonNull XmlContext context, @NonNull Element element) {
        Attr pathNode = element.getAttributeNode(ATTR_PATH);
        if (pathNode == null) {
            return "";
        }
        String value = pathNode.getValue();
        if (value.contains("//")) {
            context.report(ISSUE, element, context.getValueLocation(pathNode),
                    "Paths are not allowed to contain `//`");
        } else if (value.contains("..")) {
            context.report(ISSUE, element, context.getValueLocation(pathNode),
                    "Paths are not allowed to contain `..`");
        } else if (value.contains("/")) {
            String domain = element.getAttribute(ATTR_DOMAIN);
            if (DOMAIN_SHARED_PREF.equals(domain) || DOMAIN_DATABASE.equals(domain)) {
                context.report(ISSUE, element, context.getValueLocation(pathNode),
                        String.format("Subdirectories are not allowed for domain `%1$s`",
                                domain));
            }
        }
        return value;
    }

    @Nullable
    private static String validateDomain(@NonNull XmlContext context, @NonNull Element element) {
        Attr domainNode = element.getAttributeNode(ATTR_DOMAIN);
        if (domainNode == null) {
            context.report(ISSUE, element, context.getLocation(element),
                String.format("Missing domain attribute, expected one of %1$s",
                        Joiner.on(", ").join(VALID_DOMAINS)));
            return null;
        }
        String domain = domainNode.getValue();
        for (String availableDomain : VALID_DOMAINS) {
            if (availableDomain.equals(domain)) {
                return domain;
            }
        }
        context.report(ISSUE, element, context.getValueLocation(domainNode),
                String.format("Unexpected domain `%1$s`, expected one of %2$s", domain,
                        Joiner.on(", ").join(VALID_DOMAINS)));

        return domain;
    }

    /** Valid domains; see FullBackup#getTokenForXmlDomain for authoritative list */
    private static final String[] VALID_DOMAINS = new String[] {
            DOMAIN_ROOT, DOMAIN_FILE, DOMAIN_DATABASE, DOMAIN_SHARED_PREF, DOMAIN_EXTERNAL
    };
}
