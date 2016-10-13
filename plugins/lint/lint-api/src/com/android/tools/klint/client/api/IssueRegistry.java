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

package com.android.tools.klint.client.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.annotations.Beta;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Registry which provides a list of checks to be performed on an Android project
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class IssueRegistry {
    private static volatile List<Category> sCategories;
    private static volatile Map<String, Issue> sIdToIssue;
    private static Map<EnumSet<Scope>, List<Issue>> sScopeIssues = Maps.newHashMap();

    /**
     * Creates a new {@linkplain IssueRegistry}
     */
    protected IssueRegistry() {
    }

    private static final Implementation DUMMY_IMPLEMENTATION = new Implementation(Detector.class,
            EnumSet.noneOf(Scope.class));
    /**
     * Issue reported by lint (not a specific detector) when it cannot even
     * parse an XML file prior to analysis
     */
    @NonNull
    public static final Issue PARSER_ERROR = Issue.create(
            "ParserError", //$NON-NLS-1$
            "Parser Errors",
            "Lint will ignore any files that contain fatal parsing errors. These may contain " +
            "other errors, or contain code which affects issues in other files.",
            Category.CORRECTNESS,
            10,
            Severity.ERROR,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported by lint for various other issues which prevents lint from
     * running normally when it's not necessarily an error in the user's code base.
     */
    @NonNull
    public static final Issue LINT_ERROR = Issue.create(
            "LintError", //$NON-NLS-1$
            "Lint Failure",
            "This issue type represents a problem running lint itself. Examples include " +
            "failure to find bytecode for source files (which means certain detectors " +
            "could not be run), parsing errors in lint configuration files, etc." +
            "\n" +
            "These errors are not errors in your own code, but they are shown to make " +
            "it clear that some checks were not completed.",

            Category.LINT,
            10,
            Severity.ERROR,
            DUMMY_IMPLEMENTATION);

    /**
     * Issue reported when lint is canceled
     */
    @NonNull
    public static final Issue CANCELLED = Issue.create(
            "LintCanceled", //$NON-NLS-1$
            "Lint Canceled",
            "Lint canceled by user; the issue report may not be complete.",

            Category.LINT,
            0,
            Severity.INFORMATIONAL,
            DUMMY_IMPLEMENTATION);

    /**
     * Returns the list of issues that can be found by all known detectors.
     *
     * @return the list of issues to be checked (including those that may be
     *         disabled!)
     */
    @NonNull
    public abstract List<Issue> getIssues();

    /**
     * Get an approximate issue count for a given scope. This is just an optimization,
     * so the number does not have to be accurate.
     *
     * @param scope the scope set
     * @return an approximate ceiling of the number of issues expected for a given scope set
     */
    protected int getIssueCapacity(@NonNull EnumSet<Scope> scope) {
        return 20;
    }

    /**
     * Returns all available issues of a given scope (regardless of whether
     * they are actually enabled for a given configuration etc)
     *
     * @param scope the applicable scope set
     * @return a list of issues
     */
    @NonNull
    protected List<Issue> getIssuesForScope(@NonNull EnumSet<Scope> scope) {
        List<Issue> list = sScopeIssues.get(scope);
        if (list == null) {
            List<Issue> issues = getIssues();
            if (scope.equals(Scope.ALL)) {
                list = issues;
            } else {
                list = new ArrayList<Issue>(getIssueCapacity(scope));
                for (Issue issue : issues) {
                    // Determine if the scope matches
                    if (issue.getImplementation().isAdequate(scope)) {
                        list.add(issue);
                    }
                }
            }
            sScopeIssues.put(scope, list);
        }

        return list;
    }

    /**
     * Creates a list of detectors applicable to the given scope, and with the
     * given configuration.
     *
     * @param client the client to report errors to
     * @param configuration the configuration to look up which issues are
     *            enabled etc from
     * @param scope the scope for the analysis, to filter out detectors that
     *            require wider analysis than is currently being performed
     * @param scopeToDetectors an optional map which (if not null) will be
     *            filled by this method to contain mappings from each scope to
     *            the applicable detectors for that scope
     * @return a list of new detector instances
     */
    @NonNull
    final List<? extends Detector> createDetectors(
            @NonNull LintClient client,
            @NonNull Configuration configuration,
            @NonNull EnumSet<Scope> scope,
            @Nullable Map<Scope, List<Detector>> scopeToDetectors) {

        List<Issue> issues = getIssuesForScope(scope);
        if (issues.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Class<? extends Detector>> detectorClasses = new HashSet<Class<? extends Detector>>();
        Map<Class<? extends Detector>, EnumSet<Scope>> detectorToScope =
                new HashMap<Class<? extends Detector>, EnumSet<Scope>>();

        for (Issue issue : issues) {
            Implementation implementation = issue.getImplementation();
            Class<? extends Detector> detectorClass = implementation.getDetectorClass();
            EnumSet<Scope> issueScope = implementation.getScope();
            if (!detectorClasses.contains(detectorClass)) {
                // Determine if the issue is enabled
                if (!configuration.isEnabled(issue)) {
                    continue;
                }

                assert implementation.isAdequate(scope); // Ensured by getIssuesForScope above

                detectorClass = client.replaceDetector(detectorClass);

                assert detectorClass != null : issue.getId();
                detectorClasses.add(detectorClass);
            }

            if (scopeToDetectors != null) {
                EnumSet<Scope> s = detectorToScope.get(detectorClass);
                if (s == null) {
                    detectorToScope.put(detectorClass, issueScope);
                } else if (!s.containsAll(issueScope)) {
                    EnumSet<Scope> union = EnumSet.copyOf(s);
                    union.addAll(issueScope);
                    detectorToScope.put(detectorClass, union);
                }
            }
        }

        List<Detector> detectors = new ArrayList<Detector>(detectorClasses.size());
        for (Class<? extends Detector> clz : detectorClasses) {
            try {
                Detector detector = clz.newInstance();
                detectors.add(detector);

                if (scopeToDetectors != null) {
                    EnumSet<Scope> union = detectorToScope.get(clz);
                    for (Scope s : union) {
                        List<Detector> list = scopeToDetectors.get(s);
                        if (list == null) {
                            list = new ArrayList<Detector>();
                            scopeToDetectors.put(s, list);
                        }
                        list.add(detector);
                    }

                }
            } catch (Throwable t) {
                client.log(t, "Can't initialize detector %1$s", clz.getName()); //$NON-NLS-1$
            }
        }

        return detectors;
    }

    /**
     * Returns true if the given id represents a valid issue id
     *
     * @param id the id to be checked
     * @return true if the given id is valid
     */
    public final boolean isIssueId(@NonNull String id) {
        return getIssue(id) != null;
    }

    /**
     * Returns true if the given category is a valid category
     *
     * @param name the category name to be checked
     * @return true if the given string is a valid category
     */
    public final boolean isCategoryName(@NonNull String name) {
        for (Category category : getCategories()) {
            if (category.getName().equals(name) || category.getFullName().equals(name)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the available categories
     *
     * @return an iterator for all the categories, never null
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @NonNull
    public List<Category> getCategories() {
        List<Category> categories = sCategories;
        if (categories == null) {
            synchronized (IssueRegistry.class) {
                categories = sCategories;
                if (categories == null) {
                    sCategories = categories = Collections.unmodifiableList(createCategoryList());
                }
            }
        }

        return categories;
    }

    @NonNull
    private List<Category> createCategoryList() {
        Set<Category> categorySet = Sets.newHashSetWithExpectedSize(20);
        for (Issue issue : getIssues()) {
            categorySet.add(issue.getCategory());
        }
        List<Category> sorted = new ArrayList<Category>(categorySet);
        Collections.sort(sorted);
        return sorted;
    }

    /**
     * Returns the issue for the given id, or null if it's not a valid id
     *
     * @param id the id to be checked
     * @return the corresponding issue, or null
     */
    @SuppressWarnings("AssignmentToStaticFieldFromInstanceMethod")
    @Nullable
    public final Issue getIssue(@NonNull String id) {
        Map<String, Issue> map = sIdToIssue;
        if (map == null) {
            synchronized (IssueRegistry.class) {
                map = sIdToIssue;
                if (map == null) {
                    map = createIdToIssueMap();
                    sIdToIssue = map;
                }
            }
        }

        return map.get(id);
    }

    @NonNull
    private Map<String, Issue> createIdToIssueMap() {
        List<Issue> issues = getIssues();
        Map<String, Issue> map = Maps.newHashMapWithExpectedSize(issues.size() + 2);
        for (Issue issue : issues) {
            map.put(issue.getId(), issue);
        }

        map.put(PARSER_ERROR.getId(), PARSER_ERROR);
        map.put(LINT_ERROR.getId(), LINT_ERROR);
        return map;
    }

    /**
     * Reset the registry such that it recomputes its available issues.
     */
    protected static void reset() {
        sIdToIssue = null;
        sCategories = null;
        sScopeIssues = Maps.newHashMap();
    }
}
