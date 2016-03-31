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

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_COLOR;
import static com.android.SdkConstants.ATTR_DRAWABLE;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_TYPE;
import static com.android.SdkConstants.COLOR_RESOURCE_PREFIX;
import static com.android.SdkConstants.DRAWABLE_PREFIX;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.NEW_ID_PREFIX;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TAG_COLOR;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import com.google.common.base.Joiner;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Checks for cycles in resource definitions
 */
public class ResourceCycleDetector extends ResourceXmlDetector {
    private static final Implementation IMPLEMENTATION = new Implementation(
            ResourceCycleDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Style parent cycles, resource alias cycles, layout include cycles, etc */
    public static final Issue CYCLE = Issue.create(
            "ResourceCycle", //$NON-NLS-1$
            "Cycle in resource definitions",
            "There should be no cycles in resource definitions as this can lead to runtime " +
            "exceptions.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION
    );

    /** Parent cycles */
    public static final Issue CRASH = Issue.create(
            "AaptCrash", //$NON-NLS-1$
            "Potential AAPT crash",
            "Defining a style which sets `android:id` to a dynamically generated id can cause " +
            "many versions of `aapt`, the resource packaging tool, to crash. To work around " +
            "this, declare the id explicitly with `<item type=\"id\" name=\"...\" />` instead.",
            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION)
            .addMoreInfo("https://code.google.com/p/android/issues/detail?id=20479"); //$NON-NLS-1$

    /**
     * For each resource type, a map from a key (style name, layout name, color name, etc) to
     * a value (parent style, included layout, referenced color, etc). Note that we only initialize
     * this if we are in "batch mode" (not editor incremental mode) since we allow this detector
     * to also run incrementally to look for trivial chains (e.g. of length 1).
     */
    private Map<ResourceType, Multimap<String, String>> mReferences;

    /**
     * If in batch analysis and cycles were found, in phase 2 this map should be initialized
     * with locations for declaration definitions of the keys and values in {@link #mReferences}
     */
    private Map<ResourceType, Multimap<String, Location>> mLocations;

    /**
     * If in batch analysis and cycles were found, for each resource type this is a list
     * of chains (where each chain is a list of keys as described in {@link #mReferences})
     */
    private Map<ResourceType, List<List<String>>> mChains;

    /** Constructs a new {@link ResourceCycleDetector} */
    public ResourceCycleDetector() {
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        // In incremental mode, or checking all files (full lint analysis) ? If the latter,
        // we should store state and look for deeper cycles
        if (context.getScope().contains(Scope.ALL_RESOURCE_FILES)) {
            mReferences = Maps.newEnumMap(ResourceType.class);
        }
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES
                || folderType == ResourceFolderType.COLOR
                || folderType == ResourceFolderType.DRAWABLE
                || folderType == ResourceFolderType.LAYOUT;
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                VIEW_INCLUDE,
                TAG_STYLE,
                TAG_COLOR,
                TAG_ITEM
        );
    }

    private void recordReference(@NonNull ResourceType type, @NonNull String from,
            @NonNull String to) {
        if (to.isEmpty() || to.startsWith(ANDROID_PREFIX)) {
            return;
        }
        assert mReferences != null;
        Multimap<String, String> map = mReferences.get(type);
        if (map == null) {
            // Multimap which preserves insert order (for predictable output order)
            map = Multimaps.newListMultimap(
                    new TreeMap<String, Collection<String>>(),
                    new Supplier<List<String>>() {
                        @Override
                        public List<String> get() {
                            return Lists.newArrayListWithExpectedSize(6);
                        }
                    });
            mReferences.put(type, map);
        }

        if (to.charAt(0) == '@') {
            int index = to.indexOf('/');
            if (index != -1) {
                to = to.substring(index + 1);
            }
        }

        map.put(from, to);
    }

    private void recordLocation(@NonNull XmlContext context, @NonNull Node node,
            @NonNull ResourceType type, @NonNull String from) {
        assert mLocations != null;
        // Cycles were already found; we're now in phase 2 looking up specific
        // locations
        Multimap<String, Location> map = mLocations.get(type);
        if (map == null) {
            map = ArrayListMultimap.create(30, 4);
            mLocations.put(type, map);
        }

        Location location = context.getLocation(node);
        map.put(from, location);
    }

    @SuppressWarnings("VariableNotUsedInsideIf")
    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tagName = element.getTagName();
        if (tagName.equals(TAG_ITEM)) {
            if (mReferences == null) {
                // Nothing to do in incremental mode
                return;
            }
            ResourceFolderType folderType = context.getResourceFolderType();
            if (folderType == ResourceFolderType.VALUES) {
                // Aliases
                Attr typeNode = element.getAttributeNode(ATTR_TYPE);
                if (typeNode != null) {
                    String typeName = typeNode.getValue();
                    ResourceType type = ResourceType.getEnum(typeName);
                    Attr nameNode = element.getAttributeNode(ATTR_NAME);
                    if (type != null && nameNode != null) {
                        NodeList childNodes = element.getChildNodes();
                        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                            Node child = childNodes.item(i);
                            if (child.getNodeType() == Node.TEXT_NODE) {
                                String text = child.getNodeValue();
                                for (int k = 0, max = text.length(); k < max; k++) {
                                    char c = text.charAt(k);
                                    if (Character.isWhitespace(c)) {
                                        break;
                                    } else if (c == '@' &&
                                            text.startsWith(type.getName(), k + 1)) {
                                        String to = text.trim();
                                        if (mReferences != null) {
                                            String name = nameNode.getValue();
                                            if (mLocations != null) {
                                                recordLocation(context, child, type,
                                                        name);
                                            } else {
                                                recordReference(type, name, to);
                                            }
                                        }
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (folderType == ResourceFolderType.COLOR ) {
                String color = element.getAttributeNS(ANDROID_URI, ATTR_COLOR);
                if (color != null && color.startsWith(COLOR_RESOURCE_PREFIX)) {
                    String currentColor = LintUtils.getBaseName(context.file.getName());
                    if (mLocations != null) {
                        recordLocation(context, element, ResourceType.COLOR,
                                currentColor);
                    } else {
                        recordReference(ResourceType.COLOR, currentColor,
                                color.substring(COLOR_RESOURCE_PREFIX.length()));
                    }
                }
            } else if (folderType == ResourceFolderType.DRAWABLE) {
                String drawable = element.getAttributeNS(ANDROID_URI, ATTR_DRAWABLE);
                if (drawable != null && drawable.startsWith(DRAWABLE_PREFIX)) {
                    String currentColor = LintUtils.getBaseName(context.file.getName());
                    if (mLocations != null) {
                        recordLocation(context, element, ResourceType.DRAWABLE,
                                currentColor);
                    } else {
                        recordReference(ResourceType.DRAWABLE, currentColor,
                                drawable.substring(DRAWABLE_PREFIX.length()));
                    }
                }
            }
        } else if (tagName.equals(TAG_STYLE)) {
            Attr nameNode = element.getAttributeNode(ATTR_NAME);
            // Look for recursive style parent declarations
            Attr parentNode = element.getAttributeNode(ATTR_PARENT);
            if (parentNode != null && nameNode != null) {
                String name = nameNode.getValue();
                String parent = parentNode.getValue();
                if (parent.endsWith(name) &&
                        parent.equals(STYLE_RESOURCE_PREFIX + name) && context.isEnabled(CYCLE)
                        && context.getDriver().getPhase() == 1) {
                    context.report(CYCLE, parentNode, context.getLocation(parentNode),
                            String.format("Style `%1$s` should not extend itself", name));
                } else if (parent.startsWith(STYLE_RESOURCE_PREFIX)
                        && parent.startsWith(name, STYLE_RESOURCE_PREFIX.length())
                        && parent.startsWith(".", STYLE_RESOURCE_PREFIX.length() + name.length())
                        && context.isEnabled(CYCLE) && context.getDriver().getPhase() == 1) {
                    context.report(CYCLE, parentNode, context.getLocation(parentNode),
                        String.format("Potential cycle: `%1$s` is the implied parent of `%2$s` and " +
                                    "this defines the opposite", name,
                                    parent.substring(STYLE_RESOURCE_PREFIX.length())));
                    // Don't record this reference; we don't want to double report this
                    // as a chain, since this error is more helpful
                    return;
                }
                if (mReferences != null && !parent.isEmpty()) {
                    if (mLocations != null) {
                        recordLocation(context, parentNode, ResourceType.STYLE, name);
                    } else {
                        recordReference(ResourceType.STYLE, name, parent);
                    }
                }
            } else if (mReferences != null && nameNode != null) {
                String name = nameNode.getValue();
                int index = name.lastIndexOf('.');
                if (index > 0) {
                    String parent = name.substring(0, index);
                    if (mReferences != null) {
                        if (mLocations != null) {
                            Attr node = element.getAttributeNode(ATTR_NAME);
                            recordLocation(context, node, ResourceType.STYLE, name);
                        } else {
                            recordReference(ResourceType.STYLE, name, parent);
                        }
                    }
                }
            }

            if (context.isEnabled(CRASH) && context.getDriver().getPhase() == 1) {
                for (Element item : LintUtils.getChildren(element)) {
                    if ("android:id".equals(item.getAttribute(ATTR_NAME))) {
                        checkCrashItem(context, item);
                    }
                }
            }
        } else if (tagName.equals(VIEW_INCLUDE)) {
            Attr layoutNode = element.getAttributeNode(ATTR_LAYOUT);
            if (layoutNode != null) {
                String layout = layoutNode.getValue();
                if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) {
                    String currentLayout = LintUtils.getBaseName(context.file.getName());
                    if (mReferences != null) {
                        if (mLocations != null) {
                            recordLocation(context, layoutNode, ResourceType.LAYOUT,
                                    currentLayout);
                        } else {
                            recordReference(ResourceType.LAYOUT, currentLayout, layout);
                        }
                    }
                    if (layout.startsWith(currentLayout, LAYOUT_RESOURCE_PREFIX.length()) &&
                            layout.length() == currentLayout.length()
                                    + LAYOUT_RESOURCE_PREFIX.length()
                            && context.isEnabled(CYCLE)
                            && context.getDriver().getPhase() == 1) {
                        String message = String.format("Layout `%1$s` should not include itself",
                                currentLayout);
                        context.report(CYCLE, layoutNode, context.getLocation(layoutNode),
                                message);
                    }
                }
            }
        } else if (tagName.equals(TAG_COLOR)) {
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node child = childNodes.item(i);
                if (child.getNodeType() == Node.TEXT_NODE) {
                    String text = child.getNodeValue();
                    for (int k = 0, max = text.length(); k < max; k++) {
                        char c = text.charAt(k);
                        if (Character.isWhitespace(c)) {
                            break;
                        } else if (text.startsWith(COLOR_RESOURCE_PREFIX, k)) {
                            String color = text.trim().substring(COLOR_RESOURCE_PREFIX.length());
                            String name = element.getAttribute(ATTR_NAME);
                            if (mReferences != null) {
                                if (mLocations != null) {
                                    recordLocation(context, child, ResourceType.COLOR, name);
                                } else {
                                    recordReference(ResourceType.COLOR, name, color);
                                }
                            }
                            if (color.equals(name)
                                    && context.isEnabled(CYCLE)
                                    && context.getDriver().getPhase() == 1) {
                                context.report(CYCLE, child, context.getLocation(child),
                                        String.format("Color `%1$s` should not reference itself",
                                                color));
                            }
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }

    private static void checkCrashItem(@NonNull XmlContext context, @NonNull Element item) {
        NodeList childNodes = item.getChildNodes();
        for (int i = 0, n = childNodes.getLength(); i < n; i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                String text = child.getNodeValue();

                for (int k = 0, max = text.length(); k < max; k++) {
                    char c = text.charAt(k);
                    if (Character.isWhitespace(c)) {
                        return;
                    } else if (text.startsWith(NEW_ID_PREFIX, k)) {
                        String name = text.trim().substring(NEW_ID_PREFIX.length());
                        String message = "This construct can potentially crash `aapt` during a "
                                + "build. Change `@+id/" + name + "` to `@id/" + name + "` and define "
                                + "the id explicitly using "
                                + "`<item type=\"id\" name=\"" + name + "\"/>` instead.";
                        context.report(CRASH, item, context.getLocation(item),
                                message);
                    } else {
                        return;
                    }
                }
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mReferences == null) {
            // Incremental analysis in a single file only; nothing to do
            return;
        }

        int phase = context.getDriver().getPhase();
        if (phase == 1) {
            // Perform DFS of each resource type and look for cycles
            for (Map.Entry<ResourceType, Multimap<String, String>> entry
                    : mReferences.entrySet()) {
                ResourceType type = entry.getKey();
                Multimap<String, String> map = entry.getValue();
                findCycles(context, type, map);
            }
        } else {
            assert phase == 2;
            // Emit cycle report
            for (Map.Entry<ResourceType, List<List<String>>> entry : mChains.entrySet()) {
                ResourceType type = entry.getKey();
                Multimap<String, Location> locations = mLocations.get(type);
                if (locations == null) {
                    // No locations found. Unlikely.
                    locations = ArrayListMultimap.create();
                }
                List<List<String>> chains = entry.getValue();
                for (List<String> chain : chains) {
                    Location location = null;
                    assert !chain.isEmpty();
                    for (int i = 0, n = chain.size(); i < n; i++) {
                        String item = chain.get(i);
                        Collection<Location> itemLocations = locations.get(item);
                        if (!itemLocations.isEmpty()) {
                            Location itemLocation = itemLocations.iterator().next();
                            String next = chain.get((i + 1) % chain.size());
                            String label = "Reference from @" + type.getName() + "/" + item
                                    + " to " + type.getName() + "/" + next + " here";
                            itemLocation.setMessage(label);
                            itemLocation.setSecondary(location);
                            location = itemLocation;
                        }
                    }

                    if (location == null) {
                        location = Location.create(context.getProject().getDir());
                    } else {
                        // Break off chain
                        Location curr = location.getSecondary();
                        while (curr != null) {
                            Location next = curr.getSecondary();
                            if (next == location) {
                                curr.setSecondary(null);
                                break;
                            }
                            curr = next;
                        }
                    }

                    String message = String.format("%1$s Resource definition cycle: %2$s",
                            type.getDisplayName(), Joiner.on(" => ").join(chain));

                    context.report(CYCLE, location, message);
                }
            }
        }
    }

    private void findCycles(
            @NonNull Context context,
            @NonNull ResourceType type,
            @NonNull Multimap<String, String> map) {
        Set<String> visiting = Sets.newHashSetWithExpectedSize(map.size());
        Set<String> seen = Sets.newHashSetWithExpectedSize(map.size());
        for (String from : map.keySet()) {
            if (seen.contains(from)) {
                continue;
            }
            List<String> chain = dfs(map, from, visiting);
            if (chain != null && chain.size() > 2) { // size 1 chains are handled directly
                seen.addAll(chain);
                Collections.reverse(chain);
                if (mChains == null) {
                    mChains = Maps.newEnumMap(ResourceType.class);
                    mLocations = Maps.newEnumMap(ResourceType.class);
                    context.getDriver().requestRepeat(this, Scope.RESOURCE_FILE_SCOPE);
                }
                List<List<String>> list = mChains.get(type);
                if (list == null) {
                    list = Lists.newArrayList();
                    mChains.put(type, list);
                }
                list.add(chain);
            }
        }
    }

    // ----- Cycle detection -----

    @Nullable
    private static List<String> dfs(
            @NonNull Multimap<String, String> map,
            @NonNull String from,
            @NonNull Set<String> visiting) {
        visiting.add(from);

        Collection<String> targets = map.get(from);
        if (targets != null && !targets.isEmpty()) {
            for (String target : targets) {
                if (visiting.contains(target)) {
                    List<String> chain = Lists.newArrayList();
                    chain.add(target);
                    chain.add(from);
                    return chain;
                }
                List<String> chain = dfs(map, target, visiting);
                if (chain != null) {
                    chain.add(from);
                    return chain;
                }
            }
        }

        visiting.remove(from);

        return null;
    }
}
