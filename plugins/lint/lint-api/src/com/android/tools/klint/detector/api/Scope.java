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

import static com.android.SdkConstants.ANDROID_MANIFEST_XML;
import static com.android.SdkConstants.DOT_CLASS;
import static com.android.SdkConstants.DOT_GRADLE;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.DOT_PNG;
import static com.android.SdkConstants.DOT_PROPERTIES;
import static com.android.SdkConstants.DOT_XML;
import static com.android.SdkConstants.FN_PROJECT_PROGUARD_FILE;
import static com.android.SdkConstants.OLD_PROGUARD_FILE;
import static com.android.SdkConstants.RES_FOLDER;

import com.android.annotations.NonNull;
import com.google.common.annotations.Beta;

import java.io.File;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

/**
 * The scope of a detector is the set of files a detector must consider when
 * performing its analysis. This can be used to determine when issues are
 * potentially obsolete, whether a detector should re-run on a file save, etc.
 * <p>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
@Beta
public enum Scope {
    /**
     * The analysis only considers a single XML resource file at a time.
     * <p>
     * Issues which are only affected by a single resource file can be checked
     * for incrementally when a file is edited.
     */
    RESOURCE_FILE,

    /**
     * The analysis only considers a single binary (typically a bitmap) resource file at a time.
     * <p>
     * Issues which are only affected by a single resource file can be checked
     * for incrementally when a file is edited.
     */
    BINARY_RESOURCE_FILE,

    /**
     * The analysis considers the resource folders
     */
    RESOURCE_FOLDER,

    /**
     * The analysis considers <b>all</b> the resource file. This scope must not
     * be used in conjunction with {@link #RESOURCE_FILE}; an issue scope is
     * either considering just a single resource file or all the resources, not
     * both.
     */
    ALL_RESOURCE_FILES,

    /**
     * The analysis only considers a single Java source file at a time.
     * <p>
     * Issues which are only affected by a single Java source file can be
     * checked for incrementally when a Java source file is edited.
     */
    JAVA_FILE,

    /**
     * The analysis considers <b>all</b> the Java source files together.
     * <p>
     * This flag is mutually exclusive with {@link #JAVA_FILE}.
     */
    ALL_JAVA_FILES,

    /**
     * The analysis only considers a single Java class file at a time.
     * <p>
     * Issues which are only affected by a single Java class file can be checked
     * for incrementally when a Java source file is edited and then recompiled.
     */
    CLASS_FILE,

    /**
     * The analysis considers <b>all</b> the Java class files together.
     * <p>
     * This flag is mutually exclusive with {@link #CLASS_FILE}.
     */
    ALL_CLASS_FILES,

    /** The analysis considers the manifest file */
    MANIFEST,

    /** The analysis considers the Proguard configuration file */
    PROGUARD_FILE,

    /**
     * The analysis considers classes in the libraries for this project. These
     * will be analyzed before the classes themselves.
     */
    JAVA_LIBRARIES,

    /** The analysis considers a Gradle build file */
    GRADLE_FILE,

    /** The analysis considers Java property files */
    PROPERTY_FILE,

    /** The analysis considers test sources as well */
    TEST_SOURCES,

    /**
     * Scope for other files. Issues that specify a custom scope will be called unconditionally.
     * This will call {@link Detector#run(Context)}} on the detectors unconditionally.
     */
    OTHER;

    /**
     * Returns true if the given scope set corresponds to scanning a single file
     * rather than a whole project
     *
     * @param scopes the scope set to check
     * @return true if the scope set references a single file
     */
    public static boolean checkSingleFile(@NonNull EnumSet<Scope> scopes) {
        int size = scopes.size();
        if (size == 2) {
            // When single checking a Java source file, we check both its Java source
            // and the associated class files
            return scopes.contains(JAVA_FILE) && scopes.contains(CLASS_FILE);
        } else {
            return size == 1 &&
                (scopes.contains(JAVA_FILE)
                        || scopes.contains(CLASS_FILE)
                        || scopes.contains(RESOURCE_FILE)
                        || scopes.contains(PROGUARD_FILE)
                        || scopes.contains(PROPERTY_FILE)
                        || scopes.contains(GRADLE_FILE)
                        || scopes.contains(MANIFEST));
        }
    }

    /**
     * Returns the intersection of two scope sets
     *
     * @param scope1 the first set to intersect
     * @param scope2 the second set to intersect
     * @return the intersection of the two sets
     */
    @NonNull
    public static EnumSet<Scope> intersect(
            @NonNull EnumSet<Scope> scope1,
            @NonNull EnumSet<Scope> scope2) {
        EnumSet<Scope> scope = EnumSet.copyOf(scope1);
        scope.retainAll(scope2);

        return scope;
    }

    /**
     * Infers a suitable scope to use from the given projects to be analyzed
     * @param projects the projects to find a suitable scope for
     * @return the scope to use
     */
    @NonNull
    public static EnumSet<Scope> infer(@NonNull Collection<Project> projects) {
        // Infer the scope
        EnumSet<Scope> scope = EnumSet.noneOf(Scope.class);
        for (Project project : projects) {
            List<File> subset = project.getSubset();
            if (subset != null) {
                for (File file : subset) {
                    String name = file.getName();
                    if (name.equals(ANDROID_MANIFEST_XML)) {
                        scope.add(MANIFEST);
                    } else if (name.endsWith(DOT_XML)) {
                        scope.add(RESOURCE_FILE);
                    } else if (name.endsWith(DOT_JAVA)) {
                        scope.add(JAVA_FILE);
                    } else if (name.endsWith(DOT_CLASS)) {
                        scope.add(CLASS_FILE);
                    } else if (name.endsWith(DOT_GRADLE)) {
                        scope.add(GRADLE_FILE);
                    } else if (name.equals(OLD_PROGUARD_FILE)
                            || name.equals(FN_PROJECT_PROGUARD_FILE)) {
                        scope.add(PROGUARD_FILE);
                    } else if (name.endsWith(DOT_PROPERTIES)) {
                        scope.add(PROPERTY_FILE);
                    } else if (name.endsWith(DOT_PNG)) {
                        scope.add(BINARY_RESOURCE_FILE);
                    } else if (name.equals(RES_FOLDER)
                            || file.getParent().equals(RES_FOLDER)) {
                        scope.add(ALL_RESOURCE_FILES);
                        scope.add(RESOURCE_FILE);
                        scope.add(BINARY_RESOURCE_FILE);
                        scope.add(RESOURCE_FOLDER);
                    }
                }
            } else {
                // Specified a full project: just use the full project scope
                scope = Scope.ALL;
                break;
            }
        }

        return scope;
    }

    /** All scopes: running lint on a project will check these scopes */
    public static final EnumSet<Scope> ALL = EnumSet.allOf(Scope.class);
    /** Scope-set used for detectors which are affected by a single resource file */
    public static final EnumSet<Scope> RESOURCE_FILE_SCOPE = EnumSet.of(RESOURCE_FILE);
    /** Scope-set used for detectors which are affected by a single resource folder */
    public static final EnumSet<Scope> RESOURCE_FOLDER_SCOPE = EnumSet.of(RESOURCE_FOLDER);
    /** Scope-set used for detectors which scan all resources */
    public static final EnumSet<Scope> ALL_RESOURCES_SCOPE = EnumSet.of(ALL_RESOURCE_FILES);
    /** Scope-set used for detectors which are affected by a single Java source file */
    public static final EnumSet<Scope> JAVA_FILE_SCOPE = EnumSet.of(JAVA_FILE);
    /** Scope-set used for detectors which are affected by a single Java class file */
    public static final EnumSet<Scope> CLASS_FILE_SCOPE = EnumSet.of(CLASS_FILE);
    /** Scope-set used for detectors which are affected by a single Gradle build file */
    public static final EnumSet<Scope> GRADLE_SCOPE = EnumSet.of(GRADLE_FILE);
    /** Scope-set used for detectors which are affected by the manifest only */
    public static final EnumSet<Scope> MANIFEST_SCOPE = EnumSet.of(MANIFEST);
    /** Scope-set used for detectors which correspond to some other context */
    public static final EnumSet<Scope> OTHER_SCOPE = EnumSet.of(OTHER);
    /** Scope-set used for detectors which are affected by a single ProGuard class file */
    public static final EnumSet<Scope> PROGUARD_SCOPE = EnumSet.of(PROGUARD_FILE);
    /** Scope-set used for detectors which correspond to property files */
    public static final EnumSet<Scope> PROPERTY_SCOPE = EnumSet.of(PROPERTY_FILE);
    /** Resource XML files and manifest files */
    public static final EnumSet<Scope> MANIFEST_AND_RESOURCE_SCOPE =
            EnumSet.of(Scope.MANIFEST, Scope.RESOURCE_FILE);
    /** Scope-set used for detectors which are affected by single XML and Java source files */
    public static final EnumSet<Scope> JAVA_AND_RESOURCE_FILES =
            EnumSet.of(RESOURCE_FILE, JAVA_FILE);
    /** Scope-set used for analyzing individual class files and all resource files */
    public static final EnumSet<Scope> CLASS_AND_ALL_RESOURCE_FILES =
            EnumSet.of(ALL_RESOURCE_FILES, CLASS_FILE);
    /** Scope-set used for analyzing all class files, including those in libraries */
    public static final EnumSet<Scope> ALL_CLASSES_AND_LIBRARIES =
            EnumSet.of(Scope.ALL_CLASS_FILES, Scope.JAVA_LIBRARIES);
    /** Scope-set used for detectors which are affected by Java libraries */
    public static final EnumSet<Scope> JAVA_LIBRARY_SCOPE = EnumSet.of(JAVA_LIBRARIES);
    /** Scope-set used for detectors which are affected by a single binary resource file */
    public static final EnumSet<Scope> BINARY_RESOURCE_FILE_SCOPE =
            EnumSet.of(BINARY_RESOURCE_FILE);
}
