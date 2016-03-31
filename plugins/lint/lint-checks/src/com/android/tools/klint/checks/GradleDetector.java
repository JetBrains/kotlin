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

import static com.android.SdkConstants.FD_BUILD_TOOLS;
import static com.android.SdkConstants.GRADLE_PLUGIN_MINIMUM_VERSION;
import static com.android.SdkConstants.GRADLE_PLUGIN_RECOMMENDED_VERSION;
import static com.android.ide.common.repository.GradleCoordinate.COMPARE_PLUS_HIGHER;
import static com.android.tools.lint.checks.ManifestDetector.TARGET_NEWER;
import static com.android.tools.lint.detector.api.LintUtils.findSubstring;
import static com.google.common.base.Charsets.UTF_8;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.builder.model.AndroidLibrary;
import com.android.builder.model.Dependencies;
import com.android.builder.model.MavenCoordinates;
import com.android.builder.model.Variant;
import com.android.ide.common.repository.GradleCoordinate;
import com.android.ide.common.repository.GradleCoordinate.RevisionComponent;
import com.android.ide.common.repository.SdkMavenRepository;
import com.android.sdklib.repository.PreciseRevision;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Checks Gradle files for potential errors
 */
public class GradleDetector extends Detector implements Detector.GradleScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            GradleDetector.class,
            Scope.GRADLE_SCOPE);

    /** Obsolete dependencies */
    public static final Issue DEPENDENCY = Issue.create(
            "GradleDependency", //$NON-NLS-1$
            "Obsolete Gradle Dependency",
            "This detector looks for usages of libraries where the version you are using " +
            "is not the current stable release. Using older versions is fine, and there are " +
            "cases where you deliberately want to stick with an older version. However, " +
            "you may simply not be aware that a more recent version is available, and that is " +
            "what this lint check helps find.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Deprecated Gradle constructs */
    public static final Issue DEPRECATED = Issue.create(
            "GradleDeprecated", //$NON-NLS-1$
            "Deprecated Gradle Construct",
            "This detector looks for deprecated Gradle constructs which currently work but " +
            "will likely stop working in a future update.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Incompatible Android Gradle plugin */
    public static final Issue GRADLE_PLUGIN_COMPATIBILITY = Issue.create(
            "AndroidGradlePluginVersion", //$NON-NLS-1$
            "Incompatible Android Gradle Plugin",
            "Not all versions of the Android Gradle plugin are compatible with all versions " +
            "of the SDK. If you update your tools, or if you are trying to open a project that " +
            "was built with an old version of the tools, you may need to update your plugin " +
            "version number.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Invalid or dangerous paths */
    public static final Issue PATH = Issue.create(
            "GradlePath", //$NON-NLS-1$
            "Gradle Path Issues",
            "Gradle build scripts are meant to be cross platform, so file paths use " +
            "Unix-style path separators (a forward slash) rather than Windows path separators " +
            "(a backslash). Similarly, to keep projects portable and repeatable, avoid " +
            "using absolute paths on the system; keep files within the project instead. To " +
            "share code between projects, consider creating an android-library and an AAR " +
            "dependency",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Constructs the IDE support struggles with */
    public static final Issue IDE_SUPPORT = Issue.create(
            "GradleIdeError", //$NON-NLS-1$
            "Gradle IDE Support Issues",
            "Gradle is highly flexible, and there are things you can do in Gradle files which " +
            "can make it hard or impossible for IDEs to properly handle the project. This lint " +
            "check looks for constructs that potentially break IDE support.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using + in versions */
    public static final Issue PLUS = Issue.create(
            "GradleDynamicVersion", //$NON-NLS-1$
            "Gradle Dynamic Version",
            "Using `+` in dependencies lets you automatically pick up the latest available " +
            "version rather than a specific, named version. However, this is not recommended; " +
            "your builds are not repeatable; you may have tested with a slightly different " +
            "version than what the build server used. (Using a dynamic version as the major " +
            "version number is more problematic than using it in the minor version position.)",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Accidentally calling a getter instead of your own methods */
    public static final Issue GRADLE_GETTER = Issue.create(
            "GradleGetter", //$NON-NLS-1$
            "Gradle Implicit Getter Call",
            "Gradle will let you replace specific constants in your build scripts with method " +
            "calls, so you can for example dynamically compute a version string based on your " +
            "current version control revision number, rather than hardcoding a number.\n" +
            "\n" +
            "When computing a version name, it's tempting to for example call the method to do " +
            "that `getVersionName`. However, when you put that method call inside the " +
            "`defaultConfig` block, you will actually be calling the Groovy getter for the "  +
            "`versionName` property instead. Therefore, you need to name your method something " +
            "which does not conflict with the existing implicit getters. Consider using " +
            "`compute` as a prefix instead of `get`.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using incompatible versions */
    public static final Issue COMPATIBILITY = Issue.create(
            "GradleCompatible", //$NON-NLS-1$
            "Incompatible Gradle Versions",

            "There are some combinations of libraries, or tools and libraries, that are " +
            "incompatible, or can lead to bugs. One such incompatibility is compiling with " +
            "a version of the Android support libraries that is not the latest version (or in " +
            "particular, a version lower than your `targetSdkVersion`.)",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    /** Using a string where an integer is expected */
    public static final Issue STRING_INTEGER = Issue.create(
            "StringShouldBeInt", //$NON-NLS-1$
            "String should be int",

            "The properties `compileSdkVersion`, `minSdkVersion` and `targetSdkVersion` are " +
            "usually numbers, but can be strings when you are using an add-on (in the case " +
            "of `compileSdkVersion`) or a preview platform (for the other two properties).\n" +
            "\n" +
            "However, you can not use a number as a string (e.g. \"19\" instead of 19); that " +
            "will result in a platform not found error message at build/sync time.",

            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    /** A newer version is available on a remote server */
    public static final Issue REMOTE_VERSION = Issue.create(
            "NewerVersionAvailable", //$NON-NLS-1$
            "Newer Library Versions Available",
            "This detector checks with a central repository to see if there are newer versions " +
            "available for the dependencies used by this project. " +
            "This is similar to the `GradleDependency` check, which checks for newer versions " +
            "available in the Android SDK tools and libraries, but this works with any " +
            "MavenCentral dependency, and connects to the library every time, which makes " +
            "it more flexible but also *much* slower.",
            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION).setEnabledByDefault(false);

    /** Accidentally using octal numbers */
    public static final Issue ACCIDENTAL_OCTAL = Issue.create(
            "AccidentalOctal", //$NON-NLS-1$
            "Accidental Octal",

            "In Groovy, an integer literal that starts with a leading 0 will be interpreted " +
            "as an octal number. That is usually (always?) an accident and can lead to " +
            "subtle bugs, for example when used in the `versionCode` of an app.",

            Category.CORRECTNESS,
            2,
            Severity.ERROR,
            IMPLEMENTATION);

    /** The Gradle plugin ID for Android applications */
    public static final String APP_PLUGIN_ID = "com.android.application";
    /** The Gradle plugin ID for Android libraries */
    public static final String LIB_PLUGIN_ID = "com.android.library";

    /** Previous plugin id for applications */
    public static final String OLD_APP_PLUGIN_ID = "android";
    /** Previous plugin id for libraries */
    public static final String OLD_LIB_PLUGIN_ID = "android-library";

    private int mMinSdkVersion;
    private int mCompileSdkVersion;
    private int mTargetSdkVersion;

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @Override
    @NonNull
    public Speed getSpeed(@SuppressWarnings("UnusedParameters") @NonNull Issue issue) {
        return issue == REMOTE_VERSION ? Speed.REALLY_SLOW : Speed.NORMAL;
    }

    // ---- Implements Detector.GradleScanner ----

    @Override
    public void visitBuildScript(@NonNull Context context, Map<String, Object> sharedData) {
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingBlock(
            @NonNull String parent,
            @Nullable String parentParent) {
        return parent.equals("defaultConfig")
                || parent.equals("android")
                || parent.equals("dependencies")
                || parent.equals("repositories")
                || parentParent != null && parentParent.equals("buildTypes");
    }

    protected static boolean isInterestingStatement(
            @NonNull String statement,
            @Nullable String parent) {
        return parent == null && statement.equals("apply");
    }

    @SuppressWarnings("UnusedDeclaration")
    protected static boolean isInterestingProperty(
            @NonNull String property,
            @SuppressWarnings("UnusedParameters")
            @NonNull String parent,
            @Nullable String parentParent) {
        return property.equals("targetSdkVersion")
                || property.equals("buildToolsVersion")
                || property.equals("versionName")
                || property.equals("versionCode")
                || property.equals("compileSdkVersion")
                || property.equals("minSdkVersion")
                || property.equals("applicationIdSuffix")
                || property.equals("packageName")
                || property.equals("packageNameSuffix")
                || parent.equals("dependencies");
    }

    protected void checkOctal(
            @NonNull Context context,
            @NonNull String value,
            @NonNull Object cookie) {
        if (value.length() >= 2
                && value.charAt(0) == '0'
                && (value.length() > 2 || value.charAt(1) >= '8'
                && isInteger(value))
                && context.isEnabled(ACCIDENTAL_OCTAL)) {
            String message = "The leading 0 turns this number into octal which is probably "
                    + "not what was intended";
            try {
                long numericValue = Long.decode(value);
                message += " (interpreted as " + numericValue + ")";
            } catch (NumberFormatException nufe) {
                message += " (and it is not a valid octal number)";
            }
            report(context, cookie, ACCIDENTAL_OCTAL, message);
        }
    }

    /** Called with for example "android", "defaultConfig", "minSdkVersion", "7"  */
    @SuppressWarnings("UnusedDeclaration")
    protected void checkDslPropertyAssignment(
        @NonNull Context context,
        @NonNull String property,
        @NonNull String value,
        @NonNull String parent,
        @Nullable String parentParent,
        @NonNull Object valueCookie,
        @NonNull Object statementCookie) {
        if (parent.equals("defaultConfig")) {
            if (property.equals("targetSdkVersion")) {
                int version = getIntLiteralValue(value, -1);
                if (version > 0 && version < context.getClient().getHighestKnownApiLevel()) {
                    String message =
                            "Not targeting the latest versions of Android; compatibility " +
                            "modes apply. Consider testing and updating this version. " +
                           "Consult the android.os.Build.VERSION_CODES javadoc for details.";
                    report(context, valueCookie, TARGET_NEWER, message);
                }
                if (version > 0) {
                    mTargetSdkVersion = version;
                    checkTargetCompatibility(context, valueCookie);
                } else {
                    checkIntegerAsString(context, value, valueCookie);
                }
            } else if (property.equals("minSdkVersion")) {
              int version = getIntLiteralValue(value, -1);
              if (version > 0) {
                mMinSdkVersion = version;
              } else {
                checkIntegerAsString(context, value, valueCookie);
              }
            }

            if (value.startsWith("0")) {
                checkOctal(context, value, valueCookie);
            }

            if (property.equals("versionName") || property.equals("versionCode") &&
                    !isInteger(value) || !isStringLiteral(value)) {
                // Method call -- make sure it does not match one of the getters in the
                // configuration!
                if ((value.equals("getVersionCode") ||
                        value.equals("getVersionName"))) {
                    String message = "Bad method name: pick a unique method name which does not "
                            + "conflict with the implicit getters for the defaultConfig "
                            + "properties. For example, try using the prefix compute- "
                            + "instead of get-.";
                    report(context, valueCookie, GRADLE_GETTER, message);
                }
            } else if (property.equals("packageName")) {
                if (isModelOlderThan011(context)) {
                    return;
                }
                String message = "Deprecated: Replace 'packageName' with 'applicationId'";
                report(context, getPropertyKeyCookie(valueCookie), DEPRECATED, message);
            }
        } else if (property.equals("compileSdkVersion") && parent.equals("android")) {
            int version = getIntLiteralValue(value, -1);
            if (version > 0) {
                mCompileSdkVersion = version;
                checkTargetCompatibility(context, valueCookie);
            } else {
                checkIntegerAsString(context, value, valueCookie);
            }
        } else if (property.equals("buildToolsVersion") && parent.equals("android")) {
            String versionString = getStringLiteralValue(value);
            if (versionString != null) {
                PreciseRevision version = parseRevisionSilently(versionString);
                if (version != null) {
                    PreciseRevision recommended = getLatestBuildTools(context.getClient(),
                            version.getMajor());
                    if (recommended != null && version.compareTo(recommended) < 0) {
                        // Keep in sync with {@link #getOldValue} and {@link #getNewValue}
                        String message = "Old buildToolsVersion " + version +
                                "; recommended version is " + recommended + " or later";
                        report(context, valueCookie, DEPENDENCY, message);
                    }
                }
            }
        } else if (parent.equals("dependencies")) {
            if (value.startsWith("files('") && value.endsWith("')")) {
                String path = value.substring("files('".length(), value.length() - 2);
                if (path.contains("\\\\")) {
                    String message = "Do not use Windows file separators in .gradle files; "
                            + "use / instead";
                    report(context, valueCookie, PATH, message);

                } else if (new File(path.replace('/', File.separatorChar)).isAbsolute()) {
                    String message = "Avoid using absolute paths in .gradle files";
                    report(context, valueCookie, PATH, message);
                }
            } else {
                String dependency = getStringLiteralValue(value);
                if (dependency == null) {
                    dependency = getNamedDependency(value);
                }
                // If the dependency is a GString (i.e. it uses Groovy variable substitution,
                // with a $variable_name syntax) then don't try to parse it.
                if (dependency != null) {
                    GradleCoordinate gc = GradleCoordinate.parseCoordinateString(dependency);
                    if (gc != null && dependency.contains("$")) {
                        gc = resolveCoordinate(context, gc);
                    }
                    if (gc != null) {
                        if (gc.acceptsGreaterRevisions()) {
                            String message = "Avoid using + in version numbers; can lead "
                                    + "to unpredictable and unrepeatable builds (" + dependency + ")";
                            report(context, valueCookie, PLUS, message);
                        }
                        if (!dependency.startsWith(SdkConstants.GRADLE_PLUGIN_NAME) ||
                            !checkGradlePluginDependency(context, gc, valueCookie)) {
                            checkDependency(context, gc, valueCookie);
                        }
                    }
                }
            }
        } else if (property.equals("packageNameSuffix")) {
            if (isModelOlderThan011(context)) {
                return;
            }
            String message = "Deprecated: Replace 'packageNameSuffix' with 'applicationIdSuffix'";
            report(context, getPropertyKeyCookie(valueCookie), DEPRECATED, message);
        } else if (property.equals("applicationIdSuffix")) {
            String suffix = getStringLiteralValue(value);
            if (suffix != null && !suffix.startsWith(".")) {
                String message = "Package suffix should probably start with a \".\"";
                report(context, valueCookie, PATH, message);
            }
        }
    }

    @Nullable
    private static GradleCoordinate resolveCoordinate(@NonNull Context context,
            @NonNull GradleCoordinate gc) {
        assert gc.getFullRevision().contains("$") : gc.getFullRevision();
        Variant variant = context.getProject().getCurrentVariant();
        if (variant != null) {
            Dependencies dependencies = variant.getMainArtifact().getDependencies();
            for (AndroidLibrary library : dependencies.getLibraries()) {
                MavenCoordinates mc = library.getResolvedCoordinates();
                if (mc != null
                        && mc.getGroupId().equals(gc.getGroupId())
                        && mc.getArtifactId().equals(gc.getArtifactId())) {
                    List<RevisionComponent> revisions =
                            GradleCoordinate.parseRevisionNumber(mc.getVersion());
                    if (!revisions.isEmpty()) {
                        return new GradleCoordinate(mc.getGroupId(), mc.getArtifactId(),
                                revisions, null);
                    }
                    break;
                }
            }
        }

        return null;
    }

    // Convert a long-hand dependency, like
    //    group: 'com.android.support', name: 'support-v4', version: '21.0.+'
    // into an equivalent short-hand dependency, like
    //   com.android.support:support-v4:21.0.+
    @VisibleForTesting
    @Nullable
    static String getNamedDependency(@NonNull String expression) {
        //if (value.startsWith("group: 'com.android.support', name: 'support-v4', version: '21.0.+'"))
        if (expression.indexOf(',') != -1 && expression.contains("version:")) {
            String artifact = null;
            String group = null;
            String version = null;
            Splitter splitter = Splitter.on(',').omitEmptyStrings().trimResults();
            for (String property : splitter.split(expression)) {
                int colon = property.indexOf(':');
                if (colon == -1) {
                    return null;
                }
                char quote = '\'';
                int valueStart = property.indexOf(quote, colon + 1);
                if (valueStart == -1) {
                    quote = '"';
                    valueStart = property.indexOf(quote, colon + 1);
                }
                if (valueStart == -1) {
                    // For example, "transitive: false"
                    continue;
                }
                valueStart++;
                int valueEnd = property.indexOf(quote, valueStart);
                if (valueEnd == -1) {
                    return null;
                }
                String value = property.substring(valueStart, valueEnd);
                if (property.startsWith("group:")) {
                    group = value;
                } else if (property.startsWith("name:")) {
                    artifact = value;
                } else if (property.startsWith("version:")) {
                    version = value;
                }
            }

            if (artifact != null && group != null && version != null) {
                return group + ':' + artifact + ':' + version;
            }
        }

        return null;
    }

    private void checkIntegerAsString(Context context, String value, Object valueCookie) {
        // When done developing with a preview platform you might be tempted to switch from
        //     compileSdkVersion 'android-G'
        // to
        //     compileSdkVersion '19'
        // but that won't work; it needs to be
        //     compileSdkVersion 19
        String string = getStringLiteralValue(value);
        if (isNumberString(string)) {
            String quote = Character.toString(value.charAt(0));
            String message = String.format("Use an integer rather than a string here "
                    + "(replace %1$s%2$s%1$s with just %2$s)", quote, string);
            report(context, valueCookie, STRING_INTEGER, message);
        }
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the old value to be replaced in the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param issue the corresponding issue
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding old value, or null if not recognized
     */
    @Nullable
    public static String getOldValue(@NonNull Issue issue, @NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        // Consider extracting all the error strings as constants and handling this
        // using the LintUtils#getFormattedParameters() method to pull back out the information
        if (issue == DEPENDENCY) {
            // "A newer version of com.google.guava:guava than 11.0.2 is available: 17.0.0"
            if (errorMessage.startsWith("A newer ")) {
                return findSubstring(errorMessage, " than ", " ");
            }
            if (errorMessage.startsWith("Old buildToolsVersion ")) {
                return findSubstring(errorMessage, "Old buildToolsVersion ", ";");
            }
            // "The targetSdkVersion (20) should not be higher than the compileSdkVersion (19)"
            return findSubstring(errorMessage, "targetSdkVersion (", ")");
        } else if (issue == STRING_INTEGER) {
            return findSubstring(errorMessage, "replace ", " with ");
        } else if (issue == DEPRECATED) {
            if (errorMessage.contains(GradleDetector.APP_PLUGIN_ID) &&
                    errorMessage.contains(GradleDetector.OLD_APP_PLUGIN_ID)) {
                return GradleDetector.OLD_APP_PLUGIN_ID;
            } else if (errorMessage.contains(GradleDetector.LIB_PLUGIN_ID) &&
                    errorMessage.contains(GradleDetector.OLD_LIB_PLUGIN_ID)) {
                return GradleDetector.OLD_LIB_PLUGIN_ID;
            }
            // "Deprecated: Replace 'packageNameSuffix' with 'applicationIdSuffix'"
            return findSubstring(errorMessage, "Replace '", "'");
        } else if (issue == PLUS) {
          return findSubstring(errorMessage, "(", ")");
        } else if (issue == COMPATIBILITY) {
            if (errorMessage.startsWith("Version 5.2.08")) {
                return "5.2.08";
            }
        }

        return null;
    }

    /**
     * Given an error message produced by this lint detector for the given issue type,
     * returns the new value to be put into the source code.
     * <p>
     * Intended for IDE quickfix implementations.
     *
     * @param issue the corresponding issue
     * @param errorMessage the error message associated with the error
     * @param format the format of the error message
     * @return the corresponding new value, or null if not recognized
     */
    @Nullable
    public static String getNewValue(@NonNull Issue issue, @NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        if (issue == DEPENDENCY) {
            // "A newer version of com.google.guava:guava than 11.0.2 is available: 17.0.0"
            if (errorMessage.startsWith("A newer ")) {
                return findSubstring(errorMessage, " is available: ", null);
            }
            if (errorMessage.startsWith("Old buildToolsVersion ")) {
                return findSubstring(errorMessage, " version is ", " ");
            }
            // "The targetSdkVersion (20) should not be higher than the compileSdkVersion (19)"
            return findSubstring(errorMessage, "compileSdkVersion (", ")");
        } else if (issue == STRING_INTEGER) {
            return findSubstring(errorMessage, " just ", ")");
        } else if (issue == DEPRECATED) {
            if (errorMessage.contains(GradleDetector.APP_PLUGIN_ID) &&
                    errorMessage.contains(GradleDetector.OLD_APP_PLUGIN_ID)) {
                return GradleDetector.APP_PLUGIN_ID;
            } else if (errorMessage.contains(GradleDetector.LIB_PLUGIN_ID) &&
                    errorMessage.contains(GradleDetector.OLD_LIB_PLUGIN_ID)) {
                return GradleDetector.LIB_PLUGIN_ID;
            }
            // "Deprecated: Replace 'packageNameSuffix' with 'applicationIdSuffix'"
            return findSubstring(errorMessage, " with '", "'");
        } else if (issue == COMPATIBILITY) {
            if (errorMessage.startsWith("Version 5.2.08")) {
                return findSubstring(errorMessage, "Use version ", " ");
            }
        }

        return null;
    }

    private static boolean isNumberString(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        for (int i = 0, n = s.length(); i < n; i++) {
            if (!Character.isDigit(s.charAt(i))) {
                return false;
            }
        }

        return true;
    }

    protected void checkMethodCall(
            @NonNull Context context,
            @NonNull String statement,
            @Nullable String parent,
            @NonNull Map<String, String> namedArguments,
            @SuppressWarnings("UnusedParameters")
            @NonNull List<String> unnamedArguments,
            @NonNull Object cookie) {
        String plugin = namedArguments.get("plugin");
        if (statement.equals("apply") && parent == null) {
            boolean isOldAppPlugin = OLD_APP_PLUGIN_ID.equals(plugin);
            if (isOldAppPlugin || OLD_LIB_PLUGIN_ID.equals(plugin)) {
              String replaceWith = isOldAppPlugin ? APP_PLUGIN_ID : LIB_PLUGIN_ID;
              String message = String.format("'%1$s' is deprecated; use '%2$s' instead", plugin,
                      replaceWith);
              report(context, cookie, DEPRECATED, message);
          }
        }
    }

    @Nullable
    private static PreciseRevision parseRevisionSilently(String versionString) {
        try {
            return PreciseRevision.parseRevision(versionString);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isModelOlderThan011(@NonNull Context context) {
        return LintUtils.isModelOlderThan(context.getProject().getGradleProjectModel(), 0, 11, 0);
    }

    private static int sMajorBuildTools;
    private static PreciseRevision sLatestBuildTools;

    /** Returns the latest build tools installed for the given major version.
     * We just cache this once; we don't need to be accurate in the sense that if the
     * user opens the SDK manager and installs a more recent version, we capture this in
     * the same IDE session.
     *
     * @param client the associated client
     * @param major the major version of build tools to look up (e.g. typically 18, 19, ...)
     * @return the corresponding highest known revision
     */
    @Nullable
    private static PreciseRevision getLatestBuildTools(@NonNull LintClient client, int major) {
        if (major != sMajorBuildTools) {
            sMajorBuildTools = major;

            List<PreciseRevision> revisions = Lists.newArrayList();
            if (major == 21) {
                revisions.add(new PreciseRevision(21, 1, 2));
            } else if (major == 20) {
                revisions.add(new PreciseRevision(20));
            } else if (major == 19) {
                revisions.add(new PreciseRevision(19, 1));
            } else if (major == 18) {
                revisions.add(new PreciseRevision(18, 1, 1));
            }
            // The above versions can go stale.
            // Check if a more recent one is installed. (The above are still useful for
            // people who haven't updated with the SDK manager recently.)
            File sdkHome = client.getSdkHome();
            if (sdkHome != null) {
                File[] dirs = new File(sdkHome, FD_BUILD_TOOLS).listFiles();
                if (dirs != null) {
                    for (File dir : dirs) {
                        String name = dir.getName();
                        if (!dir.isDirectory() || !Character.isDigit(name.charAt(0))) {
                            continue;
                        }
                        PreciseRevision v = parseRevisionSilently(name);
                        if (v != null && v.getMajor() == major) {
                            revisions.add(v);
                        }
                    }
                }
            }

            if (!revisions.isEmpty()) {
                sLatestBuildTools = Collections.max(revisions);
            }
        }

        return sLatestBuildTools;
    }

    private void checkTargetCompatibility(Context context, Object cookie) {
        if (mCompileSdkVersion > 0 && mTargetSdkVersion > 0
                && mTargetSdkVersion > mCompileSdkVersion) {
            // NOTE: Keep this in sync with {@link #getOldValue} and {@link #getNewValue}
            String message = "The targetSdkVersion (" + mTargetSdkVersion
                    + ") should not be higher than the compileSdkVersion ("
                    + mCompileSdkVersion + ")";
            report(context, cookie, DEPENDENCY, message);
        }
    }

    @Nullable
    private static String getStringLiteralValue(@NonNull String value) {
        if (value.length() > 2 && (value.startsWith("'") && value.endsWith("'") ||
                value.startsWith("\"") && value.endsWith("\""))) {
            return value.substring(1, value.length() - 1);
        }

        return null;
    }

    private static int getIntLiteralValue(@NonNull String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean isInteger(String token) {
        return token.matches("\\d+");
    }

    private static boolean isStringLiteral(String token) {
        return token.startsWith("\"") && token.endsWith("\"") ||
                token.startsWith("'") && token.endsWith("'");
    }

    private void checkDependency(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull Object cookie) {
        if ("com.android.support".equals(dependency.getGroupId())) {
            checkSupportLibraries(context, dependency, cookie);
            if (mMinSdkVersion >= 14 && "appcompat-v7".equals(dependency.getArtifactId())
                  && mCompileSdkVersion >= 1 && mCompileSdkVersion < 21) {
                report(context, cookie, DEPENDENCY,
                    "Using the appcompat library when minSdkVersion >= 14 and "
                            + "compileSdkVersion < 21 is not necessary");
            }
            return;
        } else if ("com.google.android.gms".equals(dependency.getGroupId())
                && dependency.getArtifactId() != null) {

            // 5.2.08 is not supported; special case and warn about this
            if ("5.2.08".equals(dependency.getFullRevision()) && context.isEnabled(COMPATIBILITY)) {
                // This specific version is actually a preview version which should
                // not be used (https://code.google.com/p/android/issues/detail?id=75292)
                String version = "6.1.11";
                // Try to find a more recent available version, if one is available
                File sdkHome = context.getClient().getSdkHome();
                File repository = SdkMavenRepository.GOOGLE.getRepositoryLocation(sdkHome, true);
                if (repository != null) {
                    GradleCoordinate max = SdkMavenRepository.getHighestInstalledVersion(
                            dependency.getGroupId(), dependency.getArtifactId(), repository,
                            null, false);
                    if (max != null) {
                        if (COMPARE_PLUS_HIGHER.compare(dependency, max) < 0) {
                            version = max.getFullRevision();
                        }
                    }
                }
                String message = String.format("Version `5.2.08` should not be used; the app "
                        + "can not be published with this version. Use version `%1$s` "
                        + "instead.", version);
                report(context, cookie, COMPATIBILITY, message);
            }

            checkPlayServices(context, dependency, cookie);
            return;
        }

        PreciseRevision version = null;
        Issue issue = DEPENDENCY;
        if ("com.android.tools.build".equals(dependency.getGroupId()) &&
                "gradle".equals(dependency.getArtifactId())) {
            try {
                PreciseRevision v =
                        PreciseRevision.parseRevision(GRADLE_PLUGIN_RECOMMENDED_VERSION);
                if (!v.isPreview()) {
                    version = getNewerRevision(dependency, v);
                }
            } catch (NumberFormatException e) {
                context.log(e, null);
            }
        } else if ("com.google.guava".equals(dependency.getGroupId()) &&
                "guava".equals(dependency.getArtifactId())) {
            version = getNewerRevision(dependency, new PreciseRevision(18, 0));
        } else if ("com.google.code.gson".equals(dependency.getGroupId()) &&
                "gson".equals(dependency.getArtifactId())) {
            version = getNewerRevision(dependency, new PreciseRevision(2, 3));
        } else if ("org.apache.httpcomponents".equals(dependency.getGroupId()) &&
                "httpclient".equals(dependency.getArtifactId())) {
            version = getNewerRevision(dependency, new PreciseRevision(4, 3, 5));
        }

        // Network check for really up to date libraries? Only done in batch mode
        if (context.getScope().size() > 1 && context.isEnabled(REMOTE_VERSION)) {
            PreciseRevision latest = getLatestVersion(context, dependency, dependency.isPreview());
            if (latest != null && isOlderThan(dependency, latest.getMajor(), latest.getMinor(),
                    latest.getMicro())) {
                version = latest;
                issue = REMOTE_VERSION;
            }
        }

        if (version != null) {
            String message = getNewerVersionAvailableMessage(dependency, version);
            report(context, cookie, issue, message);
        }
    }

    private static String getNewerVersionAvailableMessage(GradleCoordinate dependency,
            PreciseRevision version) {
        return getNewerVersionAvailableMessage(dependency, version.toString());
    }

    private static String getNewerVersionAvailableMessage(GradleCoordinate dependency,
            String version) {
        // NOTE: Keep this in sync with {@link #getOldValue} and {@link #getNewValue}
        return "A newer version of " + dependency.getGroupId() + ":" +
                dependency.getArtifactId() + " than " + dependency.getFullRevision() +
                " is available: " + version;
    }

    /** TODO: Cache these results somewhere! */
    private static PreciseRevision getLatestVersion(@NonNull Context context,
            @NonNull GradleCoordinate dependency, boolean allowPreview) {
        return getLatestVersion(context, dependency, true, allowPreview);
    }

    private static PreciseRevision getLatestVersion(@NonNull Context context,
            @NonNull GradleCoordinate dependency, boolean firstRowOnly, boolean allowPreview) {
        StringBuilder query = new StringBuilder();
        String encoding = UTF_8.name();
        try {
            query.append("http://search.maven.org/solrsearch/select?q=g:%22");
            query.append(URLEncoder.encode(dependency.getGroupId(), encoding));
            query.append("%22+AND+a:%22");
            query.append(URLEncoder.encode(dependency.getArtifactId(), encoding));
        } catch (UnsupportedEncodingException ee) {
            return null;
        }
        query.append("%22&core=gav");
        if (firstRowOnly) {
            query.append("&rows=1");
        }
        query.append("&wt=json");

        String response = readUrlData(context, dependency, query.toString());
        if (response == null) {
            return null;
        }

        // Sample response:
        //    {
        //        "responseHeader": {
        //            "status": 0,
        //            "QTime": 0,
        //            "params": {
        //                "fl": "id,g,a,v,p,ec,timestamp,tags",
        //                "sort": "score desc,timestamp desc,g asc,a asc,v desc",
        //                "indent": "off",
        //                "q": "g:\"com.google.guava\" AND a:\"guava\"",
        //                "core": "gav",
        //                "wt": "json",
        //                "rows": "1",
        //                "version": "2.2"
        //            }
        //        },
        //        "response": {
        //            "numFound": 37,
        //            "start": 0,
        //            "docs": [{
        //                "id": "com.google.guava:guava:17.0",
        //                "g": "com.google.guava",
        //                "a": "guava",
        //                "v": "17.0",
        //                "p": "bundle",
        //                "timestamp": 1398199666000,
        //                "tags": ["spec", "libraries", "classes", "google", "code"],
        //                "ec": ["-javadoc.jar", "-sources.jar", ".jar", "-site.jar", ".pom"]
        //            }]
        //        }
        //    }

        // Look for version info:  This is just a cheap skim of the above JSON results
        boolean foundPreview = false;
        int index = response.indexOf("\"response\"");   //$NON-NLS-1$
        while (index != -1) {
            index = response.indexOf("\"v\":", index);  //$NON-NLS-1$
            if (index != -1) {
                index += 4;
                int start = response.indexOf('"', index) + 1;
                int end = response.indexOf('"', start + 1);
                if (end > start && start >= 0) {
                    PreciseRevision revision = parseRevisionSilently(response.substring(start, end));
                    if (revision != null) {
                        foundPreview = revision.isPreview();
                        if (allowPreview || !foundPreview) {
                            return revision;
                        }
                    }
                }
            }
        }

        if (!allowPreview && foundPreview && firstRowOnly) {
            // Recurse: search more than the first row this time to see if we can find a
            // non-preview version
            return getLatestVersion(context, dependency, false, false);
        }

        return null;
    }

    /** Normally null; used for testing */
    @Nullable
    @VisibleForTesting
    static Map<String,String> sMockData;

    @Nullable
    private static String readUrlData(
            @NonNull Context context,
            @NonNull GradleCoordinate dependency,
            @NonNull String query) {
        // For unit testing: avoid network as well as unexpected new versions
        if (sMockData != null) {
            String value = sMockData.get(query);
            assert value != null : query;
            return value;
        }

        LintClient client = context.getClient();
        try {
            URL url = new URL(query);

            URLConnection connection = client.openConnection(url);
            if (connection == null) {
                return null;
            }
            try {
                InputStream is = connection.getInputStream();
                if (is == null) {
                    return null;
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, UTF_8));
                try {
                    StringBuilder sb = new StringBuilder(500);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        sb.append('\n');
                    }

                    return sb.toString();
                } finally {
                    reader.close();
                }
            } finally {
                client.closeConnection(connection);
            }
        } catch (IOException ioe) {
            client.log(ioe, "Could not connect to maven central to look up the " +
                    "latest available version for %1$s", dependency);
            return null;
        }
    }

    private boolean checkGradlePluginDependency(Context context, GradleCoordinate dependency,
            Object cookie) {
        GradleCoordinate latestPlugin = GradleCoordinate.parseCoordinateString(
                SdkConstants.GRADLE_PLUGIN_NAME +
                        GRADLE_PLUGIN_MINIMUM_VERSION);
        if (GradleCoordinate.COMPARE_PLUS_HIGHER.compare(dependency, latestPlugin) < 0) {
            String message = "You must use a newer version of the Android Gradle plugin. The "
                    + "minimum supported version is " + GRADLE_PLUGIN_MINIMUM_VERSION +
                    " and the recommended version is " + GRADLE_PLUGIN_RECOMMENDED_VERSION;
            report(context, cookie, GRADLE_PLUGIN_COMPATIBILITY, message);
            return true;
        }
        return false;
    }

    private void checkSupportLibraries(Context context, GradleCoordinate dependency,
            Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        // See if the support library version is lower than the targetSdkVersion
        if (mTargetSdkVersion > 0 && dependency.getMajorVersion() < mTargetSdkVersion &&
                dependency.getMajorVersion() != GradleCoordinate.PLUS_REV_VALUE &&
                // The multidex library doesn't follow normal supportlib numbering scheme
                !dependency.getArtifactId().startsWith("multidex") &&
                context.isEnabled(COMPATIBILITY)) {
            String message = "This support library should not use a lower version ("
                + dependency.getMajorVersion() + ") than the `targetSdkVersion` ("
                    + mTargetSdkVersion + ")";
            report(context, cookie, COMPATIBILITY, message);
        }

        // Check to make sure you have the Android support repository installed
        File sdkHome = context.getClient().getSdkHome();
        File repository = SdkMavenRepository.ANDROID.getRepositoryLocation(sdkHome, true);
        if (repository == null) {
            report(context, cookie, DEPENDENCY,
                    "Dependency on a support library, but the SDK installation does not "
                            + "have the \"Extras > Android Support Repository\" installed. "
                            + "Open the SDK manager and install it.");
        } else {
            checkLocalMavenVersions(context, dependency, cookie, groupId, artifactId,
                    repository);
        }
    }

    private void checkPlayServices(Context context, GradleCoordinate dependency, Object cookie) {
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        assert groupId != null && artifactId != null;

        File sdkHome = context.getClient().getSdkHome();
        File repository = SdkMavenRepository.GOOGLE.getRepositoryLocation(sdkHome, true);
        if (repository == null) {
            report(context, cookie, DEPENDENCY,
                    "Dependency on Play Services, but the SDK installation does not "
                            + "have the \"Extras > Google Repository\" installed. "
                            + "Open the SDK manager and install it.");
        } else {
            checkLocalMavenVersions(context, dependency, cookie, groupId, artifactId,
                    repository);
        }
    }

    private void checkLocalMavenVersions(Context context, GradleCoordinate dependency,
            Object cookie, String groupId, String artifactId, File repository) {
        GradleCoordinate max = SdkMavenRepository.getHighestInstalledVersion(groupId, artifactId,
                repository, null, false);
        if (max != null) {
            if (COMPARE_PLUS_HIGHER.compare(dependency, max) < 0
                    && context.isEnabled(DEPENDENCY)) {
                String message = getNewerVersionAvailableMessage(dependency, max.getFullRevision());
                report(context, cookie, DEPENDENCY, message);
            }
        }
    }

    private static PreciseRevision getNewerRevision(@NonNull GradleCoordinate dependency,
            @NonNull PreciseRevision revision) {
        assert dependency.getGroupId() != null;
        assert dependency.getArtifactId() != null;
        GradleCoordinate coordinate;
        if (revision.isPreview()) {
            String coordinateString = dependency.getGroupId()
                    + ":" + dependency.getArtifactId()
                    + ":" + revision.toString();
            coordinate = GradleCoordinate.parseCoordinateString(coordinateString);
        } else {
            coordinate = new GradleCoordinate(dependency.getGroupId(), dependency.getArtifactId(),
                    revision.getMajor(), revision.getMinor(), revision.getMicro());
        }
        if (COMPARE_PLUS_HIGHER.compare(dependency, coordinate) < 0) {
            return revision;
        } else {
            return null;
        }
    }

    private static boolean isOlderThan(@NonNull GradleCoordinate dependency, int major, int minor,
            int micro) {
        assert dependency.getGroupId() != null;
        assert dependency.getArtifactId() != null;
        return COMPARE_PLUS_HIGHER.compare(dependency,
                new GradleCoordinate(dependency.getGroupId(),
                        dependency.getArtifactId(), major, minor, micro)) < 0;
    }

    private void report(@NonNull Context context, @NonNull Object cookie, @NonNull Issue issue,
            @NonNull String message) {
        if (context.isEnabled(issue)) {
            // Suppressed?
            // Temporarily unconditionally checking for suppress comments in Gradle files
            // since Studio insists on an AndroidLint id prefix
            boolean checkComments = /*context.getClient().checkForSuppressComments()
                    &&*/ context.containsCommentSuppress();
            if (checkComments) {
                int startOffset = getStartOffset(context, cookie);
                if (startOffset >= 0 && context.isSuppressedWithComment(startOffset, issue)) {
                    return;
                }
            }

            context.report(issue, createLocation(context, cookie), message);
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    @NonNull
    protected Object getPropertyKeyCookie(@NonNull Object cookie) {
        return cookie;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedDeclaration"})
    @NonNull
    protected Object getPropertyPairCookie(@NonNull Object cookie) {
      return cookie;
    }

    @SuppressWarnings("MethodMayBeStatic")
    protected int getStartOffset(@NonNull Context context, @NonNull Object cookie) {
        return -1;
    }

    @SuppressWarnings({"MethodMayBeStatic", "UnusedParameters"})
    protected Location createLocation(@NonNull Context context, @NonNull Object cookie) {
        return null;
    }
}
