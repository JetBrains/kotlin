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


import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.TAG_ARRAY;
import static com.android.SdkConstants.TAG_INTEGER_ARRAY;
import static com.android.SdkConstants.TAG_STRING_ARRAY;

import com.android.annotations.NonNull;
import com.android.ide.common.rendering.api.ArrayResourceValue;
import com.android.ide.common.rendering.api.ResourceValue;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceFile;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks for arrays with inconsistent item counts
 */
public class ArraySizeDetector extends ResourceXmlDetector {

    /** Are there differences in how many array elements are declared? */
    public static final Issue INCONSISTENT = Issue.create(
            "InconsistentArrays", //$NON-NLS-1$
            "Inconsistencies in array element counts",
            "When an array is translated in a different locale, it should normally have " +
            "the same number of elements as the original array. When adding or removing " +
            "elements to an array, it is easy to forget to update all the locales, and this " +
            "lint warning finds inconsistencies like these.\n" +
            "\n" +
            "Note however that there may be cases where you really want to declare a " +
            "different number of array items in each configuration (for example where " +
            "the array represents available options, and those options differ for " +
            "different layout orientations and so on), so use your own judgement to " +
            "decide if this is really an error.\n" +
            "\n" +
            "You can suppress this error type if it finds false errors in your project.",
            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            new Implementation(
                    ArraySizeDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    private Multimap<File, Pair<String, Integer>> mFileToArrayCount;

    /** Locations for each array name. Populated during phase 2, if necessary */
    private Map<String, Location> mLocations;

    /** Error messages for each array name. Populated during phase 2, if necessary */
    private Map<String, String> mDescriptions;

    /** Constructs a new {@link ArraySizeDetector} */
    public ArraySizeDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == ResourceFolderType.VALUES;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_ARRAY,
                TAG_STRING_ARRAY,
                TAG_INTEGER_ARRAY
        );
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            mFileToArrayCount = ArrayListMultimap.create(30, 5);
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (context.getPhase() == 1) {
            boolean haveAllResources = context.getScope().contains(Scope.ALL_RESOURCE_FILES);
            if (!haveAllResources) {
                return;
            }

            // Check that all arrays for the same name have the same number of translations

            Set<String> alreadyReported = new HashSet<String>();
            Map<String, Integer> countMap = new HashMap<String, Integer>();
            Map<String, File> fileMap = new HashMap<String, File>();

            // Process the file in sorted file order to ensure stable output
            List<File> keys = new ArrayList<File>(mFileToArrayCount.keySet());
            Collections.sort(keys);

            for (File file : keys) {
                Collection<Pair<String, Integer>> pairs = mFileToArrayCount.get(file);
                for (Pair<String, Integer> pair : pairs) {
                    String name = pair.getFirst();

                    if (alreadyReported.contains(name)) {
                        continue;
                    }
                    Integer count = pair.getSecond();

                    Integer current = countMap.get(name);
                    if (current == null) {
                        countMap.put(name, count);
                        fileMap.put(name, file);
                    } else if (!count.equals(current)) {
                        alreadyReported.add(name);

                        if (mLocations == null) {
                            mLocations = new HashMap<String, Location>();
                            mDescriptions = new HashMap<String, String>();
                        }
                        mLocations.put(name, null);

                        String thisName = file.getParentFile().getName() + File.separator
                                + file.getName();
                        File otherFile = fileMap.get(name);
                        String otherName = otherFile.getParentFile().getName() + File.separator
                                + otherFile.getName();
                        String message = String.format(
                             "Array `%1$s` has an inconsistent number of items (%2$d in `%3$s`, %4$d in `%5$s`)",
                             name, count, thisName, current, otherName);
                         mDescriptions.put(name,  message);
                    }
                }
            }

            //noinspection VariableNotUsedInsideIf
            if (mLocations != null) {
                // Request another scan through the resources such that we can
                // gather the actual locations
                context.getDriver().requestRepeat(this, Scope.ALL_RESOURCES_SCOPE);
            }
            mFileToArrayCount = null;
        } else {
            if (mLocations != null) {
                List<String> names = new ArrayList<String>(mLocations.keySet());
                Collections.sort(names);
                for (String name : names) {
                    Location location = mLocations.get(name);
                    if (location == null) {
                        // Suppressed; see visitElement
                        continue;
                    }
                    // We were prepending locations, but we want to prefer the base folders
                    location = Location.reverse(location);

                    // Make sure we still have a conflict, in case one or more of the
                    // elements were marked with tools:ignore
                    int count = -1;
                    LintDriver driver = context.getDriver();
                    boolean foundConflict = false;
                    Location curr;
                    for (curr = location; curr != null; curr = curr.getSecondary()) {
                        Object clientData = curr.getClientData();
                        if (clientData instanceof Node) {
                            Node node = (Node) clientData;
                            if (driver.isSuppressed(null, INCONSISTENT, node)) {
                                continue;
                            }
                            int newCount = LintUtils.getChildCount(node);
                            if (newCount != count) {
                                if (count == -1) {
                                    count = newCount; // first number encountered
                                } else {
                                    foundConflict = true;
                                    break;
                                }
                            }
                        } else {
                            foundConflict = true;
                            break;
                        }
                    }

                    // Through one or more tools:ignore, there is no more conflict so
                    // ignore this element
                    if (!foundConflict) {
                        continue;
                    }

                    String message = mDescriptions.get(name);
                    context.report(INCONSISTENT, location, message);
                }
            }

            mLocations = null;
            mDescriptions = null;
        }
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        int phase = context.getPhase();

        Attr attribute = element.getAttributeNode(ATTR_NAME);
        if (attribute == null || attribute.getValue().isEmpty()) {
            if (phase != 1) {
                return;
            }
            context.report(INCONSISTENT, element, context.getLocation(element),
                String.format("Missing name attribute in `%1$s` declaration",
                        element.getTagName()));
        } else {
            String name = attribute.getValue();
            if (phase == 1) {
                if (context.getProject().getReportIssues()) {
                    int childCount = LintUtils.getChildCount(element);

                    if (!context.getScope().contains(Scope.ALL_RESOURCE_FILES) &&
                            context.getClient().supportsProjectResources()) {
                        incrementalCheckCount(context, element, name, childCount);
                        return;
                    }

                    mFileToArrayCount.put(context.file, Pair.of(name, childCount));
                }
            } else {
                assert phase == 2;
                if (mLocations.containsKey(name)) {
                    if (context.getDriver().isSuppressed(context, INCONSISTENT, element)) {
                        return;
                    }
                    Location location = context.getLocation(element);
                    location.setClientData(element);
                    location.setMessage(String.format("Declaration with array size (%1$d)",
                                    LintUtils.getChildCount(element)));
                    location.setSecondary(mLocations.get(name));
                    mLocations.put(name, location);
                }
            }
        }
    }

    private static void incrementalCheckCount(@NonNull XmlContext context, @NonNull Element element,
            @NonNull String name, int childCount) {
        LintClient client = context.getClient();
        Project project = context.getMainProject();
        AbstractResourceRepository resources = client.getProjectResources(project, true);
        if (resources == null) {
            return;
        }
        List<ResourceItem> items = resources.getResourceItem(ResourceType.ARRAY, name);
        if (items != null) {
            for (ResourceItem item : items) {
                ResourceFile source = item.getSource();
                if (source != null && LintUtils.isSameResourceFile(context.file,
                                                                   source.getFile())) {
                    continue;
                }
                ResourceValue rv = item.getResourceValue(false);
                if (rv instanceof ArrayResourceValue) {
                    ArrayResourceValue arv = (ArrayResourceValue) rv;
                    if (childCount != arv.getElementCount()) {
                        String thisName = context.file.getParentFile().getName() + File.separator
                                + context.file.getName();
                        assert source != null;
                        File otherFile = source.getFile();
                        String otherName = otherFile.getParentFile().getName() + File.separator
                                + otherFile.getName();
                        String message = String.format(
                                "Array `%1$s` has an inconsistent number of items (%2$d in `%3$s`, %4$d in `%5$s`)",
                                name, childCount, thisName, arv.getElementCount(), otherName);

                        context.report(INCONSISTENT, element, context.getLocation(element),
                                message);
                    }
                }
            }
        }
    }
}
