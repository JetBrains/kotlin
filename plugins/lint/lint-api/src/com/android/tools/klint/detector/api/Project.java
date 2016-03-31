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

package com.android.tools.lint.detector.api;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.build.FilterData;
import com.android.build.OutputFile;
import com.android.builder.model.*;
import com.android.ide.common.repository.ResourceVisibilityLookup;
import com.android.resources.Density;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.client.api.CircularDependencyException;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.SdkInfo;
import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.SdkConstants.*;
import static com.android.sdklib.SdkVersionInfo.HIGHEST_KNOWN_API;

/**
 * A project contains information about an Android project being scanned for
 * Lint errors.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public class Project {
    protected final LintClient mClient;
    protected final File mDir;
    protected final File mReferenceDir;
    protected Configuration mConfiguration;
    protected String mPackage;
    protected int mBuildSdk = -1;
    protected IAndroidTarget mTarget;

    protected AndroidVersion mManifestMinSdk = AndroidVersion.DEFAULT;
    protected AndroidVersion mManifestTargetSdk = AndroidVersion.DEFAULT;

    protected boolean mLibrary;
    protected String mName;
    protected String mProguardPath;
    protected boolean mMergeManifests;

    /** The SDK info, if any */
    protected SdkInfo mSdkInfo;

    /**
     * If non null, specifies a non-empty list of specific files under this
     * project which should be checked.
     */
    protected List<File> mFiles;
    protected List<File> mProguardFiles;
    protected List<File> mGradleFiles;
    protected List<File> mManifestFiles;
    protected List<File> mJavaSourceFolders;
    protected List<File> mJavaClassFolders;
    protected List<File> mJavaLibraries;
    protected List<File> mTestSourceFolders;
    protected List<File> mResourceFolders;
    protected List<Project> mDirectLibraries;
    protected List<Project> mAllLibraries;
    protected boolean mReportIssues = true;
    protected Boolean mGradleProject;
    protected Boolean mSupportLib;
    protected Boolean mAppCompat;
    private Map<String, String> mSuperClassMap;
    private ResourceVisibilityLookup mResourceVisibility;

    /**
     * Creates a new {@link Project} for the given directory.
     *
     * @param client the tool running the lint check
     * @param dir the root directory of the project
     * @param referenceDir See {@link #getReferenceDir()}.
     * @return a new {@link Project}
     */
    @NonNull
    public static Project create(
            @NonNull LintClient client,
            @NonNull File dir,
            @NonNull File referenceDir) {
        return new Project(client, dir, referenceDir);
    }

    /**
     * Returns true if this project is a Gradle-based Android project
     *
     * @return true if this is a Gradle-based project
     */
    public boolean isGradleProject() {
        if (mGradleProject == null) {
            mGradleProject = mClient.isGradleProject(this);
        }

        return mGradleProject;
    }

    /**
     * Returns true if this project is an Android project.
     *
     * @return true if this project is an Android project.
     */
    @SuppressWarnings("MethodMayBeStatic")
    public boolean isAndroidProject() {
        return true;
    }

    /**
     * Returns the project model for this project if it corresponds to
     * a Gradle project. This is the case if {@link #isGradleProject()}
     * is true and {@link #isLibrary()} is false.
     *
     * @return the project model, or null
     */
    @Nullable
    public AndroidProject getGradleProjectModel() {
        return null;
    }

    /**
     * Returns the project model for this project if it corresponds to
     * a Gradle library. This is the case if both
     * {@link #isGradleProject()} and {@link #isLibrary()} return true.
     *
     * @return the project model, or null
     */
    @SuppressWarnings("UnusedDeclaration")
    @Nullable
    public AndroidLibrary getGradleLibraryModel() {
        return null;
    }

    /**
     * Returns the current selected variant, if any (and if the current project is a Gradle
     * project). This can be used by incremental lint rules to warn about problems in the current
     * context. Lint rules should however strive to perform cross variant analysis and warn about
     * problems in any configuration.
     *
     * @return the select variant, or null
     */
    @Nullable
    public Variant getCurrentVariant() {
        return null;
    }

    /** Creates a new Project. Use one of the factory methods to create. */
    protected Project(
            @NonNull LintClient client,
            @NonNull File dir,
            @NonNull File referenceDir) {
        mClient = client;
        mDir = dir;
        mReferenceDir = referenceDir;
        initialize();
    }

    protected void initialize() {
        // Default initialization: Use ADT/ant style project.properties file
        try {
            // Read properties file and initialize library state
            Properties properties = new Properties();
            File propFile = new File(mDir, PROJECT_PROPERTIES);
            if (propFile.exists()) {
                BufferedInputStream is = new BufferedInputStream(new FileInputStream(propFile));
                try {
                    properties.load(is);
                    String value = properties.getProperty(ANDROID_LIBRARY);
                    mLibrary = VALUE_TRUE.equals(value);
                    String proguardPath = properties.getProperty(PROGUARD_CONFIG);
                    if (proguardPath != null) {
                        mProguardPath = proguardPath;
                    }
                    mMergeManifests = VALUE_TRUE.equals(properties.getProperty(
                            "manifestmerger.enabled")); //$NON-NLS-1$
                    String target = properties.getProperty("target"); //$NON-NLS-1$
                    if (target != null) {
                        int index = target.lastIndexOf('-');
                        if (index == -1) {
                            index = target.lastIndexOf(':');
                        }
                        if (index != -1) {
                            String versionString = target.substring(index + 1);
                            try {
                                mBuildSdk = Integer.parseInt(versionString);
                            } catch (NumberFormatException nufe) {
                                mClient.log(Severity.WARNING, null,
                                        "Unexpected build target format: %1$s", target);
                            }
                        }
                    }

                    for (int i = 1; i < 1000; i++) {
                        String key = String.format(ANDROID_LIBRARY_REFERENCE_FORMAT, i);
                        String library = properties.getProperty(key);
                        if (library == null || library.isEmpty()) {
                            // No holes in the numbering sequence is allowed
                            break;
                        }

                        File libraryDir = new File(mDir, library).getCanonicalFile();

                        if (mDirectLibraries == null) {
                            mDirectLibraries = new ArrayList<Project>();
                        }

                        // Adjust the reference dir to be a proper prefix path of the
                        // library dir
                        File libraryReferenceDir = mReferenceDir;
                        if (!libraryDir.getPath().startsWith(mReferenceDir.getPath())) {
                            // Symlinks etc might have been resolved, so do those to
                            // the reference dir as well
                            libraryReferenceDir = libraryReferenceDir.getCanonicalFile();
                            if (!libraryDir.getPath().startsWith(mReferenceDir.getPath())) {
                                File file = libraryReferenceDir;
                                while (file != null && !file.getPath().isEmpty()) {
                                    if (libraryDir.getPath().startsWith(file.getPath())) {
                                        libraryReferenceDir = file;
                                        break;
                                    }
                                    file = file.getParentFile();
                                }
                            }
                        }

                        try {
                            Project libraryPrj = mClient.getProject(libraryDir,
                                    libraryReferenceDir);
                            mDirectLibraries.add(libraryPrj);
                            // By default, we don't report issues in inferred library projects.
                            // The driver will set report = true for those library explicitly
                            // requested.
                            libraryPrj.setReportIssues(false);
                        } catch (CircularDependencyException e) {
                            e.setProject(this);
                            e.setLocation(Location.create(propFile));
                            throw e;
                        }
                    }
                } finally {
                    try {
                        Closeables.close(is, true /* swallowIOException */);
                    } catch (IOException e) {
                        // cannot happen
                    }
                }
            }
        } catch (IOException ioe) {
            mClient.log(ioe, "Initializing project state");
        }

        if (mDirectLibraries != null) {
            mDirectLibraries = Collections.unmodifiableList(mDirectLibraries);
        } else {
            mDirectLibraries = Collections.emptyList();
        }
    }

    @Override
    public String toString() {
        return "Project [dir=" + mDir + ']';
    }

    @Override
    public int hashCode() {
        return mDir.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        return mDir.equals(other.mDir);
    }

    /**
     * Adds the given file to the list of files which should be checked in this
     * project. If no files are added, the whole project will be checked.
     *
     * @param file the file to be checked
     */
    public void addFile(@NonNull File file) {
        if (mFiles == null) {
            mFiles = new ArrayList<File>();
        }
        mFiles.add(file);
    }

    /**
     * The list of files to be checked in this project. If null, the whole
     * project should be checked.
     *
     * @return the subset of files to be checked, or null for the whole project
     */
    @Nullable
    public List<File> getSubset() {
        return mFiles;
    }

    /**
     * Returns the list of source folders for Java source files
     *
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getJavaSourceFolders() {
        if (mJavaSourceFolders == null) {
            if (isAospFrameworksProject(mDir)) {
                return Collections.singletonList(new File(mDir, "java")); //$NON-NLS-1$
            }
            if (isAospBuildEnvironment()) {
                String top = getAospTop();
                if (mDir.getAbsolutePath().startsWith(top)) {
                    mJavaSourceFolders = getAospJavaSourcePath();
                    return mJavaSourceFolders;
                }
            }

            mJavaSourceFolders = mClient.getJavaSourceFolders(this);
        }

        return mJavaSourceFolders;
    }

    /**
     * Returns the list of output folders for class files
     * @return a list of output folders to search for .class files
     */
    @NonNull
    public List<File> getJavaClassFolders() {
        if (mJavaClassFolders == null) {
            if (isAospFrameworksProject(mDir)) {
                File top = mDir.getParentFile().getParentFile().getParentFile();
                if (top != null) {
                    File out = new File(top, "out");
                    if (out.exists()) {
                        String relative =
                            "target/common/obj/JAVA_LIBRARIES/framework_intermediates/classes.jar";
                        File jar = new File(out, relative.replace('/', File.separatorChar));
                        if (jar.exists()) {
                            mJavaClassFolders = Collections.singletonList(jar);
                            return mJavaClassFolders;
                        }
                    }
                }
            }
            if (isAospBuildEnvironment()) {
                String top = getAospTop();
                if (mDir.getAbsolutePath().startsWith(top)) {
                    mJavaClassFolders = getAospJavaClassPath();
                    return mJavaClassFolders;
                }
            }

            mJavaClassFolders = mClient.getJavaClassFolders(this);
        }
        return mJavaClassFolders;
    }

    /**
     * Returns the list of Java libraries (typically .jar files) that this
     * project depends on. Note that this refers to jar libraries, not Android
     * library projects which are processed in a separate pass with their own
     * source and class folders.
     *
     * @return a list of .jar files (or class folders) that this project depends
     *         on.
     */
    @NonNull
    public List<File> getJavaLibraries() {
        if (mJavaLibraries == null) {
            // AOSP builds already merge libraries and class folders into
            // the single classes.jar file, so these have already been processed
            // in getJavaClassFolders.

            mJavaLibraries = mClient.getJavaLibraries(this);
        }

        return mJavaLibraries;
    }

    /**
     * Returns the list of source folders for Java test source files
     *
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getTestSourceFolders() {
        if (mTestSourceFolders == null) {
            mTestSourceFolders = mClient.getTestSourceFolders(this);
        }

        return mTestSourceFolders;
    }

    /**
     * Returns the resource folder.
     *
     * @return a file pointing to the resource folder, or null if the project
     *         does not contain any resources
     */
    @NonNull
    public List<File> getResourceFolders() {
        if (mResourceFolders == null) {
            List<File> folders = mClient.getResourceFolders(this);

            if (folders.size() == 1 && isAospFrameworksProject(mDir)) {
                // No manifest file for this project: just init the manifest values here
                mManifestMinSdk = mManifestTargetSdk = new AndroidVersion(HIGHEST_KNOWN_API, null);
                File folder = new File(folders.get(0), RES_FOLDER);
                if (!folder.exists()) {
                    folders = Collections.emptyList();
                }
            }

            mResourceFolders = folders;
        }

        return mResourceFolders;

    }

    /**
     * Returns the relative path of a given file relative to the user specified
     * directory (which is often the project directory but sometimes a higher up
     * directory when a directory tree is being scanned
     *
     * @param file the file under this project to check
     * @return the path relative to the reference directory (often the project directory)
     */
    @NonNull
    public String getDisplayPath(@NonNull File file) {
       String path = file.getPath();
       String referencePath = mReferenceDir.getPath();
       if (path.startsWith(referencePath)) {
           int length = referencePath.length();
           if (path.length() > length && path.charAt(length) == File.separatorChar) {
               length++;
           }

           return path.substring(length);
       }

       return path;
    }

    /**
     * Returns the relative path of a given file within the current project.
     *
     * @param file the file under this project to check
     * @return the path relative to the project
     */
    @NonNull
    public String getRelativePath(@NonNull File file) {
       String path = file.getPath();
       String referencePath = mDir.getPath();
       if (path.startsWith(referencePath)) {
           int length = referencePath.length();
           if (path.length() > length && path.charAt(length) == File.separatorChar) {
               length++;
           }

           return path.substring(length);
       }

       return path;
    }

    /**
     * Returns the project root directory
     *
     * @return the dir
     */
    @NonNull
    public File getDir() {
        return mDir;
    }

    /**
     * Returns the original user supplied directory where the lint search
     * started. For example, if you run lint against {@code /tmp/foo}, and it
     * finds a project to lint in {@code /tmp/foo/dev/src/project1}, then the
     * {@code dir} is {@code /tmp/foo/dev/src/project1} and the
     * {@code referenceDir} is {@code /tmp/foo/}.
     *
     * @return the reference directory, never null
     */
    @NonNull
    public File getReferenceDir() {
        return mReferenceDir;
    }

    /**
     * Gets the configuration associated with this project
     *
     * @return the configuration associated with this project
     */
    @NonNull
    public Configuration getConfiguration() {
        if (mConfiguration == null) {
            mConfiguration = mClient.getConfiguration(this);
        }
        return mConfiguration;
    }

    /**
     * Returns the application package specified by the manifest
     *
     * @return the application package, or null if unknown
     */
    @Nullable
    public String getPackage() {
        //assert !mLibrary; // Should call getPackage on the master project, not the library
        // Assertion disabled because you might be running lint on a standalone library project.

        return mPackage;
    }

    /**
     * Returns the minimum API level for the project
     *
     * @return the minimum API level or {@link AndroidVersion#DEFAULT} if unknown
     */
    @NonNull
    public AndroidVersion getMinSdkVersion() {
        return mManifestMinSdk == null ? AndroidVersion.DEFAULT : mManifestMinSdk;
    }

    /**
     * Returns the minimum API <b>level</b> requested by the manifest, or -1 if not
     * specified. Use {@link #getMinSdkVersion()} to get a full version if you need
     * to check if the platform is a preview platform etc.
     *
     * @return the minimum API level or -1 if unknown
     */
    public int getMinSdk() {
        AndroidVersion version = getMinSdkVersion();
        return version == AndroidVersion.DEFAULT ? -1 : version.getApiLevel();
    }

    /**
     * Returns the target API level for the project
     *
     * @return the target API level or {@link AndroidVersion#DEFAULT} if unknown
     */
    @NonNull
    public AndroidVersion getTargetSdkVersion() {
        return mManifestTargetSdk == AndroidVersion.DEFAULT
                ? getMinSdkVersion() : mManifestTargetSdk;
    }

    /**
     * Returns the target API <b>level</b> specified by the manifest, or -1 if not
     * specified. Use {@link #getTargetSdkVersion()} to get a full version if you need
     * to check if the platform is a preview platform etc.
     *
     * @return the target API level or -1 if unknown
     */
    public int getTargetSdk() {
        AndroidVersion version = getTargetSdkVersion();
        return version == AndroidVersion.DEFAULT ? -1 : version.getApiLevel();
    }

    /**
     * Returns the target API used to build the project, or -1 if not known
     *
     * @return the build target API or -1 if unknown
     */
    public int getBuildSdk() {
        return mBuildSdk;
    }

    /**
     * Returns the target used to build the project, or null if not known
     *
     * @return the build target, or null
     */
    @Nullable
    public IAndroidTarget getBuildTarget() {
        if (mTarget == null) {
            mTarget = mClient.getCompileTarget(this);
        }

        return mTarget;
    }

    /**
     * Initialized the manifest state from the given manifest model
     *
     * @param document the DOM document for the manifest XML document
     */
    public void readManifest(@NonNull Document document) {
        Element root = document.getDocumentElement();
        if (root == null) {
            return;
        }

        mPackage = root.getAttribute(ATTR_PACKAGE);

        // Treat support libraries as non-reportable (in Eclipse where we don't
        // have binary libraries, the support libraries have to be source-copied into
        // the workspace which then triggers warnings in these libraries that users
        // shouldn't have to investigate)
        if (mPackage != null && mPackage.startsWith("android.support.")) {
            mReportIssues = false;
        }

        // Initialize minSdk and targetSdk
        mManifestMinSdk = AndroidVersion.DEFAULT;
        mManifestTargetSdk = AndroidVersion.DEFAULT;
        NodeList usesSdks = root.getElementsByTagName(TAG_USES_SDK);
        if (usesSdks.getLength() > 0) {
            Element element = (Element) usesSdks.item(0);

            String minSdk = null;
            if (element.hasAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION)) {
                minSdk = element.getAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION);
            }
            if (minSdk != null) {
                IAndroidTarget[] targets = mClient.getTargets();
                mManifestMinSdk = SdkVersionInfo.getVersion(minSdk, targets);
            }

            if (element.hasAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION)) {
                String targetSdk = element.getAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION);
                if (targetSdk != null) {
                    IAndroidTarget[] targets = mClient.getTargets();
                    mManifestTargetSdk = SdkVersionInfo.getVersion(targetSdk, targets);
                }
            } else {
                mManifestTargetSdk = mManifestMinSdk;
            }
        } else if (isAospBuildEnvironment()) {
            extractAospMinSdkVersion();
            mManifestTargetSdk = mManifestMinSdk;
        }
    }

    /**
     * Returns true if this project is an Android library project
     *
     * @return true if this project is an Android library project
     */
    public boolean isLibrary() {
        return mLibrary;
    }

    /**
     * Returns the list of library projects referenced by this project
     *
     * @return the list of library projects referenced by this project, never
     *         null
     */
    @NonNull
    public List<Project> getDirectLibraries() {
        return mDirectLibraries != null ? mDirectLibraries : Collections.<Project>emptyList();
    }

    /**
     * Returns the transitive closure of the library projects for this project
     *
     * @return the transitive closure of the library projects for this project
     */
    @NonNull
    public List<Project> getAllLibraries() {
        if (mAllLibraries == null) {
            if (mDirectLibraries.isEmpty()) {
                return mDirectLibraries;
            }

            List<Project> all = new ArrayList<Project>();
            Set<Project> seen = Sets.newHashSet();
            Set<Project> path = Sets.newHashSet();
            seen.add(this);
            path.add(this);
            addLibraryProjects(all, seen, path);
            mAllLibraries = all;
        }

        return mAllLibraries;
    }

    /**
     * Adds this project's library project and their library projects
     * recursively into the given collection of projects
     *
     * @param collection the collection to add the projects into
     * @param seen full set of projects we've processed
     * @param path the current path of library dependencies followed
     */
    private void addLibraryProjects(@NonNull Collection<Project> collection,
            @NonNull Set<Project> seen, @NonNull Set<Project> path) {
        for (Project library : mDirectLibraries) {
            if (seen.contains(library)) {
                if (path.contains(library)) {
                    mClient.log(Severity.WARNING, null,
                            "Internal lint error: cyclic library dependency for %1$s", library);
                }
                continue;
            }
            collection.add(library);
            seen.add(library);
            path.add(library);
            // Recurse
            library.addLibraryProjects(collection, seen, path);
            path.remove(library);
        }
    }

    /**
     * Gets the SDK info for the current project.
     *
     * @return the SDK info for the current project, never null
     */
    @NonNull
    public SdkInfo getSdkInfo() {
        if (mSdkInfo == null) {
            mSdkInfo = mClient.getSdkInfo(this);
        }

        return mSdkInfo;
    }

    /**
     * Gets the paths to the manifest files in this project, if any exists. The manifests
     * should be provided such that the main manifest comes first, then any flavor versions,
     * then any build types.
     *
     * @return the path to the manifest file, or null if it does not exist
     */
    @NonNull
    public List<File> getManifestFiles() {
        if (mManifestFiles == null) {
            File manifestFile = new File(mDir, ANDROID_MANIFEST_XML);
            if (manifestFile.exists()) {
                mManifestFiles = Collections.singletonList(manifestFile);
            } else {
                mManifestFiles = Collections.emptyList();
            }
        }

        return mManifestFiles;
    }

    /**
     * Returns the proguard files configured for this project, if any
     *
     * @return the proguard files, if any
     */
    @NonNull
    public List<File> getProguardFiles() {
        if (mProguardFiles == null) {
            List<File> files = new ArrayList<File>();
            if (mProguardPath != null) {
                Splitter splitter = Splitter.on(CharMatcher.anyOf(":;")); //$NON-NLS-1$
                for (String path : splitter.split(mProguardPath)) {
                    if (path.contains("${")) { //$NON-NLS-1$
                        // Don't analyze the global/user proguard files
                        continue;
                    }
                    File file = new File(path);
                    if (!file.isAbsolute()) {
                        file = new File(getDir(), path);
                    }
                    if (file.exists()) {
                        files.add(file);
                    }
                }
            }
            if (files.isEmpty()) {
                File file = new File(getDir(), OLD_PROGUARD_FILE);
                if (file.exists()) {
                    files.add(file);
                }
                file = new File(getDir(), FN_PROJECT_PROGUARD_FILE);
                if (file.exists()) {
                    files.add(file);
                }
            }
            mProguardFiles = files;
        }
        return mProguardFiles;
    }

    /**
     * Returns the Gradle build script files configured for this project, if any
     *
     * @return the Gradle files, if any
     */
    @NonNull
    public List<File> getGradleBuildScripts() {
        if (mGradleFiles == null) {
            if (isGradleProject()) {
                mGradleFiles = Lists.newArrayListWithExpectedSize(2);
                File build = new File(mDir, SdkConstants.FN_BUILD_GRADLE);
                if (build.exists()) {
                    mGradleFiles.add(build);
                }
                File settings = new File(mDir, SdkConstants.FN_SETTINGS_GRADLE);
                if (settings.exists()) {
                    mGradleFiles.add(settings);
                }
            } else {
                mGradleFiles = Collections.emptyList();
            }
        }

        return mGradleFiles;
    }

    /**
     * Returns the name of the project
     *
     * @return the name of the project, never null
     */
    @NonNull
    public String getName() {
        if (mName == null) {
            mName = mClient.getProjectName(this);
        }

        return mName;
    }

    /**
     * Sets the name of the project
     *
     * @param name the name of the project, never null
     */
    public void setName(@NonNull String name) {
        assert !name.isEmpty();
        mName = name;
    }

    /**
     * Sets whether lint should report issues in this project. See
     * {@link #getReportIssues()} for a full description of what that means.
     *
     * @param reportIssues whether lint should report issues in this project
     */
    public void setReportIssues(boolean reportIssues) {
        mReportIssues = reportIssues;
    }

    /**
     * Returns whether lint should report issues in this project.
     * <p>
     * If a user specifies a project and its library projects for analysis, then
     * those library projects are all "included", and all errors found in all
     * the projects are reported. But if the user is only running lint on the
     * main project, we shouldn't report errors in any of the library projects.
     * We still need to <b>consider</b> them for certain types of checks, such
     * as determining whether resources found in the main project are unused, so
     * the detectors must still get a chance to look at these projects. The
     * {@code #getReportIssues()} attribute is used for this purpose.
     *
     * @return whether lint should report issues in this project
     */
    public boolean getReportIssues() {
        return mReportIssues;
    }

    /**
     * Returns whether manifest merging is in effect
     *
     * @return true if manifests in library projects should be merged into main projects
     */
    public boolean isMergingManifests() {
        return mMergeManifests;
    }


    // ---------------------------------------------------------------------------
    // Support for running lint on the AOSP source tree itself

    private static Boolean sAospBuild;

    /** Is lint running in an AOSP build environment */
    private static boolean isAospBuildEnvironment() {
        if (sAospBuild == null) {
            sAospBuild = getAospTop() != null;
        }

        return sAospBuild;
    }

    /**
     * Is this the frameworks AOSP project? Needs some hardcoded support since
     * it doesn't have a manifest file, etc.
     *
     * @param dir the project directory to check
     * @return true if this looks like the frameworks/base/core project
     */
    public static boolean isAospFrameworksProject(@NonNull File dir) {
        if (!dir.getPath().endsWith("core")) { //$NON-NLS-1$
            return false;
        }

        File parent = dir.getParentFile();
        if (parent == null || !parent.getName().equals("base")) { //$NON-NLS-1$
            return false;
        }

        parent = parent.getParentFile();
        //noinspection RedundantIfStatement
        if (parent == null || !parent.getName().equals("frameworks")) { //$NON-NLS-1$
            return false;
        }

        return true;
    }

    /** Get the root AOSP dir, if any */
    private static String getAospTop() {
        return System.getenv("ANDROID_BUILD_TOP");   //$NON-NLS-1$
    }

    /** Get the host out directory in AOSP, if any */
    private static String getAospHostOut() {
        return System.getenv("ANDROID_HOST_OUT");    //$NON-NLS-1$
    }

    /** Get the product out directory in AOSP, if any */
    private static String getAospProductOut() {
        return System.getenv("ANDROID_PRODUCT_OUT"); //$NON-NLS-1$
    }

    private List<File> getAospJavaSourcePath() {
        List<File> sources = new ArrayList<File>(2);
        // Normal sources
        File src = new File(mDir, "src"); //$NON-NLS-1$
        if (src.exists()) {
            sources.add(src);
        }

        // Generates sources
        for (File dir : getIntermediateDirs()) {
            File classes = new File(dir, "src"); //$NON-NLS-1$
            if (classes.exists()) {
                sources.add(classes);
            }
        }

        if (sources.isEmpty()) {
            mClient.log(null,
                    "Warning: Could not find sources or generated sources for project %1$s",
                    getName());
        }

        return sources;
    }

    private List<File> getAospJavaClassPath() {
        List<File> classDirs = new ArrayList<File>(1);

        for (File dir : getIntermediateDirs()) {
            File classes = new File(dir, "classes"); //$NON-NLS-1$
            if (classes.exists()) {
                classDirs.add(classes);
            } else {
                classes = new File(dir, "classes.jar"); //$NON-NLS-1$
                if (classes.exists()) {
                    classDirs.add(classes);
                }
            }
        }

        if (classDirs.isEmpty()) {
            mClient.log(null,
                    "No bytecode found: Has the project been built? (%1$s)", getName());
        }

        return classDirs;
    }

    /** Find the _intermediates directories for a given module name */
    private List<File> getIntermediateDirs() {
        // See build/core/definitions.mk and in particular the "intermediates-dir-for" definition
        List<File> intermediates = new ArrayList<File>();

        // TODO: Look up the module name, e.g. LOCAL_MODULE. However,
        // some Android.mk files do some complicated things with it - and most
        // projects use the same module name as the directory name.
        String moduleName = mDir.getName();

        String top = getAospTop();
        final String[] outFolders = new String[] {
            top + "/out/host/common/obj",             //$NON-NLS-1$
            top + "/out/target/common/obj",           //$NON-NLS-1$
            getAospHostOut() + "/obj",                //$NON-NLS-1$
            getAospProductOut() + "/obj"              //$NON-NLS-1$
        };
        final String[] moduleClasses = new String[] {
                "APPS",                //$NON-NLS-1$
                "JAVA_LIBRARIES",      //$NON-NLS-1$
        };

        for (String out : outFolders) {
            assert new File(out.replace('/', File.separatorChar)).exists() : out;
            for (String moduleClass : moduleClasses) {
                String path = out + '/' + moduleClass + '/' + moduleName
                        + "_intermediates"; //$NON-NLS-1$
                File file = new File(path.replace('/', File.separatorChar));
                if (file.exists()) {
                    intermediates.add(file);
                }
            }
        }

        return intermediates;
    }

    private void extractAospMinSdkVersion() {
        // Is the SDK level specified by a Makefile?
        boolean found = false;
        File makefile = new File(mDir, "Android.mk"); //$NON-NLS-1$
        if (makefile.exists()) {
            try {
                List<String> lines = Files.readLines(makefile, Charsets.UTF_8);
                Pattern p = Pattern.compile("LOCAL_SDK_VERSION\\s*:=\\s*(.*)"); //$NON-NLS-1$
                for (String line : lines) {
                    line = line.trim();
                    Matcher matcher = p.matcher(line);
                    if (matcher.matches()) {
                        found = true;
                        String version = matcher.group(1);
                        if (version.equals("current")) { //$NON-NLS-1$
                            mManifestMinSdk = findCurrentAospVersion();
                        } else {
                            mManifestMinSdk = SdkVersionInfo.getVersion(version,
                                    mClient.getTargets());
                        }
                        break;
                    }
                }
            } catch (IOException ioe) {
                mClient.log(ioe, null);
            }
        }

        if (!found) {
            mManifestMinSdk = findCurrentAospVersion();
        }
    }

    /** Cache for {@link #findCurrentAospVersion()} */
    private static AndroidVersion sCurrentVersion;

    /** In an AOSP build environment, identify the currently built image version, if available */
    private static AndroidVersion findCurrentAospVersion() {
        if (sCurrentVersion == null) {
            File apiDir = new File(getAospTop(), "frameworks/base/api" //$NON-NLS-1$
                    .replace('/', File.separatorChar));
            File[] apiFiles = apiDir.listFiles();
            if (apiFiles == null) {
                sCurrentVersion = AndroidVersion.DEFAULT;
                return sCurrentVersion;
            }
            int max = 1;
            for (File apiFile : apiFiles) {
                String name = apiFile.getName();
                int index = name.indexOf('.');
                if (index > 0) {
                    String base = name.substring(0, index);
                    if (Character.isDigit(base.charAt(0))) {
                        try {
                            int version = Integer.parseInt(base);
                            if (version > max) {
                                max = version;
                            }
                        } catch (NumberFormatException nufe) {
                            // pass
                        }
                    }
                }
            }
            sCurrentVersion = new AndroidVersion(max, null);
        }

        return sCurrentVersion;
    }

    /**
     * Returns true if this project depends on the given artifact. Note that
     * the project doesn't have to be a Gradle project; the artifact is just
     * an identifier for name a specific library, such as com.android.support:support-v4
     * to identify the support library
     *
     * @param artifact the Gradle/Maven name of a library
     * @return true if the library is installed, false if it is not, and null if
     *   we're not sure
     */
    @Nullable
    public Boolean dependsOn(@NonNull String artifact) {
        if (SUPPORT_LIB_ARTIFACT.equals(artifact)) {
            if (mSupportLib == null) {
                for (File file : getJavaLibraries()) {
                    String name = file.getName();
                    if (name.equals("android-support-v4.jar")      //$NON-NLS-1$
                            || name.startsWith("support-v4-")) {   //$NON-NLS-1$
                        mSupportLib = true;
                        break;
                    }
                }
                if (mSupportLib == null) {
                    for (Project dependency : getDirectLibraries()) {
                        Boolean b = dependency.dependsOn(artifact);
                        if (b != null && b) {
                            mSupportLib = true;
                            break;
                        }
                    }
                }
                if (mSupportLib == null) {
                    mSupportLib = false;
                }
            }

            return mSupportLib;
        } else if (APPCOMPAT_LIB_ARTIFACT.equals(artifact)) {
            if (mAppCompat == null) {
                for (File file : getJavaLibraries()) {
                    String name = file.getName();
                    if (name.startsWith("appcompat-v7-")) { //$NON-NLS-1$
                        mAppCompat = true;
                        break;
                    }
                }
                if (mAppCompat == null) {
                    for (Project dependency : getDirectLibraries()) {
                        Boolean b = dependency.dependsOn(artifact);
                        if (b != null && b) {
                            mAppCompat = true;
                            break;
                        }
                    }
                }
                if (mAppCompat == null) {
                    mAppCompat = false;
                }
            }

            return mAppCompat;
        }

        return null;
    }

    private List<String> mCachedApplicableDensities;

    /**
     * Returns the set of applicable densities for this project. If null, there are no density
     * restrictions and all densities apply.
     *
     * @return the list of specific densities that apply in this project, or null if all densities
     * apply
     */
    @Nullable
    public List<String> getApplicableDensities() {
        if (mCachedApplicableDensities == null) {
            // Use the gradle API to set up relevant densities. For example, if the
            // build.gradle file contains this:
            // android {
            //     defaultConfig {
            //         resConfigs "nodpi", "hdpi"
            //     }
            // }
            // ...then we should only enforce hdpi densities, not all these others!
            if (isGradleProject() && getGradleProjectModel() != null &&
                    getCurrentVariant() != null) {
                Set<String> relevantDensities = Sets.newHashSet();
                Variant variant = getCurrentVariant();
                List<String> variantFlavors = variant.getProductFlavors();
                AndroidProject gradleProjectModel = getGradleProjectModel();

                addResConfigsFromFlavor(relevantDensities, null,
                        getGradleProjectModel().getDefaultConfig());
                for (ProductFlavorContainer container : gradleProjectModel.getProductFlavors()) {
                    addResConfigsFromFlavor(relevantDensities, variantFlavors, container);
                }

                // Are there any splits that specify densities?
                if (relevantDensities.isEmpty()) {
                    AndroidArtifact mainArtifact = variant.getMainArtifact();
                    Collection<AndroidArtifactOutput> outputs = mainArtifact.getOutputs();
                    for (AndroidArtifactOutput output : outputs) {
                        for (OutputFile file : output.getOutputs()) {
                            final String DENSITY_NAME = OutputFile.FilterType.DENSITY.name();
                            if (file.getFilterTypes().contains(DENSITY_NAME)) {
                                for (FilterData data : file.getFilters()) {
                                    if (DENSITY_NAME.equals(data.getFilterType())) {
                                        relevantDensities.add(data.getIdentifier());
                                    }
                                }
                            }
                        }
                    }
                }

                if (!relevantDensities.isEmpty()) {
                    mCachedApplicableDensities = Lists.newArrayListWithExpectedSize(10);
                    for (String density : relevantDensities) {
                        String folder = ResourceFolderType.DRAWABLE.getName() + '-' + density;
                        mCachedApplicableDensities.add(folder);
                    }
                    Collections.sort(mCachedApplicableDensities);
                } else {
                    mCachedApplicableDensities = Collections.emptyList();
                }
            } else {
                mCachedApplicableDensities = Collections.emptyList();
            }
        }

        return mCachedApplicableDensities.isEmpty() ? null : mCachedApplicableDensities;
    }

    /**
     * Returns a super class map for this project. The keys and values are internal
     * class names (e.g. java/lang/Integer, not java.lang.Integer).
     * @return a map, possibly empty but never null
     */
    @NonNull
    public Map<String, String> getSuperClassMap() {
        if (mSuperClassMap == null) {
            mSuperClassMap = mClient.createSuperClassMap(this);
        }

        return mSuperClassMap;
    }

    /**
     * Adds in the resConfig values specified by the given flavor container, assuming
     * it's in one of the relevant variantFlavors, into the given set
     */
    private static void addResConfigsFromFlavor(@NonNull Set<String> relevantDensities,
            @Nullable List<String> variantFlavors,
            @NonNull ProductFlavorContainer container) {
        ProductFlavor flavor = container.getProductFlavor();
        if (variantFlavors == null || variantFlavors.contains(flavor.getName())) {
            if (!flavor.getResourceConfigurations().isEmpty()) {
                for (String densityName : flavor.getResourceConfigurations()) {
                    Density density = Density.getEnum(densityName);
                    if (density != null && density.isRecommended()
                            && density != Density.NODPI && density != Density.ANYDPI) {
                        relevantDensities.add(densityName);
                    }
                }
            }
        }
    }

    /**
     * Returns a shared {@link ResourceVisibilityLookup}
     *
     * @return a shared provider for looking up resource visibility
     */
    @NonNull
    public ResourceVisibilityLookup getResourceVisibility() {
        if (mResourceVisibility == null) {
            if (isGradleProject()) {
                AndroidProject project = getGradleProjectModel();
                Variant variant = getCurrentVariant();
                if (project != null && variant != null) {
                    mResourceVisibility = mClient.getResourceVisibilityProvider().get(project,
                            variant);

                } else if (getGradleLibraryModel() != null) {
                    try {
                        mResourceVisibility = mClient.getResourceVisibilityProvider()
                                .get(getGradleLibraryModel());
                    } catch (Exception ignore) {
                        // Handle talking to older Gradle plugins (where we don't
                        // have access to the model version to check up front
                    }
                }
            }
            if (mResourceVisibility == null) {
                mResourceVisibility = ResourceVisibilityLookup.NONE;
            }
        }

        return mResourceVisibility;
    }

    /**
     * Returns the associated client
     *
     * @return the client
     */
    @NonNull
    public LintClient getClient() {
        return mClient;
    }

    /**
     * Returns the compile target to use for this project
     *
     * @return the compile target to use to build this project
     */
    @Nullable
    public IAndroidTarget getCompileTarget() {
        return mClient.getCompileTarget(this);
    }
}
