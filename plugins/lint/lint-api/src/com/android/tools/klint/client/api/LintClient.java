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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.ResourceVisibilityLookup;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.res2.ResourceItem;
import com.android.prefs.AndroidLocation;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Project;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.android.utils.XmlUtils;
import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import org.jetbrains.uast.UastLanguagePlugin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.detector.api.LintUtils.endsWith;

/**
 * Information about the tool embedding the lint analyzer. IDEs and other tools
 * implementing lint support will extend this to integrate logging, displaying errors,
 * etc.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public abstract class LintClient {
    private static final String PROP_BIN_DIR  = "com.android.tools.lint.bindir";  //$NON-NLS-1$

    /**
     * Returns a configuration for use by the given project. The configuration
     * provides information about which issues are enabled, any customizations
     * to the severity of an issue, etc.
     * <p>
     * By default this method returns a {@link DefaultConfiguration}.
     *
     * @param project the project to obtain a configuration for
     * @return a configuration, never null.
     */
    public Configuration getConfiguration(@NonNull Project project) {
        return DefaultConfiguration.create(this, project, null);
    }

    /**
     * Report the given issue. This method will only be called if the configuration
     * provided by {@link #getConfiguration(Project)} has reported the corresponding
     * issue as enabled and has not filtered out the issue with its
     * {@link Configuration#ignore(Context,Issue,Location,String)} method.
     * <p>
     * @param context the context used by the detector when the issue was found
     * @param issue the issue that was found
     * @param severity the severity of the issue
     * @param location the location of the issue
     * @param message the associated user message
     * @param format the format of the description and location descriptions
     */
    public abstract void report(
            @NonNull Context context,
            @NonNull Issue issue,
            @NonNull Severity severity,
            @Nullable Location location,
            @NonNull String message,
            @NonNull TextFormat format);

    /**
     * Send an exception or error message (with warning severity) to the log
     *
     * @param exception the exception, possibly null
     * @param format the error message using {@link String#format} syntax, possibly null
     *    (though in that case the exception should not be null)
     * @param args any arguments for the format string
     */
    public void log(
            @Nullable Throwable exception,
            @Nullable String format,
            @Nullable Object... args) {
        log(Severity.WARNING, exception, format, args);
    }

    /**
     * Send an exception or error message to the log
     *
     * @param severity the severity of the warning
     * @param exception the exception, possibly null
     * @param format the error message using {@link String#format} syntax, possibly null
     *    (though in that case the exception should not be null)
     * @param args any arguments for the format string
     */
    public abstract void log(
            @NonNull Severity severity,
            @Nullable Throwable exception,
            @Nullable String format,
            @Nullable Object... args);

    /**
     * Returns a {@link XmlParser} to use to parse XML
     *
     * @return a new {@link XmlParser}, or null if this client does not support
     *         XML analysis
     */
    @Nullable
    public abstract XmlParser getXmlParser();

    /**
     * Returns an optimal detector, if applicable. By default, just returns the
     * original detector, but tools can replace detectors using this hook with a version
     * that takes advantage of native capabilities of the tool.
     *
     * @param detectorClass the class of the detector to be replaced
     * @return the new detector class, or just the original detector (not null)
     */
    @NonNull
    public Class<? extends Detector> replaceDetector(
            @NonNull Class<? extends Detector> detectorClass) {
        return detectorClass;
    }

    /**
     * Reads the given text file and returns the content as a string
     *
     * @param file the file to read
     * @return the string to return, never null (will be empty if there is an
     *         I/O error)
     */
    @NonNull
    public abstract String readFile(@NonNull File file);

    /**
     * Reads the given binary file and returns the content as a byte array.
     * By default this method will read the bytes from the file directly,
     * but this can be customized by a client if for example I/O could be
     * held in memory and not flushed to disk yet.
     *
     * @param file the file to read
     * @return the bytes in the file, never null
     * @throws IOException if the file does not exist, or if the file cannot be
     *             read for some reason
     */
    @NonNull
    public byte[] readBytes(@NonNull File file) throws IOException {
        return Files.toByteArray(file);
    }

    /**
     * Returns the list of source folders for Java source files
     *
     * @param project the project to look up Java source file locations for
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getJavaSourceFolders(@NonNull Project project) {
        return getClassPath(project).getSourceFolders();
    }

    /**
     * Returns the list of output folders for class files
     *
     * @param project the project to look up class file locations for
     * @return a list of output folders to search for .class files
     */
    @NonNull
    public List<File> getJavaClassFolders(@NonNull Project project) {
        return getClassPath(project).getClassFolders();

    }

    /**
     * Returns the list of Java libraries
     *
     * @param project the project to look up jar dependencies for
     * @return a list of jar dependencies containing .class files
     */
    @NonNull
    public List<File> getJavaLibraries(@NonNull Project project) {
        return getClassPath(project).getLibraries();
    }

    /**
     * Returns the list of source folders for test source files
     *
     * @param project the project to look up test source file locations for
     * @return a list of source folders to search for .java files
     */
    @NonNull
    public List<File> getTestSourceFolders(@NonNull Project project) {
        return getClassPath(project).getTestSourceFolders();
    }

    /**
     * Returns the resource folders.
     *
     * @param project the project to look up the resource folder for
     * @return a list of files pointing to the resource folders, possibly empty
     */
    @NonNull
    public List<File> getResourceFolders(@NonNull Project project) {
        File res = new File(project.getDir(), RES_FOLDER);
        if (res.exists()) {
            return Collections.singletonList(res);
        }

        return Collections.emptyList();
    }

    /**
     * Returns the {@link SdkInfo} to use for the given project.
     *
     * @param project the project to look up an {@link SdkInfo} for
     * @return an {@link SdkInfo} for the project
     */
    @NonNull
    public SdkInfo getSdkInfo(@NonNull Project project) {
        // By default no per-platform SDK info
        return new DefaultSdkInfo();
    }

    /**
     * Returns a suitable location for storing cache files. Note that the
     * directory may not exist.
     *
     * @param create if true, attempt to create the cache dir if it does not
     *            exist
     * @return a suitable location for storing cache files, which may be null if
     *         the create flag was false, or if for some reason the directory
     *         could not be created
     */
    @Nullable
    public File getCacheDir(boolean create) {
        String home = System.getProperty("user.home");
        String relative = ".android" + File.separator + "cache"; //$NON-NLS-1$ //$NON-NLS-2$
        File dir = new File(home, relative);
        if (create && !dir.exists()) {
            if (!dir.mkdirs()) {
                return null;
            }
        }
        return dir;
    }

    /**
     * Returns the File corresponding to the system property or the environment variable
     * for {@link #PROP_BIN_DIR}.
     * This property is typically set by the SDK/tools/lint[.bat] wrapper.
     * It denotes the path of the wrapper on disk.
     *
     * @return A new File corresponding to {@link LintClient#PROP_BIN_DIR} or null.
     */
    @Nullable
    private static File getLintBinDir() {
        // First check the Java properties (e.g. set using "java -jar ... -Dname=value")
        String path = System.getProperty(PROP_BIN_DIR);
        if (path == null || path.isEmpty()) {
            // If not found, check environment variables.
            path = System.getenv(PROP_BIN_DIR);
        }
        if (path != null && !path.isEmpty()) {
            File file = new File(path);
            if (file.exists()) {
                return file;
            }
        }
        return null;
    }

    /**
     * Returns the File pointing to the user's SDK install area. This is generally
     * the root directory containing the lint tool (but also platforms/ etc).
     *
     * @return a file pointing to the user's install area
     */
    @Nullable
    public File getSdkHome() {
        File binDir = getLintBinDir();
        if (binDir != null) {
            assert binDir.getName().equals("tools");

            File root = binDir.getParentFile();
            if (root != null && root.isDirectory()) {
                return root;
            }
        }

        String home = System.getenv("ANDROID_HOME"); //$NON-NLS-1$
        if (home != null) {
            return new File(home);
        }

        return null;
    }

    /**
     * Locates an SDK resource (relative to the SDK root directory).
     * <p>
     * TODO: Consider switching to a {@link URL} return type instead.
     *
     * @param relativePath A relative path (using {@link File#separator} to
     *            separate path components) to the given resource
     * @return a {@link File} pointing to the resource, or null if it does not
     *         exist
     */
    @Nullable
    public File findResource(@NonNull String relativePath) {
        File top = getSdkHome();
        if (top == null) {
            throw new IllegalArgumentException("Lint must be invoked with the System property "
                   + PROP_BIN_DIR + " pointing to the ANDROID_SDK tools directory");
        }

        File file = new File(top, relativePath);
        if (file.exists()) {
            return file;
        } else {
            return null;
        }
    }

    private Map<Project, ClassPathInfo> mProjectInfo;

    /**
     * Returns true if this project is a Gradle-based Android project
     *
     * @param project the project to check
     * @return true if this is a Gradle-based project
     */
    public boolean isGradleProject(Project project) {
        // This is not an accurate test; specific LintClient implementations (e.g.
        // IDEs or a gradle-integration of lint) have more context and can perform a more accurate
        // check
        if (new File(project.getDir(), SdkConstants.FN_BUILD_GRADLE).exists()) {
            return true;
        }

        File parent = project.getDir().getParentFile();
        if (parent != null && parent.getName().equals(SdkConstants.FD_SOURCES)) {
            File root = parent.getParentFile();
            if (root != null && new File(root, SdkConstants.FN_BUILD_GRADLE).exists()) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public com.intellij.openapi.project.Project getProject() {
        return null;
    }

    public List<UastLanguagePlugin> getLanguagePlugins() {
        return Collections.emptyList();
    }

    /**
     * Information about class paths (sources, class files and libraries)
     * usually associated with a project.
     */
    protected static class ClassPathInfo {
        private final List<File> mClassFolders;
        private final List<File> mSourceFolders;
        private final List<File> mLibraries;
        private final List<File> mTestFolders;

        public ClassPathInfo(
                @NonNull List<File> sourceFolders,
                @NonNull List<File> classFolders,
                @NonNull List<File> libraries,
                @NonNull List<File> testFolders) {
            mSourceFolders = sourceFolders;
            mClassFolders = classFolders;
            mLibraries = libraries;
            mTestFolders = testFolders;
        }

        @NonNull
        public List<File> getSourceFolders() {
            return mSourceFolders;
        }

        @NonNull
        public List<File> getClassFolders() {
            return mClassFolders;
        }

        @NonNull
        public List<File> getLibraries() {
            return mLibraries;
        }

        public List<File> getTestSourceFolders() {
            return mTestFolders;
        }
    }

    /**
     * Considers the given project as an Eclipse project and returns class path
     * information for the project - the source folder(s), the output folder and
     * any libraries.
     * <p>
     * Callers will not cache calls to this method, so if it's expensive to compute
     * the classpath info, this method should perform its own caching.
     *
     * @param project the project to look up class path info for
     * @return a class path info object, never null
     */
    @NonNull
    protected ClassPathInfo getClassPath(@NonNull Project project) {
        ClassPathInfo info;
        if (mProjectInfo == null) {
            mProjectInfo = Maps.newHashMap();
            info = null;
        } else {
            info = mProjectInfo.get(project);
        }

        if (info == null) {
            List<File> sources = new ArrayList<File>(2);
            List<File> classes = new ArrayList<File>(1);
            List<File> libraries = new ArrayList<File>();
            // No test folders in Eclipse:
            // https://bugs.eclipse.org/bugs/show_bug.cgi?id=224708
            List<File> tests = Collections.emptyList();

            File projectDir = project.getDir();
            File classpathFile = new File(projectDir, ".classpath"); //$NON-NLS-1$
            if (classpathFile.exists()) {
                String classpathXml = readFile(classpathFile);
                try {
                    Document document = XmlUtils.parseDocument(classpathXml, false);
                    NodeList tags = document.getElementsByTagName("classpathentry"); //$NON-NLS-1$
                    for (int i = 0, n = tags.getLength(); i < n; i++) {
                        Element element = (Element) tags.item(i);
                        String kind = element.getAttribute("kind"); //$NON-NLS-1$
                        List<File> addTo = null;
                        if (kind.equals("src")) {            //$NON-NLS-1$
                            addTo = sources;
                        } else if (kind.equals("output")) {  //$NON-NLS-1$
                            addTo = classes;
                        } else if (kind.equals("lib")) {     //$NON-NLS-1$
                            addTo = libraries;
                        }
                        if (addTo != null) {
                            String path = element.getAttribute("path"); //$NON-NLS-1$
                            File folder = new File(projectDir, path);
                            if (folder.exists()) {
                                addTo.add(folder);
                            }
                        }
                    }
                } catch (Exception e) {
                    log(null, null);
                }
            }

            // Add in libraries that aren't specified in the .classpath file
            File libs = new File(project.getDir(), LIBS_FOLDER);
            if (libs.isDirectory()) {
                File[] jars = libs.listFiles();
                if (jars != null) {
                    for (File jar : jars) {
                        if (LintUtils.endsWith(jar.getPath(), DOT_JAR)
                                && !libraries.contains(jar)) {
                            libraries.add(jar);
                        }
                    }
                }
            }

            if (classes.isEmpty()) {
                File folder = new File(projectDir, CLASS_FOLDER);
                if (folder.exists()) {
                    classes.add(folder);
                } else {
                    // Maven checks
                    folder = new File(projectDir,
                            "target" + File.separator + "classes"); //$NON-NLS-1$ //$NON-NLS-2$
                    if (folder.exists()) {
                        classes.add(folder);

                        // If it's maven, also correct the source path, "src" works but
                        // it's in a more specific subfolder
                        if (sources.isEmpty()) {
                            File src = new File(projectDir,
                                    "src" + File.separator     //$NON-NLS-1$
                                    + "main" + File.separator  //$NON-NLS-1$
                                    + "java");                 //$NON-NLS-1$
                            if (src.exists()) {
                                sources.add(src);
                            } else {
                                src = new File(projectDir, SRC_FOLDER);
                                if (src.exists()) {
                                    sources.add(src);
                                }
                            }

                            File gen = new File(projectDir,
                                    "target" + File.separator                  //$NON-NLS-1$
                                    + "generated-sources" + File.separator     //$NON-NLS-1$
                                    + "r");                                    //$NON-NLS-1$
                            if (gen.exists()) {
                                sources.add(gen);
                            }
                        }
                    }
                }
            }

            // Fallback, in case there is no Eclipse project metadata here
            if (sources.isEmpty()) {
                File src = new File(projectDir, SRC_FOLDER);
                if (src.exists()) {
                    sources.add(src);
                }
                File gen = new File(projectDir, GEN_FOLDER);
                if (gen.exists()) {
                    sources.add(gen);
                }
            }

            info = new ClassPathInfo(sources, classes, libraries, tests);
            mProjectInfo.put(project, info);
        }

        return info;
    }

    /**
     * A map from directory to existing projects, or null. Used to ensure that
     * projects are unique for a directory (in case we process a library project
     * before its including project for example)
     */
    private Map<File, Project> mDirToProject;

    /**
     * Returns a project for the given directory. This should return the same
     * project for the same directory if called repeatedly.
     *
     * @param dir the directory containing the project
     * @param referenceDir See {@link Project#getReferenceDir()}.
     * @return a project, never null
     */
    @NonNull
    public Project getProject(@NonNull File dir, @NonNull File referenceDir) {
        if (mDirToProject == null) {
            mDirToProject = new HashMap<File, Project>();
        }

        File canonicalDir = dir;
        try {
            // Attempt to use the canonical handle for the file, in case there
            // are symlinks etc present (since when handling library projects,
            // we also call getCanonicalFile to compute the result of appending
            // relative paths, which can then resolve symlinks and end up with
            // a different prefix)
            canonicalDir = dir.getCanonicalFile();
        } catch (IOException ioe) {
            // pass
        }

        Project project = mDirToProject.get(canonicalDir);
        if (project != null) {
            return project;
        }

        project = createProject(dir, referenceDir);
        mDirToProject.put(canonicalDir, project);
        return project;
    }

    /**
     * Returns the list of known projects (projects registered via
     * {@link #getProject(File, File)}
     *
     * @return a collection of projects in any order
     */
    public Collection<Project> getKnownProjects() {
        return mDirToProject != null ? mDirToProject.values() : Collections.<Project>emptyList();
    }

    /**
     * Registers the given project for the given directory. This can
     * be used when projects are initialized outside of the client itself.
     *
     * @param dir the directory of the project, which must be unique
     * @param project the project
     */
    public void registerProject(@NonNull File dir, @NonNull Project project) {
        File canonicalDir = dir;
        try {
            // Attempt to use the canonical handle for the file, in case there
            // are symlinks etc present (since when handling library projects,
            // we also call getCanonicalFile to compute the result of appending
            // relative paths, which can then resolve symlinks and end up with
            // a different prefix)
            canonicalDir = dir.getCanonicalFile();
        } catch (IOException ioe) {
            // pass
        }


        if (mDirToProject == null) {
            mDirToProject = new HashMap<File, Project>();
        } else {
            assert !mDirToProject.containsKey(dir) : dir;
        }
        mDirToProject.put(canonicalDir, project);
    }

    private Set<File> mProjectDirs = Sets.newHashSet();

    /**
     * Create a project for the given directory
     * @param dir the root directory of the project
     * @param referenceDir See {@link Project#getReferenceDir()}.
     * @return a new project
     */
    @NonNull
    protected Project createProject(@NonNull File dir, @NonNull File referenceDir) {
        if (mProjectDirs.contains(dir)) {
            throw new CircularDependencyException(
                "Circular library dependencies; check your project.properties files carefully");
        }
        mProjectDirs.add(dir);
        return Project.create(this, dir, referenceDir);
    }

    /**
     * Returns the name of the given project
     *
     * @param project the project to look up
     * @return the name of the project
     */
    @NonNull
    public String getProjectName(@NonNull Project project) {
        return project.getDir().getName();
    }

    protected IAndroidTarget[] mTargets;

    /**
     * Returns all the {@link IAndroidTarget} versions installed in the user's SDK install
     * area.
     *
     * @return all the installed targets
     */
    @NonNull
    public IAndroidTarget[] getTargets() {
        if (mTargets == null) {
            SdkWrapper localSdk = getSdk();
            if (localSdk != null) {
                mTargets = localSdk.getTargets();
            } else {
                mTargets = new IAndroidTarget[0];
            }
        }

        return mTargets;
    }

    protected SdkWrapper mSdk;

    /**
     * Returns the SDK installation (used to look up platforms etc)
     *
     * @return the SDK if known
     */
    @Nullable
    public SdkWrapper getSdk() {
         if (mSdk == null) {
             File sdkHome = getSdkHome();
             if (sdkHome != null) {
                 mSdk = SdkWrapper.createLocalSdk(sdkHome);
             }
         }

        return mSdk;
    }

    /**
     * Returns the compile target to use for the given project
     *
     * @param project the project in question
     *
     * @return the compile target to use to build the given project
     */
    @Nullable
    public IAndroidTarget getCompileTarget(@NonNull Project project) {
        int buildSdk = project.getBuildSdk();
        IAndroidTarget[] targets = getTargets();
        for (int i = targets.length - 1; i >= 0; i--) {
            IAndroidTarget target = targets[i];
            if (target.isPlatform() && target.getVersion().getApiLevel() == buildSdk) {
                return target;
            }
        }

        return null;
    }

    /**
     * Returns the highest known API level.
     *
     * @return the highest known API level
     */
    public int getHighestKnownApiLevel() {
        int max = SdkVersionInfo.HIGHEST_KNOWN_STABLE_API;

        for (IAndroidTarget target : getTargets()) {
            if (target.isPlatform()) {
                int api = target.getVersion().getApiLevel();
                if (api > max && !target.getVersion().isPreview()) {
                    max = api;
                }
            }
        }

        return max;
    }

    /**
     * Returns the super class for the given class name, which should be in VM
     * format (e.g. java/lang/Integer, not java.lang.Integer, and using $ rather
     * than . for inner classes). If the super class is not known, returns null.
     * <p>
     * This is typically not necessary, since lint analyzes all the available
     * classes. However, if this lint client is invoking lint in an incremental
     * context (for example, an IDE offering incremental analysis of a single
     * source file), then lint may not see all the classes, and the client can
     * provide its own super class lookup.
     *
     * @param project the project containing the class
     * @param name the fully qualified class name
     * @return the corresponding super class name (in VM format), or null if not
     *         known
     */
    @Nullable
    public String getSuperClass(@NonNull Project project, @NonNull String name) {
        assert name.indexOf('.') == -1 : "Use VM signatures, e.g. java/lang/Integer";

        if ("java/lang/Object".equals(name)) {  //$NON-NLS-1$
            return null;
        }

        String superClass = project.getSuperClassMap().get(name);
        if (superClass != null) {
            return superClass;
        }

        for (Project library : project.getAllLibraries()) {
            superClass = library.getSuperClassMap().get(name);
            if (superClass != null) {
                return superClass;
            }
        }

        return null;
    }

    /**
     * Creates a super class map for the given project. The map maps from
     * internal class name (e.g. java/lang/Integer, not java.lang.Integer) to its
     * corresponding super class name. The root class, java/lang/Object, is not in the map.
     *
     * @param project the project to initialize the super class with; this will include
     *                local classes as well as any local .jar libraries; not transitive
     *                dependencies
     * @return a map from class to its corresponding super class; never null
     */
    @NonNull
    public Map<String, String> createSuperClassMap(@NonNull Project project) {
        List<File> libraries = project.getJavaLibraries();
        List<File> classFolders = project.getJavaClassFolders();
        List<ClassEntry> classEntries = ClassEntry.fromClassPath(this, classFolders, true);
        if (libraries.isEmpty()) {
            return ClassEntry.createSuperClassMap(this, classEntries);
        }
        List<ClassEntry> libraryEntries = ClassEntry.fromClassPath(this, libraries, true);
        return ClassEntry.createSuperClassMap(this, libraryEntries, classEntries);
    }

    /**
     * Checks whether the given name is a subclass of the given super class. If
     * the method does not know, it should return null, and otherwise return
     * {@link Boolean#TRUE} or {@link Boolean#FALSE}.
     * <p>
     * Note that the class names are in internal VM format (java/lang/Integer,
     * not java.lang.Integer, and using $ rather than . for inner classes).
     *
     * @param project the project context to look up the class in
     * @param name the name of the class to be checked
     * @param superClassName the name of the super class to compare to
     * @return true if the class of the given name extends the given super class
     */
    @SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
    @Nullable
    public Boolean isSubclassOf(
            @NonNull Project project,
            @NonNull String name,
            @NonNull String superClassName) {
        return null;
    }

    /**
     * Finds any custom lint rule jars that should be included for analysis,
     * regardless of project.
     * <p>
     * The default implementation locates custom lint jars in ~/.android/lint/ and
     * in $ANDROID_LINT_JARS
     *
     * @return a list of rule jars (possibly empty).
     */
    @SuppressWarnings("MethodMayBeStatic") // Intentionally instance method so it can be overridden
    @NonNull
    public List<File> findGlobalRuleJars() {
        // Look for additional detectors registered by the user, via
        // (1) an environment variable (useful for build servers etc), and
        // (2) via jar files in the .android/lint directory
        List<File> files = null;
        try {
            String androidHome = AndroidLocation.getFolder();
            File lint = new File(androidHome + File.separator + "lint"); //$NON-NLS-1$
            if (lint.exists()) {
                File[] list = lint.listFiles();
                if (list != null) {
                    for (File jarFile : list) {
                        if (endsWith(jarFile.getName(), DOT_JAR)) {
                            if (files == null) {
                                files = new ArrayList<File>();
                            }
                            files.add(jarFile);
                        }
                    }
                }
            }
        } catch (AndroidLocation.AndroidLocationException e) {
            // Ignore -- no android dir, so no rules to load.
        }

        String lintClassPath = System.getenv("ANDROID_LINT_JARS"); //$NON-NLS-1$
        if (lintClassPath != null && !lintClassPath.isEmpty()) {
            String[] paths = lintClassPath.split(File.pathSeparator);
            for (String path : paths) {
                File jarFile = new File(path);
                if (jarFile.exists()) {
                    if (files == null) {
                        files = new ArrayList<File>();
                    } else if (files.contains(jarFile)) {
                        continue;
                    }
                    files.add(jarFile);
                }
            }
        }

        return files != null ? files : Collections.<File>emptyList();
    }

    /**
     * Finds any custom lint rule jars that should be included for analysis
     * in the given project
     *
     * @param project the project to look up rule jars from
     * @return a list of rule jars (possibly empty).
     */
    @SuppressWarnings("MethodMayBeStatic") // Intentionally instance method so it can be overridden
    @NonNull
    public List<File> findRuleJars(@NonNull Project project) {
        if (project.getDir().getPath().endsWith(DOT_AAR)) {
            File lintJar = new File(project.getDir(), "lint.jar"); //$NON-NLS-1$
            if (lintJar.exists()) {
                return Collections.singletonList(lintJar);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Opens a URL connection.
     *
     * Clients such as IDEs can override this to for example consider the user's IDE proxy
     * settings.
     *
     * @param url the URL to read
     * @return a {@link java.net.URLConnection} or null
     * @throws IOException if any kind of IO exception occurs
     */
    @Nullable
    public URLConnection openConnection(@NonNull URL url) throws IOException {
        return url.openConnection();
    }

    /** Closes a connection previously returned by {@link #openConnection(java.net.URL)} */
    public void closeConnection(@NonNull URLConnection connection) throws IOException {
        if (connection instanceof HttpURLConnection) {
            ((HttpURLConnection)connection).disconnect();
        }
    }

    /**
     * Returns true if the given directory is a lint project directory.
     * By default, a project directory is the directory containing a manifest file,
     * but in Gradle projects for example it's the root gradle directory.
     *
     * @param dir the directory to check
     * @return true if the directory represents a lint project
     */
    @SuppressWarnings("MethodMayBeStatic") // Intentionally instance method so it can be overridden
    public boolean isProjectDirectory(@NonNull File dir) {
        return LintUtils.isManifestFolder(dir) || Project.isAospFrameworksProject(dir);
    }

    /**
     * Returns whether lint should look for suppress comments. Tools that already do
     * this on their own can return false here to avoid doing unnecessary work.
     */
    public boolean checkForSuppressComments() {
        return true;
    }

    /**
     * Adds in any custom lint rules and returns the result as a new issue registry,
     * or the same one if no custom rules were found
     *
     * @param registry the main registry to add rules to
     * @return a new registry containing the passed in rules plus any custom rules,
     *   or the original registry if no custom rules were found
     */
    public IssueRegistry addCustomLintRules(@NonNull IssueRegistry registry) {
        List<File> jarFiles = findGlobalRuleJars();

        if (!jarFiles.isEmpty()) {
            List<IssueRegistry> registries = Lists.newArrayListWithExpectedSize(jarFiles.size());
            registries.add(registry);
            for (File jarFile : jarFiles) {
                try {
                    registries.add(JarFileIssueRegistry.get(this, jarFile));
                } catch (Throwable e) {
                    log(e, "Could not load custom rule jar file %1$s", jarFile);
                }
            }
            if (registries.size() > 1) { // the first item is the passed in registry itself
                return new CompositeIssueRegistry(registries);
            }
        }

        return registry;
    }

    /**
     * Returns true if this client supports project resource repository lookup via
     * {@link #getProjectResources(Project,boolean)}
     *
     * @return true if the client can provide project resources
     */
    public boolean supportsProjectResources() {
        return false;
    }

    /**
     * Returns the project resources, if available
     *
     * @param includeDependencies if true, include merged view of all dependencies
     * @return the project resources, or null if not available
     */
    @Nullable
    public AbstractResourceRepository getProjectResources(Project project,
            boolean includeDependencies) {
        return null;
    }

    /**
     * For a lint client which supports resource items (via {@link #supportsProjectResources()})
     * return a handle for a resource item
     *
     * @param item the resource item to look up a location handle for
     * @return a corresponding handle
     */
    @NonNull
    public Location.Handle createResourceItemHandle(@NonNull ResourceItem item) {
        return new Location.ResourceItemHandle(item);
    }

    private ResourceVisibilityLookup.Provider mResourceVisibility;

    /**
     * Returns a shared {@link ResourceVisibilityLookup.Provider}
     *
     * @return a shared provider for looking up resource visibility
     */
    @NonNull
    public ResourceVisibilityLookup.Provider getResourceVisibilityProvider() {
        if (mResourceVisibility == null) {
            mResourceVisibility = new ResourceVisibilityLookup.Provider();
        }
        return mResourceVisibility;
    }
}
