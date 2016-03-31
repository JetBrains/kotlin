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
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.FD_RES_VALUES;
import static com.android.SdkConstants.ID_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.RELATIVE_LAYOUT;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.VALUE_ID;
import static com.android.tools.lint.detector.api.LintUtils.editDistance;
import static com.android.tools.lint.detector.api.LintUtils.getChildren;
import static com.android.tools.lint.detector.api.LintUtils.isSameResourceFile;
import static com.android.tools.lint.detector.api.LintUtils.stripIdPrefix;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.Handle;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks for duplicate ids within a layout and within an included layout
 */
public class WrongIdDetector extends LayoutDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            WrongIdDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Ids bound to widgets in any of the layout files */
    private final Set<String> mGlobalIds = new HashSet<String>(100);

    /** Ids bound to widgets in the current layout file */
    private Set<String> mFileIds;

    /** Ids declared in a value's file, e.g. {@code <item type="id" name="foo"/>} */
    private Set<String> mDeclaredIds;

    /**
     * Location handles for the various id references that were not found as
     * defined in the same layout, to be checked after the whole project has
     * been scanned
     */
    private List<Pair<String, Location.Handle>> mHandles;

    /** List of RelativeLayout elements in the current layout */
    private List<Element> mRelativeLayouts;

    /** Reference to an unknown id */
    @SuppressWarnings("unchecked")
    public static final Issue UNKNOWN_ID = Issue.create(
            "UnknownId", //$NON-NLS-1$
            "Reference to an unknown id",
            "The `@+id/` syntax refers to an existing id, or creates a new one if it has " +
            "not already been defined elsewhere. However, this means that if you have a " +
            "typo in your reference, or if the referred view no longer exists, you do not " +
            "get a warning since the id will be created on demand. This check catches " +
            "errors where you have renamed an id without updating all of the references to " +
            "it.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            new Implementation(
                    WrongIdDetector.class,
                    Scope.ALL_RESOURCES_SCOPE,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Reference to an id that is not a sibling */
    public static final Issue NOT_SIBLING = Issue.create(
            "NotSibling", //$NON-NLS-1$
            "RelativeLayout Invalid Constraints",
            "Layout constraints in a given `RelativeLayout` should reference other views " +
            "within the same relative layout (but not itself!)",
            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** An ID declaration which is not valid */
    public static final Issue INVALID = Issue.create(
            "InvalidId", //$NON-NLS-1$
            "Invalid ID declaration",
            "An id definition *must* be of the form `@+id/yourname`. The tools have not " +
            "rejected strings of the form `@+foo/bar` in the past, but that was an error, " +
            "and could lead to tricky errors because of the way the id integers are assigned.\n" +
            "\n" +
            "If you really want to have different \"scopes\" for your id's, use prefixes " +
            "instead, such as `login_button1` and `login_button2`.",
            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Reference to an id that is not in the current layout */
    public static final Issue UNKNOWN_ID_LAYOUT = Issue.create(
            "UnknownIdInLayout", //$NON-NLS-1$
            "Reference to an id that is not in the current layout",

            "The `@+id/` syntax refers to an existing id, or creates a new one if it has " +
            "not already been defined elsewhere. However, this means that if you have a " +
            "typo in your reference, or if the referred view no longer exists, you do not " +
            "get a warning since the id will be created on demand.\n" +
            "\n" +
            "This is sometimes intentional, for example where you are referring to a view " +
            "which is provided in a different layout via an include. However, it is usually " +
            "an accident where you have a typo or you have renamed a view without updating " +
            "all the references to it.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            new Implementation(
                    WrongIdDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    /** Constructs a duplicate id check */
    public WrongIdDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT || folderType == ResourceFolderType.VALUES;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return Collections.singletonList(ATTR_ID);
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(RELATIVE_LAYOUT, TAG_ITEM);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mFileIds = new HashSet<String>();
        mRelativeLayouts = null;
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        if (mRelativeLayouts != null) {
            if (!context.getProject().getReportIssues()) {
                // If this is a library project not being analyzed, ignore it
                return;
            }

            for (Element layout : mRelativeLayouts) {
                List<Element> children = getChildren(layout);
                Set<String> ids = Sets.newHashSetWithExpectedSize(children.size());
                for (Element child : children) {
                    String id = child.getAttributeNS(ANDROID_URI, ATTR_ID);
                    if (id != null && !id.isEmpty()) {
                        ids.add(id);
                    }
                }

                for (Element element : children) {
                    String selfId = stripIdPrefix(element.getAttributeNS(ANDROID_URI, ATTR_ID));

                    NamedNodeMap attributes = element.getAttributes();
                    for (int i = 0, n = attributes.getLength(); i < n; i++) {
                        Attr attr = (Attr) attributes.item(i);
                        String value = attr.getValue();
                        if ((value.startsWith(NEW_ID_PREFIX) ||
                                value.startsWith(ID_PREFIX))
                                && ANDROID_URI.equals(attr.getNamespaceURI())
                                && attr.getLocalName().startsWith(ATTR_LAYOUT_RESOURCE_PREFIX)) {
                            if (!idDefined(mFileIds, value)) {
                                // Stash a reference to this id and location such that
                                // we can check after the *whole* layout has been processed,
                                // since it's too early to conclude here that the id does
                                // not exist (you are allowed to have forward references)
                                XmlContext xmlContext = (XmlContext) context;
                                Handle handle = xmlContext.createLocationHandle(attr);
                                handle.setClientData(attr);

                                if (mHandles == null) {
                                    mHandles = new ArrayList<Pair<String,Handle>>();
                                }
                                mHandles.add(Pair.of(value, handle));
                            } else {
                                // Check siblings. TODO: Look for cycles!
                                if (ids.contains(value)) {
                                    // Make sure it's not pointing to self
                                    if (!ATTR_ID.equals(attr.getLocalName())
                                            && !selfId.isEmpty()
                                            && value.endsWith(selfId)
                                            && stripIdPrefix(value).equals(selfId)) {
                                        XmlContext xmlContext = (XmlContext) context;
                                        String message = String.format(
                                                "Cannot be relative to self: id=%1$s, %2$s=%3$s",
                                                selfId, attr.getLocalName(), selfId);
                                        Location location = xmlContext.getLocation(attr);
                                        xmlContext.report(NOT_SIBLING, attr, location, message);
                                    }

                                    continue;
                                }
                                if (value.startsWith(NEW_ID_PREFIX)) {
                                    if (ids.contains(ID_PREFIX + stripIdPrefix(value))) {
                                        continue;
                                    }
                                } else {
                                    assert value.startsWith(ID_PREFIX) : value;
                                    if (ids.contains(NEW_ID_PREFIX + stripIdPrefix(value))) {
                                        continue;
                                    }
                                }
                                if (context.isEnabled(NOT_SIBLING)) {
                                    XmlContext xmlContext = (XmlContext) context;
                                    String message = String.format(
                                            "`%1$s` is not a sibling in the same `RelativeLayout`",
                                            value);
                                    Location location = xmlContext.getLocation(attr);
                                    xmlContext.report(NOT_SIBLING, attr, location, message);
                                }
                            }
                        }
                    }
                }
            }
        }

        mFileIds = null;

        if (!context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
            checkHandles(context);
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
            checkHandles(context);
        }
    }

    private void checkHandles(@NonNull Context context) {
        if (mHandles != null) {
            boolean checkSameLayout = context.isEnabled(UNKNOWN_ID_LAYOUT);
            boolean checkExists = context.isEnabled(UNKNOWN_ID);
            boolean projectScope = context.getScope().contains(Scope.ALL_RESOURCE_FILES);
            for (Pair<String, Handle> pair : mHandles) {
                String id = pair.getFirst();
                boolean isBound = projectScope ? idDefined(mGlobalIds, id) :
                        idDefined(context, id, context.file);
                LintClient client = context.getClient();
                if (!isBound && checkExists
                        && (projectScope || client.supportsProjectResources())) {
                    Handle handle = pair.getSecond();
                    boolean isDeclared = idDefined(mDeclaredIds, id);
                    id = stripIdPrefix(id);
                    String suggestionMessage;
                    Set<String> spellingDictionary = mGlobalIds;
                    if (!projectScope && client.supportsProjectResources()) {
                        AbstractResourceRepository resources =
                                client.getProjectResources(context.getProject(), true);
                        if (resources != null) {
                            spellingDictionary = Sets.newHashSet(
                                    resources.getItemsOfType(ResourceType.ID));
                            spellingDictionary.remove(id);
                        }
                    }
                    List<String> suggestions = getSpellingSuggestions(id, spellingDictionary);
                    if (suggestions.size() > 1) {
                        suggestionMessage = String.format(" Did you mean one of {%2$s} ?",
                                id, Joiner.on(", ").join(suggestions));
                    } else if (!suggestions.isEmpty()) {
                        suggestionMessage = String.format(" Did you mean %2$s ?",
                                id, suggestions.get(0));
                    } else {
                        suggestionMessage = "";
                    }
                    String message;
                    if (isDeclared) {
                        message = String.format(
                                "The id \"`%1$s`\" is defined but not assigned to any views.%2$s",
                                id, suggestionMessage);
                    } else {
                        message = String.format(
                                "The id \"`%1$s`\" is not defined anywhere.%2$s",
                                id, suggestionMessage);
                    }
                    report(context, UNKNOWN_ID, handle, message);
                } else if (checkSameLayout && (!projectScope || isBound)
                        && id.startsWith(NEW_ID_PREFIX)) {
                    // The id was defined, but in a different layout. Usually not intentional
                    // (might be referring to a random other view that happens to have the same
                    // name.)
                    Handle handle = pair.getSecond();
                    report(context, UNKNOWN_ID_LAYOUT, handle,
                            String.format(
                                    "The id \"`%1$s`\" is not referring to any views in this layout",
                                    stripIdPrefix(id)));
                }
            }
        }
    }

    private static void report(Context context, Issue issue, Handle handle, String message) {
        Location location = handle.resolve();
        Object clientData = handle.getClientData();
        if (clientData instanceof Node) {
            if (context.getDriver().isSuppressed(null, issue, (Node) clientData)) {
                return;
            }
        }

        context.report(issue, location, message);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (element.getTagName().equals(RELATIVE_LAYOUT)) {
            if (mRelativeLayouts == null) {
                mRelativeLayouts = new ArrayList<Element>();
            }
            mRelativeLayouts.add(element);
        } else {
            assert element.getTagName().equals(TAG_ITEM);
            String type = element.getAttribute(ATTR_TYPE);
            if (VALUE_ID.equals(type)) {
                String name = element.getAttribute(ATTR_NAME);
                if (!name.isEmpty()) {
                    if (mDeclaredIds == null) {
                        mDeclaredIds = Sets.newHashSet();
                    }
                    mDeclaredIds.add(ID_PREFIX + name);
                }
            }
        }
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        assert attribute.getName().equals(ATTR_ID) || attribute.getLocalName().equals(ATTR_ID);
        String id = attribute.getValue();
        mFileIds.add(id);
        mGlobalIds.add(id);

        if (id.equals(NEW_ID_PREFIX) || id.equals(ID_PREFIX) || "@+id".equals(ID_PREFIX)) {
            String message = "Invalid id: missing value";
            context.report(INVALID, attribute, context.getLocation(attribute), message);
        } else if (id.startsWith("@+") && !id.startsWith(NEW_ID_PREFIX) //$NON-NLS-1$
                && !id.startsWith("@+android:id/")  //$NON-NLS-1$
                || id.startsWith(NEW_ID_PREFIX)
                && id.indexOf('/', NEW_ID_PREFIX.length()) != -1) {
            int nameStart = id.startsWith(NEW_ID_PREFIX) ? NEW_ID_PREFIX.length() : 2;
            String suggested = NEW_ID_PREFIX + id.substring(nameStart).replace('/', '_');
            String message = String.format(
                    "ID definitions *must* be of the form `@+id/name`; try using `%1$s`", suggested);
            context.report(INVALID, attribute, context.getLocation(attribute), message);
        }
    }

    private static boolean idDefined(Set<String> ids, String id) {
        if (ids == null) {
            return false;
        }
        boolean definedLocally = ids.contains(id);
        if (!definedLocally) {
            if (id.startsWith(NEW_ID_PREFIX)) {
                definedLocally = ids.contains(ID_PREFIX +
                        id.substring(NEW_ID_PREFIX.length()));
            } else if (id.startsWith(ID_PREFIX)) {
                definedLocally = ids.contains(NEW_ID_PREFIX +
                        id.substring(ID_PREFIX.length()));
            }
        }

        return definedLocally;
    }

    private boolean idDefined(@NonNull Context context, @NonNull String id,
            @Nullable File notIn) {
        AbstractResourceRepository resources =
                context.getClient().getProjectResources(context.getProject(), true);
        if (resources != null) {
            List<ResourceItem> items = resources.getResourceItem(ResourceType.ID,
                    stripIdPrefix(id));
            if (items == null || items.isEmpty()) {
                return false;
            }
            for (ResourceItem item : items) {
                ResourceFile source = item.getSource();
                if (source != null) {
                    File file = source.getFile();
                    if (file.getParentFile().getName().startsWith(FD_RES_VALUES)) {
                        if (mDeclaredIds == null) {
                            mDeclaredIds = Sets.newHashSet();
                        }
                        mDeclaredIds.add(id);
                        continue;
                    }

                    // Ignore definitions in the given file. This is used to ignore
                    // matches in the same file as the reference, since the reference
                    // is often expressed as a definition
                    if (!isSameResourceFile(file, notIn)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static List<String> getSpellingSuggestions(String id, Collection<String> ids) {
        int maxDistance = id.length() >= 4 ? 2 : 1;

        // Look for typos and try to match with custom views and android views
        Multimap<Integer, String> matches = ArrayListMultimap.create(2, 10);
        int count = 0;
        if (!ids.isEmpty()) {
            for (String matchWith : ids) {
                matchWith = stripIdPrefix(matchWith);
                if (Math.abs(id.length() - matchWith.length()) > maxDistance) {
                    // The string lengths differ more than the allowed edit distance;
                    // no point in even attempting to compute the edit distance (requires
                    // O(n*m) storage and O(n*m) speed, where n and m are the string lengths)
                    continue;
                }
                int distance = editDistance(id, matchWith);
                if (distance <= maxDistance) {
                    matches.put(distance, matchWith);
                }

                if (count++ > 100) {
                    // Make sure that for huge projects we don't completely grind to a halt
                    break;
                }
            }
        }

        for (int i = 0; i < maxDistance; i++) {
            Collection<String> strings = matches.get(i);
            if (strings != null && !strings.isEmpty()) {
                List<String> suggestions = new ArrayList<String>(strings);
                Collections.sort(suggestions);
                return suggestions;
            }
        }

        return Collections.emptyList();
    }
}
