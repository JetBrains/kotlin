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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.model.*;
import com.android.ide.common.res2.AbstractResourceRepository;
import com.android.ide.common.resources.ResourceUrl;
import com.android.tools.lint.detector.api.*;
import com.google.common.collect.Maps;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;

import static com.android.SdkConstants.*;
import static com.android.xml.AndroidManifest.*;

/**
 * Checks for issues in AndroidManifest files such as declaring elements in the
 * wrong order.
 */
public class ManifestDetector extends Detector implements Detector.XmlScanner {
    private static final Implementation IMPLEMENTATION = new Implementation(
            ManifestDetector.class,
            Scope.MANIFEST_SCOPE
    );

    /** Wrong order of elements in the manifest */
    public static final Issue ORDER = Issue.create(
            "ManifestOrder", //$NON-NLS-1$
            "Incorrect order of elements in manifest",
            "The <application> tag should appear after the elements which declare " +
            "which version you need, which features you need, which libraries you " +
            "need, and so on. In the past there have been subtle bugs (such as " +
            "themes not getting applied correctly) when the `<application>` tag appears " +
            "before some of these other elements, so it's best to order your " +
            "manifest in the logical dependency order.",
            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue USES_SDK = Issue.create(
            "UsesMinSdkAttributes", //$NON-NLS-1$
            "Minimum SDK and target SDK attributes not defined",

            "The manifest should contain a `<uses-sdk>` element which defines the " +
            "minimum API Level required for the application to run, " +
            "as well as the target version (the highest API level you have tested " +
            "the version for.)",

            Category.CORRECTNESS,
            9,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/uses-sdk-element.html"); //$NON-NLS-1$

    /** Using a targetSdkVersion that isn't recent */
    public static final Issue TARGET_NEWER = Issue.create(
            "OldTargetApi", //$NON-NLS-1$
            "Target SDK attribute is not targeting latest version",

            "When your application runs on a version of Android that is more recent than your " +
            "`targetSdkVersion` specifies that it has been tested with, various compatibility " +
            "modes kick in. This ensures that your application continues to work, but it may " +
            "look out of place. For example, if the `targetSdkVersion` is less than 14, your " +
            "app may get an option button in the UI.\n" +
            "\n" +
            "To fix this issue, set the `targetSdkVersion` to the highest available value. Then " +
            "test your app to make sure everything works correctly. You may want to consult " +
            "the compatibility notes to see what changes apply to each version you are adding " +
            "support for: " +
            "http://developer.android.com/reference/android/os/Build.VERSION_CODES.html",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/reference/android/os/Build.VERSION_CODES.html"); //$NON-NLS-1$

    /** Using multiple {@code <uses-sdk>} elements */
    public static final Issue MULTIPLE_USES_SDK = Issue.create(
            "MultipleUsesSdk", //$NON-NLS-1$
            "Multiple `<uses-sdk>` elements in the manifest",

            "The `<uses-sdk>` element should appear just once; the tools will *not* merge the " +
            "contents of all the elements so if you split up the attributes across multiple " +
            "elements, only one of them will take effect. To fix this, just merge all the " +
            "attributes from the various elements into a single <uses-sdk> element.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/uses-sdk-element.html"); //$NON-NLS-1$

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue WRONG_PARENT = Issue.create(
            "WrongManifestParent", //$NON-NLS-1$
            "Wrong manifest parent",

            "The `<uses-library>` element should be defined as a direct child of the " +
            "`<application>` tag, not the `<manifest>` tag or an `<activity>` tag. Similarly, " +
            "a `<uses-sdk>` tag much be declared at the root level, and so on. This check " +
            "looks for incorrect declaration locations in the manifest, and complains " +
            "if an element is found in the wrong place.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/guide/topics/manifest/manifest-intro.html"); //$NON-NLS-1$

    /** Missing a {@code <uses-sdk>} element */
    public static final Issue DUPLICATE_ACTIVITY = Issue.create(
            "DuplicateActivity", //$NON-NLS-1$
            "Activity registered more than once",

            "An activity should only be registered once in the manifest. If it is " +
            "accidentally registered more than once, then subtle errors can occur, " +
            "since attribute declarations from the two elements are not merged, so " +
            "you may accidentally remove previous declarations.",

            Category.CORRECTNESS,
            5,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Not explicitly defining allowBackup */
    public static final Issue ALLOW_BACKUP = Issue.create(
            "AllowBackup", //$NON-NLS-1$
            "Missing `allowBackup` attribute",

            "The allowBackup attribute determines if an application's data can be backed up " +
            "and restored. It is documented at " +
            "http://developer.android.com/reference/android/R.attr.html#allowBackup\n" +
            "\n" +
            "By default, this flag is set to `true`. When this flag is set to `true`, " +
            "application data can be backed up and restored by the user using `adb backup` " +
            "and `adb restore`.\n" +
            "\n" +
            "This may have security consequences for an application. `adb backup` allows " +
            "users who have enabled USB debugging to copy application data off of the " +
            "device. Once backed up, all application data can be read by the user. " +
            "`adb restore` allows creation of application data from a source specified " +
            "by the user. Following a restore, applications should not assume that the " +
            "data, file permissions, and directory permissions were created by the " +
            "application itself.\n" +
            "\n" +
            "Setting `allowBackup=\"false\"` opts an application out of both backup and " +
            "restore.\n" +
            "\n" +
            "To fix this warning, decide whether your application should support backup, " +
            "and explicitly set `android:allowBackup=(true|false)\"`",

            Category.SECURITY,
            3,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/reference/android/R.attr.html#allowBackup");

    /** Conflicting permission names */
    public static final Issue UNIQUE_PERMISSION = Issue.create(
            "UniquePermission", //$NON-NLS-1$
            "Permission names are not unique",

            "The unqualified names or your permissions must be unique. The reason for this " +
            "is that at build time, the `aapt` tool will generate a class named `Manifest` " +
            "which contains a field for each of your permissions. These fields are named " +
            "using your permission unqualified names (i.e. the name portion after the last " +
            "dot).\n" +
            "\n" +
            "If more than one permission maps to the same field name, that field will " +
            "arbitrarily name just one of them.",

            Category.CORRECTNESS,
            6,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Using a resource for attributes that do not allow it */
    public static final Issue SET_VERSION = Issue.create(
            "MissingVersion", //$NON-NLS-1$
            "Missing application name/version",

            "You should define the version information for your application.\n" +
            "`android:versionCode`: An integer value that represents the version of the " +
            "application code, relative to other versions.\n" +
            "\n" +
            "`android:versionName`: A string value that represents the release version of " +
            "the application code, as it should be shown to users.",

            Category.CORRECTNESS,
            2,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/tools/publishing/versioning.html#appversioning");

    /** Using a resource for attributes that do not allow it */
    public static final Issue ILLEGAL_REFERENCE = Issue.create(
            "IllegalResourceRef", //$NON-NLS-1$
            "Name and version must be integer or string, not resource",

            "For the `versionCode` attribute, you have to specify an actual integer " +
            "literal; you cannot use an indirection with a `@dimen/name` resource. " +
            "Similarly, the `versionName` attribute should be an actual string, not " +
            "a string resource url.",

            Category.CORRECTNESS,
            8,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Declaring a uses-feature multiple time */
    public static final Issue DUPLICATE_USES_FEATURE = Issue.create(
            "DuplicateUsesFeature", //$NON-NLS-1$
            "Feature declared more than once",

            "A given feature should only be declared once in the manifest.",

            Category.CORRECTNESS,
            5,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Not explicitly defining application icon */
    public static final Issue APPLICATION_ICON = Issue.create(
            "MissingApplicationIcon", //$NON-NLS-1$
            "Missing application icon",

            "You should set an icon for the application as whole because there is no " +
            "default. This attribute must be set as a reference to a drawable resource " +
            "containing the image (for example `@drawable/icon`).",

            Category.ICONS,
            5,
            Severity.WARNING,
            IMPLEMENTATION).addMoreInfo(
            "http://developer.android.com/tools/publishing/preparing.html#publishing-configure"); //$NON-NLS-1$

    /** Malformed Device Admin */
    public static final Issue DEVICE_ADMIN = Issue.create(
            "DeviceAdmin", //$NON-NLS-1$
            "Malformed Device Admin",
            "If you register a broadcast receiver which acts as a device admin, you must also " +
            "register an `<intent-filter>` for the action " +
            "`android.app.action.DEVICE_ADMIN_ENABLED`, without any `<data>`, such that the " +
            "device admin can be activated/deactivated.\n" +
            "\n" +
            "To do this, add\n" +
            "`<intent-filter>`\n" +
            "    `<action android:name=\"android.app.action.DEVICE_ADMIN_ENABLED\" />`\n" +
            "`</intent-filter>`\n" +
            "to your `<receiver>`.",
            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Using a mock location in a non-debug-specific manifest file */
    public static final Issue MOCK_LOCATION = Issue.create(
            "MockLocation", //$NON-NLS-1$
            "Using mock location provider in production",

            "Using a mock location provider (by requiring the permission " +
            "`android.permission.ACCESS_MOCK_LOCATION`) should *only* be done " +
            "in debug builds (or from tests). In Gradle projects, that means you should only " +
            "request this permission in a test or debug source set specific manifest file.\n" +
            "\n" +
            "To fix this, create a new manifest file in the debug folder and move " +
            "the `<uses-permission>` element there. A typical path to a debug manifest " +
            "override file in a Gradle project is src/debug/AndroidManifest.xml.",

            Category.CORRECTNESS,
            8,
            Severity.FATAL,
            IMPLEMENTATION);

    /** Defining a value that is overridden by Gradle */
    public static final Issue GRADLE_OVERRIDES = Issue.create(
            "GradleOverrides", //$NON-NLS-1$
            "Value overridden by Gradle build script",

            "The value of (for example) `minSdkVersion` is only used if it is not specified in " +
            "the `build.gradle` build scripts. When specified in the Gradle build scripts, " +
            "the manifest value is ignored and can be misleading, so should be removed to " +
            "avoid ambiguity.",

            Category.CORRECTNESS,
            4,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Using drawable rather than mipmap launcher icons */
    public static final Issue MIPMAP = Issue.create(
            "MipmapIcons", //$NON-NLS-1$
            "Use Mipmap Launcher Icons",

            "Launcher icons should be provided in the `mipmap` resource directory. " +
            "This is the same as the `drawable` resource directory, except resources in " +
            "the `mipmap` directory will not get stripped out when creating density-specific " +
            "APKs.\n" +
            "\n" +
            "In certain cases, the Launcher app may use a higher resolution asset (than " +
            "would normally be computed for the device) to display large app shortcuts. " +
            "If drawables for densities other than the device's resolution have been " +
            "stripped out, then the app shortcut could appear blurry.\n" +
            "\n" +
            "To fix this, move your launcher icons from `drawable-`dpi to `mipmap-`dpi " +
            "and change references from @drawable/ and R.drawable to @mipmap/ and R.mipmap.\n" +
            "In Android Studio this lint warning has a quickfix to perform this automatically.",
            Category.ICONS,
            5,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Permission name of mock location permission */
    public static final String MOCK_LOCATION_PERMISSION =
            "android.permission.ACCESS_MOCK_LOCATION";   //$NON-NLS-1$

    /** Constructs a new {@link ManifestDetector} check */
    public ManifestDetector() {
    }

    private boolean mSeenApplication;

    /** Number of times we've seen the <uses-sdk> element */
    private int mSeenUsesSdk;

    /** Activities we've encountered */
    private Set<String> mActivities;

    /** Features we've encountered */
    private Set<String> mUsesFeatures;

    /** Permission basenames */
    private Map<String, String> mPermissionNames;

    /** Handle to the {@code <application>} tag */
    private Location.Handle mApplicationTagHandle;

    /** Whether we've seen an application icon definition in any of the manifest files (or
     * if a manifest tag warning for this has been explicitly disabled) */
    private boolean mSeenAppIcon;

    /** Whether we've seen an allow backup definition in any of the manifest files (or
     * if a manifest tag warning for this has been explicitly disabled) */
    private boolean mSeenAllowBackup;

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return file.getName().equals(ANDROID_MANIFEST_XML);
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mSeenApplication = false;
        mSeenUsesSdk = 0;
        mActivities = new HashSet<String>();
        mUsesFeatures = new HashSet<String>();
    }

    @Override
    public void afterCheckFile(@NonNull Context context) {
        XmlContext xmlContext = (XmlContext) context;
        Element element = xmlContext.document.getDocumentElement();
        if (element != null) {
            checkDocumentElement(xmlContext, element);
        }

        if (mSeenUsesSdk == 0 && context.isEnabled(USES_SDK)
                // Not required in Gradle projects; typically defined in build.gradle instead
                // and inserted at build time
                && !context.getMainProject().isGradleProject()) {
            context.report(USES_SDK, Location.create(context.file),
                    "Manifest should specify a minimum API level with " +
                    "`<uses-sdk android:minSdkVersion=\"?\" />`; if it really supports " +
                    "all versions of Android set it to 1.");
        }
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (!mSeenAllowBackup && context.isEnabled(ALLOW_BACKUP)
                && !context.getProject().isLibrary()
                && context.getMainProject().getMinSdk() >= 4) {
            Location location = getMainApplicationTagLocation(context);
            context.report(ALLOW_BACKUP, location,
                    "Should explicitly set `android:allowBackup` to `true` or " +
                            "`false` (it's `true` by default, and that can have some security " +
                            "implications for the application's data)");
        }

        if (!context.getMainProject().isLibrary()
                && !mSeenAppIcon && context.isEnabled(APPLICATION_ICON)) {
            Location location = getMainApplicationTagLocation(context);
            context.report(APPLICATION_ICON, location,
                    "Should explicitly set `android:icon`, there is no default");
        }
    }

    @Nullable
    private Location getMainApplicationTagLocation(@NonNull Context context) {
        if (mApplicationTagHandle != null) {
            return mApplicationTagHandle.resolve();
        }

        List<File> manifestFiles = context.getMainProject().getManifestFiles();
        if (!manifestFiles.isEmpty()) {
            return Location.create(manifestFiles.get(0));
        }

        return null;
    }

    private static void checkDocumentElement(XmlContext context, Element element) {
        Attr codeNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_VERSION_CODE);
        if (codeNode != null && codeNode.getValue().startsWith(PREFIX_RESOURCE_REF)
                && context.isEnabled(ILLEGAL_REFERENCE)) {
            context.report(ILLEGAL_REFERENCE, element, context.getLocation(codeNode),
                    "The `android:versionCode` cannot be a resource url, it must be "
                            + "a literal integer");
        } else if (codeNode == null && context.isEnabled(SET_VERSION)
                // Not required in Gradle projects; typically defined in build.gradle instead
                // and inserted at build time
                && !context.getMainProject().isGradleProject()) {
            context.report(SET_VERSION, element, context.getLocation(element),
                    "Should set `android:versionCode` to specify the application version");
        }
        Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_VERSION_NAME);
        if (nameNode == null && context.isEnabled(SET_VERSION)
                // Not required in Gradle projects; typically defined in build.gradle instead
                // and inserted at build time
                && !context.getMainProject().isGradleProject()) {
            context.report(SET_VERSION, element, context.getLocation(element),
                    "Should set `android:versionName` to specify the application version");
        }

        checkOverride(context, element, ATTR_VERSION_CODE);
        checkOverride(context, element, ATTR_VERSION_NAME);

        Attr pkgNode = element.getAttributeNode(ATTR_PACKAGE);
        if (pkgNode != null) {
            String pkg = pkgNode.getValue();
            if (pkg.contains("${") && context.getMainProject().isGradleProject()) {
                context.report(GRADLE_OVERRIDES, pkgNode, context.getLocation(pkgNode),
                        "Cannot use placeholder for the package in the manifest; "
                                + "set `applicationId` in `build.gradle` instead");
            }
        }
    }

    private static void checkOverride(XmlContext context, Element element, String attributeName) {
        Project project = context.getProject();
        Attr attribute = element.getAttributeNodeNS(ANDROID_URI, attributeName);
        if (project.isGradleProject() && attribute != null && context.isEnabled(GRADLE_OVERRIDES)) {
            Variant variant = project.getCurrentVariant();
            if (variant != null) {
                ProductFlavor flavor = variant.getMergedFlavor();
                String gradleValue = null;
                if (ATTR_MIN_SDK_VERSION.equals(attributeName)) {
                    try {
                        ApiVersion minSdkVersion = flavor.getMinSdkVersion();
                        gradleValue = minSdkVersion != null ? minSdkVersion.getApiString() : null;
                    } catch (Throwable e) {
                        // TODO: REMOVE ME
                        // This method was added in the 0.11 model. We'll need to drop support
                        // for 0.10 shortly but until 0.11 is available this is a stopgap measure
                    }
                } else if (ATTR_TARGET_SDK_VERSION.equals(attributeName)) {
                    try {
                        ApiVersion targetSdkVersion = flavor.getTargetSdkVersion();
                        gradleValue = targetSdkVersion != null ? targetSdkVersion.getApiString() : null;
                    } catch (Throwable e) {
                        // TODO: REMOVE ME
                        // This method was added in the 0.11 model. We'll need to drop support
                        // for 0.10 shortly but until 0.11 is available this is a stopgap measure
                    }
                } else if (ATTR_VERSION_CODE.equals(attributeName)) {
                    Integer versionCode = flavor.getVersionCode();
                    if (versionCode != null) {
                        gradleValue = versionCode.toString();
                    }
                } else if (ATTR_VERSION_NAME.equals(attributeName)) {
                    gradleValue = flavor.getVersionName();
                } else {
                    assert false : attributeName;
                    return;
                }

                if (gradleValue != null) {
                    String manifestValue = attribute.getValue();

                    String message = String.format("This `%1$s` value (`%2$s`) is not used; it is "
                            + "always overridden by the value specified in the Gradle build "
                            + "script (`%3$s`)", attributeName,  manifestValue, gradleValue);
                    context.report(GRADLE_OVERRIDES, attribute, context.getLocation(attribute),
                            message);
                }
            }
        }
    }

    // ---- Implements Detector.XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(
                TAG_APPLICATION,
                TAG_USES_PERMISSION,
                TAG_PERMISSION,
                "permission-tree",         //$NON-NLS-1$
                "permission-group",        //$NON-NLS-1$
                TAG_USES_SDK,
                "uses-configuration",      //$NON-NLS-1$
                TAG_USES_FEATURE,
                "supports-screens",        //$NON-NLS-1$
                "compatible-screens",      //$NON-NLS-1$
                "supports-gl-texture",     //$NON-NLS-1$
                TAG_USES_LIBRARY,
                TAG_ACTIVITY,
                TAG_SERVICE,
                TAG_PROVIDER,
                TAG_RECEIVER
        );
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        Node parentNode = element.getParentNode();

        boolean isReceiver = tag.equals(TAG_RECEIVER);
        if (isReceiver) {
            checkDeviceAdmin(context, element);
        }

        if (tag.equals(TAG_USES_LIBRARY) || tag.equals(TAG_ACTIVITY) || tag.equals(TAG_SERVICE)
                || tag.equals(TAG_PROVIDER) || isReceiver) {
            if (!TAG_APPLICATION.equals(parentNode.getNodeName())
                    && context.isEnabled(WRONG_PARENT)) {
                context.report(WRONG_PARENT, element, context.getLocation(element),
                        String.format(
                        "The `<%1$s>` element must be a direct child of the <application> element",
                        tag));
            }

            if (tag.equals(TAG_ACTIVITY)) {
                Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (nameNode != null) {
                    String name = nameNode.getValue();
                    if (!name.isEmpty()) {
                        String pkg = context.getMainProject().getPackage();
                        if (name.charAt(0) == '.') {
                            name = pkg + name;
                        } else if (name.indexOf('.') == -1) {
                            name = pkg + '.' + name;
                        }
                        if (mActivities.contains(name)) {
                            String message = String.format(
                                    "Duplicate registration for activity `%1$s`", name);
                            context.report(DUPLICATE_ACTIVITY, element,
                                    context.getLocation(nameNode), message);
                        } else {
                            mActivities.add(name);
                        }
                    }
                }

                checkMipmapIcon(context, element);
            }

            return;
        }

        if (parentNode != element.getOwnerDocument().getDocumentElement()
                && context.isEnabled(WRONG_PARENT)) {
            context.report(WRONG_PARENT, element, context.getLocation(element),
                    String.format(
                    "The `<%1$s>` element must be a direct child of the " +
                    "`<manifest>` root element", tag));
        }

        if (tag.equals(TAG_USES_SDK)) {
            mSeenUsesSdk++;

            if (mSeenUsesSdk == 2) { // Only warn when we encounter the first one
                Location location = context.getLocation(element);

                // Link up *all* encountered locations in the document
                NodeList elements = element.getOwnerDocument().getElementsByTagName(TAG_USES_SDK);
                Location secondary = null;
                for (int i = elements.getLength() - 1; i >= 0; i--) {
                    Element e = (Element) elements.item(i);
                    if (e != element) {
                        Location l = context.getLocation(e);
                        l.setSecondary(secondary);
                        l.setMessage("Also appears here");
                        secondary = l;
                    }
                }
                location.setSecondary(secondary);

                if (context.isEnabled(MULTIPLE_USES_SDK)) {
                    context.report(MULTIPLE_USES_SDK, element, location,
                        "There should only be a single `<uses-sdk>` element in the manifest:" +
                        " merge these together");
                }
                return;
            }

            if (!element.hasAttributeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION)) {
                if (context.isEnabled(USES_SDK) && !context.getMainProject().isGradleProject()) {
                    context.report(USES_SDK, element, context.getLocation(element),
                        "`<uses-sdk>` tag should specify a minimum API level with " +
                        "`android:minSdkVersion=\"?\"`");
                }
            } else {
                Attr codeNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_MIN_SDK_VERSION);
                if (codeNode != null && codeNode.getValue().startsWith(PREFIX_RESOURCE_REF)
                        && context.isEnabled(ILLEGAL_REFERENCE)) {
                    context.report(ILLEGAL_REFERENCE, element, context.getLocation(codeNode),
                            "The `android:minSdkVersion` cannot be a resource url, it must be "
                                    + "a literal integer (or string if a preview codename)");
                }

                checkOverride(context, element, ATTR_MIN_SDK_VERSION);
            }

            if (!element.hasAttributeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION)) {
                // Warn if not setting target SDK -- but only if the min SDK is somewhat
                // old so there's some compatibility stuff kicking in (such as the menu
                // button etc)
                if (context.isEnabled(USES_SDK) && !context.getMainProject().isGradleProject()) {
                    context.report(USES_SDK, element, context.getLocation(element),
                        "`<uses-sdk>` tag should specify a target API level (the " +
                        "highest verified version; when running on later versions, " +
                        "compatibility behaviors may be enabled) with " +
                        "`android:targetSdkVersion=\"?\"`");
                }
            } else {
                checkOverride(context, element, ATTR_TARGET_SDK_VERSION);

                if (context.isEnabled(TARGET_NEWER)) {
                    Attr targetSdkVersionNode = element.getAttributeNodeNS(ANDROID_URI,
                            ATTR_TARGET_SDK_VERSION);
                    if (targetSdkVersionNode != null) {
                        String target = targetSdkVersionNode.getValue();
                        try {
                            int api = Integer.parseInt(target);
                            if (api < context.getClient().getHighestKnownApiLevel()) {
                                context.report(TARGET_NEWER, element,
                                  context.getLocation(targetSdkVersionNode),
                                  "Not targeting the latest versions of Android; compatibility " +
                                  "modes apply. Consider testing and updating this version. " +
                                  "Consult the `android.os.Build.VERSION_CODES` javadoc for details.");
                            }
                        } catch (NumberFormatException nufe) {
                            // Ignore: AAPT will enforce this.
                        }
                    }
                }
            }

            Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_TARGET_SDK_VERSION);
            if (nameNode != null && nameNode.getValue().startsWith(PREFIX_RESOURCE_REF)
                    && context.isEnabled(ILLEGAL_REFERENCE)) {
                context.report(ILLEGAL_REFERENCE, element, context.getLocation(nameNode),
                        "The `android:targetSdkVersion` cannot be a resource url, it must be "
                                + "a literal integer (or string if a preview codename)");
            }
        }
        if (tag.equals(TAG_PERMISSION)) {
            Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            if (nameNode != null) {
                String name = nameNode.getValue();
                String base = name.substring(name.lastIndexOf('.') + 1);
                if (mPermissionNames == null) {
                    mPermissionNames = Maps.newHashMap();
                } else if (mPermissionNames.containsKey(base)) {
                    String prevName = mPermissionNames.get(base);
                    Location location = context.getLocation(nameNode);
                    NodeList siblings = element.getParentNode().getChildNodes();
                    for (int i = 0, n = siblings.getLength(); i < n; i++) {
                        Node node = siblings.item(i);
                        if (node == element) {
                            break;
                        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                            Element sibling = (Element) node;
                            String suffix = '.' + base;
                            if (sibling.getTagName().equals(TAG_PERMISSION)) {
                                String b = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
                                if (b.endsWith(suffix)) {
                                    Location prevLocation = context.getLocation(node);
                                    prevLocation.setMessage("Previous permission here");
                                    location.setSecondary(prevLocation);
                                    break;
                                }

                            }
                        }
                    }

                    String message = String.format("Permission name `%1$s` is not unique " +
                            "(appears in both `%2$s` and `%3$s`)", base, prevName, name);
                    context.report(UNIQUE_PERMISSION, element, location, message);
                }

                mPermissionNames.put(base, name);
            }
        }

        if (tag.equals(TAG_USES_PERMISSION)) {
            Attr name = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            if (name != null && name.getValue().equals(MOCK_LOCATION_PERMISSION)
                    && context.getMainProject().isGradleProject()
                    && !isDebugOrTestManifest(context, context.file)
                    && context.isEnabled(MOCK_LOCATION)) {
                String message = "Mock locations should only be requested in a test or " +
                        "debug-specific manifest file (typically `src/debug/AndroidManifest.xml`)";
                Location location = context.getLocation(name);
                context.report(MOCK_LOCATION, element, location, message);
            }
        }

        if (tag.equals(TAG_APPLICATION)) {
            mSeenApplication = true;
            boolean recordLocation = false;
            if (element.hasAttributeNS(ANDROID_URI, ATTR_ALLOW_BACKUP)
                    || context.getDriver().isSuppressed(context, ALLOW_BACKUP, element)) {
                mSeenAllowBackup = true;
            } else {
                recordLocation = true;
            }
            if (element.hasAttributeNS(ANDROID_URI, ATTR_ICON)
                    || context.getDriver().isSuppressed(context, APPLICATION_ICON, element)) {
                checkMipmapIcon(context, element);
                mSeenAppIcon = true;
            } else {
                recordLocation = true;
            }
            if (recordLocation && !context.getProject().isLibrary() &&
                    (mApplicationTagHandle == null || isMainManifest(context, context.file))) {
                mApplicationTagHandle = context.createLocationHandle(element);
            }
            Attr fullBackupNode = element.getAttributeNodeNS(ANDROID_URI, "fullBackupContent");
            if (fullBackupNode != null &&
                    fullBackupNode.getValue().startsWith(PREFIX_RESOURCE_REF) &&
                    context.getClient().supportsProjectResources()) {
                AbstractResourceRepository resources = context.getClient()
                        .getProjectResources(context.getProject(), true);
                ResourceUrl url = ResourceUrl.parse(fullBackupNode.getValue());
                if (url != null && !url.framework
                        && resources != null
                        && !resources.hasResourceItem(url.type, url.name)) {
                    Location location = context.getValueLocation(fullBackupNode);
                    context.report(ALLOW_BACKUP, location,
                            "Missing `<full-backup-content>` resource");
                }
            } else if (fullBackupNode == null && context.getMainProject().getTargetSdk() >= 23) {
                Location location = context.getLocation(element);
                context.report(ALLOW_BACKUP, location,
                        "Should explicitly set `android:fullBackupContent` to `true` or `false` "
                                + "to opt-in to or out of full app data back-up and restore, or "
                                + "alternatively to an `@xml` resource which specifies which "
                                + "files to backup");
            } else if (fullBackupNode == null && hasGcmReceiver(element)) {
                Location location = context.getLocation(element);
                context.report(ALLOW_BACKUP, location,
                        "Should explicitly set `android:fullBackupContent` to avoid backing up "
                                + "the GCM device specific regId.");
            }
        } else if (mSeenApplication) {
            if (context.isEnabled(ORDER)) {
                context.report(ORDER, element, context.getLocation(element),
                    String.format("`<%1$s>` tag appears after `<application>` tag", tag));
            }

            // Don't complain for *every* element following the <application> tag
            mSeenApplication = false;
        }

        if (tag.equals(TAG_USES_FEATURE)) {
            Attr nameNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
            if (nameNode != null) {
                String name = nameNode.getValue();
                if (!name.isEmpty()) {
                    if (mUsesFeatures.contains(name)) {
                        String message = String.format(
                                "Duplicate declaration of uses-feature `%1$s`", name);
                        context.report(DUPLICATE_USES_FEATURE, element,
                                context.getLocation(nameNode), message);
                    } else {
                        mUsesFeatures.add(name);
                    }
                }
            }
        }
    }

    /**
     * Returns true if the given application element has a receiver with an intent filter
     * action for GCM receive
     */
    private static boolean hasGcmReceiver(@NonNull Element application) {
        NodeList applicationChildren = application.getChildNodes();
        for (int i1 = 0, n1 = applicationChildren.getLength(); i1 < n1; i1++) {
            Node applicationChild = applicationChildren.item(i1);
            if (applicationChild.getNodeType() == Node.ELEMENT_NODE
                    && TAG_RECEIVER.equals(applicationChild.getNodeName())) {
                NodeList receiverChildren = applicationChild.getChildNodes();
                for (int i2 = 0, n2 = receiverChildren.getLength(); i2 < n2; i2++) {
                    Node receiverChild = receiverChildren.item(i2);
                    if (receiverChild.getNodeType() == Node.ELEMENT_NODE
                            && TAG_INTENT_FILTER.equals(receiverChild.getNodeName())) {
                        NodeList filterChildren = receiverChild.getChildNodes();
                        for (int i3 = 0, n3 = filterChildren.getLength(); i3 < n3; i3++) {
                            Node filterChild = filterChildren.item(i3);
                            if (filterChild.getNodeType() == Node.ELEMENT_NODE
                                    && NODE_ACTION.equals(filterChild.getNodeName())) {
                                Element action = (Element) filterChild;
                                String name = action.getAttributeNS(ANDROID_URI, ATTR_NAME);
                                if ("com.google.android.c2dm.intent.RECEIVE".equals(name)) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void checkMipmapIcon(@NonNull XmlContext context, @NonNull Element element) {
        Attr attribute = element.getAttributeNodeNS(ANDROID_URI, ATTR_ICON);
        if (attribute == null) {
            return;
        }
        String icon = attribute.getValue();
        if (icon.startsWith(DRAWABLE_PREFIX)) {
            if (TAG_ACTIVITY.equals(element.getTagName()) && !isLaunchableActivity(element)) {
                return;
            }

            if (context.isEnabled(MIPMAP)
                    // Only complain if this app is skipping some densities
                    && context.getProject().getApplicableDensities() != null) {
                context.report(MIPMAP, element, context.getLocation(attribute),
                        "Should use `@mipmap` instead of `@drawable` for launcher icons");
            }
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static boolean isLaunchableActivity(@NonNull Element element) {
        if (!TAG_ACTIVITY.equals(element.getTagName())) {
            return false;
        }

        for (Element child : LintUtils.getChildren(element)) {
            if (child.getTagName().equals(TAG_INTENT_FILTER)) {
                for (Element innerChild : LintUtils.getChildren(child)) {
                    if (innerChild.getTagName().equals("category")) { //$NON-NLS-1$
                        String categoryString = innerChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                        return "android.intent.category.LAUNCHER".equals(categoryString);
                    }
                }
            }
        }

        return false;
    }

    /** Returns true iff the given manifest file is the main manifest file */
    private static boolean isMainManifest(XmlContext context, File manifestFile) {
        if (!context.getProject().isGradleProject()) {
            // In non-gradle projects, just one manifest per project
            return true;
        }

        AndroidProject model = context.getProject().getGradleProjectModel();
        return model == null || manifestFile
                .equals(model.getDefaultConfig().getSourceProvider().getManifestFile());
    }

    /**
     * Returns true iff the given manifest file is in a debug-specific source set,
     * or a test source set
     */
    private static boolean isDebugOrTestManifest(
            @NonNull XmlContext context,
            @NonNull File manifestFile) {
        AndroidProject model = context.getProject().getGradleProjectModel();
        if (model != null) {
            // Quickly check if it's the main manifest first; that's the most likely scenario
            if (manifestFile.equals(model.getDefaultConfig().getSourceProvider().getManifestFile())) {
                return false;
            }

            // Debug build type?
            for (BuildTypeContainer container : model.getBuildTypes()) {
                if (container.getBuildType().isDebuggable()) {
                    if (manifestFile.equals(container.getSourceProvider().getManifestFile())) {
                        return true;
                    }
                }
            }

            // Test source set?
            for (ProductFlavorContainer container : model.getProductFlavors()) {
                for (SourceProviderContainer extra : container.getExtraSourceProviders()) {
                    String artifactName = extra.getArtifactName();
                    if (AndroidProject.ARTIFACT_ANDROID_TEST.equals(artifactName)
                            && manifestFile.equals(extra.getSourceProvider().getManifestFile())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static void checkDeviceAdmin(XmlContext context, Element element) {
        List<Element> children = LintUtils.getChildren(element);
        boolean requiredIntentFilterFound = false;
        boolean deviceAdmin = false;
        Attr locationNode = null;
        for (Element child : children) {
            String tagName = child.getTagName();
            if (tagName.equals(TAG_INTENT_FILTER) && !requiredIntentFilterFound) {
                boolean dataFound = false;
                boolean actionFound = false;
                for (Element filterChild : LintUtils.getChildren(child)) {
                    String filterTag = filterChild.getTagName();
                    if (filterTag.equals(NODE_ACTION)) {
                        String name = filterChild.getAttributeNS(ANDROID_URI, ATTR_NAME);
                        if ("android.app.action.DEVICE_ADMIN_ENABLED".equals(name)) { //$NON-NLS-1$
                            actionFound = true;
                        }
                    } else if (filterTag.equals(NODE_DATA)) {
                        dataFound = true;
                    }
                }
                if (actionFound && !dataFound) {
                    requiredIntentFilterFound = true;
                }
            } else if (tagName.equals(NODE_METADATA)) {
                Attr valueNode = child.getAttributeNodeNS(ANDROID_URI, ATTR_NAME);
                if (valueNode != null) {
                    String name = valueNode.getValue();
                    if ("android.app.device_admin".equals(name)) { //$NON-NLS-1$
                        deviceAdmin = true;
                        locationNode = valueNode;
                    }
                }
            }
        }

        if (deviceAdmin && !requiredIntentFilterFound && context.isEnabled(DEVICE_ADMIN)) {
            context.report(DEVICE_ADMIN, locationNode, context.getLocation(locationNode),
                "You must have an intent filter for action "
                        + "`android.app.action.DEVICE_ADMIN_ENABLED`");
        }
    }
}
