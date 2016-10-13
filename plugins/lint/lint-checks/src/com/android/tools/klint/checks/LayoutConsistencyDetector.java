/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.tools.klint.detector.api.LintUtils.stripIdPrefix;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.klint.client.api.LintDriver;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LayoutDetector;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElement;

import org.jetbrains.uast.UElement;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks for consistency in layouts across different resource folders
 */
public class LayoutConsistencyDetector extends LayoutDetector implements Detector.UastScanner {

    /** Map from layout resource names to a list of files defining that resource,
     * and within each file the value is a map from string ids to the widget type
     * used by that id in this file */
    private final Map<String, List<Pair<File, Map<String, String>>>> mMap =
            Maps.newHashMapWithExpectedSize(64);

    /** Ids referenced from .java files. Only ids referenced from code are considered
     * vital to be consistent among the layout variations (others could just have ids
     * assigned to them in the layout either automatically by the layout editor or there
     * in order to support RelativeLayout constraints etc, but not be problematic
     * in findViewById calls.)
     */
    private final Set<String> mRelevantIds = Sets.newLinkedHashSetWithExpectedSize(64);

    /** Map from layout to id name to a list of locations */
    private Map<String, Map<String, List<Location>>> mLocations;

    /** Map from layout to id name to the error message to display for each */
    private Map<String, Map<String, String>> mErrorMessages;

    /** Inconsistent widget types */
    public static final Issue INCONSISTENT_IDS = Issue.create(
            "InconsistentLayout", //$NON-NLS-1$
            "Inconsistent Layouts",

            "This check ensures that a layout resource which is defined in multiple "
            + "resource folders, specifies the same set of widgets.\n"
            + "\n"
            + "This finds cases where you have accidentally forgotten to add "
            + "a widget to all variations of the layout, which could result "
            + "in a runtime crash for some resource configurations when a "
            + "`findViewById()` fails.\n"
            + "\n"
            + "There *are* cases where this is intentional. For example, you "
            + "may have a dedicated large tablet layout which adds some extra "
            + "widgets that are not present in the phone version of the layout. "
            + "As long as the code accessing the layout resource is careful to "
            + "handle this properly, it is valid. In that case, you can suppress "
            + "this lint check for the given extra or missing views, or the whole "
            + "layout",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    LayoutConsistencyDetector.class,
                    Scope.JAVA_AND_RESOURCE_FILES));

    /** Constructs a consistency check */
    public LayoutConsistencyDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.LAYOUT;
    }

    @Override
    public void visitDocument(@NonNull XmlContext context, @NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root != null) {
            if (context.getPhase() == 1) {
                // Map from ids to types
                Map<String,String> fileMap = Maps.newHashMapWithExpectedSize(10);
                addIds(root, fileMap);

                getFileMapList(context).add(Pair.of(context.file, fileMap));
            } else {
                String name = LintUtils.getLayoutName(context.file);
                Map<String, List<Location>> map = mLocations.get(name);
                if (map != null) {
                    lookupLocations(context, root, map);
                }
            }
        }
    }

    @NonNull
    private List<Pair<File, Map<String, String>>> getFileMapList(
            @NonNull XmlContext context) {
        String name = LintUtils.getLayoutName(context.file);
        List<Pair<File, Map<String, String>>> list = mMap.get(name);
        if (list == null) {
            list = Lists.newArrayListWithCapacity(4);
            mMap.put(name, list);
        }
        return list;
    }

    @Nullable
    private static String getId(@NonNull Element element) {
        String id = element.getAttributeNS(ANDROID_URI, ATTR_ID);
        if (id != null && !id.isEmpty() && !id.startsWith(ANDROID_PREFIX)) {
            return stripIdPrefix(id);
        }
        return null;
    }

    private static void addIds(Element element, Map<String,String> map) {
        String id = getId(element);
        if (id != null) {
            String s = stripIdPrefix(id);
            map.put(s, element.getTagName());
        }

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                addIds((Element) child, map);
            }
        }
    }

    private static void lookupLocations(
            @NonNull XmlContext context,
            @NonNull Element element,
            @NonNull Map<String, List<Location>> map) {
        String id = getId(element);
        if (id != null) {
            if (map.containsKey(id)) {
                if (context.getDriver().isSuppressed(context, INCONSISTENT_IDS, element)) {
                    map.remove(id);
                    return;
                }

                List<Location> locations = map.get(id);
                if (locations == null) {
                    locations = Lists.newArrayList();
                    map.put(id, locations);
                }
                Attr attr = element.getAttributeNodeNS(ANDROID_URI, ATTR_ID);
                assert attr != null;
                Location location = context.getLocation(attr);
                String folder = context.file.getParentFile().getName();
                location.setMessage(String.format("Occurrence in %1$s", folder));
                locations.add(location);
            }
        }

        NodeList childNodes = element.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                lookupLocations(context, (Element) child, map);
            }
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        LintDriver driver = context.getDriver();
        if (driver.getPhase() == 1) {
            // First phase: gather all the ids and look for consistency issues.
            // If any are found, request location computation in phase 2 by
            // writing the ids needed for each layout in the {@link #mLocations} map.
            for (Map.Entry<String,List<Pair<File,Map<String,String>>>> entry : mMap.entrySet()) {
                String layout = entry.getKey();
                List<Pair<File, Map<String, String>>> files = entry.getValue();
                if (files.size() < 2) {
                    // No consistency problems for files that don't have resource variations
                    continue;
                }

                checkConsistentIds(layout, files);
            }

            if (mLocations != null) {
                driver.requestRepeat(this, Scope.ALL_RESOURCES_SCOPE);
            }
        } else {
            // Collect results and print
            if (!mLocations.isEmpty()) {
                reportErrors(context);
            }
        }
    }

    @NonNull
    private Set<String> stripIrrelevantIds(@NonNull Set<String> ids) {
        if (!mRelevantIds.isEmpty()) {
            Set<String> stripped = new HashSet<String>(ids);
            stripped.retainAll(mRelevantIds);
            return stripped;
        }

        return Collections.emptySet();
    }

    private void checkConsistentIds(
            @NonNull String layout,
            @NonNull List<Pair<File, Map<String, String>>> files) {
        int layoutCount = files.size();
        assert layoutCount >= 2;

        Map<File, Set<String>> idMap = getIdMap(files, layoutCount);
        Set<String> inconsistent = getInconsistentIds(idMap);
        if (inconsistent.isEmpty()) {
            return;
        }

        if (mLocations == null) {
            mLocations = Maps.newHashMap();
        }
        if (mErrorMessages == null) {
            mErrorMessages = Maps.newHashMap();
        }

        // Map from each id, to a list of layout folders it is present in
        int idCount = inconsistent.size();
        Map<String, List<String>> presence = Maps.newHashMapWithExpectedSize(idCount);
        Set<String> allLayouts = Sets.newHashSetWithExpectedSize(layoutCount);
        for (Map.Entry<File, Set<String>> entry : idMap.entrySet()) {
            File file = entry.getKey();
            String folder = file.getParentFile().getName();
            allLayouts.add(folder);
            Set<String> ids = entry.getValue();
            for (String id : ids) {
                List<String> list = presence.get(id);
                if (list == null) {
                    list = Lists.newArrayListWithExpectedSize(layoutCount);
                    presence.put(id, list);
                }
                list.add(folder);
            }
        }

        // Compute lookup maps which will be used in phase 2 to initialize actual
        // locations for the id references

        Map<String, List<Location>> map = Maps.newHashMapWithExpectedSize(idCount);
        mLocations.put(layout, map);
        Map<String, String> messages = Maps.newHashMapWithExpectedSize(idCount);
        mErrorMessages.put(layout, messages);
        for (String id : inconsistent) {
            map.put(id, null); // The locations will be filled in during the second phase

            // Determine presence description for this id
            String message;
            List<String> layouts = presence.get(id);
            Collections.sort(layouts);

            Set<String> missingSet = new HashSet<String>(allLayouts);
            missingSet.removeAll(layouts);
            List<String> missing = new ArrayList<String>(missingSet);
            Collections.sort(missing);

            if (layouts.size() < layoutCount / 2) {
                message = String.format(
                        "The id \"%1$s\" in layout \"%2$s\" is only present in the following "
                                + "layout configurations: %3$s (missing from %4$s)",
                        id, layout,
                        LintUtils.formatList(layouts, Integer.MAX_VALUE),
                        LintUtils.formatList(missing, Integer.MAX_VALUE));
            } else {
                message = String.format(
                        "The id \"%1$s\" in layout \"%2$s\" is missing from the following layout "
                                + "configurations: %3$s (present in %4$s)",
                        id, layout, LintUtils.formatList(missing, Integer.MAX_VALUE),
                        LintUtils.formatList(layouts, Integer.MAX_VALUE));
            }
            messages.put(id, message);
        }
    }

    private static Set<String> getInconsistentIds(Map<File, Set<String>> idMap) {
        Set<String> union = getAllIds(idMap);
        Set<String> inconsistent = new HashSet<String>();
        for (Map.Entry<File, Set<String>> entry : idMap.entrySet()) {
            Set<String> ids = entry.getValue();
            if (ids.size() < union.size()) {
                Set<String> missing = new HashSet<String>(union);
                missing.removeAll(ids);
                inconsistent.addAll(missing);
            }
        }
        return inconsistent;
    }

    private static Set<String> getAllIds(Map<File, Set<String>> idMap) {
        Iterator<Set<String>> iterator = idMap.values().iterator();
        assert iterator.hasNext();
        Set<String> union = new HashSet<String>(iterator.next());
        while (iterator.hasNext()) {
            union.addAll(iterator.next());
        }
        return union;
    }

    private Map<File, Set<String>> getIdMap(List<Pair<File, Map<String, String>>> files,
            int layoutCount) {
        Map<File, Set<String>> idMap = new HashMap<File, Set<String>>(layoutCount);
        for (Pair<File, Map<String, String>> pair : files) {
            File file = pair.getFirst();
            Map<String, String> typeMap = pair.getSecond();
            Set<String> ids = typeMap.keySet();
            idMap.put(file, stripIrrelevantIds(ids));
        }
        return idMap;
    }

    private void reportErrors(Context context) {
        List<String> layouts = new ArrayList<String>(mLocations.keySet());
        Collections.sort(layouts);

        for (String layout : layouts) {
            Map<String, List<Location>> locationMap = mLocations.get(layout);
            Map<String, String> messageMap = mErrorMessages.get(layout);
            assert locationMap != null;
            assert messageMap != null;

            List<String> ids = new ArrayList<String>(locationMap.keySet());
            Collections.sort(ids);
            for (String id : ids) {
                String message = messageMap.get(id);
                List<Location> locations = locationMap.get(id);
                if (locations != null) {
                    Location location = chainLocations(locations);

                    context.report(INCONSISTENT_IDS, location, message);
                }
            }
        }
    }

    @NonNull
    private static Location chainLocations(@NonNull List<Location> locations) {
        assert !locations.isEmpty();

        // Sort locations by the file parent folders
        if (locations.size() > 1) {
            Collections.sort(locations, new Comparator<Location>() {
                @Override
                public int compare(Location location1, Location location2) {
                    File file1 = location1.getFile();
                    File file2 = location2.getFile();
                    String folder1 = file1.getParentFile().getName();
                    String folder2 = file2.getParentFile().getName();
                    return folder1.compareTo(folder2);
                }
            });
            // Chain locations together
            Iterator<Location> iterator = locations.iterator();
            assert iterator.hasNext();
            Location prev = iterator.next();
            while (iterator.hasNext()) {
                Location next = iterator.next();
                prev.setSecondary(next);
                prev = next;
            }
        }

        return locations.get(0);
    }

    // ---- Implements UastScanner ----

    @Override
    public boolean appliesToResourceRefs() {
        return true;
    }

    @Override
    public void visitResourceReference(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UElement node, @NonNull ResourceType type, @NonNull String name,
            boolean isFramework) {
        if (!isFramework && type == ResourceType.ID) {
            mRelevantIds.add(name);
        }
    }
}
