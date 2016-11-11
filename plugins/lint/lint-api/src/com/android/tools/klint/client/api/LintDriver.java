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

import static com.android.SdkConstants.ATTR_IGNORE;
import static com.android.SdkConstants.CLASS_CONSTRUCTOR;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_JAR;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.FD_GRADLE_WRAPPER;
import static com.android.SdkConstants.FN_GRADLE_WRAPPER_PROPERTIES;
import static com.android.SdkConstants.FN_LOCAL_PROPERTIES;
import static com.android.SdkConstants.FQCN_SUPPRESS_LINT;
import static com.android.SdkConstants.RES_FOLDER;
import static com.android.SdkConstants.SUPPRESS_ALL;
import static com.android.SdkConstants.SUPPRESS_LINT;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.ide.common.resources.configuration.FolderConfiguration.QUALIFIER_SPLITTER;
import static com.android.tools.klint.detector.api.LintUtils.isAnonymousClass;
import static java.io.File.separator;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.ResourceVisibilityLookup;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.klint.client.api.LintListener.EventType;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.ResourceContext;
import com.android.tools.klint.detector.api.ResourceXmlDetector;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.android.tools.klint.detector.api.XmlContext;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayInitializerExpression;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;

import org.jetbrains.uast.UElement;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.ast.Annotation;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationMethodDeclaration;
import lombok.ast.AnnotationValue;
import lombok.ast.ArrayInitializer;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.Expression;
import lombok.ast.MethodDeclaration;
import lombok.ast.Modifiers;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.TypeDeclaration;
import lombok.ast.TypeReference;
import lombok.ast.VariableDefinition;

/**
 * Analyzes Android projects and files
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class LintDriver {
    /**
     * Max number of passes to run through the lint runner if requested by
     * {@link #requestRepeat}
     */
    private static final int MAX_PHASES = 3;
    private static final String SUPPRESS_LINT_VMSIG = '/' + SUPPRESS_LINT + ';';
    /** Prefix used by the comment suppress mechanism in Studio/IntelliJ */
    private static final String STUDIO_ID_PREFIX = "AndroidLint";

    private final LintClient mClient;
    private LintRequest mRequest;
    private IssueRegistry mRegistry;
    private volatile boolean mCanceled;
    private EnumSet<Scope> mScope;
    private List<? extends Detector> mApplicableDetectors;
    private Map<Scope, List<Detector>> mScopeDetectors;
    private List<LintListener> mListeners;
    private int mPhase;
    private List<Detector> mRepeatingDetectors;
    private EnumSet<Scope> mRepeatScope;
    private Project[] mCurrentProjects;
    private Project mCurrentProject;
    private boolean mAbbreviating = true;
    private boolean mParserErrors;
    private Map<Object,Object> mProperties;
    /** Whether we need to look for legacy (old Lombok-based Java API) detectors */
    private boolean mRunCompatChecks = true;

    /**
     * Creates a new {@link LintDriver}
     *
     * @param registry The registry containing issues to be checked
     * @param client the tool wrapping the analyzer, such as an IDE or a CLI
     */
    public LintDriver(@NonNull IssueRegistry registry, @NonNull LintClient client) {
        mRegistry = registry;
        mClient = new LintClientWrapper(client);
    }

    /** Cancels the current lint run as soon as possible */
    public void cancel() {
        mCanceled = true;
    }

    /**
     * Returns the scope for the lint job
     *
     * @return the scope, never null
     */
    @NonNull
    public EnumSet<Scope> getScope() {
        return mScope;
    }

    /**
     * Sets the scope for the lint job
     *
     * @param scope the scope to use
     */
    public void setScope(@NonNull EnumSet<Scope> scope) {
        mScope = scope;
    }

    /**
     * Returns the lint client requesting the lint check. This may not be the same
     * instance as the one passed in to this driver; lint uses a wrapper which performs
     * additional validation to ensure that for example badly behaved detectors which report
     * issues that have been disabled will get muted without the real lint client getting
     * notified. Thus, this {@link LintClient} is suitable for use by detectors to look
     * up a client to for example get location handles from, but tool handling code should
     * never try to cast this client back to their original lint client. For the original
     * lint client, use {@link LintRequest} instead.
     *
     * @return the client, never null
     */
    @NonNull
    public LintClient getClient() {
        return mClient;
    }

    /**
     * Returns the current request, which points to the original files to be checked,
     * the original scope, the original {@link LintClient}, as well as the release mode.
     *
     * @return the request
     */
    @NonNull
    public LintRequest getRequest() {
        return mRequest;
    }

    /**
     * Records a property for later retrieval by {@link #getProperty(Object)}
     *
     * @param key the key to associate the value with
     * @param value the value, or null to remove a previous binding
     */
    public void putProperty(@NonNull Object key, @Nullable Object value) {
        if (mProperties == null) {
            mProperties = Maps.newHashMap();
        }
        if (value == null) {
            mProperties.remove(key);
        } else {
            mProperties.put(key, value);
        }
    }

    /**
     * Returns the property previously stored with the given key, or null
     *
     * @param key the key
     * @return the value or null if not found
     */
    @Nullable
    public Object getProperty(@NonNull Object key) {
        if (mProperties != null) {
            return mProperties.get(key);
        }

        return null;
    }

    /**
     * Returns the current phase number. The first pass is numbered 1. Only one pass
     * will be performed, unless a {@link Detector} calls {@link #requestRepeat}.
     *
     * @return the current phase, usually 1
     */
    public int getPhase() {
        return mPhase;
    }

    /**
     * Returns the current {@link IssueRegistry}.
     *
     * @return the current {@link IssueRegistry}
     */
    @NonNull
    public IssueRegistry getRegistry() {
        return mRegistry;
    }

    /**
     * Returns the project containing a given file, or null if not found. This searches
     * only among the currently checked project and its library projects, not among all
     * possible projects being scanned sequentially.
     *
     * @param file the file to be checked
     * @return the corresponding project, or null if not found
     */
    @Nullable
    public Project findProjectFor(@NonNull File file) {
        if (mCurrentProjects != null) {
            if (mCurrentProjects.length == 1) {
                return mCurrentProjects[0];
            }
            String path = file.getPath();
            for (Project project : mCurrentProjects) {
                if (path.startsWith(project.getDir().getPath())) {
                    return project;
                }
            }
        }

        return null;
    }

    /**
     * Sets whether lint should abbreviate output when appropriate.
     *
     * @param abbreviating true to abbreviate output, false to include everything
     */
    public void setAbbreviating(boolean abbreviating) {
        mAbbreviating = abbreviating;
    }

    /**
     * Returns whether lint should abbreviate output when appropriate.
     *
     * @return true if lint should abbreviate output, false when including everything
     */
    public boolean isAbbreviating() {
        return mAbbreviating;
    }

    /**
     * Returns whether lint has encountered any files with fatal parser errors
     * (e.g. broken source code, or even broken parsers)
     * <p>
     * This is useful for checks that need to make sure they've seen all data in
     * order to be conclusive (such as an unused resource check).
     *
     * @return true if any files were not properly processed because they
     *         contained parser errors
     */
    public boolean hasParserErrors() {
        return mParserErrors;
    }

    /**
     * Sets whether lint has encountered files with fatal parser errors.
     *
     * @see #hasParserErrors()
     * @param hasErrors whether parser errors have been encountered
     */
    public void setHasParserErrors(boolean hasErrors) {
        mParserErrors = hasErrors;
    }

    /**
     * Returns the projects being analyzed
     *
     * @return the projects being analyzed
     */
    @NonNull
    public List<Project> getProjects() {
        if (mCurrentProjects != null) {
            return Arrays.asList(mCurrentProjects);
        }
        return Collections.emptyList();
    }

    /**
     * Analyze the given file (which can point to an Android project). Issues found
     * are reported to the associated {@link LintClient}.
     *
     * @param files the files and directories to be analyzed
     * @param scope the scope of the analysis; detectors with a wider scope will
     *            not be run. If null, the scope will be inferred from the files.
     * @deprecated use {@link #analyze(LintRequest) instead}
     */
    @Deprecated
    public void analyze(@NonNull List<File> files, @Nullable EnumSet<Scope> scope) {
        analyze(new LintRequest(mClient, files).setScope(scope));
    }

    /**
     * Analyze the given files (which can point to Android projects or directories
     * containing Android projects). Issues found are reported to the associated
     * {@link LintClient}.
     * <p>
     * Note that the {@link LintDriver} is not multi thread safe or re-entrant;
     * if you want to run potentially overlapping lint jobs, create a separate driver
     * for each job.
     *
     * @param request the files and directories to be analyzed
     */
    public void analyze(@NonNull LintRequest request) {
        try {
            mRequest = request;
            analyze();
        } finally {
            mRequest = null;
        }
    }

    /** Runs the driver to analyze the requested files */
    private void analyze() {
        mCanceled = false;
        mScope = mRequest.getScope();
        assert mScope == null || !mScope.contains(Scope.ALL_RESOURCE_FILES) ||
                mScope.contains(Scope.RESOURCE_FILE);

        Collection<Project> projects;
        try {
            projects = mRequest.getProjects();
            if (projects == null) {
                projects = computeProjects(mRequest.getFiles());
            }
        } catch (CircularDependencyException e) {
            mCurrentProject = e.getProject();
            if (mCurrentProject != null) {
                Location location = e.getLocation();
                File file = location != null ? location.getFile() : mCurrentProject.getDir();
                Context context = new Context(this, mCurrentProject, null, file);
                context.report(IssueRegistry.LINT_ERROR, e.getLocation(), e.getMessage());
                mCurrentProject = null;
            }
            return;
        }
        if (projects.isEmpty()) {
            mClient.log(null, "No projects found for %1$s", mRequest.getFiles().toString());
            return;
        }
        if (mCanceled) {
            return;
        }

        if (mScope == null) {
            mScope = Scope.infer(projects);
        }

        fireEvent(EventType.STARTING, null);

        for (Project project : projects) {
            mPhase = 1;

            Project main = mRequest.getMainProject(project);

            // The set of available detectors varies between projects
            computeDetectors(project);

            if (mApplicableDetectors.isEmpty()) {
                // No detectors enabled in this project: skip it
                continue;
            }

            checkProject(project, main);
            if (mCanceled) {
                break;
            }

            runExtraPhases(project, main);
        }

        fireEvent(mCanceled ? EventType.CANCELED : EventType.COMPLETED, null);
    }

    @Nullable
    private Set<Issue> myCustomIssues;

    /**
     * Returns true if the given issue is an issue that was loaded as a custom rule
     * (e.g. a 3rd-party library provided the detector, it's not built in)
     *
     * @param issue the issue to be looked up
     * @return true if this is a custom (non-builtin) check
     */
    public boolean isCustomIssue(@NonNull Issue issue) {
        return myCustomIssues != null && myCustomIssues.contains(issue);
    }

    private void registerCustomDetectors(Collection<Project> projects) {
        // Look at the various projects, and if any of them provide a custom
        // lint jar, "add" them (this will replace the issue registry with
        // a CompositeIssueRegistry containing the original issue registry
        // plus JarFileIssueRegistry instances for each lint jar
        Set<File> jarFiles = Sets.newHashSet();
        for (Project project : projects) {
            jarFiles.addAll(mClient.findRuleJars(project));
            for (Project library : project.getAllLibraries()) {
                jarFiles.addAll(mClient.findRuleJars(library));
            }
        }

        jarFiles.addAll(mClient.findGlobalRuleJars());

        if (!jarFiles.isEmpty()) {
            List<IssueRegistry> registries = Lists.newArrayListWithExpectedSize(jarFiles.size());
            registries.add(mRegistry);
            for (File jarFile : jarFiles) {
                try {
                    JarFileIssueRegistry registry = JarFileIssueRegistry.get(mClient, jarFile);
                    if (registry.hasLegacyDetectors()) {
                        mRunCompatChecks = true;
                    }
                    if (myCustomIssues == null) {
                        myCustomIssues = Sets.newHashSet();
                    }
                    myCustomIssues.addAll(registry.getIssues());
                    registries.add(registry);
                } catch (Throwable e) {
                    mClient.log(e, "Could not load custom rule jar file %1$s", jarFile);
                }
            }
            if (registries.size() > 1) { // the first item is mRegistry itself
                mRegistry = new CompositeIssueRegistry(registries);
            }
        }
    }

    private void runExtraPhases(@NonNull Project project, @NonNull Project main) {
        // Did any detectors request another phase?
        if (mRepeatingDetectors != null) {
            // Yes. Iterate up to MAX_PHASES times.

            // During the extra phases, we might be narrowing the scope, and setting it in the
            // scope field such that detectors asking about the available scope will get the
            // correct result. However, we need to restore it to the original scope when this
            // is done in case there are other projects that will be checked after this, since
            // the repeated phases is done *per project*, not after all projects have been
            // processed.
            EnumSet<Scope> oldScope = mScope;

            do {
                mPhase++;
                fireEvent(EventType.NEW_PHASE,
                        new Context(this, project, null, project.getDir()));

                // Narrow the scope down to the set of scopes requested by
                // the rules.
                if (mRepeatScope == null) {
                    mRepeatScope = Scope.ALL;
                }
                mScope = Scope.intersect(mScope, mRepeatScope);
                if (mScope.isEmpty()) {
                    break;
                }

                // Compute the detectors to use for this pass.
                // Unlike the normal computeDetectors(project) call,
                // this is going to use the existing instances, and include
                // those that apply for the configuration.
                computeRepeatingDetectors(mRepeatingDetectors, project);

                if (mApplicableDetectors.isEmpty()) {
                    // No detectors enabled in this project: skip it
                    continue;
                }

                checkProject(project, main);
                if (mCanceled) {
                    break;
                }
            } while (mPhase < MAX_PHASES && mRepeatingDetectors != null);

            mScope = oldScope;
        }
    }

    private void computeRepeatingDetectors(List<Detector> detectors, Project project) {
        // Ensure that the current visitor is recomputed
        mCurrentFolderType = null;
        mCurrentVisitor = null;
        mCurrentXmlDetectors = null;
        mCurrentBinaryDetectors = null;

        // Create map from detector class to issue such that we can
        // compute applicable issues for each detector in the list of detectors
        // to be repeated
        List<Issue> issues = mRegistry.getIssues();
        Multimap<Class<? extends Detector>, Issue> issueMap =
                ArrayListMultimap.create(issues.size(), 3);
        for (Issue issue : issues) {
            issueMap.put(issue.getImplementation().getDetectorClass(), issue);
        }

        Map<Class<? extends Detector>, EnumSet<Scope>> detectorToScope =
                new HashMap<Class<? extends Detector>, EnumSet<Scope>>();
        Map<Scope, List<Detector>> scopeToDetectors =
                new EnumMap<Scope, List<Detector>>(Scope.class);

        List<Detector> detectorList = new ArrayList<Detector>();
        // Compute the list of detectors (narrowed down from mRepeatingDetectors),
        // and simultaneously build up the detectorToScope map which tracks
        // the scopes each detector is affected by (this is used to populate
        // the mScopeDetectors map which is used during iteration).
        Configuration configuration = project.getConfiguration(this);
        for (Detector detector : detectors) {
            Class<? extends Detector> detectorClass = detector.getClass();
            Collection<Issue> detectorIssues = issueMap.get(detectorClass);
            if (detectorIssues != null) {
                boolean add = false;
                for (Issue issue : detectorIssues) {
                    // The reason we have to check whether the detector is enabled
                    // is that this is a per-project property, so when running lint in multiple
                    // projects, a detector enabled only in a different project could have
                    // requested another phase, and we end up in this project checking whether
                    // the detector is enabled here.
                    if (!configuration.isEnabled(issue)) {
                        continue;
                    }

                    add = true; // Include detector if any of its issues are enabled

                    EnumSet<Scope> s = detectorToScope.get(detectorClass);
                    EnumSet<Scope> issueScope = issue.getImplementation().getScope();
                    if (s == null) {
                        detectorToScope.put(detectorClass, issueScope);
                    } else if (!s.containsAll(issueScope)) {
                        EnumSet<Scope> union = EnumSet.copyOf(s);
                        union.addAll(issueScope);
                        detectorToScope.put(detectorClass, union);
                    }
                }

                if (add) {
                    detectorList.add(detector);
                    EnumSet<Scope> union = detectorToScope.get(detector.getClass());
                    for (Scope s : union) {
                        List<Detector> list = scopeToDetectors.get(s);
                        if (list == null) {
                            list = new ArrayList<Detector>();
                            scopeToDetectors.put(s, list);
                        }
                        list.add(detector);
                    }
                }
            }
        }

        mApplicableDetectors = detectorList;
        mScopeDetectors = scopeToDetectors;
        mRepeatingDetectors = null;
        mRepeatScope = null;

        validateScopeList();
    }

    private void computeDetectors(@NonNull Project project) {
        // Ensure that the current visitor is recomputed
        mCurrentFolderType = null;
        mCurrentVisitor = null;

        Configuration configuration = project.getConfiguration(this);
        mScopeDetectors = new EnumMap<Scope, List<Detector>>(Scope.class);
        mApplicableDetectors = mRegistry.createDetectors(mClient, configuration,
                mScope, mScopeDetectors);

        validateScopeList();
    }

    /** Development diagnostics only, run with assertions on */
    @SuppressWarnings("all") // Turn off warnings for the intentional assertion side effect below
    private void validateScopeList() {
        boolean assertionsEnabled = false;
        assert assertionsEnabled = true; // Intentional side-effect
        if (assertionsEnabled) {
            List<Detector> resourceFileDetectors = mScopeDetectors.get(Scope.RESOURCE_FILE);
            if (resourceFileDetectors != null) {
                for (Detector detector : resourceFileDetectors) {
                    // This is wrong; it should allow XmlScanner instead of ResourceXmlScanner!
                    assert detector instanceof ResourceXmlDetector : detector;
                }
            }

            List<Detector> manifestDetectors = mScopeDetectors.get(Scope.MANIFEST);
            if (manifestDetectors != null) {
                for (Detector detector : manifestDetectors) {
                    assert detector instanceof Detector.XmlScanner : detector;
                }
            }
            List<Detector> javaCodeDetectors = mScopeDetectors.get(Scope.ALL_JAVA_FILES);
            if (javaCodeDetectors != null) {
                for (Detector detector : javaCodeDetectors) {
                    assert detector instanceof Detector.JavaScanner ||
                            // TODO: Migrate all
                            detector instanceof Detector.UastScanner ||
                            detector instanceof Detector.JavaPsiScanner : detector;
                }
            }
            List<Detector> javaFileDetectors = mScopeDetectors.get(Scope.JAVA_FILE);
            if (javaFileDetectors != null) {
                for (Detector detector : javaFileDetectors) {
                    assert detector instanceof Detector.JavaScanner ||
                            // TODO: Migrate all
                            detector instanceof Detector.UastScanner ||
                            detector instanceof Detector.JavaPsiScanner : detector;
                }
            }

            List<Detector> classDetectors = mScopeDetectors.get(Scope.CLASS_FILE);
            if (classDetectors != null) {
                for (Detector detector : classDetectors) {
                    assert detector instanceof Detector.ClassScanner : detector;
                }
            }

            List<Detector> classCodeDetectors = mScopeDetectors.get(Scope.ALL_CLASS_FILES);
            if (classCodeDetectors != null) {
                for (Detector detector : classCodeDetectors) {
                    assert detector instanceof Detector.ClassScanner : detector;
                }
            }

            List<Detector> gradleDetectors = mScopeDetectors.get(Scope.GRADLE_FILE);
            if (gradleDetectors != null) {
                for (Detector detector : gradleDetectors) {
                    assert detector instanceof Detector.GradleScanner : detector;
                }
            }

            List<Detector> otherDetectors = mScopeDetectors.get(Scope.OTHER);
            if (otherDetectors != null) {
                for (Detector detector : otherDetectors) {
                    assert detector instanceof Detector.OtherFileScanner : detector;
                }
            }

            List<Detector> dirDetectors = mScopeDetectors.get(Scope.RESOURCE_FOLDER);
            if (dirDetectors != null) {
                for (Detector detector : dirDetectors) {
                    assert detector instanceof Detector.ResourceFolderScanner : detector;
                }
            }

            List<Detector> binaryDetectors = mScopeDetectors.get(Scope.BINARY_RESOURCE_FILE);
            if (binaryDetectors != null) {
                for (Detector detector : binaryDetectors) {
                    assert detector instanceof Detector.BinaryResourceScanner : detector;
                }
            }
        }
    }

    private void registerProjectFile(
            @NonNull Map<File, Project> fileToProject,
            @NonNull File file,
            @NonNull File projectDir,
            @NonNull File rootDir) {
        fileToProject.put(file, mClient.getProject(projectDir, rootDir));
    }

    private Collection<Project> computeProjects(@NonNull List<File> files) {
        // Compute list of projects
        Map<File, Project> fileToProject = new LinkedHashMap<File, Project>();

        File sharedRoot = null;

        // Ensure that we have absolute paths such that if you lint
        //  "foo bar" in "baz" we can show baz/ as the root
        List<File> absolute = new ArrayList<File>(files.size());
        for (File file : files) {
            absolute.add(file.getAbsoluteFile());
        }
        // Always use absoluteFiles so that we can check the file's getParentFile()
        // which is null if the file is not absolute.
        files = absolute;

        if (files.size() > 1) {
            sharedRoot = LintUtils.getCommonParent(files);
            if (sharedRoot != null && sharedRoot.getParentFile() == null) { // "/" ?
                sharedRoot = null;
            }
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File rootDir = sharedRoot;
                if (rootDir == null) {
                    rootDir = file;
                    if (files.size() > 1) {
                        rootDir = file.getParentFile();
                        if (rootDir == null) {
                            rootDir = file;
                        }
                    }
                }

                // Figure out what to do with a directory. Note that the meaning of the
                // directory can be ambiguous:
                // If you pass a directory which is unknown, we don't know if we should
                // search upwards (in case you're pointing at a deep java package folder
                // within the project), or if you're pointing at some top level directory
                // containing lots of projects you want to scan. We attempt to do the
                // right thing, which is to see if you're pointing right at a project or
                // right within it (say at the src/ or res/) folder, and if not, you're
                // hopefully pointing at a project tree that you want to scan recursively.
                if (mClient.isProjectDirectory(file)) {
                    registerProjectFile(fileToProject, file, file, rootDir);
                    continue;
                } else {
                    File parent = file.getParentFile();
                    if (parent != null) {
                        if (mClient.isProjectDirectory(parent)) {
                            registerProjectFile(fileToProject, file, parent, parent);
                            continue;
                        } else {
                            parent = parent.getParentFile();
                            if (parent != null && mClient.isProjectDirectory(parent)) {
                                registerProjectFile(fileToProject, file, parent, parent);
                                continue;
                            }
                        }
                    }

                    // Search downwards for nested projects
                    addProjects(file, fileToProject, rootDir);
                }
            } else {
                // Pointed at a file: Search upwards for the containing project
                File parent = file.getParentFile();
                while (parent != null) {
                    if (mClient.isProjectDirectory(parent)) {
                        registerProjectFile(fileToProject, file, parent, parent);
                        break;
                    }
                    parent = parent.getParentFile();
                }
            }

            if (mCanceled) {
                return Collections.emptySet();
            }
        }

        for (Map.Entry<File, Project> entry : fileToProject.entrySet()) {
            File file = entry.getKey();
            Project project = entry.getValue();
            if (!file.equals(project.getDir())) {
                if (file.isDirectory()) {
                    try {
                        File dir = file.getCanonicalFile();
                        if (dir.equals(project.getDir())) {
                            continue;
                        }
                    } catch (IOException ioe) {
                        // pass
                    }
                }

                project.addFile(file);
            }
        }

        // Partition the projects up such that we only return projects that aren't
        // included by other projects (e.g. because they are library projects)

        Collection<Project> allProjects = fileToProject.values();
        Set<Project> roots = new HashSet<Project>(allProjects);
        for (Project project : allProjects) {
            roots.removeAll(project.getAllLibraries());
        }

        // Report issues for all projects that are explicitly referenced. We need to
        // do this here, since the project initialization will mark all library
        // projects as no-report projects by default.
        for (Project project : allProjects) {
            // Report issues for all projects explicitly listed or found via a directory
            // traversal -- including library projects.
            project.setReportIssues(true);
        }

        if (LintUtils.assertionsEnabled()) {
            // Make sure that all the project directories are unique. This ensures
            // that we didn't accidentally end up with different project instances
            // for a library project discovered as a directory as well as one
            // initialized from the library project dependency list
            IdentityHashMap<Project, Project> projects =
                    new IdentityHashMap<Project, Project>();
            for (Project project : roots) {
                projects.put(project, project);
                for (Project library : project.getAllLibraries()) {
                    projects.put(library, library);
                }
            }
            Set<File> dirs = new HashSet<File>();
            for (Project project : projects.keySet()) {
                assert !dirs.contains(project.getDir());
                dirs.add(project.getDir());
            }
        }

        return roots;
    }

    private void addProjects(
            @NonNull File dir,
            @NonNull Map<File, Project> fileToProject,
            @NonNull File rootDir) {
        if (mCanceled) {
            return;
        }

        if (mClient.isProjectDirectory(dir)) {
            registerProjectFile(fileToProject, dir, dir, rootDir);
        } else {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        addProjects(file, fileToProject, rootDir);
                    }
                }
            }
        }
    }

    private void checkProject(@NonNull Project project, @NonNull Project main) {
        File projectDir = project.getDir();

        Context projectContext = new Context(this, project, null, projectDir);
        fireEvent(EventType.SCANNING_PROJECT, projectContext);

        List<Project> allLibraries = project.getAllLibraries();
        Set<Project> allProjects = new HashSet<Project>(allLibraries.size() + 1);
        allProjects.add(project);
        allProjects.addAll(allLibraries);
        mCurrentProjects = allProjects.toArray(new Project[allProjects.size()]);

        mCurrentProject = project;

        for (Detector check : mApplicableDetectors) {
            check.beforeCheckProject(projectContext);
            if (mCanceled) {
                return;
            }
        }

        assert mCurrentProject == project;
        runFileDetectors(project, main);

        if (!Scope.checkSingleFile(mScope)) {
            List<Project> libraries = project.getAllLibraries();
            for (Project library : libraries) {
                Context libraryContext = new Context(this, library, project, projectDir);
                fireEvent(EventType.SCANNING_LIBRARY_PROJECT, libraryContext);
                mCurrentProject = library;

                for (Detector check : mApplicableDetectors) {
                    check.beforeCheckLibraryProject(libraryContext);
                    if (mCanceled) {
                        return;
                    }
                }
                assert mCurrentProject == library;

                runFileDetectors(library, main);
                if (mCanceled) {
                    return;
                }

                assert mCurrentProject == library;

                for (Detector check : mApplicableDetectors) {
                    check.afterCheckLibraryProject(libraryContext);
                    if (mCanceled) {
                        return;
                    }
                }
            }
        }

        mCurrentProject = project;

        for (Detector check : mApplicableDetectors) {
            check.afterCheckProject(projectContext);
            if (mCanceled) {
                return;
            }
        }

        if (mCanceled) {
            mClient.report(
                projectContext,
                    // Must provide an issue since API guarantees that the issue parameter
                IssueRegistry.CANCELLED,
                Severity.INFORMATIONAL,
                Location.create(project.getDir()),
                "Lint canceled by user", TextFormat.RAW);
        }

        mCurrentProjects = null;
    }

    private void runFileDetectors(@NonNull Project project, @Nullable Project main) {
        // Look up manifest information (but not for library projects)
        if (project.isAndroidProject()) {
            for (File manifestFile : project.getManifestFiles()) {
                XmlParser parser = mClient.getXmlParser();
                if (parser != null) {
                    XmlContext context = new XmlContext(this, project, main, manifestFile, null,
                            parser);
                    context.document = parser.parseXml(context);
                    if (context.document != null) {
                        try {
                            project.readManifest(context.document);

                            if ((!project.isLibrary() || (main != null
                                    && main.isMergingManifests()))
                                    && mScope.contains(Scope.MANIFEST)) {
                                List<Detector> detectors = mScopeDetectors.get(Scope.MANIFEST);
                                if (detectors != null) {
                                    ResourceVisitor v = new ResourceVisitor(parser, detectors,
                                            null);
                                    fireEvent(EventType.SCANNING_FILE, context);
                                    v.visitFile(context, manifestFile);
                                }
                            }
                        } finally {
                          if (context.document != null) { // else: freed by XmlVisitor above
                              parser.dispose(context, context.document);
                          }
                        }
                    }
                }
            }

            // Process both Scope.RESOURCE_FILE and Scope.ALL_RESOURCE_FILES detectors together
            // in a single pass through the resource directories.
            if (mScope.contains(Scope.ALL_RESOURCE_FILES)
                    || mScope.contains(Scope.RESOURCE_FILE)
                    || mScope.contains(Scope.RESOURCE_FOLDER)
                    || mScope.contains(Scope.BINARY_RESOURCE_FILE)) {
                List<Detector> dirChecks = mScopeDetectors.get(Scope.RESOURCE_FOLDER);
                List<Detector> binaryChecks = mScopeDetectors.get(Scope.BINARY_RESOURCE_FILE);
                List<Detector> checks = union(mScopeDetectors.get(Scope.RESOURCE_FILE),
                        mScopeDetectors.get(Scope.ALL_RESOURCE_FILES));
                boolean haveXmlChecks = checks != null && !checks.isEmpty();
                List<ResourceXmlDetector> xmlDetectors;
                if (haveXmlChecks) {
                    xmlDetectors = new ArrayList<ResourceXmlDetector>(checks.size());
                    for (Detector detector : checks) {
                        if (detector instanceof ResourceXmlDetector) {
                            xmlDetectors.add((ResourceXmlDetector) detector);
                        }
                    }
                    haveXmlChecks = !xmlDetectors.isEmpty();
                } else {
                    xmlDetectors = Collections.emptyList();
                }
                if (haveXmlChecks
                        || dirChecks != null && !dirChecks.isEmpty()
                        || binaryChecks != null && !binaryChecks.isEmpty()) {
                    List<File> files = project.getSubset();
                    if (files != null) {
                        checkIndividualResources(project, main, xmlDetectors, dirChecks,
                                binaryChecks, files);
                    } else {
                        List<File> resourceFolders = project.getResourceFolders();
                        if (!resourceFolders.isEmpty()) {
                            for (File res : resourceFolders) {
                                checkResFolder(project, main, res, xmlDetectors, dirChecks,
                                        binaryChecks);
                            }
                        }
                    }
                }
            }

            if (mCanceled) {
                return;
            }
        }

        if (mScope.contains(Scope.JAVA_FILE) || mScope.contains(Scope.ALL_JAVA_FILES)) {
            List<Detector> checks = union(mScopeDetectors.get(Scope.JAVA_FILE),
                    mScopeDetectors.get(Scope.ALL_JAVA_FILES));
            if (checks != null && !checks.isEmpty()) {
                List<File> files = project.getSubset();
                if (files != null) {
                    checkIndividualJavaFiles(project, main, checks, files);
                } else {
                    List<File> sourceFolders = project.getJavaSourceFolders();
                    if (mScope.contains(Scope.TEST_SOURCES)) {
                        List<File> testFolders = project.getTestSourceFolders();
                        if (!testFolders.isEmpty()) {
                            List<File> combined = Lists.newArrayListWithExpectedSize(
                                    sourceFolders.size() + testFolders.size());
                            combined.addAll(sourceFolders);
                            combined.addAll(testFolders);
                            sourceFolders = combined;
                        }
                    }

                    checkJava(project, main, sourceFolders, checks);

                }
            }
        }

        if (mCanceled) {
            return;
        }

        if (mScope.contains(Scope.CLASS_FILE)
                || mScope.contains(Scope.ALL_CLASS_FILES)
                || mScope.contains(Scope.JAVA_LIBRARIES)) {
            checkClasses(project, main);
        }

        if (mCanceled) {
            return;
        }

        if (mScope.contains(Scope.GRADLE_FILE)) {
            checkBuildScripts(project, main);
        }

        if (mCanceled) {
            return;
        }

        if (mScope.contains(Scope.OTHER)) {
            List<Detector> checks = mScopeDetectors.get(Scope.OTHER);
            if (checks != null) {
                OtherFileVisitor visitor = new OtherFileVisitor(checks);
                visitor.scan(this, project, main);
            }
        }

        if (mCanceled) {
            return;
        }

        if (project == main && mScope.contains(Scope.PROGUARD_FILE) &&
                project.isAndroidProject()) {
            checkProGuard(project, main);
        }

        if (project == main && mScope.contains(Scope.PROPERTY_FILE)) {
            checkProperties(project, main);
        }
    }

    private void checkBuildScripts(Project project, Project main) {
        List<Detector> detectors = mScopeDetectors.get(Scope.GRADLE_FILE);
        if (detectors != null) {
            List<File> files = project.getSubset();
            if (files == null) {
                files = project.getGradleBuildScripts();
            }
            for (File file : files) {
                Context context = new Context(this, project, main, file);
                fireEvent(EventType.SCANNING_FILE, context);
                for (Detector detector : detectors) {
                    if (detector.appliesTo(context, file)) {
                        detector.beforeCheckFile(context);
                        detector.visitBuildScript(context, Maps.<String, Object>newHashMap());
                        detector.afterCheckFile(context);
                    }
                }
            }
        }
    }

    private void checkProGuard(Project project, Project main) {
        List<Detector> detectors = mScopeDetectors.get(Scope.PROGUARD_FILE);
        if (detectors != null) {
            List<File> files = project.getProguardFiles();
            for (File file : files) {
                Context context = new Context(this, project, main, file);
                fireEvent(EventType.SCANNING_FILE, context);
                for (Detector detector : detectors) {
                    if (detector.appliesTo(context, file)) {
                        detector.beforeCheckFile(context);
                        detector.run(context);
                        detector.afterCheckFile(context);
                    }
                }
            }
        }
    }

    private void checkProperties(Project project, Project main) {
        List<Detector> detectors = mScopeDetectors.get(Scope.PROPERTY_FILE);
        if (detectors != null) {
            checkPropertyFile(project, main, detectors, FN_LOCAL_PROPERTIES);
            checkPropertyFile(project, main, detectors, FD_GRADLE_WRAPPER + separator +
                    FN_GRADLE_WRAPPER_PROPERTIES);
        }
    }

    private void checkPropertyFile(Project project, Project main, List<Detector> detectors,
            String relativePath) {
        File file = new File(project.getDir(), relativePath);
        if (file.exists()) {
            Context context = new Context(this, project, main, file);
            fireEvent(EventType.SCANNING_FILE, context);
            for (Detector detector : detectors) {
                if (detector.appliesTo(context, file)) {
                    detector.beforeCheckFile(context);
                    detector.run(context);
                    detector.afterCheckFile(context);
                }
            }
        }
    }

    /** True if execution has been canceled */
    boolean isCanceled() {
        return mCanceled;
    }

    /**
     * Returns the super class for the given class name,
     * which should be in VM format (e.g. java/lang/Integer, not java.lang.Integer).
     * If the super class is not known, returns null. This can happen if
     * the given class is not a known class according to the project or its
     * libraries, for example because it refers to one of the core libraries which
     * are not analyzed by lint.
     *
     * @param name the fully qualified class name
     * @return the corresponding super class name (in VM format), or null if not known
     */
    @Nullable
    public String getSuperClass(@NonNull String name) {
        return mClient.getSuperClass(mCurrentProject, name);
    }

    /**
     * Returns true if the given class is a subclass of the given super class.
     *
     * @param classNode the class to check whether it is a subclass of the given
     *            super class name
     * @param superClassName the fully qualified super class name (in VM format,
     *            e.g. java/lang/Integer, not java.lang.Integer.
     * @return true if the given class is a subclass of the given super class
     */
    public boolean isSubclassOf(@NonNull ClassNode classNode, @NonNull String superClassName) {
        if (superClassName.equals(classNode.superName)) {
            return true;
        }

        if (mCurrentProject != null) {
            Boolean isSub = mClient.isSubclassOf(mCurrentProject, classNode.name, superClassName);
            if (isSub != null) {
                return isSub;
            }
        }

        String className = classNode.name;
        while (className != null) {
            if (className.equals(superClassName)) {
                return true;
            }
            className = getSuperClass(className);
        }

        return false;
    }
    @Nullable
    private static List<Detector> union(
            @Nullable List<Detector> list1,
            @Nullable List<Detector> list2) {
        if (list1 == null) {
            return list2;
        } else if (list2 == null) {
            return list1;
        } else {
            // Use set to pick out unique detectors, since it's possible for there to be overlap,
            // e.g. the DuplicateIdDetector registers both a cross-resource issue and a
            // single-file issue, so it shows up on both scope lists:
            Set<Detector> set = new HashSet<Detector>(list1.size() + list2.size());
            set.addAll(list1);
            set.addAll(list2);

            return new ArrayList<Detector>(set);
        }
    }

    /** Check the classes in this project (and if applicable, in any library projects */
    private void checkClasses(Project project, Project main) {
        List<File> files = project.getSubset();
        if (files != null) {
            checkIndividualClassFiles(project, main, files);
            return;
        }

        // We need to read in all the classes up front such that we can initialize
        // the parent chains (such that for example for a virtual dispatch, we can
        // also check the super classes).

        List<File> libraries = project.getJavaLibraries(false);
        List<ClassEntry> libraryEntries = ClassEntry.fromClassPath(mClient, libraries, true);

        List<File> classFolders = project.getJavaClassFolders();
        List<ClassEntry> classEntries;
        if (classFolders.isEmpty()) {
            String message = String.format("No `.class` files were found in project \"%1$s\", "
                    + "so none of the classfile based checks could be run. "
                    + "Does the project need to be built first?", project.getName());
            Location location = Location.create(project.getDir());
            mClient.report(new Context(this, project, main, project.getDir()),
                    IssueRegistry.LINT_ERROR,
                    project.getConfiguration(this).getSeverity(IssueRegistry.LINT_ERROR),
                    location, message, TextFormat.RAW);
            classEntries = Collections.emptyList();
        } else {
            classEntries = ClassEntry.fromClassPath(mClient, classFolders, true);
        }

        // Actually run the detectors. Libraries should be called before the
        // main classes.
        runClassDetectors(Scope.JAVA_LIBRARIES, libraryEntries, project, main);

        if (mCanceled) {
            return;
        }

        runClassDetectors(Scope.CLASS_FILE, classEntries, project, main);
        runClassDetectors(Scope.ALL_CLASS_FILES, classEntries, project, main);
    }

    private void checkIndividualClassFiles(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull List<File> files) {
        List<File> classFiles = Lists.newArrayListWithExpectedSize(files.size());
        List<File> classFolders = project.getJavaClassFolders();
        if (!classFolders.isEmpty()) {
            for (File file : files) {
                String path = file.getPath();
                if (file.isFile() && path.endsWith(DOT_CLASS)) {
                    classFiles.add(file);
                }
            }
        }

        List<ClassEntry> entries = ClassEntry.fromClassFiles(mClient, classFiles, classFolders,
                true);
        if (!entries.isEmpty()) {
            Collections.sort(entries);
            runClassDetectors(Scope.CLASS_FILE, entries, project, main);
        }
    }

    /**
     * Stack of {@link ClassNode} nodes for outer classes of the currently
     * processed class, including that class itself. Populated by
     * {@link #runClassDetectors(Scope, List, Project, Project)} and used by
     * {@link #getOuterClassNode(ClassNode)}
     */
    private Deque<ClassNode> mOuterClasses;

    private void runClassDetectors(Scope scope, List<ClassEntry> entries,
            Project project, Project main) {
        if (mScope.contains(scope)) {
            List<Detector> classDetectors = mScopeDetectors.get(scope);
            if (classDetectors != null && !classDetectors.isEmpty() && !entries.isEmpty()) {
                AsmVisitor visitor = new AsmVisitor(mClient, classDetectors);

                String sourceContents = null;
                String sourceName = "";
                mOuterClasses = new ArrayDeque<ClassNode>();
                ClassEntry prev = null;
                for (ClassEntry entry : entries) {
                    if (prev != null && prev.compareTo(entry) == 0) {
                        // Duplicate entries for some reason: ignore
                        continue;
                    }
                    prev = entry;

                    ClassReader reader;
                    ClassNode classNode;
                    try {
                        reader = new ClassReader(entry.bytes);
                        classNode = new ClassNode();
                        reader.accept(classNode, 0 /* flags */);
                    } catch (Throwable t) {
                        mClient.log(null, "Error processing %1$s: broken class file?",
                                entry.path());
                        continue;
                    }

                    ClassNode peek;
                    while ((peek = mOuterClasses.peek()) != null) {
                        if (classNode.name.startsWith(peek.name)) {
                            break;
                        } else {
                            mOuterClasses.pop();
                        }
                    }
                    mOuterClasses.push(classNode);

                    if (isSuppressed(null, classNode)) {
                        // Class was annotated with suppress all -- no need to look any further
                        continue;
                    }

                    if (sourceContents != null) {
                        // Attempt to reuse the source buffer if initialized
                        // This means making sure that the source files
                        //    foo/bar/MyClass and foo/bar/MyClass$Bar
                        //    and foo/bar/MyClass$3 and foo/bar/MyClass$3$1 have the same prefix.
                        String newName = classNode.name;
                        int newRootLength = newName.indexOf('$');
                        if (newRootLength == -1) {
                            newRootLength = newName.length();
                        }
                        int oldRootLength = sourceName.indexOf('$');
                        if (oldRootLength == -1) {
                            oldRootLength = sourceName.length();
                        }
                        if (newRootLength != oldRootLength ||
                                !sourceName.regionMatches(0, newName, 0, newRootLength)) {
                            sourceContents = null;
                        }
                    }

                    ClassContext context = new ClassContext(this, project, main,
                            entry.file, entry.jarFile, entry.binDir, entry.bytes,
                            classNode, scope == Scope.JAVA_LIBRARIES /*fromLibrary*/,
                            sourceContents);

                    try {
                        visitor.runClassDetectors(context);
                    } catch (Exception e) {
                        mClient.log(e, null);
                    }

                    if (mCanceled) {
                        return;
                    }

                    sourceContents = context.getSourceContents(false/*read*/);
                    sourceName = classNode.name;
                }

                mOuterClasses = null;
            }
        }
    }

    /** Returns the outer class node of the given class node
     * @param classNode the inner class node
     * @return the outer class node */
    public ClassNode getOuterClassNode(@NonNull ClassNode classNode) {
        String outerName = classNode.outerClass;

        Iterator<ClassNode> iterator = mOuterClasses.iterator();
        while (iterator.hasNext()) {
            ClassNode node = iterator.next();
            if (outerName != null) {
                if (node.name.equals(outerName)) {
                    return node;
                }
            } else if (node == classNode) {
                return iterator.hasNext() ? iterator.next() : null;
            }
        }

        return null;
    }

    /**
     * Returns the {@link ClassNode} corresponding to the given type, if possible, or null
     *
     * @param type the fully qualified type, using JVM signatures (/ and $, not . as path
     *             separators)
     * @param flags the ASM flags to pass to the {@link ClassReader}, normally 0 but can
     *              for example be {@link ClassReader#SKIP_CODE} and/oor
     *              {@link ClassReader#SKIP_DEBUG}
     * @return the class node for the type, or null
     */
    @Nullable
    public ClassNode findClass(@NonNull ClassContext context, @NonNull String type, int flags) {
        String relative = type.replace('/', File.separatorChar) + DOT_CLASS;
        File classFile = findClassFile(context.getProject(), relative);
        if (classFile != null) {
            if (classFile.getPath().endsWith(DOT_JAR)) {
                // TODO: Handle .jar files
                return null;
            }

            try {
                byte[] bytes = mClient.readBytes(classFile);
                ClassReader reader = new ClassReader(bytes);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, flags);

                return classNode;
            } catch (Throwable t) {
                mClient.log(null, "Error processing %1$s: broken class file?",
                        classFile.getPath());
            }
        }

        return null;
    }

    @Nullable
    private File findClassFile(@NonNull Project project, String relativePath) {
        for (File root : mClient.getJavaClassFolders(project)) {
            File path = new File(root, relativePath);
            if (path.exists()) {
                return path;
            }
        }
        // Search in the libraries
        for (File root : mClient.getJavaLibraries(project, true)) {
            // TODO: Handle .jar files!
            //if (root.getPath().endsWith(DOT_JAR)) {
            //}

            File path = new File(root, relativePath);
            if (path.exists()) {
                return path;
            }
        }

        // Search dependent projects
        for (Project library : project.getDirectLibraries()) {
            File path = findClassFile(library, relativePath);
            if (path != null) {
                return path;
            }
        }

        return null;
    }

    private void checkJava(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull List<File> sourceFolders,
            @NonNull List<Detector> checks) {
        JavaParser javaParser = mClient.getJavaParser(project);
        if (javaParser == null) {
            mClient.log(null, "No java parser provided to lint: not running Java checks");
            return;
        }

        assert !checks.isEmpty();

        // Gather all Java source files in a single pass; more efficient.
        List<File> sources = new ArrayList<File>(100);
        for (File folder : sourceFolders) {
            gatherJavaFiles(folder, sources);
        }
        if (!sources.isEmpty()) {
            List<JavaContext> contexts = Lists.newArrayListWithExpectedSize(sources.size());
            for (File file : sources) {
                JavaContext context = new JavaContext(this, project, main, file, javaParser);
                contexts.add(context);
            }
            visitJavaFiles(checks, javaParser, contexts);
        }
    }

    private void visitJavaFiles(@NonNull List<Detector> checks, JavaParser javaParser,
            List<JavaContext> contexts) {
        // Temporary: we still have some builtin checks that aren't migrated to
        // PSI. Until that's complete, remove them from the list here
        //List<Detector> scanners = checks;
        List<Detector> scanners = Lists.newArrayListWithCapacity(0);
        List<Detector> uastScanners = Lists.newArrayListWithCapacity(checks.size());
        for (Detector detector : checks) {
            if (detector instanceof Detector.JavaPsiScanner) {
                scanners.add(detector);
            } else if (detector instanceof Detector.UastScanner) {
                uastScanners.add(detector);
            }
        }

        if (!scanners.isEmpty()) {
            JavaPsiVisitor visitor = new JavaPsiVisitor(javaParser, scanners);
            visitor.prepare(contexts);
            for (JavaContext context : contexts) {
                fireEvent(EventType.SCANNING_FILE, context);
                visitor.visitFile(context);
                if (mCanceled) {
                    return;
                }
            }

            visitor.dispose();
        }

        if (!uastScanners.isEmpty()) {
            final UElementVisitor uElementVisitor = new UElementVisitor(javaParser, uastScanners);
            uElementVisitor.prepare(contexts);
            for (final JavaContext context : contexts) {
                fireEvent(EventType.SCANNING_FILE, context);
                ApplicationManager.getApplication().runReadAction(new Runnable() {
                    @Override
                    public void run() {
                        uElementVisitor.visitFile(context);
                    }
                });
                if (mCanceled) {
                    return;
                }
            }
            
            uElementVisitor.dispose();
        }

        // Only if the user is using some custom lint rules that haven't been updated
        // yet
        //noinspection ConstantConditions
        if (mRunCompatChecks) {
            // Filter the checks to only those that implement JavaScanner
            List<Detector> filtered = Lists.newArrayListWithCapacity(checks.size());
            for (Detector detector : checks) {
                if (detector instanceof Detector.JavaScanner) {
                    filtered.add(detector);
                }
            }

            if (!filtered.isEmpty()) {
                List<String> detectorNames = Lists.newArrayListWithCapacity(filtered.size());
                for (Detector detector : filtered) {
                    detectorNames.add(detector.getClass().getName());
                }
                Collections.sort(detectorNames);

                /* Let's not complain quite yet
                String message = String.format("Lint found one or more custom checks using its "
                        + "older Java API; these checks are still run in compatibility mode, "
                        + "but this causes duplicated parsing, and in the next version lint "
                        + "will no longer include this legacy mode. Make sure the following "
                        + "lint detectors are upgraded to the new API: %1$s",
                        Joiner.on(", ").join(detectorNames));
                JavaContext first = contexts.get(0);
                Project project = first.getProject();
                Location location = Location.create(project.getDir());
                mClient.report(first,
                        IssueRegistry.LINT_ERROR,
                        project.getConfiguration(this).getSeverity(IssueRegistry.LINT_ERROR),
                        location, message, TextFormat.RAW);
                */


                JavaVisitor oldVisitor = new JavaVisitor(javaParser, filtered);

                oldVisitor.prepare(contexts);
                for (JavaContext context : contexts) {
                    fireEvent(EventType.SCANNING_FILE, context);
                    oldVisitor.visitFile(context);
                    if (mCanceled) {
                        return;
                    }
                }
                oldVisitor.dispose();
            }
        }
    }

    private void checkIndividualJavaFiles(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull List<Detector> checks,
            @NonNull List<File> files) {

        JavaParser javaParser = mClient.getJavaParser(project);
        if (javaParser == null) {
            mClient.log(null, "No java parser provided to lint: not running Java checks");
            return;
        }

        List<JavaContext> contexts = Lists.newArrayListWithExpectedSize(files.size());
        for (File file : files) {
            if (file.isFile() && file.getPath().endsWith(".kt")) {
                contexts.add(new JavaContext(this, project, main, file, javaParser));
            }
        }

        if (contexts.isEmpty()) {
            return;
        }

        visitJavaFiles(checks, javaParser, contexts);
    }

    private static void gatherJavaFiles(@NonNull File dir, @NonNull List<File> result) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".java")) { //$NON-NLS-1$
                    result.add(file);
                } else if (file.isDirectory()) {
                    gatherJavaFiles(file, result);
                }
            }
        }
    }

    private ResourceFolderType mCurrentFolderType;
    private List<ResourceXmlDetector> mCurrentXmlDetectors;
    private List<Detector> mCurrentBinaryDetectors;
    private ResourceVisitor mCurrentVisitor;

    @Nullable
    private ResourceVisitor getVisitor(
            @NonNull ResourceFolderType type,
            @NonNull List<ResourceXmlDetector> checks,
            @Nullable List<Detector> binaryChecks) {
        if (type != mCurrentFolderType) {
            mCurrentFolderType = type;

            // Determine which XML resource detectors apply to the given folder type
            List<ResourceXmlDetector> applicableXmlChecks =
                    new ArrayList<ResourceXmlDetector>(checks.size());
            for (ResourceXmlDetector check : checks) {
                if (check.appliesTo(type)) {
                    applicableXmlChecks.add(check);
                }
            }
            List<Detector> applicableBinaryChecks = null;
            if (binaryChecks != null) {
                applicableBinaryChecks = new ArrayList<Detector>(binaryChecks.size());
                for (Detector check : binaryChecks) {
                    if (check.appliesTo(type)) {
                        applicableBinaryChecks.add(check);
                    }
                }
            }

            // If the list of detectors hasn't changed, then just use the current visitor!
            if (mCurrentXmlDetectors != null && mCurrentXmlDetectors.equals(applicableXmlChecks)
                    && Objects.equal(mCurrentBinaryDetectors, applicableBinaryChecks)) {
                return mCurrentVisitor;
            }

            mCurrentXmlDetectors = applicableXmlChecks;
            mCurrentBinaryDetectors = applicableBinaryChecks;

            if (applicableXmlChecks.isEmpty()
                    && (applicableBinaryChecks == null || applicableBinaryChecks.isEmpty())) {
                mCurrentVisitor = null;
                return null;
            }

            XmlParser parser = mClient.getXmlParser();
            if (parser != null) {
                mCurrentVisitor = new ResourceVisitor(parser, applicableXmlChecks,
                        applicableBinaryChecks);
            } else {
                mCurrentVisitor = null;
            }
        }

        return mCurrentVisitor;
    }

    private void checkResFolder(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File res,
            @NonNull List<ResourceXmlDetector> xmlChecks,
            @Nullable List<Detector> dirChecks,
            @Nullable List<Detector> binaryChecks) {
        File[] resourceDirs = res.listFiles();
        if (resourceDirs == null) {
            return;
        }

        // Sort alphabetically such that we can process related folder types at the
        // same time, and to have a defined behavior such that detectors can rely on
        // predictable ordering, e.g. layouts are seen before menus are seen before
        // values, etc (l < m < v).

        Arrays.sort(resourceDirs);
        for (File dir : resourceDirs) {
            ResourceFolderType type = ResourceFolderType.getFolderType(dir.getName());
            if (type != null) {
                checkResourceFolder(project, main, dir, type, xmlChecks, dirChecks, binaryChecks);
            }

            if (mCanceled) {
                return;
            }
        }
    }

    private void checkResourceFolder(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File dir,
            @NonNull ResourceFolderType type,
            @NonNull List<ResourceXmlDetector> xmlChecks,
            @Nullable List<Detector> dirChecks,
            @Nullable List<Detector> binaryChecks) {

        // Process the resource folder

        if (dirChecks != null && !dirChecks.isEmpty()) {
            ResourceContext context = new ResourceContext(this, project, main, dir, type);
            String folderName = dir.getName();
            fireEvent(EventType.SCANNING_FILE, context);
            for (Detector check : dirChecks) {
                if (check.appliesTo(type)) {
                    check.beforeCheckFile(context);
                    check.checkFolder(context, folderName);
                    check.afterCheckFile(context);
                }
            }
            if (binaryChecks == null && xmlChecks.isEmpty()) {
                return;
            }
        }

        File[] files = dir.listFiles();
        if (files == null || files.length <= 0) {
            return;
        }

        ResourceVisitor visitor = getVisitor(type, xmlChecks, binaryChecks);
        if (visitor != null) { // if not, there are no applicable rules in this folder
            // Process files in alphabetical order, to ensure stable output
            // (for example for the duplicate resource detector)
            Arrays.sort(files);
            for (File file : files) {
                if (LintUtils.isXmlFile(file)) {
                    XmlContext context = new XmlContext(this, project, main, file, type,
                            visitor.getParser());
                    fireEvent(EventType.SCANNING_FILE, context);
                    visitor.visitFile(context, file);
                } else if (binaryChecks != null && (LintUtils.isBitmapFile(file) ||
                            type == ResourceFolderType.RAW)) {
                    ResourceContext context = new ResourceContext(this, project, main, file, type);
                    fireEvent(EventType.SCANNING_FILE, context);
                    visitor.visitBinaryResource(context);
                }
                if (mCanceled) {
                    return;
                }
            }
        }
    }

    /** Checks individual resources */
    private void checkIndividualResources(
            @NonNull Project project,
            @Nullable Project main,
            @NonNull List<ResourceXmlDetector> xmlDetectors,
            @Nullable List<Detector> dirChecks,
            @Nullable List<Detector> binaryChecks,
            @NonNull List<File> files) {
        for (File file : files) {
            if (file.isDirectory()) {
                // Is it a resource folder?
                ResourceFolderType type = ResourceFolderType.getFolderType(file.getName());
                if (type != null && new File(file.getParentFile(), RES_FOLDER).exists()) {
                    // Yes.
                    checkResourceFolder(project, main, file, type, xmlDetectors, dirChecks,
                            binaryChecks);
                } else if (file.getName().equals(RES_FOLDER)) { // Is it the res folder?
                    // Yes
                    checkResFolder(project, main, file, xmlDetectors, dirChecks, binaryChecks);
                } else {
                    mClient.log(null, "Unexpected folder %1$s; should be project, " +
                            "\"res\" folder or resource folder", file.getPath());
                }
            } else if (file.isFile() && LintUtils.isXmlFile(file)) {
                // Yes, find out its resource type
                String folderName = file.getParentFile().getName();
                ResourceFolderType type = ResourceFolderType.getFolderType(folderName);
                if (type != null) {
                    ResourceVisitor visitor = getVisitor(type, xmlDetectors, binaryChecks);
                    if (visitor != null) {
                        XmlContext context = new XmlContext(this, project, main, file, type,
                                visitor.getParser());
                        fireEvent(EventType.SCANNING_FILE, context);
                        visitor.visitFile(context, file);
                    }
                }
            } else if (binaryChecks != null && file.isFile() && LintUtils.isBitmapFile(file)) {
                // Yes, find out its resource type
                String folderName = file.getParentFile().getName();
                ResourceFolderType type = ResourceFolderType.getFolderType(folderName);
                if (type != null) {
                    ResourceVisitor visitor = getVisitor(type, xmlDetectors, binaryChecks);
                    if (visitor != null) {
                        ResourceContext context = new ResourceContext(this, project, main, file,
                                type);
                        fireEvent(EventType.SCANNING_FILE, context);
                        visitor.visitBinaryResource(context);
                        if (mCanceled) {
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Adds a listener to be notified of lint progress
     *
     * @param listener the listener to be added
     */
    public void addLintListener(@NonNull LintListener listener) {
        if (mListeners == null) {
            mListeners = new ArrayList<LintListener>(1);
        }
        mListeners.add(listener);
    }

    /**
     * Removes a listener such that it is no longer notified of progress
     *
     * @param listener the listener to be removed
     */
    public void removeLintListener(@NonNull LintListener listener) {
        mListeners.remove(listener);
        if (mListeners.isEmpty()) {
            mListeners = null;
        }
    }

    /** Notifies listeners, if any, that the given event has occurred */
    private void fireEvent(@NonNull LintListener.EventType type, @Nullable Context context) {
        if (mListeners != null) {
            for (LintListener listener : mListeners) {
                listener.update(this, type, context);
            }
        }
    }

    /**
     * Wrapper around the lint client. This sits in the middle between a
     * detector calling for example {@link LintClient#report} and
     * the actual embedding tool, and performs filtering etc such that detectors
     * and lint clients don't have to make sure they check for ignored issues or
     * filtered out warnings.
     */
    private class LintClientWrapper extends LintClient {
        @NonNull
        private final LintClient mDelegate;

        public LintClientWrapper(@NonNull LintClient delegate) {
            super(getClientName());
            mDelegate = delegate;
        }

        @Override
        public void report(
                @NonNull Context context,
                @NonNull Issue issue,
                @NonNull Severity severity,
                @NonNull Location location,
                @NonNull String message,
                @NonNull TextFormat format) {
            //noinspection ConstantConditions
            if (location == null) {
                // Misbehaving third-party lint detectors
                assert false : issue;
                return;
            }

            assert mCurrentProject != null;
            if (!mCurrentProject.getReportIssues()) {
                return;
            }

            Configuration configuration = context.getConfiguration();
            if (!configuration.isEnabled(issue)) {
                if (issue != IssueRegistry.PARSER_ERROR && issue != IssueRegistry.LINT_ERROR) {
                    mDelegate.log(null, "Incorrect detector reported disabled issue %1$s",
                            issue.toString());
                }
                return;
            }

            if (configuration.isIgnored(context, issue, location, message)) {
                return;
            }

            if (severity == Severity.IGNORE) {
                return;
            }

            mDelegate.report(context, issue, severity, location, message, format);
        }

        // Everything else just delegates to the embedding lint client

        @Override
        @NonNull
        public Configuration getConfiguration(@NonNull Project project,
          @Nullable LintDriver driver) {
            return mDelegate.getConfiguration(project, driver);
        }

        @Override
        public void log(@NonNull Severity severity, @Nullable Throwable exception,
                @Nullable String format, @Nullable Object... args) {
            mDelegate.log(exception, format, args);
        }

        @Override
        @NonNull
        public String readFile(@NonNull File file) {
            return mDelegate.readFile(file);
        }

        @Override
        @NonNull
        public byte[] readBytes(@NonNull File file) throws IOException {
            return mDelegate.readBytes(file);
        }

        @Override
        @NonNull
        public List<File> getJavaSourceFolders(@NonNull Project project) {
            return mDelegate.getJavaSourceFolders(project);
        }

        @Override
        @NonNull
        public List<File> getJavaClassFolders(@NonNull Project project) {
            return mDelegate.getJavaClassFolders(project);
        }

        @NonNull
        @Override
        public List<File> getJavaLibraries(@NonNull Project project, boolean includeProvided) {
            return mDelegate.getJavaLibraries(project, includeProvided);
        }

        @NonNull
        @Override
        public List<File> getTestSourceFolders(@NonNull Project project) {
            return mDelegate.getTestSourceFolders(project);
        }

        @Override
        public Collection<Project> getKnownProjects() {
            return mDelegate.getKnownProjects();
        }

        @Nullable
        @Override
        public BuildToolInfo getBuildTools(@NonNull Project project) {
            return mDelegate.getBuildTools(project);
        }

        @NonNull
        @Override
        public Map<String, String> createSuperClassMap(@NonNull Project project) {
            return mDelegate.createSuperClassMap(project);
        }

        @NonNull
        @Override
        public ResourceVisibilityLookup.Provider getResourceVisibilityProvider() {
            return mDelegate.getResourceVisibilityProvider();
        }

        @Override
        @NonNull
        public List<File> getResourceFolders(@NonNull Project project) {
            return mDelegate.getResourceFolders(project);
        }

        @Override
        @Nullable
        public XmlParser getXmlParser() {
            return mDelegate.getXmlParser();
        }

        @Override
        @NonNull
        public Class<? extends Detector> replaceDetector(
                @NonNull Class<? extends Detector> detectorClass) {
            return mDelegate.replaceDetector(detectorClass);
        }

        @Override
        @NonNull
        public SdkInfo getSdkInfo(@NonNull Project project) {
            return mDelegate.getSdkInfo(project);
        }

        @Override
        @NonNull
        public Project getProject(@NonNull File dir, @NonNull File referenceDir) {
            return mDelegate.getProject(dir, referenceDir);
        }

        @Nullable
        @Override
        public JavaParser getJavaParser(@Nullable Project project) {
            return mDelegate.getJavaParser(project);
        }

        @Override
        public File findResource(@NonNull String relativePath) {
            return mDelegate.findResource(relativePath);
        }

        @Override
        @Nullable
        public File getCacheDir(boolean create) {
            return mDelegate.getCacheDir(create);
        }

        @Override
        @NonNull
        protected ClassPathInfo getClassPath(@NonNull Project project) {
            return mDelegate.getClassPath(project);
        }

        @Override
        public void log(@Nullable Throwable exception, @Nullable String format,
                @Nullable Object... args) {
            mDelegate.log(exception, format, args);
        }

        @Override
        @Nullable
        public File getSdkHome() {
            return mDelegate.getSdkHome();
        }

        @Override
        @NonNull
        public IAndroidTarget[] getTargets() {
            return mDelegate.getTargets();
        }

        @Nullable
        @Override
        public AndroidSdkHandler getSdk() {
            return mDelegate.getSdk();
        }

        @Nullable
        @Override
        public IAndroidTarget getCompileTarget(@NonNull Project project) {
            return mDelegate.getCompileTarget(project);
        }

        @Override
        public int getHighestKnownApiLevel() {
            return mDelegate.getHighestKnownApiLevel();
        }

        @Override
        @Nullable
        public String getSuperClass(@NonNull Project project, @NonNull String name) {
            return mDelegate.getSuperClass(project, name);
        }

        @Override
        @Nullable
        public Boolean isSubclassOf(@NonNull Project project, @NonNull String name,
                @NonNull String superClassName) {
            return mDelegate.isSubclassOf(project, name, superClassName);
        }

        @Override
        @NonNull
        public String getProjectName(@NonNull Project project) {
            return mDelegate.getProjectName(project);
        }

        @Override
        public boolean isGradleProject(Project project) {
            return mDelegate.isGradleProject(project);
        }

        @NonNull
        @Override
        protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
            return mDelegate.createProject(dir, referenceDir);
        }

        @NonNull
        @Override
        public List<File> findGlobalRuleJars() {
            return mDelegate.findGlobalRuleJars();
        }

        @NonNull
        @Override
        public List<File> findRuleJars(@NonNull Project project) {
            return mDelegate.findRuleJars(project);
        }

        @Override
        public boolean isProjectDirectory(@NonNull File dir) {
            return mDelegate.isProjectDirectory(dir);
        }

        @Override
        public void registerProject(@NonNull File dir, @NonNull Project project) {
            log(Severity.WARNING, null, "Too late to register projects");
            mDelegate.registerProject(dir, project);
        }

        @Override
        public IssueRegistry addCustomLintRules(@NonNull IssueRegistry registry) {
            return mDelegate.addCustomLintRules(registry);
        }

        @NonNull
        @Override
        public List<File> getAssetFolders(@NonNull Project project) {
            return mDelegate.getAssetFolders(project);
        }

        @Override
        public ClassLoader createUrlClassLoader(@NonNull URL[] urls, @NonNull ClassLoader parent) {
            return mDelegate.createUrlClassLoader(urls, parent);
        }

        @Override
        public boolean checkForSuppressComments() {
            return mDelegate.checkForSuppressComments();
        }

        @Override
        public boolean supportsProjectResources() {
            return mDelegate.supportsProjectResources();
        }

        @Nullable
        @Override
        public AbstractResourceRepository getProjectResources(Project project,
                boolean includeDependencies) {
            return mDelegate.getProjectResources(project, includeDependencies);
        }

        @NonNull
        @Override
        public Location.Handle createResourceItemHandle(@NonNull ResourceItem item) {
            return mDelegate.createResourceItemHandle(item);
        }

        @Nullable
        @Override
        public URLConnection openConnection(@NonNull URL url) throws IOException {
            return mDelegate.openConnection(url);
        }

        @Override
        public void closeConnection(@NonNull URLConnection connection) throws IOException {
            mDelegate.closeConnection(connection);
        }
    }

    /**
     * Requests another pass through the data for the given detector. This is
     * typically done when a detector needs to do more expensive computation,
     * but it only wants to do this once it <b>knows</b> that an error is
     * present, or once it knows more specifically what to check for.
     *
     * @param detector the detector that should be included in the next pass.
     *            Note that the lint runner may refuse to run more than a couple
     *            of runs.
     * @param scope the scope to be revisited. This must be a subset of the
     *       current scope ({@link #getScope()}, and it is just a performance hint;
     *       in particular, the detector should be prepared to be called on other
     *       scopes as well (since they may have been requested by other detectors).
     *       You can pall null to indicate "all".
     */
    public void requestRepeat(@NonNull Detector detector, @Nullable EnumSet<Scope> scope) {
        if (mRepeatingDetectors == null) {
            mRepeatingDetectors = new ArrayList<Detector>();
        }
        mRepeatingDetectors.add(detector);

        if (scope != null) {
            if (mRepeatScope == null) {
                mRepeatScope = scope;
            } else {
                mRepeatScope = EnumSet.copyOf(mRepeatScope);
                mRepeatScope.addAll(scope);
            }
        } else {
            mRepeatScope = Scope.ALL;
        }
    }

    // Unfortunately, ASMs nodes do not extend a common DOM node type with parent
    // pointers, so we have to have multiple methods which pass in each type
    // of node (class, method, field) to be checked.

    /**
     * Returns whether the given issue is suppressed in the given method.
     *
     * @param issue the issue to be checked, or null to just check for "all"
     * @param classNode the class containing the issue
     * @param method the method containing the issue
     * @param instruction the instruction within the method, if any
     * @return true if there is a suppress annotation covering the specific
     *         issue on this method
     */
    public boolean isSuppressed(
            @Nullable Issue issue,
            @NonNull ClassNode classNode,
            @NonNull MethodNode method,
            @Nullable AbstractInsnNode instruction) {
        if (method.invisibleAnnotations != null) {
            @SuppressWarnings("unchecked")
            List<AnnotationNode> annotations = method.invisibleAnnotations;
            return isSuppressed(issue, annotations);
        }

        // Initializations of fields end up placed in generated methods (<init>
        // for members and <clinit> for static fields).
        if (instruction != null && method.name.charAt(0) == '<') {
            AbstractInsnNode next = LintUtils.getNextInstruction(instruction);
            if (next != null && next.getType() == AbstractInsnNode.FIELD_INSN) {
                FieldInsnNode fieldRef = (FieldInsnNode) next;
                FieldNode field = findField(classNode, fieldRef.owner, fieldRef.name);
                if (field != null && isSuppressed(issue, field)) {
                    return true;
                }
            } else if (classNode.outerClass != null && classNode.outerMethod == null
                        && isAnonymousClass(classNode)) {
                if (isSuppressed(issue, classNode)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Nullable
    private static MethodInsnNode findConstructorInvocation(
            @NonNull MethodNode method,
            @NonNull String className) {
        InsnList nodes = method.instructions;
        for (int i = 0, n = nodes.size(); i < n; i++) {
            AbstractInsnNode instruction = nodes.get(i);
            if (instruction.getOpcode() == Opcodes.INVOKESPECIAL) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (className.equals(call.owner)) {
                    return call;
                }
            }
        }

        return null;
    }

    @Nullable
    private FieldNode findField(
            @NonNull ClassNode classNode,
            @NonNull String owner,
            @NonNull String name) {
        ClassNode current = classNode;
        while (current != null) {
            if (owner.equals(current.name)) {
                @SuppressWarnings("rawtypes") // ASM API
                List fieldList = current.fields;
                for (Object f : fieldList) {
                    FieldNode field = (FieldNode) f;
                    if (field.name.equals(name)) {
                        return field;
                    }
                }
                return null;
            }
            current = getOuterClassNode(current);
        }
        return null;
    }

    @Nullable
    private MethodNode findMethod(
            @NonNull ClassNode classNode,
            @NonNull String name,
            boolean includeInherited) {
        ClassNode current = classNode;
        while (current != null) {
            @SuppressWarnings("rawtypes") // ASM API
            List methodList = current.methods;
            for (Object f : methodList) {
                MethodNode method = (MethodNode) f;
                if (method.name.equals(name)) {
                    return method;
                }
            }

            if (includeInherited) {
                current = getOuterClassNode(current);
            } else {
                break;
            }
        }
        return null;
    }

    /**
     * Returns whether the given issue is suppressed for the given field.
     *
     * @param issue the issue to be checked, or null to just check for "all"
     * @param field the field potentially annotated with a suppress annotation
     * @return true if there is a suppress annotation covering the specific
     *         issue on this field
     */
    @SuppressWarnings("MethodMayBeStatic") // API; reserve need to require driver state later
    public boolean isSuppressed(@Nullable Issue issue, @NonNull FieldNode field) {
        if (field.invisibleAnnotations != null) {
            @SuppressWarnings("unchecked")
            List<AnnotationNode> annotations = field.invisibleAnnotations;
            return isSuppressed(issue, annotations);
        }

        return false;
    }

    /**
     * Returns whether the given issue is suppressed in the given class.
     *
     * @param issue the issue to be checked, or null to just check for "all"
     * @param classNode the class containing the issue
     * @return true if there is a suppress annotation covering the specific
     *         issue in this class
     */
    public boolean isSuppressed(@Nullable Issue issue, @NonNull ClassNode classNode) {
        if (classNode.invisibleAnnotations != null) {
            @SuppressWarnings("unchecked")
            List<AnnotationNode> annotations = classNode.invisibleAnnotations;
            return isSuppressed(issue, annotations);
        }

        if (classNode.outerClass != null && classNode.outerMethod == null
                && isAnonymousClass(classNode)) {
            ClassNode outer = getOuterClassNode(classNode);
            if (outer != null) {
                MethodNode m = findMethod(outer, CONSTRUCTOR_NAME, false);
                if (m != null) {
                    MethodInsnNode call = findConstructorInvocation(m, classNode.name);
                    if (call != null) {
                        if (isSuppressed(issue, outer, m, call)) {
                            return true;
                        }
                    }
                }
                m = findMethod(outer, CLASS_CONSTRUCTOR, false);
                if (m != null) {
                    MethodInsnNode call = findConstructorInvocation(m, classNode.name);
                    if (call != null) {
                        if (isSuppressed(issue, outer, m, call)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean isSuppressed(@Nullable Issue issue, List<AnnotationNode> annotations) {
        for (AnnotationNode annotation : annotations) {
            String desc = annotation.desc;

            // We could obey @SuppressWarnings("all") too, but no need to look for it
            // because that annotation only has source retention.

            if (desc.endsWith(SUPPRESS_LINT_VMSIG)) {
                if (annotation.values != null) {
                    for (int i = 0, n = annotation.values.size(); i < n; i += 2) {
                        String key = (String) annotation.values.get(i);
                        if (key.equals("value")) {   //$NON-NLS-1$
                            Object value = annotation.values.get(i + 1);
                            if (value instanceof String) {
                                String id = (String) value;
                                if (matches(issue, id)) {
                                    return true;
                                }
                            } else if (value instanceof List) {
                                @SuppressWarnings("rawtypes")
                                List list = (List) value;
                                for (Object v : list) {
                                    if (v instanceof String) {
                                        String id = (String) v;
                                        if (matches(issue, id)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean matches(@Nullable Issue issue, @NonNull String id) {
        if (id.equalsIgnoreCase(SUPPRESS_ALL)) {
            return true;
        }

        if (issue != null) {
            String issueId = issue.getId();
            if (id.equalsIgnoreCase(issueId)) {
                return true;
            }
            if (id.startsWith(STUDIO_ID_PREFIX)
                && id.regionMatches(true, STUDIO_ID_PREFIX.length(), issueId, 0, issueId.length())
                && id.substring(STUDIO_ID_PREFIX.length()).equalsIgnoreCase(issueId)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if the given issue is suppressed by the given suppress string; this
     * is typically the same as the issue id, but is allowed to not match case sensitively,
     * and is allowed to be a comma separated list, and can be the string "all"
     *
     * @param issue  the issue id to match
     * @param string the suppress string -- typically the id, or "all", or a comma separated list
     *               of ids
     * @return true if the issue is suppressed by the given string
     */
    private static boolean isSuppressed(@NonNull Issue issue, @NonNull String string) {
        if (string.isEmpty()) {
            return false;
        }

        if (string.indexOf(',') == -1) {
            if (matches(issue, string)) {
                return true;
            }
        } else {
            for (String id : Splitter.on(',').trimResults().split(string)) {
                if (matches(issue, id)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether the given issue is suppressed in the given parse tree node.
     *
     * @param context the context for the source being scanned
     * @param issue the issue to be checked, or null to just check for "all"
     * @param scope the AST node containing the issue
     * @return true if there is a suppress annotation covering the specific
     *         issue in this class
     */
    public boolean isSuppressed(@Nullable JavaContext context, @NonNull Issue issue,
            @Nullable Node scope) {
        boolean checkComments = mClient.checkForSuppressComments() &&
                context != null && context.containsCommentSuppress();
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == VariableDefinition.class) {
                // Variable
                VariableDefinition declaration = (VariableDefinition) scope;
                if (isSuppressed(issue, declaration.astModifiers())) {
                    return true;
                }
            } else if (type == MethodDeclaration.class) {
                // Method
                // Look for annotations on the method
                MethodDeclaration declaration = (MethodDeclaration) scope;
                if (isSuppressed(issue, declaration.astModifiers())) {
                    return true;
                }
            } else if (type == ConstructorDeclaration.class) {
                // Constructor
                // Look for annotations on the method
                ConstructorDeclaration declaration = (ConstructorDeclaration) scope;
                if (isSuppressed(issue, declaration.astModifiers())) {
                    return true;
                }
            } else if (TypeDeclaration.class.isAssignableFrom(type)) {
                // Class, annotation, enum, interface
                TypeDeclaration declaration = (TypeDeclaration) scope;
                if (isSuppressed(issue, declaration.astModifiers())) {
                    return true;
                }
            } else if (type == AnnotationMethodDeclaration.class) {
                // Look for annotations on the method
                AnnotationMethodDeclaration declaration = (AnnotationMethodDeclaration) scope;
                if (isSuppressed(issue, declaration.astModifiers())) {
                    return true;
                }
            }

            if (checkComments && context.isSuppressedWithComment(scope, issue)) {
                return true;
            }

            scope = scope.getParent();
        }

        return false;
    }

    public boolean isSuppressed(@Nullable JavaContext context, @NonNull Issue issue,
            @Nullable UElement scope) {
        boolean checkComments = mClient.checkForSuppressComments() &&
                context != null && context.containsCommentSuppress();
        while (scope != null) {
            if (scope instanceof PsiModifierListOwner) {
                PsiModifierListOwner owner = (PsiModifierListOwner) scope;
                if (isSuppressed(issue, owner.getModifierList())) {
                    return true;
                }
            }

            if (checkComments && context.isSuppressedWithComment(scope, issue)) {
                return true;
            }

            scope = scope.getUastParent();
            if (scope instanceof PsiFile) {
                return false;
            }
        }

        return false;
    }
    
    public boolean isSuppressed(@Nullable JavaContext context, @NonNull Issue issue,
            @Nullable PsiElement scope) {
        boolean checkComments = mClient.checkForSuppressComments() &&
                context != null && context.containsCommentSuppress();
        while (scope != null) {
            if (scope instanceof PsiModifierListOwner) {
                PsiModifierListOwner owner = (PsiModifierListOwner) scope;
                if (isSuppressed(issue, owner.getModifierList())) {
                    return true;
                }
            }

            if (checkComments && context.isSuppressedWithComment(scope, issue)) {
                return true;
            }

            scope = scope.getParent();
            if (scope instanceof PsiFile) {
                return false;
            }
        }

        return false;
    }

    /**
     * Returns true if the given AST modifier has a suppress annotation for the
     * given issue (which can be null to check for the "all" annotation)
     *
     * @param issue the issue to be checked
     * @param modifiers the modifier to check
     * @return true if the issue or all issues should be suppressed for this
     *         modifier
     */
    private static boolean isSuppressed(@Nullable Issue issue, @Nullable Modifiers modifiers) {
        if (modifiers == null) {
            return false;
        }
        StrictListAccessor<Annotation, Modifiers> annotations = modifiers.astAnnotations();
        if (annotations == null) {
            return false;
        }

        for (Annotation annotation : annotations) {
            TypeReference t = annotation.astAnnotationTypeReference();
            String typeName = t.getTypeName();
            if (typeName.endsWith(SUPPRESS_LINT)
                    || typeName.endsWith("SuppressWarnings")) {     //$NON-NLS-1$
                StrictListAccessor<AnnotationElement, Annotation> values =
                        annotation.astElements();
                if (values != null) {
                    for (AnnotationElement element : values) {
                        AnnotationValue valueNode = element.astValue();
                        if (valueNode == null) {
                            continue;
                        }
                        if (valueNode instanceof StringLiteral) {
                            StringLiteral literal = (StringLiteral) valueNode;
                            String value = literal.astValue();
                            if (matches(issue, value)) {
                                return true;
                            }
                        } else if (valueNode instanceof ArrayInitializer) {
                            ArrayInitializer array = (ArrayInitializer) valueNode;
                            StrictListAccessor<Expression, ArrayInitializer> expressions =
                                    array.astExpressions();
                            if (expressions == null) {
                                continue;
                            }
                            for (Expression arrayElement : expressions) {
                                if (arrayElement instanceof StringLiteral) {
                                    String value = ((StringLiteral) arrayElement).astValue();
                                    if (matches(issue, value)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    public static final String SUPPRESS_WARNINGS_FQCN = "java.lang.SuppressWarnings";


    /**
     * Returns true if the given AST modifier has a suppress annotation for the
     * given issue (which can be null to check for the "all" annotation)
     *
     * @param issue the issue to be checked
     * @param modifierList the modifier to check
     * @return true if the issue or all issues should be suppressed for this
     *         modifier
     */
    public static boolean isSuppressed(@NonNull Issue issue,
            @Nullable PsiModifierList modifierList) {
        if (modifierList == null) {
            return false;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String fqcn = annotation.getQualifiedName();
            if (fqcn != null && (fqcn.equals(FQCN_SUPPRESS_LINT)
                    || fqcn.equals(SUPPRESS_WARNINGS_FQCN)
                    || fqcn.equals(SUPPRESS_LINT))) { // when missing imports
                PsiAnnotationParameterList parameterList = annotation.getParameterList();
                for (PsiNameValuePair pair : parameterList.getAttributes()) {
                    if (isSuppressed(issue, pair.getValue())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns true if the annotation member value, assumed to be specified on a a SuppressWarnings
     * or SuppressLint annotation, specifies the given id (or "all").
     *
     * @param issue the issue to be checked
     * @param value     the member value to check
     * @return true if the issue or all issues should be suppressed for this modifier
     */
    public static boolean isSuppressed(@NonNull Issue issue,
            @Nullable PsiAnnotationMemberValue value) {
        if (value instanceof PsiLiteral) {
            PsiLiteral literal = (PsiLiteral)value;
            Object literalValue = literal.getValue();
            if (literalValue instanceof String) {
                if (isSuppressed(issue, (String) literalValue)) {
                    return true;
                }
            }
        } else if (value instanceof PsiArrayInitializerMemberValue) {
            PsiArrayInitializerMemberValue mv = (PsiArrayInitializerMemberValue)value;
            for (PsiAnnotationMemberValue mmv : mv.getInitializers()) {
                if (isSuppressed(issue, mmv)) {
                    return true;
                }
            }
        } else if (value instanceof PsiArrayInitializerExpression) {
            PsiArrayInitializerExpression expression = (PsiArrayInitializerExpression) value;
            PsiExpression[] initializers = expression.getInitializers();
            for (PsiExpression e : initializers) {
                if (isSuppressed(issue, e)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether the given issue is suppressed in the given XML DOM node.
     *
     * @param issue the issue to be checked, or null to just check for "all"
     * @param node the DOM node containing the issue
     * @return true if there is a suppress annotation covering the specific
     *         issue in this class
     */
    public boolean isSuppressed(@Nullable XmlContext context, @NonNull Issue issue,
            @Nullable org.w3c.dom.Node node) {
        if (node instanceof Attr) {
            node = ((Attr) node).getOwnerElement();
        }
        boolean checkComments = mClient.checkForSuppressComments()
                && context != null && context.containsCommentSuppress();
        while (node != null) {
            if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                Element element = (Element) node;
                if (element.hasAttributeNS(TOOLS_URI, ATTR_IGNORE)) {
                    String ignore = element.getAttributeNS(TOOLS_URI, ATTR_IGNORE);
                    if (isSuppressed(issue, ignore)) {
                        return true;
                    }
                } else if (checkComments && context.isSuppressedWithComment(node, issue)) {
                    return true;
                }
            }

            node = node.getParentNode();
        }

        return false;
    }

    private File mCachedFolder = null;
    private int mCachedFolderVersion = -1;
    /** Pattern for version qualifiers */
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v(\\d+)$"); //$NON-NLS-1$

    /**
     * Returns the folder version of the given file. For example, for the file values-v14/foo.xml,
     * it returns 14.
     *
     * @param resourceFile the file to be checked
     * @return the folder version, or -1 if no specific version was specified
     */
    public int getResourceFolderVersion(@NonNull File resourceFile) {
        File parent = resourceFile.getParentFile();
        if (parent == null) {
            return -1;
        }
        if (parent.equals(mCachedFolder)) {
            return mCachedFolderVersion;
        }

        mCachedFolder = parent;
        mCachedFolderVersion = -1;

        for (String qualifier : QUALIFIER_SPLITTER.split(parent.getName())) {
            Matcher matcher = VERSION_PATTERN.matcher(qualifier);
            if (matcher.matches()) {
                String group = matcher.group(1);
                assert group != null;
                mCachedFolderVersion = Integer.parseInt(group);
                break;
            }
        }

        return mCachedFolderVersion;
    }

}
