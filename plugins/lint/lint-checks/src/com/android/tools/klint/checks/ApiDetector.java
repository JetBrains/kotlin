/*
 * Copyright (C) 2012 The Android Open Source Project
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

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.ide.common.repository.GradleVersion;
import com.android.repository.Revision;
import com.android.repository.api.LocalPackage;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.BuildToolInfo;
import com.android.sdklib.SdkVersionInfo;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.tools.klint.client.api.*;
import com.android.tools.klint.detector.api.*;
import com.android.tools.klint.detector.api.Detector.ClassScanner;
import com.intellij.psi.*;
import com.intellij.psi.util.MethodSignatureUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.android.inspections.klint.IntellijLintUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.android.SdkConstants.*;
import static com.android.tools.klint.detector.api.ClassContext.getFqcn;
import static com.android.utils.SdkUtils.getResourceFieldName;

/**
 * Looks for usages of APIs that are not supported in all the versions targeted
 * by this application (according to its minimum API requirement in the manifest).
 */
public class ApiDetector extends ResourceXmlDetector
        implements ClassScanner, Detector.UastScanner {

    private static final String ATTR_WIDTH = "width";
    private static final String ATTR_HEIGHT = "height";
    private static final String ATTR_SUPPORTS_RTL = "supportsRtl";

    public static final String REQUIRES_API_ANNOTATION = SUPPORT_ANNOTATIONS_PREFIX + "RequiresApi"; //$NON-NLS-1$

    /** Accessing an unsupported API */
    @SuppressWarnings("unchecked")
    public static final Issue UNSUPPORTED = Issue.create(
            "NewApi", //$NON-NLS-1$
            "Calling new methods on older versions",

            "This check scans through all the Android API calls in the application and " +
            "warns about any calls that are not available on *all* versions targeted " +
            "by this application (according to its minimum SDK attribute in the manifest).\n" +
            "\n" +
            "If you really want to use this API and don't need to support older devices just " +
            "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files.\n" +
            "\n" +
            "If your code is *deliberately* accessing newer APIs, and you have ensured " +
            "(e.g. with conditional execution) that this code will only ever be called on a " +
            "supported platform, then you can annotate your class or method with the " +
            "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
            "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
            "file's minimum SDK as the required API level.\n" +
            "\n" +
            "If you are deliberately setting `android:` attributes in style definitions, " +
            "make sure you place this in a `values-vNN` folder in order to avoid running " +
            "into runtime conflicts on certain devices where manufacturers have added " +
            "custom attributes whose ids conflict with the new ones on later platforms.\n" +
            "\n" +
            "Similarly, you can use tools:targetApi=\"11\" in an XML file to indicate that " +
            "the element will only be inflated in an adequate context.",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    ApiDetector.class,
                    EnumSet.of(Scope.JAVA_FILE, Scope.RESOURCE_FILE, Scope.MANIFEST),
                    Scope.JAVA_FILE_SCOPE,
                    Scope.RESOURCE_FILE_SCOPE,
                    Scope.MANIFEST_SCOPE));

    /** Accessing an inlined API on older platforms */
    public static final Issue INLINED = Issue.create(
            "InlinedApi", //$NON-NLS-1$
            "Using inlined constants on older versions",

            "This check scans through all the Android API field references in the application " +
            "and flags certain constants, such as static final integers and Strings, " +
            "which were introduced in later versions. These will actually be copied " +
            "into the class files rather than being referenced, which means that " +
            "the value is available even when running on older devices. In some " +
            "cases that's fine, and in other cases it can result in a runtime " +
            "crash or incorrect behavior. It depends on the context, so consider " +
            "the code carefully and device whether it's safe and can be suppressed " +
            "or whether the code needs tbe guarded.\n" +
            "\n" +
            "If you really want to use this API and don't need to support older devices just " +
            "set the `minSdkVersion` in your `build.gradle` or `AndroidManifest.xml` files." +
            "\n" +
            "If your code is *deliberately* accessing newer APIs, and you have ensured " +
            "(e.g. with conditional execution) that this code will only ever be called on a " +
            "supported platform, then you can annotate your class or method with the " +
            "`@TargetApi` annotation specifying the local minimum SDK to apply, such as " +
            "`@TargetApi(11)`, such that this check considers 11 rather than your manifest " +
            "file's minimum SDK as the required API level.\n",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ApiDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    /** Method conflicts with new inherited method */
    public static final Issue OVERRIDE = Issue.create(
            "Override", //$NON-NLS-1$
            "Method conflicts with new inherited method",

            "Suppose you are building against Android API 8, and you've subclassed Activity. " +
            "In your subclass you add a new method called `isDestroyed`(). At some later point, " +
            "a method of the same name and signature is added to Android. Your method will " +
            "now override the Android method, and possibly break its contract. Your method " +
            "is not calling `super.isDestroyed()`, since your compilation target doesn't " +
            "know about the method.\n" +
            "\n" +
            "The above scenario is what this lint detector looks for. The above example is " +
            "real, since `isDestroyed()` was added in API 17, but it will be true for *any* " +
            "method you have added to a subclass of an Android class where your build target " +
            "is lower than the version the method was introduced in.\n" +
            "\n" +
            "To fix this, either rename your method, or if you are really trying to augment " +
            "the builtin method if available, switch to a higher build target where you can " +
            "deliberately add `@Override` on your overriding method, and call `super` if " +
            "appropriate etc.\n",
            Category.CORRECTNESS,
            6,
            Severity.ERROR,
            new Implementation(
                    ApiDetector.class,
                    Scope.CLASS_FILE_SCOPE));

    /** Attribute unused on older versions */
    public static final Issue UNUSED = Issue.create(
            "UnusedAttribute", //$NON-NLS-1$
            "Attribute unused on older versions",

            "This check finds attributes set in XML files that were introduced in a version " +
            "newer than the oldest version targeted by your application (with the " +
            "`minSdkVersion` attribute).\n" +
            "\n" +
            "This is not an error; the application will simply ignore the attribute. However, " +
            "if the attribute is important to the appearance of functionality of your " +
            "application, you should consider finding an alternative way to achieve the " +
            "same result with only available attributes, and then you can optionally create " +
            "a copy of the layout in a layout-vNN folder which will be used on API NN or " +
            "higher where you can take advantage of the newer attribute.\n" +
            "\n" +
            "Note: This check does not only apply to attributes. For example, some tags can be " +
            "unused too, such as the new `<tag>` element in layouts introduced in API 21.",
            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    ApiDetector.class,
                    Scope.RESOURCE_FILE_SCOPE));

    private static final String TAG_RIPPLE = "ripple";
    private static final String TAG_VECTOR = "vector";
    private static final String TAG_ANIMATED_VECTOR = "animated-vector";
    private static final String TAG_ANIMATED_SELECTOR = "animated-selector";

    private static final String SDK_INT = "SDK_INT";
    private static final String REFLECTIVE_OPERATION_EXCEPTION = "java.lang.ReflectiveOperationException";
    public static final String ERROR = "error";

    private ApiLookup mApiDatabase;
    private boolean mWarnedMissingDb;
    private int mMinApi = -1;

    /** Constructs a new API check */
    public ApiDetector() {
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        if (mApiDatabase == null) {
            mApiDatabase = ApiLookup.get(context.getClient());
            // We can't look up the minimum API required by the project here:
            // The manifest file hasn't been processed yet in the -before- project hook.
            // For now it's initialized lazily in getMinSdk(Context), but the
            // lint infrastructure should be fixed to parse manifest file up front.

            if (mApiDatabase == null && !mWarnedMissingDb) {
                mWarnedMissingDb = true;
                context.report(IssueRegistry.LINT_ERROR, Location.create(context.file),
                               "Can't find API database; API check not performed");
            } else {
                // See if you don't have at least version 23.0.1 of platform tools installed
                AndroidSdkHandler sdk = context.getClient().getSdk();
                if (sdk == null) {
                    return;
                }
                LocalPackage pkgInfo = sdk.getLocalPackage(SdkConstants.FD_PLATFORM_TOOLS,
                                                           context.getClient().getRepositoryLogger());
                if (pkgInfo == null) {
                    return;
                }
                Revision revision = pkgInfo.getVersion();

                // The platform tools must be at at least the same revision
                // as the compileSdkVersion!
                // And as a special case, for 23, they must be at 23.0.1
                // because 23.0.0 accidentally shipped without Android M APIs.
                int compileSdkVersion = context.getProject().getBuildSdk();
                if (compileSdkVersion == 23) {
                    if (revision.getMajor() > 23 || revision.getMajor() == 23
                                                    && (revision.getMinor() > 0 || revision.getMicro() > 0)) {
                        return;
                    }
                } else if (compileSdkVersion <= revision.getMajor()) {
                    return;
                }

                // Pick a location: when incrementally linting in the IDE, tie
                // it to the current file
                List<File> currentFiles = context.getProject().getSubset();
                Location location;
                if (currentFiles != null && currentFiles.size() == 1) {
                    File file = currentFiles.get(0);
                    String contents = context.getClient().readFile(file);
                    int firstLineEnd = contents.indexOf('\n');
                    if (firstLineEnd == -1) {
                        firstLineEnd = contents.length();
                    }
                    location = Location.create(file,
                                               new DefaultPosition(0, 0, 0), new
                                                       DefaultPosition(0, firstLineEnd, firstLineEnd));
                } else {
                    location = Location.create(context.file);
                }
                context.report(UNSUPPORTED,
                               location,
                               String.format("The SDK platform-tools version (%1$s) is too old "
                                             + " to check APIs compiled with API %2$d; please update",
                                             revision.toShortString(),
                                             compileSdkVersion));
            }
        }
    }

    // ---- Implements XmlScanner ----

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return true;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return ALL;
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        return ALL;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        if (mApiDatabase == null) {
            return;
        }

        int attributeApiLevel = -1;
        if (ANDROID_URI.equals(attribute.getNamespaceURI())) {
            String name = attribute.getLocalName();
            if (!(name.equals(ATTR_LAYOUT_WIDTH) && !(name.equals(ATTR_LAYOUT_HEIGHT)) &&
                  !(name.equals(ATTR_ID)))) {
                String owner = "android/R$attr"; //$NON-NLS-1$
                attributeApiLevel = mApiDatabase.getFieldVersion(owner, name);
                int minSdk = getMinSdk(context);
                if (attributeApiLevel > minSdk && attributeApiLevel > context.getFolderVersion()
                    && attributeApiLevel > getLocalMinSdk(attribute.getOwnerElement())
                    && !isBenignUnusedAttribute(name)
                    && !isAlreadyWarnedDrawableFile(context, attribute, attributeApiLevel)) {
                    if (RtlDetector.isRtlAttributeName(name) || ATTR_SUPPORTS_RTL.equals(name)) {
                        // No need to warn for example that
                        //  "layout_alignParentEnd will only be used in API level 17 and higher"
                        // since we have a dedicated RTL lint rule dealing with those attributes

                        // However, paddingStart in particular is known to cause crashes
                        // when used on TextViews (and subclasses of TextViews), on some
                        // devices, because vendor specific attributes conflict with the
                        // later-added framework resources, and these are apparently read
                        // by the text views.
                        //
                        // However, as of build tools 23.0.1 aapt works around this by packaging
                        // the resources differently.
                        if (name.equals(ATTR_PADDING_START)) {
                            BuildToolInfo buildToolInfo = context.getProject().getBuildTools();
                            Revision buildTools = buildToolInfo != null
                                                  ? buildToolInfo.getRevision() : null;
                            boolean isOldBuildTools = buildTools != null &&
                                                      (buildTools.getMajor() < 23 || buildTools.getMajor() == 23
                                                                                     && buildTools.getMinor() == 0 && buildTools.getMicro() == 0);
                            if ((buildTools == null || isOldBuildTools) &&
                                viewMayExtendTextView(attribute.getOwnerElement())) {
                                Location location = context.getLocation(attribute);
                                String message = String.format(
                                        "Attribute `%1$s` referenced here can result in a crash on "
                                        + "some specific devices older than API %2$d "
                                        + "(current min is %3$d)",
                                        attribute.getLocalName(), attributeApiLevel, minSdk);
                                //noinspection VariableNotUsedInsideIf
                                if (buildTools != null) {
                                    message = String.format("Upgrade `buildToolsVersion` from "
                                                            + "`%1$s` to at least `23.0.1`; if not, ",
                                                            buildTools.toShortString())
                                              + Character.toLowerCase(message.charAt(0))
                                              + message.substring(1);
                                }
                                context.report(UNSUPPORTED, attribute, location, message);
                            }
                        }
                    } else {
                        Location location = context.getLocation(attribute);
                        String message = String.format(
                                "Attribute `%1$s` is only used in API level %2$d and higher "
                                + "(current min is %3$d)",
                                attribute.getLocalName(), attributeApiLevel, minSdk);
                        context.report(UNUSED, attribute, location, message);
                    }
                }
            }

            // Special case:
            // the dividers attribute is present in API 1, but it won't be read on older
            // versions, so don't flag the common pattern
            //    android:divider="?android:attr/dividerHorizontal"
            // since this will work just fine. See issue 67440 for more.
            if (name.equals("divider")) {
                return;
            }
        }

        String value = attribute.getValue();
        String owner = null;
        String name = null;
        String prefix;
        if (value.startsWith(ANDROID_PREFIX)) {
            prefix = ANDROID_PREFIX;
        } else if (value.startsWith(ANDROID_THEME_PREFIX)) {
            prefix = ANDROID_THEME_PREFIX;
            if (context.getResourceFolderType() == ResourceFolderType.DRAWABLE) {
                int api = 21;
                int minSdk = getMinSdk(context);
                if (api > minSdk && api > context.getFolderVersion()
                    && api > getLocalMinSdk(attribute.getOwnerElement())) {
                    Location location = context.getLocation(attribute);
                    String message;
                    message = String.format(
                            "Using theme references in XML drawables requires API level %1$d "
                            + "(current min is %2$d)", api,
                            minSdk);
                    context.report(UNSUPPORTED, attribute, location, message);
                    // Don't flag individual theme attribute requirements here, e.g. once
                    // we've told you that you need at least v21 to reference themes, we don't
                    // need to also tell you that ?android:selectableItemBackground requires
                    // API level 11
                    return;
                }
            }
        } else if (value.startsWith(PREFIX_ANDROID) && ATTR_NAME.equals(attribute.getName())
                   && TAG_ITEM.equals(attribute.getOwnerElement().getTagName())
                   && attribute.getOwnerElement().getParentNode() != null
                   && TAG_STYLE.equals(attribute.getOwnerElement().getParentNode().getNodeName())) {
            owner = "android/R$attr"; //$NON-NLS-1$
            name = value.substring(PREFIX_ANDROID.length());
            prefix = null;
        } else if (value.startsWith(PREFIX_ANDROID) && ATTR_PARENT.equals(attribute.getName())
                   && TAG_STYLE.equals(attribute.getOwnerElement().getTagName())) {
            owner = "android/R$style"; //$NON-NLS-1$
            name = getResourceFieldName(value.substring(PREFIX_ANDROID.length()));
            prefix = null;
        } else {
            return;
        }

        if (owner == null) {
            // Convert @android:type/foo into android/R$type and "foo"
            int index = value.indexOf('/', prefix.length());
            if (index != -1) {
                owner = "android/R$"    //$NON-NLS-1$
                        + value.substring(prefix.length(), index);
                name = getResourceFieldName(value.substring(index + 1));
            } else if (value.startsWith(ANDROID_THEME_PREFIX)) {
                owner = "android/R$attr";  //$NON-NLS-1$
                name = value.substring(ANDROID_THEME_PREFIX.length());
            } else {
                return;
            }
        }
        int api = mApiDatabase.getFieldVersion(owner, name);
        int minSdk = getMinSdk(context);
        if (api > minSdk && api > context.getFolderVersion()
            && api > getLocalMinSdk(attribute.getOwnerElement())) {
            // Don't complain about resource references in the tools namespace,
            // such as for example "tools:layout="@android:layout/list_content",
            // used only for designtime previews
            if (TOOLS_URI.equals(attribute.getNamespaceURI())) {
                return;
            }

            //noinspection StatementWithEmptyBody
            if (attributeApiLevel >= api) {
                // The attribute will only be *read* on platforms >= attributeApiLevel.
                // If this isn't lower than the attribute reference's API level, it
                // won't be a problem
            } else if (attributeApiLevel > minSdk) {
                String attributeName = attribute.getLocalName();
                Location location = context.getLocation(attribute);
                String message = String.format(
                        "`%1$s` requires API level %2$d (current min is %3$d), but note "
                        + "that attribute `%4$s` is only used in API level %5$d "
                        + "and higher",
                        name, api, minSdk, attributeName, attributeApiLevel);
                context.report(UNSUPPORTED, attribute, location, message);
            } else {
                Location location = context.getLocation(attribute);
                String message = String.format(
                        "`%1$s` requires API level %2$d (current min is %3$d)",
                        value, api, minSdk);
                context.report(UNSUPPORTED, attribute, location, message);
            }
        }
    }

    /**
     * Returns true if the view tag is possibly a text view. It may not be certain,
     * but will err on the side of caution (for example, any custom view is considered
     * to be a potential text view.)
     */
    private static boolean viewMayExtendTextView(@NonNull Element element) {
        String tag = element.getTagName();
        if (tag.equals(VIEW_TAG)) {
            tag = element.getAttribute(ATTR_CLASS);
            if (tag == null || tag.isEmpty()) {
                return false;
            }
        }

        //noinspection SimplifiableIfStatement
        if (tag.indexOf('.') != -1) {
            // Custom views: not sure. Err on the side of caution.
            return true;

        }

        return tag.contains("Text")  // TextView, EditText, etc
               || tag.contains(BUTTON)  // Button, ToggleButton, etc
               || tag.equals("DigitalClock")
               || tag.equals("Chronometer")
               || tag.equals(CHECK_BOX)
               || tag.equals(SWITCH);
    }

    /**
     * Returns true if this attribute is in a drawable document with one of the
     * root tags that require API 21
     */
    private static boolean isAlreadyWarnedDrawableFile(@NonNull XmlContext context,
            @NonNull Attr attribute, int attributeApiLevel) {
        // Don't complain if it's in a drawable file where we've already
        // flagged the root drawable type as being unsupported
        if (context.getResourceFolderType() == ResourceFolderType.DRAWABLE
            && attributeApiLevel == 21) {
            String root = attribute.getOwnerDocument().getDocumentElement().getTagName();
            if (TAG_RIPPLE.equals(root)
                || TAG_VECTOR.equals(root)
                || TAG_ANIMATED_VECTOR.equals(root)
                || TAG_ANIMATED_SELECTOR.equals(root)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Is the given attribute a "benign" unused attribute, one we probably don't need to
     * flag to the user as not applicable on all versions? These are typically attributes
     * which add some nice platform behavior when available, but that are not critical
     * and developers would not typically need to be aware of to try to implement workarounds
     * on older platforms.
     */
    private static boolean isBenignUnusedAttribute(@NonNull String name) {
        return ATTR_LABEL_FOR.equals(name)
               || ATTR_TEXT_IS_SELECTABLE.equals(name)
               || "textAlignment".equals(name)
               || ATTR_FULL_BACKUP_CONTENT.equals(name);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        if (mApiDatabase == null) {
            return;
        }

        String tag = element.getTagName();

        ResourceFolderType folderType = context.getResourceFolderType();
        if (folderType != ResourceFolderType.LAYOUT) {
            if (folderType == ResourceFolderType.DRAWABLE) {
                checkElement(context, element, TAG_VECTOR, 21, "1.4", UNSUPPORTED);
                checkElement(context, element, TAG_RIPPLE, 21, null, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_SELECTOR, 21, null, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_VECTOR, 21, null, UNSUPPORTED);
                checkElement(context, element, "drawable", 24, null, UNSUPPORTED);
                if ("layer-list".equals(tag)) {
                    checkLevelList(context, element);
                } else if (tag.contains(".")) {
                    checkElement(context, element, tag, 24, null, UNSUPPORTED);
                }
            }
            if (element.getParentNode().getNodeType() != Node.ELEMENT_NODE) {
                // Root node
                return;
            }
            NodeList childNodes = element.getChildNodes();
            for (int i = 0, n = childNodes.getLength(); i < n; i++) {
                Node textNode = childNodes.item(i);
                if (textNode.getNodeType() == Node.TEXT_NODE) {
                    String text = textNode.getNodeValue();
                    if (text.contains(ANDROID_PREFIX)) {
                        text = text.trim();
                        // Convert @android:type/foo into android/R$type and "foo"
                        int index = text.indexOf('/', ANDROID_PREFIX.length());
                        if (index != -1) {
                            String typeString = text.substring(ANDROID_PREFIX.length(), index);
                            if (ResourceType.getEnum(typeString) != null) {
                                String owner = "android/R$" + typeString;
                                String name = getResourceFieldName(text.substring(index + 1));
                                int api = mApiDatabase.getFieldVersion(owner, name);
                                int minSdk = getMinSdk(context);
                                if (api > minSdk && api > context.getFolderVersion()
                                    && api > getLocalMinSdk(element)) {
                                    Location location = context.getLocation(textNode);
                                    String message = String.format(
                                            "`%1$s` requires API level %2$d (current min is %3$d)",
                                            text, api, minSdk);
                                    context.report(UNSUPPORTED, element, location, message);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (VIEW_TAG.equals(tag)) {
                tag = element.getAttribute(ATTR_CLASS);
                if (tag == null || tag.isEmpty()) {
                    return;
                }
            } else {
                // TODO: Complain if <tag> is used at the root level!
                checkElement(context, element, TAG, 21, null, UNUSED);
            }

            // Check widgets to make sure they're available in this version of the SDK.
            if (tag.indexOf('.') != -1) {
                // Custom views aren't in the index
                return;
            }
            String fqn = "android/widget/" + tag;    //$NON-NLS-1$
            if (tag.equals("TextureView")) {         //$NON-NLS-1$
                fqn = "android/view/TextureView";    //$NON-NLS-1$
            }
            // TODO: Consider other widgets outside of android.widget.*
            int api = mApiDatabase.getClassVersion(fqn);
            int minSdk = getMinSdk(context);
            if (api > minSdk && api > context.getFolderVersion()
                && api > getLocalMinSdk(element)) {
                Location location = context.getLocation(element);
                String message = String.format(
                        "View requires API level %1$d (current min is %2$d): `<%3$s>`",
                        api, minSdk, tag);
                context.report(UNSUPPORTED, element, location, message);
            }
        }
    }

    /** Checks whether the given element is the given tag, and if so, whether it satisfied
     * the minimum version that the given tag is supported in */
    private void checkLevelList(@NonNull XmlContext context, @NonNull Element element) {
        Node curr = element.getFirstChild();
        while (curr != null) {
            if (curr.getNodeType() == Node.ELEMENT_NODE
                && TAG_ITEM.equals(curr.getNodeName())) {
                Element e = (Element) curr;
                if (e.hasAttributeNS(ANDROID_URI, ATTR_WIDTH)
                    || e.hasAttributeNS(ANDROID_URI, ATTR_HEIGHT)) {
                    int attributeApiLevel = 23; // Using width and height on layer-list children requires M
                    int minSdk = getMinSdk(context);
                    if (attributeApiLevel > minSdk
                        && attributeApiLevel > context.getFolderVersion()
                        && attributeApiLevel > getLocalMinSdk(element)) {
                        for (String attributeName : new String[] { ATTR_WIDTH, ATTR_HEIGHT}) {
                            Attr attribute = e.getAttributeNodeNS(ANDROID_URI, attributeName);
                            if (attribute == null) {
                                continue;
                            }
                            Location location = context.getLocation(attribute);
                            String message = String.format(
                                    "Attribute `%1$s` is only used in API level %2$d and higher "
                                    + "(current min is %3$d)",
                                    attribute.getLocalName(), attributeApiLevel, minSdk);
                            context.report(UNUSED, attribute, location, message);
                        }
                    }
                }
            }
            curr = curr.getNextSibling();
        }
    }

    /** Checks whether the given element is the given tag, and if so, whether it satisfied
     * the minimum version that the given tag is supported in */
    private void checkElement(@NonNull XmlContext context, @NonNull Element element,
            @NonNull String tag, int api, @Nullable String gradleVersion, @NonNull Issue issue) {
        if (tag.equals(element.getTagName())) {
            int minSdk = getMinSdk(context);
            if (api > minSdk
                && api > context.getFolderVersion()
                && api > getLocalMinSdk(element)
                && !featureProvidedByGradle(context, gradleVersion)) {
                Location location = context.getLocation(element);

                // For the <drawable> tag we report it against the class= attribute
                if ("drawable".equals(tag)) {
                    Attr attribute = element.getAttributeNode(ATTR_CLASS);
                    if (attribute == null) {
                        return;
                    }
                    location = context.getLocation(attribute);
                    tag = ATTR_CLASS;
                }

                String message;
                if (issue == UNSUPPORTED) {
                    message = String.format(
                            "`<%1$s>` requires API level %2$d (current min is %3$d)", tag, api,
                            minSdk);
                    if (gradleVersion != null) {
                        message += String.format(
                                " or building with Android Gradle plugin %1$s or higher",
                                gradleVersion);
                    } else if (tag.contains(".")) {
                        message = String.format(
                                "Custom drawables requires API level %1$d (current min is %2$d)",
                                api, minSdk);
                    }
                } else {
                    assert issue == UNUSED : issue;
                    message = String.format(
                            "`<%1$s>` is only used in API level %2$d and higher "
                            + "(current min is %3$d)", tag, api, minSdk);
                }
                context.report(issue, element, location, message);
            }
        }
    }

    protected int getMinSdk(Context context) {
        if (mMinApi == -1) {
            AndroidVersion minSdkVersion = context.getMainProject().getMinSdkVersion();
            mMinApi = minSdkVersion.getFeatureLevel();
        }

        return mMinApi;
    }

    /**
     * Returns the minimum SDK to use in the given element context, or -1 if no
     * {@code tools:targetApi} attribute was found.
     *
     * @param element the element to look at, including parents
     * @return the API level to use for this element, or -1
     */
    private static int getLocalMinSdk(@NonNull Element element) {
        //noinspection ConstantConditions
        while (element != null) {
            String targetApi = element.getAttributeNS(TOOLS_URI, ATTR_TARGET_API);
            if (targetApi != null && !targetApi.isEmpty()) {
                if (Character.isDigit(targetApi.charAt(0))) {
                    try {
                        return Integer.parseInt(targetApi);
                    } catch (NumberFormatException e) {
                        break;
                    }
                } else {
                    return SdkVersionInfo.getApiByBuildCode(targetApi, true);
                }
            }

            Node parent = element.getParentNode();
            if (parent != null && parent.getNodeType() == Node.ELEMENT_NODE) {
                element = (Element) parent;
            } else {
                break;
            }
        }

        return -1;
    }

    /**
     * Checks if the current project supports features added in {@code minGradleVersion} version of
     * the Android gradle plugin.
     *
     * @param context                Current context.
     * @param minGradleVersionString Version in which support for a given feature was added, or null
     *                               if it's not supported at build time.
     */
    private static boolean featureProvidedByGradle(@NonNull XmlContext context,
            @Nullable String minGradleVersionString) {
        if (minGradleVersionString == null) {
            return false;
        }

        GradleVersion gradleModelVersion = context.getProject().getGradleModelVersion();
        if (gradleModelVersion != null) {
            GradleVersion minVersion = GradleVersion.tryParse(minGradleVersionString);
            if (minVersion != null
                && gradleModelVersion.compareIgnoringQualifiers(minVersion) >= 0) {
                return true;
            }
        }
        return false;
    }

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        if (mApiDatabase == null) {
            return new AbstractUastVisitor() {
                @Override
                public boolean visitElement(@NotNull UElement element) {
                    // No-op. Workaround for super currently calling
                    //   ProgressIndicatorProvider.checkCanceled();
                    return super.visitElement(element);
                }
            };
        }
        return new ApiVisitor(context);
    }

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        List<Class<? extends UElement>> types = new ArrayList<Class<? extends UElement>>(9);
        types.add(UImportStatement.class);
        types.add(USimpleNameReferenceExpression.class);
        types.add(UVariable.class);
        types.add(UTryExpression.class);
        types.add(UBinaryExpressionWithType.class);
        types.add(UBinaryExpression.class);
        types.add(UCallExpression.class);
        types.add(UClass.class);
        types.add(UTypeReferenceExpression.class);
        types.add(UClassLiteralExpression.class);
        types.add(UMethod.class);
        return types;
    }

    /**
     * Checks whether the given instruction is a benign usage of a constant defined in
     * a later version of Android than the application's {@code minSdkVersion}.
     *
     * @param node  the instruction to check
     * @param name  the name of the constant
     * @param owner the field owner
     * @return true if the given usage is safe on older versions than the introduction
     *              level of the constant
     */
    private static boolean isBenignConstantUsage(
            @Nullable UElement node,
            @NonNull String name,
            @NonNull String owner
    ) {
        if (owner.equals("android/os/Build$VERSION_CODES")) {     //$NON-NLS-1$
            // These constants are required for compilation, not execution
            // and valid code checks it even on older platforms
            return true;
        }
        if (owner.equals("android/view/ViewGroup$LayoutParams")   //$NON-NLS-1$
            && name.equals("MATCH_PARENT")) {                 //$NON-NLS-1$
            return true;
        }
        if (owner.equals("android/widget/AbsListView")            //$NON-NLS-1$
            && ((name.equals("CHOICE_MODE_NONE")              //$NON-NLS-1$
                 || name.equals("CHOICE_MODE_MULTIPLE")            //$NON-NLS-1$
                 || name.equals("CHOICE_MODE_SINGLE")))) {         //$NON-NLS-1$
            // android.widget.ListView#CHOICE_MODE_MULTIPLE and friends have API=1,
            // but in API 11 it was moved up to the parent class AbsListView.
            // Referencing AbsListView#CHOICE_MODE_MULTIPLE technically requires API 11,
            // but the constant is the same as the older version, so accept this without
            // warning.
            return true;
        }

        // Gravity#START and Gravity#END are okay; these were specifically written to
        // be backwards compatible (by using the same lower bits for START as LEFT and
        // for END as RIGHT)
        if ("android/view/Gravity".equals(owner)                   //$NON-NLS-1$
            && ("START".equals(name) || "END".equals(name))) { //$NON-NLS-1$ //$NON-NLS-2$
            return true;
        }

        if (node == null) {
            return false;
        }

        // It's okay to reference the constant as a case constant (since that
        // code path won't be taken) or in a condition of an if statement
        UElement curr = node.getUastParent();
        while (curr != null) {
            if (curr instanceof USwitchClauseExpression) {
                List<UExpression> caseValues = ((USwitchClauseExpression) curr).getCaseValues();
                if (caseValues != null) {
                    for (UExpression condition : caseValues) {
                        if (condition != null && UastUtils.isChildOf(node, condition, false)) {
                            return true;
                        }
                    }
                }
                return false;
            } else if (curr instanceof UIfExpression) {
                UExpression condition = ((UIfExpression) curr).getCondition();
                return UastUtils.isChildOf(node, condition, false);
            } else if (curr instanceof UMethod || curr instanceof UClass) {
                break;
            }
            curr = curr.getUastParent();
        }

        return false;
    }

    public static int getRequiredVersion(String errorMessage) {
        Pattern pattern = Pattern.compile("\\s(\\d+)\\s");
        Matcher matcher = pattern.matcher(errorMessage);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }

        return -1;
    }

    private final class ApiVisitor extends AbstractUastVisitor {
        private final JavaContext mContext;

        private ApiVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitTypeReferenceExpression(@NotNull UTypeReferenceExpression node) {
            checkType(node.getType(), node);
            return super.visitTypeReferenceExpression(node);
        }

        @Override
        public boolean visitClassLiteralExpression(@NotNull UClassLiteralExpression node) {
            checkType(node.getType(), node);
            return super.visitClassLiteralExpression(node);
        }

        @Override
        public boolean visitImportStatement(@NotNull UImportStatement statement) {
            if (!statement.isOnDemand()) {
                PsiElement resolved = statement.resolve();
                if (resolved instanceof PsiField) {
                    checkField(statement, (PsiField)resolved);
                }
            }

            return super.visitImportStatement(statement);
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(@NotNull USimpleNameReferenceExpression node) {
            PsiElement resolved = node.resolve();
            if (resolved instanceof PsiField) {
                checkField(node, (PsiField)resolved);
            }

            return super.visitSimpleNameReferenceExpression(node);
        }

        @Override
        public boolean visitBinaryExpressionWithType(@NotNull UBinaryExpressionWithType node) {
            visitTypeCastExpression(node);
            return super.visitBinaryExpressionWithType(node);
        }

        private void visitTypeCastExpression(UBinaryExpressionWithType expression) {
            UExpression operand = expression.getOperand();

            PsiType operandType = operand.getExpressionType();
            PsiType castType = expression.getType();

            if (castType.equals(operandType)) {
                return;
            }
            if (!(operandType instanceof PsiClassType)) {
                return;
            }
            if (!(castType instanceof PsiClassType)) {
                return;
            }
            PsiClassType classType = (PsiClassType)operandType;
            PsiClassType interfaceType = (PsiClassType)castType;
            checkCast(expression, classType, interfaceType);
        }

        private void checkCast(@NonNull UElement node, @NonNull PsiClassType classType,
                @NonNull PsiClassType interfaceType) {
            if (classType.equals(interfaceType)) {
                return;
            }
            JavaEvaluator evaluator = mContext.getEvaluator();
            String classTypeInternal = evaluator.getInternalName(classType);
            String interfaceTypeInternal = evaluator.getInternalName(interfaceType);
            if ("java/lang/Object".equals(interfaceTypeInternal)) {
                return;
            }

            int api = mApiDatabase.getValidCastVersion(classTypeInternal, interfaceTypeInternal);
            if (api == -1) {
                return;
            }

            int minSdk = getMinSdk(mContext);
            if (api <= minSdk) {
                return;
            }

            if (isSuppressed(api, node, minSdk, mContext, UNSUPPORTED)) {
                return;
            }

            Location location = mContext.getUastLocation(node);
            String message = String.format("Cast from %1$s to %2$s requires API level %3$d (current min is %4$d)",
                                           UastLintUtils.getClassName(classType),
                                           UastLintUtils.getClassName(interfaceType), api, minSdk);
            mContext.report(UNSUPPORTED, location, message);
        }

        @Override
        public boolean visitMethod(@NotNull UMethod method) {
            // API check for default methods
            if (method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT)) {
                int api = 24; // minSdk for default methods
                int minSdk = getMinSdk(mContext);

                if (!isSuppressed(api, method, minSdk, mContext, UNSUPPORTED)) {
                    Location location = mContext.getLocation(method);
                    String message = String.format("Default method requires API level %1$d "
                                                   + "(current min is %2$d)", api, minSdk);
                    mContext.reportUast(UNSUPPORTED, method, location, message);
                }
            }

            return super.visitMethod(method);
        }

        @Override
        public boolean visitClass(@NotNull UClass aClass) {
            // Check for repeatable annotations
            if (aClass.isAnnotationType()) {
                PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                        String name = annotation.getQualifiedName();
                        if ("java.lang.annotation.Repeatable".equals(name)) {
                            int api = 24; // minSdk for repeatable annotations
                            int minSdk = getMinSdk(mContext);
                            if (!isSuppressed(api, aClass, minSdk, mContext, UNSUPPORTED)) {
                                Location location = mContext.getLocation(annotation);
                                String message = String.format("Repeatable annotation requires "
                                                               + "API level %1$d (current min is %2$d)", api, minSdk);
                                mContext.report(UNSUPPORTED, annotation, location, message);
                            }
                        } else if ("java.lang.annotation.Target".equals(name)) {
                            PsiNameValuePair[] attributes = annotation.getParameterList()
                                    .getAttributes();
                            for (PsiNameValuePair pair : attributes) {
                                PsiAnnotationMemberValue value = pair.getValue();
                                if (value instanceof PsiArrayInitializerMemberValue) {
                                    PsiArrayInitializerMemberValue array
                                            = (PsiArrayInitializerMemberValue) value;
                                    for (PsiAnnotationMemberValue t : array.getInitializers()) {
                                        checkAnnotationTarget(t, modifierList);
                                    }
                                } else if (value != null) {
                                    checkAnnotationTarget(value, modifierList);
                                }
                            }
                        }
                    }
                }
            } else {
                for (UTypeReferenceExpression t : aClass.getUastSuperTypes()) {
                    checkType(t.getType(), t);
                }
            }

            return super.visitClass(aClass);
        }

        private void checkAnnotationTarget(@NonNull PsiAnnotationMemberValue element,
                PsiModifierList modifierList) {
            if (element instanceof UReferenceExpression) {
                UReferenceExpression ref = (UReferenceExpression) element;
                String referenceName = UastLintUtils.getReferenceName(ref);
                if ("TYPE_PARAMETER".equals(referenceName)
                    || "TYPE_USE".equals(referenceName)) {
                    PsiAnnotation retention = modifierList
                            .findAnnotation("java.lang.annotation.Retention");
                    if (retention == null ||
                        retention.getText().contains("RUNTIME")) {
                        Location location = mContext.getLocation(element);
                        String message = String.format("Type annotations are not "
                                                       + "supported in Android: %1$s", referenceName);
                        mContext.report(UNSUPPORTED, element, location, message);
                    }
                }
            }
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression expression) {
            checkMethodCallExpression(expression);
            return super.visitCallExpression(expression);
        }

        private void checkMethodCallExpression(@NotNull UCallExpression expression) {
            PsiMethod method = expression.resolve();
            if (method != null) {
                PsiClass containingClass = method.getContainingClass();
                if (containingClass == null) {
                    return;
                }

                PsiParameterList parameterList = method.getParameterList();
                if (parameterList.getParametersCount() > 0) {
                    PsiParameter[] parameters = parameterList.getParameters();

                    List<UExpression> arguments = expression.getValueArguments();
                    for (int i = 0; i < parameters.length; i++) {
                        PsiType parameterType = parameters[i].getType();
                        if (parameterType instanceof PsiClassType) {
                            if (i >= arguments.size()) {
                                // We can end up with more arguments than parameters when
                                // there is a varargs call.
                                break;
                            }
                            UExpression argument = arguments.get(i);
                            PsiType argumentType = argument.getExpressionType();
                            if (argumentType == null || parameterType.equals(argumentType) || !(argumentType instanceof PsiClassType)) {
                                continue;
                            }
                            checkCast(argument, (PsiClassType)argumentType, (PsiClassType)parameterType);
                        }
                    }
                }

                JavaEvaluator evaluator = mContext.getEvaluator();
                String fqcn = containingClass.getQualifiedName();
                String owner = evaluator.getInternalName(containingClass);
                if (owner == null) {
                    return; // Couldn't resolve type
                }

                String name = IntellijLintUtils.getInternalMethodName(method);
                String desc = IntellijLintUtils.getInternalDescription(method, false, false);
                if (desc == null) {
                    // Couldn't compute description of method for some reason; probably
                    // failure to resolve parameter types
                    return;
                }

                boolean hasApiAnnotation = false;
                int api = mApiDatabase.getCallVersion(owner, name, desc);
                if (api == -1) {
                    api = getTargetApi(method.getModifierList());
                    if (api == -1 && method.isConstructor()) {
                        api = getTargetApi(method.getContainingClass().getModifierList());
                    }

                    if (api == -1) {
                        return;
                    } else {
                        hasApiAnnotation = true;
                    }
                }

                int minSdk = getMinSdk(mContext);
                if (api <= minSdk) {
                    return;
                }

                // The lint API database contains two optimizations:
                // First, all members that were available in API 1 are omitted from the database, since that saves
                // about half of the size of the database, and for API check purposes, we don't need to distinguish
                // between "doesn't exist" and "available in all versions".
                // Second, all inherited members were inlined into each class, so that it doesn't have to do a
                // repeated search up the inheritance chain.
                //
                // Unfortunately, in this custom PSI detector, we look up the real resolved method, which can sometimes
                // have a different minimum API.
                //
                // For example, SQLiteDatabase had a close() method from API 1. Therefore, calling SQLiteDatabase is supported
                // in all versions. However, it extends SQLiteClosable, which in API 16 added "implements Closable". In
                // this detector, if we have the following code:
                //     void test(SQLiteDatabase db) { db.close }
                // here the call expression will be the close method on type SQLiteClosable. And that will result in an API
                // requirement of API 16, since the close method it now resolves to is in API 16.
                //
                // To work around this, we can now look up the type of the call expression ("db" in the above, but it could
                // have been more complicated), and if that's a different type than the type of the method, we look up
                // *that* method from lint's database instead. Furthermore, it's possible for that method to return "-1"
                // and we can't tell if that means "doesn't exist" or "present in API 1", we then check the package prefix
                // to see whether we know it's an API method whose members should all have been inlined.
                if (!hasApiAnnotation && UastExpressionUtils.isMethodCall(expression)) {
                    UExpression qualifier = expression.getReceiver();
                    if (qualifier != null && !(qualifier instanceof UThisExpression) && !(qualifier instanceof USuperExpression)) {
                        PsiType type = qualifier.getExpressionType();
                        if (type != null && type instanceof PsiClassType) {
                            String expressionOwner = evaluator.getInternalName((PsiClassType)type);
                            if (expressionOwner != null && !expressionOwner.equals(owner)) {
                                int specificApi = mApiDatabase.getCallVersion(expressionOwner, name, desc);
                                if (specificApi == -1) {
                                    if (ApiLookup.isRelevantOwner(expressionOwner)) {
                                        return;
                                    }
                                } else if (specificApi <= minSdk) {
                                    return;
                                } else {
                                    // For example, for Bundle#getString(String,String) the API level is 12, whereas for
                                    // BaseBundle#getString(String,String) the API level is 21. If the code specified a Bundle instead of
                                    // a BaseBundle, reported the Bundle level in the error message instead.
                                    if (specificApi < api) {
                                        api = specificApi;
                                        fqcn = expressionOwner.replace('/', '.');
                                    }
                                    api = Math.min(specificApi, api);
                                }
                            }
                        }
                    } else {
                        // Unqualified call; need to search in our super hierarchy
                        PsiClass cls = null;
                        PsiType receiverType = expression.getReceiverType();
                        if (receiverType instanceof PsiClassType) {
                            cls = ((PsiClassType) receiverType).resolve();
                        }

                        while (cls != null) {
                            if (cls instanceof PsiAnonymousClass) {
                                // If it's an unqualified call in an anonymous class, we need to rely on the
                                // resolve method to find out whether the method is picked up from the anonymous
                                // class chain or any outer classes
                                boolean found = false;
                                PsiClassType anonymousBaseType = ((PsiAnonymousClass)cls).getBaseClassType();
                                PsiClass anonymousBase = anonymousBaseType.resolve();
                                if (anonymousBase != null && anonymousBase.isInheritor(containingClass, true)) {
                                    cls = anonymousBase;
                                    found = true;
                                } else {
                                    PsiClass surroundingBaseType = PsiTreeUtil.getParentOfType(cls, PsiClass.class, true);
                                    if (surroundingBaseType != null && surroundingBaseType.isInheritor(containingClass, true)) {
                                        cls = surroundingBaseType;
                                        found = true;
                                    }
                                }
                                if (!found) {
                                    break;
                                }
                            }
                            String expressionOwner = evaluator.getInternalName(cls);
                            if (expressionOwner == null) {
                                break;
                            }
                            int specificApi = mApiDatabase.getCallVersion(expressionOwner, name, desc);
                            if (specificApi == -1) {
                                if (ApiLookup.isRelevantOwner(expressionOwner)) {
                                    return;
                                }
                            } else if (specificApi <= minSdk) {
                                return;
                            } else {
                                if (specificApi < api) {
                                    api = specificApi;
                                    fqcn = expressionOwner.replace('/', '.');
                                }
                                api = Math.min(specificApi, api);
                                break;
                            }
                            cls = cls.getSuperClass();
                        }
                    }
                }

                if (isSuppressed(api, expression, minSdk, mContext, UNSUPPORTED)) {
                    return;
                }

                // If you're simply calling super.X from method X, even if method X is in a higher API level than the minSdk, we're
                // generally safe; that method should only be called by the framework on the right API levels. (There is a danger of
                // somebody calling that method locally in other contexts, but this is hopefully unlikely.)
                if (UastExpressionUtils.isMethodCall(expression)) {
                    if (expression.getReceiver() instanceof USuperExpression) {
                        PsiMethod containingMethod = UastUtils.getContainingMethod(expression);
                        if (containingMethod != null && name.equals(containingMethod.getName())
                            && MethodSignatureUtil.areSignaturesEqual(method, containingMethod)
                            // We specifically exclude constructors from this check, because we do want to flag constructors requiring the
                            // new API level; it's highly likely that the constructor is called by local code so you should specifically
                            // investigate this as a developer
                            && !method.isConstructor()) {
                            return;
                        }
                    }
                }

                UElement locationNode = expression.getMethodIdentifier();
                if (locationNode == null) {
                    locationNode = expression;
                }
                Location location = mContext.getUastLocation(locationNode);
                String message = String.format("Call requires API level %1$d (current min is %2$d): %3$s", api, minSdk,
                                               fqcn + '#' + method.getName());

                mContext.report(UNSUPPORTED, location, message);
            }
        }

        @Override
        public boolean visitLocalVariable(ULocalVariable variable) {
            UExpression initializer = variable.getUastInitializer();
            if (initializer == null) {
                return true;
            }

            PsiType initializerType = initializer.getExpressionType();
            if (!(initializerType instanceof PsiClassType)) {
                return true;
            }

            PsiType interfaceType = variable.getType();
            if (initializerType.equals(interfaceType)) {
                return true;
            }

            if (!(interfaceType instanceof PsiClassType)) {
                return true;
            }

            checkCast(initializer, (PsiClassType)initializerType, (PsiClassType)interfaceType);
            return true;
        }

        @Override
        public boolean visitBinaryExpression(@NotNull UBinaryExpression node) {
            if (UastExpressionUtils.isAssignment(node)) {
                visitAssignmentExpression(node);
            }

            return super.visitBinaryExpression(node);
        }

        private void visitAssignmentExpression(UBinaryExpression expression) {
            UExpression rExpression = expression.getRightOperand();
            PsiType rhsType = rExpression.getExpressionType();
            if (!(rhsType instanceof PsiClassType)) {
                return;
            }

            PsiType interfaceType = expression.getLeftOperand().getExpressionType();
            if (rhsType.equals(interfaceType)) {
                return;
            }

            if (!(interfaceType instanceof PsiClassType)) {
                return;
            }

            checkCast(rExpression, (PsiClassType)rhsType, (PsiClassType)interfaceType);
        }

        @Override
        public boolean visitTryExpression(@NotNull UTryExpression statement) {
            if (statement.getHasResources()) {
                int api = 19; // minSdk for try with resources
                int minSdk = getMinSdk(mContext);

                if (api > minSdk && api > getTargetApi(statement)) {
                    Location location = mContext.getUastLocation(statement);
                    String message = String.format("Try-with-resources requires "
                                                   + "API level %1$d (current min is %2$d)", api, minSdk);
                    LintDriver driver = mContext.getDriver();
                    if (!driver.isSuppressed(mContext, UNSUPPORTED, statement)) {
                        mContext.report(UNSUPPORTED, statement, location, message);
                    }
                }
            }

            for (UCatchClause catchClause : statement.getCatchClauses()) {

                // Special case reflective operation exception which can be implicitly used
                // with multi-catches: see issue 153406
                int minSdk = getMinSdk(mContext);
                if(minSdk < 19 && isMultiCatchReflectiveOperationException(catchClause)) {
                    String message = String.format("Multi-catch with these reflection exceptions requires API level 19 (current min is %d) " +
                                                   "because they get compiled to the common but new super type `ReflectiveOperationException`. " +
                                                   "As a workaround either create individual catch statements, or catch `Exception`.",
                                                   minSdk);

                    mContext.report(UNSUPPORTED, getCatchParametersLocation(mContext, catchClause), message);
                    continue;
                }

                for (UTypeReferenceExpression typeReference : catchClause.getTypeReferences()) {
                    checkCatchTypeElement(statement, typeReference, typeReference.getType());
                }
            }

            return super.visitTryExpression(statement);
        }

        private void checkCatchTypeElement(@NonNull UTryExpression statement,
                @NonNull UTypeReferenceExpression typeReference,
                @Nullable PsiType type) {
            PsiClass resolved = null;
            if (type instanceof PsiClassType) {
                resolved = ((PsiClassType) type).resolve();
            }
            if (resolved != null) {
                String signature = mContext.getEvaluator().getInternalName(resolved);
                int api = mApiDatabase.getClassVersion(signature);
                if (api == -1) {
                    return;
                }
                int minSdk = getMinSdk(mContext);
                if (api <= minSdk) {
                    return;
                }
                int target = getTargetApi(statement);
                if (target != -1 && api <= target) {
                    return;
                }

                Location location = mContext.getUastLocation(typeReference);
                String fqcn = resolved.getQualifiedName();
                String message = String.format("Class requires API level %1$d (current min is %2$d): %3$s",
                                               api, minSdk, fqcn);
                mContext.report(UNSUPPORTED, location, message);
            }
        }

        private void checkType(PsiType type, UElement element) {
            if (!(type instanceof PsiClassType)) {
                return;
            }

            PsiClass resolved = ((PsiClassType) type).resolve();
            if (resolved == null) {
                return;
            }

            String signature = mContext.getEvaluator().getInternalName(resolved);
            int api = mApiDatabase.getClassVersion(signature);
            if (api == -1) {
                return;
            }

            int minSdk = getMinSdk(mContext);
            if (api <= minSdk) {
                return;
            }

            if (isSuppressed(api, element, minSdk, mContext, UNSUPPORTED)) {
                return;
            }

            Location location = mContext.getUastLocation(element);
            String fqcn = resolved.getQualifiedName();
            String message = String.format("Class requires API level %1$d (current min is %2$d): %3$s",
                                           api, minSdk, fqcn);
            mContext.report(UNSUPPORTED, location, message);
        }

        /**
         * Checks a Java source field reference. Returns true if the field is known
         * regardless of whether it's an invalid field or not
         */
        private boolean checkField(@NonNull UElement node, @NonNull PsiField field) {
            PsiType type = field.getType();
            Issue issue;
            if ((type instanceof PsiPrimitiveType) || LintUtils.isString(type)) {
                issue = INLINED;
            } else {
                issue = UNSUPPORTED;
            }

            String name = field.getName();
            PsiClass containingClass = field.getContainingClass();
            if (containingClass == null || name == null) {
                return false;
            }
            String owner = mContext.getEvaluator().getInternalName(containingClass);
            int api = mApiDatabase.getFieldVersion(owner, name);
            if (api != -1) {
                int minSdk = getMinSdk(mContext);
                if (api > minSdk
                    && api > getTargetApi(node)) {
                    if (isBenignConstantUsage(node, name, owner)) {
                        return true;
                    }

                    String fqcn = getFqcn(owner) + '#' + name;

                    // For import statements, place the underlines only under the
                    // reference, not the import and static keywords
                    if (node instanceof UImportStatement) {
                        UElement reference = ((UImportStatement) node).getImportReference();
                        if (reference != null) {
                            node = reference;
                        }
                    }

                    LintDriver driver = mContext.getDriver();
                    if (driver.isSuppressed(mContext, INLINED, node)) {
                        return true;
                    }

                    // backwards compatibility: lint used to use this issue type so respect
                    // older suppress annotations
                    if (driver.isSuppressed(mContext, UNSUPPORTED, node)) {
                        return true;
                    }
                    if (isWithinVersionCheckConditional(node, api, mContext)) {
                        return true;
                    }
                    if (isPrecededByVersionCheckExit(node, api, mContext)) {
                        return true;
                    }

                    String message = String.format(
                            "Field requires API level %1$d (current min is %2$d): `%3$s`",
                            api, minSdk, fqcn);

                    Location location = mContext.getUastLocation(node);
                    mContext.report(issue, node, location, message);
                }

                return true;
            }

            return false;
        }
    }

    private static boolean isSuppressed(int api, UElement element, int minSdk, JavaContext context, Issue issue) {
        if (api <= minSdk) {
            return true;
        }

        int target = getTargetApi(element);
        if (target != -1) {
            if (api <= target) {
                return true;
            }
        }

        LintDriver driver = context.getDriver();
        if(driver.isSuppressed(context, issue, element)) {
            return true;
        }

        if (isWithinVersionCheckConditional(element, api, context)) {
            return true;
        }
        if (isPrecededByVersionCheckExit(element, api, context)) {
            return true;
        }

        return false;
    }

    private static int getTargetApi(@Nullable UElement scope) {
        while (scope != null) {
            if (scope instanceof PsiModifierListOwner) {
                PsiModifierList modifierList = ((PsiModifierListOwner) scope).getModifierList();
                int targetApi = getTargetApi(modifierList);
                if (targetApi != -1) {
                    return targetApi;
                }
            }
            scope = scope.getUastParent();
            if (scope instanceof PsiFile) {
                break;
            }
        }

        return -1;
    }

    /**
     * Returns the API level for the given AST node if specified with
     * an {@code @TargetApi} annotation.
     *
     * @param modifierList the modifier list to check
     * @return the target API level, or -1 if not specified
     */
    public static int getTargetApi(@Nullable PsiModifierList modifierList) {
        if (modifierList == null) {
            return -1;
        }

        for (PsiAnnotation annotation : modifierList.getAnnotations()) {
            String fqcn = annotation.getQualifiedName();
            if (fqcn != null &&
                (fqcn.equals(FQCN_TARGET_API)
                 || fqcn.equals(REQUIRES_API_ANNOTATION)
                 || fqcn.equals(TARGET_API))) { // when missing imports
                PsiAnnotationParameterList parameterList = annotation.getParameterList();
                for (PsiNameValuePair pair : parameterList.getAttributes()) {
                    PsiAnnotationMemberValue v = pair.getValue();
                    if (v instanceof PsiLiteral) {
                        PsiLiteral literal = (PsiLiteral)v;
                        Object value = literal.getValue();
                        if (value instanceof Integer) {
                            return (Integer) value;
                        } else if (value instanceof String) {
                            return codeNameToApi((String) value);
                        }
                    } else if (v instanceof PsiArrayInitializerMemberValue) {
                        PsiArrayInitializerMemberValue mv = (PsiArrayInitializerMemberValue)v;
                        for (PsiAnnotationMemberValue mmv : mv.getInitializers()) {
                            if (mmv instanceof PsiLiteral) {
                                PsiLiteral literal = (PsiLiteral)mmv;
                                Object value = literal.getValue();
                                if (value instanceof Integer) {
                                    return (Integer) value;
                                } else if (value instanceof String) {
                                    return codeNameToApi((String) value);
                                }
                            }
                        }
                    } else if (v instanceof PsiExpression) {
                        // PsiExpression nodes are not present in light classes, so
                        // we can use Java PSI api to get the qualified name
                        if (v instanceof PsiReferenceExpression) {
                            String name = ((PsiReferenceExpression)v).getQualifiedName();
                            return codeNameToApi(name);
                        } else {
                            return codeNameToApi(v.getText());
                        }
                    }
                }
            }
        }

        return -1;
    }

    private static int codeNameToApi(@NonNull String text) {
        int dotIndex = text.lastIndexOf('.');
        if (dotIndex != -1) {
            text = text.substring(dotIndex + 1);
        }

        return SdkVersionInfo.getApiByBuildCode(text, true);
    }

    private static class VersionCheckWithExitFinder extends AbstractUastVisitor {

        private final UExpression mExpression;
        private final UElement mEndElement;
        private final int mApi;
        private final JavaContext mContext;

        private boolean mFound = false;
        private boolean mDone = false;

        public VersionCheckWithExitFinder(UExpression expression, UElement endElement,
                int api, JavaContext context) {
            mExpression = expression;

            mEndElement = endElement;
            mApi = api;
            mContext = context;
        }

        @Override
        public boolean visitElement(@NotNull UElement node) {
            if (mDone) {
                return true;
            }

            if (node.equals(mEndElement)) {
                mDone = true;
            }

            return mDone || !mExpression.equals(node);
        }

        @Override
        public boolean visitIfExpression(@NotNull UIfExpression ifStatement) {

            if (mDone) {
                return true;
            }

            UExpression thenBranch = ifStatement.getThenExpression();
            UExpression elseBranch = ifStatement.getElseExpression();

            if (thenBranch != null) {
                Boolean level = isVersionCheckConditional(mApi, thenBranch, ifStatement, mContext);
                //noinspection VariableNotUsedInsideIf
                if (level != null) {
                    // See if the body does an immediate return
                    if (isUnconditionalReturn(thenBranch)) {
                        mFound = true;
                        mDone = true;
                    }
                }
            }

            if (elseBranch != null) {
                Boolean level = isVersionCheckConditional(mApi, elseBranch, ifStatement, mContext);
                //noinspection VariableNotUsedInsideIf
                if (level != null) {
                    if (isUnconditionalReturn(elseBranch)) {
                        mFound = true;
                        mDone = true;
                    }
                }
            }

            return true;
        }

        public boolean found() {
            return mFound;
        }
    }

    private static boolean isPrecededByVersionCheckExit(
            UElement element,
            int api,
            JavaContext context
    ) {
        //noinspection unchecked
        UExpression currentExpression = UastUtils.getParentOfType(element, UExpression.class,
                                                                  true, UMethod.class, UClass.class);

        while(currentExpression != null) {
            VersionCheckWithExitFinder visitor = new VersionCheckWithExitFinder(
                    currentExpression, element, api, context);
            currentExpression.accept(visitor);

            if (visitor.found()) {
                return true;
            }

            element = currentExpression;
            //noinspection unchecked
            currentExpression = UastUtils.getParentOfType(currentExpression, UExpression.class,
                                                          true, UMethod.class, UClass.class);
        }

        return false;
    }

    private static boolean isUnconditionalReturn(UExpression expression) {
        if (expression instanceof UBlockExpression) {
            List<UExpression> expressions = ((UBlockExpression) expression).getExpressions();
            return !expressions.isEmpty() && (isUnconditionalReturn(expressions.get(expressions.size() - 1)));
        }

        return expression instanceof UReturnExpression ||
               expression instanceof UThrowExpression ||
               (expression instanceof UCallExpression &&
                ERROR.equals(((UCallExpression)expression).getMethodName()));
    }

    private static boolean isWithinVersionCheckConditional(
            UElement element,
            int api,
            JavaContext context
    ) {
        UElement current = element.getUastParent();
        UElement prev = element;
        while (current != null) {
            if (current instanceof UIfExpression) {
                UIfExpression ifStatement = (UIfExpression) current;
                Boolean isConditional = isVersionCheckConditional(api, prev, ifStatement, context);
                if (isConditional != null) {
                    return isConditional;
                }
            } else if (current instanceof UBinaryExpression) {
                if (isAndedWithConditional(current, api, prev) || isOredWithConditional(current, api, prev)) {
                    return true;
                }
            } else if (current instanceof UMethod || current instanceof UFile) {
                return false;
            }
            prev = current;
            current = current.getUastParent();
        }

        return false;
    }

    @Nullable
    private static Boolean isVersionCheckConditional(
            int api,
            UElement prev,
            UIfExpression ifStatement,
            @NonNull JavaContext context) {
        UExpression condition = ifStatement.getCondition();
        if (condition != prev && condition instanceof UBinaryExpression) {
            Boolean isConditional = isVersionCheckConditional(api, prev, ifStatement, (UBinaryExpression) condition);
            if (isConditional != null) {
                return isConditional;
            }
        } else if (condition instanceof UCallExpression) {
            UCallExpression call = (UCallExpression) condition;
            PsiMethod method = call.resolve();
            if (method != null && !method.hasModifierProperty(PsiModifier.ABSTRACT)) {
                UExpression body = context.getUastContext().getMethodBody(method);
                List<UExpression> expressions;
                if (body instanceof UBlockExpression) {
                    expressions = ((UBlockExpression) body).getExpressions();
                } else {
                    expressions = Collections.singletonList(body);
                }

                if (expressions.size() == 1) {
                    UExpression statement = expressions.get(0);
                    if (statement instanceof UReturnExpression) {
                        UReturnExpression returnStatement = (UReturnExpression) statement;
                        UExpression returnValue = returnStatement.getReturnExpression();
                        if (returnValue instanceof UBinaryExpression) {
                            Boolean isConditional = isVersionCheckConditional(api, null, null,
                                                                              (UBinaryExpression) returnValue);
                            if (isConditional != null) {
                                return isConditional;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static Boolean isVersionCheckConditional(
            int api,
            @Nullable UElement prev,
            @Nullable UIfExpression ifStatement,
            @NonNull UBinaryExpression binary) {
        UastBinaryOperator tokenType = binary.getOperator();
        if (tokenType == UastBinaryOperator.GREATER || tokenType == UastBinaryOperator.GREATER_OR_EQUALS ||
            tokenType == UastBinaryOperator.LESS_OR_EQUALS || tokenType == UastBinaryOperator.LESS ||
            tokenType == UastBinaryOperator.EQUALS || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
            UExpression left = binary.getLeftOperand();
            if (left instanceof UReferenceExpression) {
                UReferenceExpression ref = (UReferenceExpression)left;
                if (SDK_INT.equals(ref.getResolvedName())) {
                    UExpression right = binary.getRightOperand();
                    int level = -1;
                    if (right instanceof UReferenceExpression) {
                        UReferenceExpression ref2 = (UReferenceExpression)right;
                        String codeName = ref2.getResolvedName();
                        if (codeName == null) {
                            return false;
                        }
                        level = SdkVersionInfo.getApiByBuildCode(codeName, true);
                    } else if (right instanceof ULiteralExpression) {
                        ULiteralExpression lit = (ULiteralExpression)right;
                        Object value = lit.getValue();
                        if (value instanceof Integer) {
                            level = (Integer) value;
                        }
                    }
                    if (level != -1) {
                        boolean fromThen = ifStatement == null || prev == ifStatement.getThenExpression();
                        boolean fromElse = ifStatement != null && prev == ifStatement.getElseExpression();
                        assert fromThen == !fromElse;
                        if (tokenType == UastBinaryOperator.GREATER_OR_EQUALS) {
                            // if (SDK_INT >= ICE_CREAM_SANDWICH) { <call> } else { ... }
                            return level >= api && fromThen;
                        }
                        else if (tokenType == UastBinaryOperator.GREATER) {
                            // if (SDK_INT > ICE_CREAM_SANDWICH) { <call> } else { ... }
                            return level >= api - 1 && fromThen;
                        }
                        else if (tokenType == UastBinaryOperator.LESS_OR_EQUALS) {
                            // if (SDK_INT <= ICE_CREAM_SANDWICH) { ... } else { <call> }
                            return level >= api - 1 && fromElse;
                        }
                        else if (tokenType == UastBinaryOperator.LESS) {
                            // if (SDK_INT < ICE_CREAM_SANDWICH) { ... } else { <call> }
                            return level >= api && fromElse;
                        }
                        else if (tokenType == UastBinaryOperator.EQUALS
                                 || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
                            // if (SDK_INT == ICE_CREAM_SANDWICH) { <call> } else {  }
                            return level >= api && fromThen;
                        } else {
                            assert false : tokenType;
                        }
                    }
                }
            }
        } else if (tokenType == UastBinaryOperator.LOGICAL_AND
                   && (ifStatement != null && prev == ifStatement.getThenExpression())) {
            if (isAndedWithConditional(ifStatement.getCondition(), api, prev)) {
                return true;
            }
        }
        return null;
    }

    private static Location getCatchParametersLocation(JavaContext context, UCatchClause catchClause) {
        List<UTypeReferenceExpression> types = catchClause.getTypeReferences();
        if (types.isEmpty()) {
            return Location.NONE;
        }

        Location first = context.getUastLocation(types.get(0));
        if (types.size() < 2) {
            return first;
        }

        Location last = context.getUastLocation(types.get(types.size() - 1));
        File file = first.getFile();
        Position start = first.getStart();
        Position end = last.getEnd();

        if (start == null) {
            return Location.create(file);
        }

        return Location.create(file, start, end);
    }

    private static boolean isMultiCatchReflectiveOperationException(UCatchClause catchClause) {
        List<PsiType> types = catchClause.getTypes();
        if (types.size() < 2) {
            return false;
        }

        for (PsiType t : types) {
            if(!isSubclassOfReflectiveOperationException(t)) {
                return false;
            }
        }

        return true;
    }

    private static boolean isAndedWithConditional(UElement element, int api, @Nullable UElement target) {
        if (element instanceof UBinaryExpression) {
            UBinaryExpression inner = (UBinaryExpression) element;
            if (inner.getOperator() == UastBinaryOperator.LOGICAL_AND) {
                return isAndedWithConditional(inner.getLeftOperand(), api, target) ||
                       inner.getRightOperand() != target && isAndedWithConditional(inner.getRightOperand(), api, target);
            } else if (inner.getLeftOperand() instanceof UReferenceExpression &&
                        SDK_INT.equals(((UReferenceExpression)inner.getLeftOperand()).getResolvedName())) {
                UastOperator tokenType = inner.getOperator();
                UExpression right = inner.getRightOperand();
                int level = getApiLevel(right);
                if (level != -1) {
                    if (tokenType == UastBinaryOperator.GREATER_OR_EQUALS) {
                        // if (SDK_INT >= ICE_CREAM_SANDWICH && <call>
                        return level >= api;
                    }
                    else if (tokenType == UastBinaryOperator.GREATER) {
                        // if (SDK_INT > ICE_CREAM_SANDWICH) && <call>
                        return level >= api - 1;
                    }
                    else if (tokenType == UastBinaryOperator.EQUALS
                             || tokenType == UastBinaryOperator.IDENTITY_EQUALS) {
                        // if (SDK_INT == ICE_CREAM_SANDWICH) && <call>
                        return level >= api;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isOredWithConditional(UElement element, int api, @Nullable UElement target) {
        if (element instanceof UBinaryExpression) {
            UBinaryExpression inner = (UBinaryExpression) element;
            if (inner.getOperator() == UastBinaryOperator.LOGICAL_OR) {
                return isOredWithConditional(inner.getLeftOperand(), api, target) ||
                       inner.getRightOperand() != target && isOredWithConditional(inner.getRightOperand(), api, target);
            } else if (inner.getLeftOperand() instanceof UReferenceExpression &&
                       SDK_INT.equals(((UReferenceExpression)inner.getLeftOperand()).getResolvedName())) {
                UastOperator tokenType = inner.getOperator();
                UExpression right = inner.getRightOperand();
                int level = getApiLevel(right);
                if (level != -1) {
                    if (tokenType == UastBinaryOperator.LESS_OR_EQUALS) {
                        // if (SDK_INT <= ICE_CREAM_SANDWICH || <call>
                        return level >= api - 1;
                    }
                    else if (tokenType == UastBinaryOperator.LESS) {
                        // if (SDK_INT < ICE_CREAM_SANDWICH) || <call>
                        return level >= api;
                    }
                    else if (tokenType == UastBinaryOperator.NOT_EQUALS) {
                        // if (SDK_INT < ICE_CREAM_SANDWICH) || <call>
                        return level == api;
                    }
                }
            }
        }

        return false;
    }

    private static int getApiLevel(UElement apiLevelElement) {
        if (apiLevelElement instanceof UReferenceExpression) {
            UReferenceExpression ref2 = (UReferenceExpression)apiLevelElement;
            String codeName = ref2.getResolvedName();
            if (codeName == null) {
                return -1;
            }
            return SdkVersionInfo.getApiByBuildCode(codeName, true);
        } else if (apiLevelElement instanceof ULiteralExpression) {
            ULiteralExpression lit = (ULiteralExpression)apiLevelElement;
            Object value = lit.getValue();
            if (value instanceof Integer) {
                return  ((Integer)value).intValue();
            }
        }
        return -1;
    }

    private static boolean isSubclassOfReflectiveOperationException(PsiType type) {
        for (PsiType t : type.getSuperTypes()) {
            if (REFLECTIVE_OPERATION_EXCEPTION.equals(t.getCanonicalText())) {
                return true;
            }
        }
        return false;
    }
}
