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

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_FULL_BACKUP_CONTENT;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LABEL_FOR;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PADDING_START;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_TARGET_API;
import static com.android.SdkConstants.ATTR_TEXT_IS_SELECTABLE;
import static com.android.SdkConstants.ATTR_VALUE;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.CHECK_BOX;
import static com.android.SdkConstants.CLASS_CONSTRUCTOR;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.FQCN_TARGET_API;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.SUPPORT_ANNOTATIONS_PREFIX;
import static com.android.SdkConstants.SWITCH;
import static com.android.SdkConstants.TAG;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.TARGET_API;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.tools.klint.detector.api.ClassContext.getFqcn;
import static com.android.tools.klint.detector.api.LintUtils.getNextInstruction;
import static com.android.tools.klint.detector.api.Location.SearchDirection.BACKWARD;
import static com.android.tools.klint.detector.api.Location.SearchDirection.FORWARD;
import static com.android.tools.klint.detector.api.Location.SearchDirection.NEAREST;
import static com.android.utils.SdkUtils.getResourceFieldName;

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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.ClassContext;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.DefaultPosition;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.ClassScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Location.SearchHints;
import com.android.tools.klint.detector.api.Position;
import com.android.tools.klint.detector.api.ResourceXmlDetector;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.TextFormat;
import com.android.tools.klint.detector.api.XmlContext;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.PsiPrimitiveType;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiResourceListElement;
import com.intellij.psi.PsiType;

import org.jetbrains.uast.*;
import org.jetbrains.uast.expressions.UReferenceExpression;
import org.jetbrains.uast.expressions.UTypeReferenceExpression;
import org.jetbrains.uast.java.JavaUAnnotation;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.AnnotationNode;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;
import org.jetbrains.org.objectweb.asm.tree.FieldNode;
import org.jetbrains.org.objectweb.asm.tree.InsnList;
import org.jetbrains.org.objectweb.asm.tree.IntInsnNode;
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode;
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode;
import org.jetbrains.org.objectweb.asm.tree.LocalVariableNode;
import org.jetbrains.org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.TryCatchBlockNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Looks for usages of APIs that are not supported in all the versions targeted
 * by this application (according to its minimum API requirement in the manifest).
 */
public class ApiDetector extends ResourceXmlDetector
        implements ClassScanner, Detector.UastScanner {

    /**
     * Whether we flag variable, field, parameter and return type declarations of a type
     * not yet available. It appears Dalvik is very forgiving and doesn't try to preload
     * classes until actually needed, so there is no need to flag these, and in fact,
     * patterns used for supporting new and old versions sometimes declares these methods
     * and only conditionally end up actually accessing methods and fields, so only check
     * method and field accesses.
     */
    private static final boolean CHECK_DECLARATIONS = false;

    private static final String ATTR_WIDTH = "width";
    private static final String ATTR_HEIGHT = "height";
    private static final String ATTR_SUPPORTS_RTL = "supportsRtl";

    private static final boolean AOSP_BUILD = System.getenv("ANDROID_BUILD_TOP") != null; //$NON-NLS-1$

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
                    EnumSet.of(Scope.CLASS_FILE, Scope.RESOURCE_FILE, Scope.MANIFEST),
                    Scope.RESOURCE_FILE_SCOPE,
                    Scope.CLASS_FILE_SCOPE,
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

    private static final String TARGET_API_VMSIG = '/' + TARGET_API + ';';
    private static final String REQ_API_VMSIG = "/RequiresApi;";
    private static final String SWITCH_TABLE_PREFIX = "$SWITCH_TABLE$";  //$NON-NLS-1$
    private static final String ORDINAL_METHOD = "ordinal"; //$NON-NLS-1$
    public static final String ENUM_SWITCH_PREFIX = "$SwitchMap$";  //$NON-NLS-1$

    private static final String TAG_RIPPLE = "ripple";
    private static final String TAG_VECTOR = "vector";
    private static final String TAG_ANIMATED_VECTOR = "animated-vector";
    private static final String TAG_ANIMATED_SELECTOR = "animated-selector";

    private static final String SDK_INT = "SDK_INT";
    private static final String ANDROID_OS_BUILD_VERSION = "android/os/Build$VERSION";
    private static final String REFLECTIVE_OPERATION_EXCEPTION
            = "java.lang.ReflectiveOperationException";

    protected ApiLookup mApiDatabase;
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
    public static boolean isBenignUnusedAttribute(@NonNull String name) {
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

    // ---- Implements ClassScanner ----

    @SuppressWarnings("rawtypes") // ASM API
    @Override
    public void checkClass(@NonNull final ClassContext context, @NonNull ClassNode classNode) {
        if (mApiDatabase == null) {
            return;
        }

        if (AOSP_BUILD && classNode.name.startsWith("android/support/")) { //$NON-NLS-1$
            return;
        }

        // Requires util package (add prebuilts/tools/common/asm-tools/asm-debug-all-4.0.jar)
        //classNode.accept(new TraceClassVisitor(new PrintWriter(System.out)));

        int classMinSdk = getClassMinSdk(context, classNode);
        if (classMinSdk == -1) {
            classMinSdk = getMinSdk(context);
        }

        List methodList = classNode.methods;
        if (methodList.isEmpty()) {
            return;
        }

        boolean checkCalls = context.isEnabled(UNSUPPORTED)
                             || context.isEnabled(INLINED);
        boolean checkMethods = context.isEnabled(OVERRIDE)
                               && context.getMainProject().getBuildSdk() >= 1;
        String frameworkParent = null;
        if (checkMethods) {
            LintDriver driver = context.getDriver();
            String owner = classNode.superName;
            while (owner != null) {
                // For virtual dispatch, walk up the inheritance chain checking
                // each inherited method
                if ((owner.startsWith("android/")                       //$NON-NLS-1$
                     && !owner.startsWith("android/support/"))   //$NON-NLS-1$
                    || owner.startsWith("java/")                    //$NON-NLS-1$
                    || owner.startsWith("javax/")) {                //$NON-NLS-1$
                    frameworkParent = owner;
                    break;
                }
                owner = driver.getSuperClass(owner);
            }
            if (frameworkParent == null) {
                checkMethods = false;
            }
        }

        if (checkCalls) { // Check implements/extends
            if (classNode.superName != null) {
                String signature = classNode.superName;
                checkExtendsClass(context, classNode, classMinSdk, signature);
            }
            if (classNode.interfaces != null) {
                @SuppressWarnings("unchecked") // ASM API
                        List<String> interfaceList = classNode.interfaces;
                for (String signature : interfaceList) {
                    checkExtendsClass(context, classNode, classMinSdk, signature);
                }
            }
        }

        for (Object m : methodList) {
            MethodNode method = (MethodNode) m;

            int minSdk = getLocalMinSdk(method.invisibleAnnotations);
            if (minSdk == -1) {
                minSdk = classMinSdk;
            }

            InsnList nodes = method.instructions;

            if (checkMethods && Character.isJavaIdentifierStart(method.name.charAt(0))) {
                int buildSdk = context.getMainProject().getBuildSdk();
                String name = method.name;
                assert frameworkParent != null;
                int api = mApiDatabase.getCallVersion(frameworkParent, name, method.desc);
                if (api > buildSdk && buildSdk != -1) {
                    // TODO: Don't complain if it's annotated with @Override; that means
                    // somehow the build target isn't correct.
                    String fqcn;
                    String owner = classNode.name;
                    if (CONSTRUCTOR_NAME.equals(name)) {
                        fqcn = "new " + getFqcn(owner); //$NON-NLS-1$
                    } else {
                        fqcn = getFqcn(owner) + '#' + name;
                    }
                    String message = String.format(
                            "This method is not overriding anything with the current build " +
                            "target, but will in API level %1$d (current target is %2$d): `%3$s`",
                            api, buildSdk, fqcn);

                    Location location = context.getLocation(method, classNode);
                    context.report(OVERRIDE, method, null, location, message);
                }
            }

            if (!checkCalls) {
                continue;
            }

            if (CHECK_DECLARATIONS) {
                // Check types in parameter list and types of local variables
                List localVariables = method.localVariables;
                if (localVariables != null) {
                    for (Object v : localVariables) {
                        LocalVariableNode var = (LocalVariableNode) v;
                        String desc = var.desc;
                        if (desc.charAt(0) == 'L') {
                            // "Lpackage/Class;" ⇒ "package/Bar"
                            String className = desc.substring(1, desc.length() - 1);
                            int api = mApiDatabase.getClassVersion(className);
                            if (api > minSdk) {
                                String fqcn = getFqcn(className);
                                String message = String.format(
                                        "Class requires API level %1$d (current min is %2$d): `%3$s`",
                                        api, minSdk, fqcn);
                                report(context, message, var.start, method,
                                       className.substring(className.lastIndexOf('/') + 1), null,
                                       SearchHints.create(NEAREST).matchJavaSymbol());
                            }
                        }
                    }
                }

                // Check return type
                // The parameter types are already handled as local variables so we can skip
                // right to the return type.
                // Check types in parameter list
                String signature = method.desc;
                if (signature != null) {
                    int args = signature.indexOf(')');
                    if (args != -1 && signature.charAt(args + 1) == 'L') {
                        String type = signature.substring(args + 2, signature.length() - 1);
                        int api = mApiDatabase.getClassVersion(type);
                        if (api > minSdk) {
                            String fqcn = getFqcn(type);
                            String message = String.format(
                                    "Class requires API level %1$d (current min is %2$d): `%3$s`",
                                    api, minSdk, fqcn);
                            AbstractInsnNode first = nodes.size() > 0 ? nodes.get(0) : null;
                            report(context, message, first, method, method.name, null,
                                   SearchHints.create(BACKWARD).matchJavaSymbol());
                        }
                    }
                }
            }

            for (int i = 0, n = nodes.size(); i < n; i++) {
                AbstractInsnNode instruction = nodes.get(i);
                int type = instruction.getType();
                if (type == AbstractInsnNode.METHOD_INSN) {
                    MethodInsnNode node = (MethodInsnNode) instruction;
                    String name = node.name;
                    String owner = node.owner;
                    String desc = node.desc;

                    // No need to check methods in this local class; we know they
                    // won't be an API match
                    if (node.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && owner.equals(classNode.name)) {
                        owner = classNode.superName;
                    }

                    boolean checkingSuperClass = false;
                    while (owner != null) {
                        int api = mApiDatabase.getCallVersion(owner, name, desc);
                        if (api > minSdk) {
                            if (method.name.startsWith(SWITCH_TABLE_PREFIX)) {
                                // We're in a compiler-generated method to generate an
                                // array indexed by enum ordinal values to enum values. The enum
                                // itself must be requiring a higher API number than is
                                // currently used, but the call site for the switch statement
                                // will also be referencing it, so no need to report these
                                // calls.
                                break;
                            }

                            if (!checkingSuperClass
                                && node.getOpcode() == Opcodes.INVOKEVIRTUAL
                                && methodDefinedLocally(classNode, name, desc)) {
                                break;
                            }

                            String fqcn;
                            if (CONSTRUCTOR_NAME.equals(name)) {
                                fqcn = "new " + getFqcn(owner); //$NON-NLS-1$
                            } else {
                                fqcn = getFqcn(owner) + '#' + name;
                            }
                            String message = String.format(
                                    "Call requires API level %1$d (current min is %2$d): `%3$s`",
                                    api, minSdk, fqcn);

                            if (name.equals(ORDINAL_METHOD)
                                && instruction.getNext() != null
                                && instruction.getNext().getNext() != null
                                && instruction.getNext().getOpcode() == Opcodes.IALOAD
                                && instruction.getNext().getNext().getOpcode()
                                   == Opcodes.TABLESWITCH) {
                                message = String.format(
                                        "Enum for switch requires API level %1$d " +
                                        "(current min is %2$d): `%3$s`",
                                        api, minSdk, getFqcn(owner));
                            }

                            // If you're simply calling super.X from method X, even if method X
                            // is in a higher API level than the minSdk, we're generally safe;
                            // that method should only be called by the framework on the right
                            // API levels. (There is a danger of somebody calling that method
                            // locally in other contexts, but this is hopefully unlikely.)
                            if (instruction.getOpcode() == Opcodes.INVOKESPECIAL &&
                                name.equals(method.name) && desc.equals(method.desc) &&
                                // We specifically exclude constructors from this check,
                                // because we do want to flag constructors requiring the
                                // new API level; it's highly likely that the constructor
                                // is called by local code so you should specifically
                                // investigate this as a developer
                                !name.equals(CONSTRUCTOR_NAME)) {
                                break;
                            }

                            if (isWithinSdkConditional(context, classNode, method, instruction,
                                                       api)) {
                                break;
                            }

                            if (api == 19
                                && owner.equals("java/lang/ReflectiveOperationException")
                                && !method.tryCatchBlocks.isEmpty()) {
                                boolean direct = false;
                                for (Object o : method.tryCatchBlocks) {
                                    if (((TryCatchBlockNode)o).type.equals("java/lang/ReflectiveOperationException")) {
                                        direct = true;
                                        break;
                                    }
                                }
                                if (!direct) {
                                    message = String.format("Multi-catch with these reflection "
                                                            + "exceptions requires API level 19 (current min is"
                                                            + " %2$d) because they get compiled to the common but "
                                                            + "new super type `ReflectiveOperationException`. "
                                                            + "As a workaround either create individual catch "
                                                            + "statements, or catch `Exception`.",
                                                            api, minSdk);
                                }
                            }

                            if (api == 24
                                && "java.util.concurrent.ConcurrentHashMap.KeySetView#iterator".equals(fqcn)) {
                                message += ". The `keySet()` method in `ConcurrentHashMap` "
                                           + "changed in a backwards incompatible way in Java 8; "
                                           + "to work around this issue, add an explicit cast to "
                                           + "`(Map)` before the `keySet()` call.";
                            }

                            report(context, message, node, method, name, null,
                                   SearchHints.create(FORWARD).matchJavaSymbol());
                            break;
                        }

                        // For virtual dispatch, walk up the inheritance chain checking
                        // each inherited method
                        if (owner.startsWith("android/")           //$NON-NLS-1$
                            || owner.startsWith("javax/")) {   //$NON-NLS-1$
                            // The API map has already inlined all inherited methods
                            // so no need to keep checking up the chain
                            // -- unless it's the support library which is also in
                            // the android/ namespace:
                            if (owner.startsWith("android/support/") && api == -1) { //$NON-NLS-1$
                                owner = context.getDriver().getSuperClass(owner);
                            } else {
                                owner = null;
                            }
                        } else if (owner.startsWith("java/")) {    //$NON-NLS-1$
                            if (owner.equals("java/text/SimpleDateFormat")) {
                                checkSimpleDateFormat(context, method, node, minSdk);
                            }
                            // Already inlined; see comment above
                            owner = null;
                        } else if (node.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            owner = context.getDriver().getSuperClass(owner);
                        } else if (node.getOpcode() == Opcodes.INVOKESTATIC && api == -1) {
                            // Inherit through static classes as well
                            owner = context.getDriver().getSuperClass(owner);
                        } else {
                            owner = null;
                        }

                        checkingSuperClass = true;
                    }
                } else if (type == AbstractInsnNode.FIELD_INSN) {
                    FieldInsnNode node = (FieldInsnNode) instruction;
                    String name = node.name;
                    String owner = node.owner;
                    int api = mApiDatabase.getFieldVersion(owner, name);
                    if (api > minSdk) {
                        if (method.name.startsWith(SWITCH_TABLE_PREFIX)) {
                            checkSwitchBlock(context, classNode, node, method, name, owner,
                                             api, minSdk);
                            continue;
                        }

                        if (isSkippedEnumSwitch(context, classNode, method, node, owner, api)) {
                            continue;
                        }

                        if (isWithinSdkConditional(context, classNode, method, instruction, api)) {
                            continue;
                        }

                        String fqcn = getFqcn(owner) + '#' + name;
                        String message = String.format(
                                "Field requires API level %1$d (current min is %2$d): `%3$s`",
                                api, minSdk, fqcn);
                        report(context, message, node, method, name, null,
                               SearchHints.create(FORWARD).matchJavaSymbol());
                    }
                } else if (type == AbstractInsnNode.LDC_INSN) {
                    LdcInsnNode node = (LdcInsnNode) instruction;
                    if (node.cst instanceof Type) {
                        Type t = (Type) node.cst;
                        String className = t.getInternalName();

                        int api = mApiDatabase.getClassVersion(className);
                        if (api > minSdk) {
                            String fqcn = getFqcn(className);
                            String message = String.format(
                                    "Class requires API level %1$d (current min is %2$d): `%3$s`",
                                    api, minSdk, fqcn);
                            report(context, message, node, method,
                                   className.substring(className.lastIndexOf('/') + 1), null,
                                   SearchHints.create(FORWARD).matchJavaSymbol());
                        }
                    }
                }
            }
        }
    }

    private void checkExtendsClass(ClassContext context, ClassNode classNode, int classMinSdk,
            String signature) {
        int api = mApiDatabase.getClassVersion(signature);
        if (api > classMinSdk) {
            String fqcn = getFqcn(signature);
            String message = String.format(
                    "Class requires API level %1$d (current min is %2$d): `%3$s`",
                    api, classMinSdk, fqcn);

            String name = signature.substring(signature.lastIndexOf('/') + 1);
            name = name.substring(name.lastIndexOf('$') + 1);
            SearchHints hints = SearchHints.create(BACKWARD).matchJavaSymbol();
            int lineNumber = ClassContext.findLineNumber(classNode);
            Location location = context.getLocationForLine(lineNumber, name, null,
                                                           hints);
            context.report(UNSUPPORTED, location, message);
        }
    }

    private static void checkSimpleDateFormat(ClassContext context, MethodNode method,
            MethodInsnNode node, int minSdk) {
        if (minSdk >= 9) {
            // Already OK
            return;
        }
        if (node.name.equals(CONSTRUCTOR_NAME) && !node.desc.equals("()V")) { //$NON-NLS-1$
            // Check first argument
            AbstractInsnNode prev = LintUtils.getPrevInstruction(node);
            if (prev != null && !node.desc.equals("(Ljava/lang/String;)V")) { //$NON-NLS-1$
                prev = LintUtils.getPrevInstruction(prev);
            }
            if (prev != null && prev.getOpcode() == Opcodes.LDC) {
                LdcInsnNode ldc = (LdcInsnNode) prev;
                Object cst = ldc.cst;
                if (cst instanceof String) {
                    String pattern = (String) cst;
                    boolean isEscaped = false;
                    for (int i = 0; i < pattern.length(); i++) {
                        char c = pattern.charAt(i);
                        if (c == '\'') {
                            isEscaped = !isEscaped;
                        } else  if (!isEscaped && (c == 'L' || c == 'c')) {
                            String message = String.format(
                                    "The pattern character '%1$c' requires API level 9 (current " +
                                    "min is %2$d) : \"`%3$s`\"", c, minSdk, pattern);
                            report(context, message, node, method, pattern, null,
                                   SearchHints.create(FORWARD));
                            return;
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("rawtypes") // ASM API
    private static boolean methodDefinedLocally(ClassNode classNode, String name, String desc) {
        List methodList = classNode.methods;
        for (Object m : methodList) {
            MethodNode method = (MethodNode) m;
            if (name.equals(method.name) && desc.equals(method.desc)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("rawtypes") // ASM API
    private static void checkSwitchBlock(ClassContext context, ClassNode classNode,
            FieldInsnNode field, MethodNode method, String name, String owner, int api,
            int minSdk) {
        // Switch statements on enums are tricky. The compiler will generate a method
        // which returns an array of the enum constants, indexed by their ordinal() values.
        // However, we only want to complain if the code is actually referencing one of
        // the non-available enum fields.
        //
        // For the android.graphics.PorterDuff.Mode enum for example, the first few items
        // in the array are populated like this:
        //
        //   L0
        //    ALOAD 0
        //    GETSTATIC android/graphics/PorterDuff$Mode.ADD : Landroid/graphics/PorterDuff$Mode;
        //    INVOKEVIRTUAL android/graphics/PorterDuff$Mode.ordinal ()I
        //    ICONST_1
        //    IASTORE
        //   L1
        //    GOTO L3
        //   L2
        //   FRAME FULL [[I] [java/lang/NoSuchFieldError]
        //    POP
        //   L3
        //   FRAME SAME
        //    ALOAD 0
        //    GETSTATIC android/graphics/PorterDuff$Mode.CLEAR : Landroid/graphics/PorterDuff$Mode;
        //    INVOKEVIRTUAL android/graphics/PorterDuff$Mode.ordinal ()I
        //    ICONST_2
        //    IASTORE
        //    ...
        // So if we for example find that the "ADD" field isn't accessible, since it requires
        // API 11, we need to
        //   (1) First find out what its ordinal number is. We can look at the following
        //       instructions to discover this; it's the "ICONST_1" and "IASTORE" instructions.
        //       (After ICONST_5 it moves on to BIPUSH 6, BIPUSH 7, etc.)
        //   (2) Find the corresponding *usage* of this switch method. For the above enum,
        //       the switch ordinal lookup method will be called
        //         "$SWITCH_TABLE$android$graphics$PorterDuff$Mode" with desc "()[I".
        //       This means we will be looking for an invocation in some other method which looks
        //       like this:
        //         INVOKESTATIC (current class).$SWITCH_TABLE$android$graphics$PorterDuff$Mode ()[I
        //       (obviously, it can be invoked more than once)
        //       Note that it can be used more than once in this class and all sites should be
        //       checked!
        //   (3) Look up the corresponding table switch, which should look something like this:
        //        INVOKESTATIC (current class).$SWITCH_TABLE$android$graphics$PorterDuff$Mode ()[I
        //        ALOAD 0
        //        INVOKEVIRTUAL android/graphics/PorterDuff$Mode.ordinal ()I
        //        IALOAD
        //        LOOKUPSWITCH
        //          2: L1
        //          11: L2
        //          default: L3
        //       Here we need to see if the LOOKUPSWITCH instruction is referencing our target
        //       case. Above we were looking for the "ADD" case which had ordinal 1. Since this
        //       isn't explicitly referenced, we can ignore this field reference.
        AbstractInsnNode next = field.getNext();
        if (next == null || next.getOpcode() != Opcodes.INVOKEVIRTUAL) {
            return;
        }
        next = next.getNext();
        if (next == null) {
            return;
        }
        int ordinal;
        switch (next.getOpcode()) {
            case Opcodes.ICONST_0: ordinal = 0; break;
            case Opcodes.ICONST_1: ordinal = 1; break;
            case Opcodes.ICONST_2: ordinal = 2; break;
            case Opcodes.ICONST_3: ordinal = 3; break;
            case Opcodes.ICONST_4: ordinal = 4; break;
            case Opcodes.ICONST_5: ordinal = 5; break;
            case Opcodes.BIPUSH: {
                IntInsnNode iin = (IntInsnNode) next;
                ordinal = iin.operand;
                break;
            }
            default:
                return;
        }

        // Find usages of this call site
        List methodList = classNode.methods;
        for (Object m : methodList) {
            InsnList nodes = ((MethodNode) m).instructions;
            for (int i = 0, n = nodes.size(); i < n; i++) {
                AbstractInsnNode instruction = nodes.get(i);
                if (instruction.getOpcode() != Opcodes.INVOKESTATIC){
                    continue;
                }
                MethodInsnNode node = (MethodInsnNode) instruction;
                if (node.name.equals(method.name)
                    && node.desc.equals(method.desc)
                    && node.owner.equals(classNode.name)) {
                    // Find lookup switch
                    AbstractInsnNode target = getNextInstruction(node);
                    while (target != null) {
                        if (target.getOpcode() == Opcodes.LOOKUPSWITCH) {
                            LookupSwitchInsnNode lookup = (LookupSwitchInsnNode) target;
                            @SuppressWarnings("unchecked") // ASM API
                                    List<Integer> keys = lookup.keys;
                            if (keys != null && keys.contains(ordinal)) {
                                String fqcn = getFqcn(owner) + '#' + name;
                                String message = String.format(
                                        "Enum value requires API level %1$d " +
                                        "(current min is %2$d): `%3$s`",
                                        api, minSdk, fqcn);
                                report(context, message, lookup, (MethodNode) m, name, null,
                                       SearchHints.create(FORWARD).matchJavaSymbol());

                                // Break out of the inner target search only; the switch
                                // statement could be used in other places in this class as
                                // well and we want to report all problematic usages.
                                break;
                            }
                        }
                        target = getNextInstruction(target);
                    }
                }
            }
        }
    }

    private static boolean isEnumSwitchInitializer(ClassNode classNode) {
        @SuppressWarnings("rawtypes") // ASM API
                List fieldList = classNode.fields;
        for (Object f : fieldList) {
            FieldNode field = (FieldNode) f;
            if (field.name.startsWith(ENUM_SWITCH_PREFIX)) {
                return true;
            }
        }
        return false;
    }

    private static MethodNode findEnumSwitchUsage(ClassNode classNode, String owner) {
        String target = ENUM_SWITCH_PREFIX + owner.replace('/', '$');
        @SuppressWarnings("rawtypes") // ASM API
                List methodList = classNode.methods;
        for (Object f : methodList) {
            MethodNode method = (MethodNode) f;
            InsnList nodes = method.instructions;
            for (int i = 0, n = nodes.size(); i < n; i++) {
                AbstractInsnNode instruction = nodes.get(i);
                if (instruction.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    if (field.name.equals(target)) {
                        return method;
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSkippedEnumSwitch(ClassContext context, ClassNode classNode,
            MethodNode method, FieldInsnNode node, String owner, int api) {
        // Enum-style switches are handled in a different way: it generates
        // an innerclass where the class initializer creates a mapping from
        // the ordinals to the corresponding values.
        // Here we need to check to see if the call site which *used* the
        // table switch had a suppress node on it (or up that node's parent
        // chain
        AbstractInsnNode next = getNextInstruction(node);
        if (next != null && next.getOpcode() == Opcodes.INVOKEVIRTUAL
            && CLASS_CONSTRUCTOR.equals(method.name)
            && ORDINAL_METHOD.equals(((MethodInsnNode) next).name)
            && classNode.outerClass != null
            && isEnumSwitchInitializer(classNode)) {
            LintDriver driver = context.getDriver();
            ClassNode outer = driver.getOuterClassNode(classNode);
            if (outer != null) {
                MethodNode switchUser = findEnumSwitchUsage(outer, owner);
                if (switchUser != null) {
                    // Is the API check suppressed at the call site?
                    if (driver.isSuppressed(UNSUPPORTED, outer, switchUser,
                                            null)) {
                        return true;
                    }
                    // Is there a @TargetAPI annotation on the method or
                    // class referencing this switch map class?
                    if (getLocalMinSdk(switchUser.invisibleAnnotations) >= api
                        || getLocalMinSdk(outer.invisibleAnnotations) >= api) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Return the {@code @TargetApi} level to use for the given {@code classNode};
     * this will be the {@code @TargetApi} annotation on the class, or any outer
     * methods (for anonymous inner classes) or outer classes (for inner classes)
     * of the given class.
     */
    private static int getClassMinSdk(ClassContext context, ClassNode classNode) {
        int classMinSdk = getLocalMinSdk(classNode.invisibleAnnotations);
        if (classMinSdk != -1) {
            return classMinSdk;
        }

        LintDriver driver = context.getDriver();
        while (classNode != null) {
            ClassNode prev = classNode;
            classNode = driver.getOuterClassNode(classNode);
            if (classNode != null) {
                // TODO: Should this be "curr" instead?
                if (prev.outerMethod != null) {
                    @SuppressWarnings("rawtypes") // ASM API
                            List methods = classNode.methods;
                    for (Object m : methods) {
                        MethodNode method = (MethodNode) m;
                        if (method.name.equals(prev.outerMethod)
                            && method.desc.equals(prev.outerMethodDesc)) {
                            // Found the outer method for this anonymous class; check method
                            // annotations on it, then continue up the class hierarchy
                            int methodMinSdk = getLocalMinSdk(method.invisibleAnnotations);
                            if (methodMinSdk != -1) {
                                return methodMinSdk;
                            }

                            break;
                        }
                    }
                }

                classMinSdk = getLocalMinSdk(classNode.invisibleAnnotations);
                if (classMinSdk != -1) {
                    return classMinSdk;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the minimum SDK to use according to the given annotation list, or
     * -1 if no annotation was found.
     *
     * @param annotations a list of annotation nodes from ASM
     * @return the API level to use for this node, or -1
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static int getLocalMinSdk(List annotations) {
        if (annotations != null) {
            for (AnnotationNode annotation : (List<AnnotationNode>)annotations) {
                String desc = annotation.desc;
                if (desc.endsWith(TARGET_API_VMSIG)) {
                    if (annotation.values != null) {
                        for (int i = 0, n = annotation.values.size(); i < n; i += 2) {
                            String key = (String) annotation.values.get(i);
                            if (key.equals(ATTR_VALUE)) {  //$NON-NLS-1$
                                Object value = annotation.values.get(i + 1);
                                if (value instanceof Integer) {
                                    return (Integer) value;
                                }
                            }
                        }
                    }
                } else if (desc.endsWith(REQ_API_VMSIG)) {
                    if (annotation.values != null) {
                        for (int i = 0, n = annotation.values.size(); i < n; i += 2) {
                            String key = (String) annotation.values.get(i);
                            if (key.equals(ATTR_VALUE) || key.equals("api")) {
                                Object value = annotation.values.get(i + 1);
                                if (value instanceof Integer) {
                                    int api = (Integer) value;
                                    if (api > 1) {
                                        return api;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return -1;
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

    private static void report(final ClassContext context, String message, AbstractInsnNode node,
            MethodNode method, String patternStart, String patternEnd, SearchHints hints) {
        int lineNumber = node != null ? ClassContext.findLineNumber(node) : -1;

        // If looking for a constructor, the string we'll see in the source is not the
        // method name (<init>) but the class name
        if (patternStart != null && patternStart.equals(CONSTRUCTOR_NAME)
            && node instanceof MethodInsnNode) {
            if (hints != null) {
                hints = hints.matchConstructor();
            }
            patternStart = ((MethodInsnNode) node).owner;
        }

        if (patternStart != null) {
            int index = patternStart.lastIndexOf('$');
            if (index != -1) {
                patternStart = patternStart.substring(index + 1);
            }
            index = patternStart.lastIndexOf('/');
            if (index != -1) {
                patternStart = patternStart.substring(index + 1);
            }
        }

        Location location = context.getLocationForLine(lineNumber, patternStart, patternEnd,
                                                       hints);
        context.report(UNSUPPORTED, method, node, location, message);
    }

    // ---- Implements UastScanner ----


    @Nullable
    @Override
    public UastVisitor createUastVisitor(@NonNull JavaContext context) {
        if (mApiDatabase == null) {
            return new AbstractUastVisitor() {
                @Override
                public boolean visitElement(UElement element) {
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
    public static boolean isBenignConstantUsage(
            @Nullable UElement node,
            @NonNull String name,
            @NonNull String owner) {
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
        UElement curr = node.getContainingElement();
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
            curr = curr.getContainingElement();
        }

        return false;
    }

    private final class ApiVisitor extends AbstractUastVisitor {
        private final JavaContext mContext;

        private ApiVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitImportStatement(UImportStatement statement) {
            if (!statement.isOnDemand()) {
                PsiElement resolved = statement.resolve();
                if (resolved instanceof PsiField) {
                    checkField(statement, (PsiField)resolved);
                }
            }

            return super.visitImportStatement(statement);
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(USimpleNameReferenceExpression node) {
            PsiElement resolved = node.resolve();
            if (resolved instanceof PsiField) {
                checkField(node, (PsiField)resolved);
            }

            return super.visitSimpleNameReferenceExpression(node);
        }

        @Override
        public boolean visitBinaryExpressionWithType(UBinaryExpressionWithType node) {
            if (UastExpressionUtils.isTypeCast(node)) {
                visitTypeCastExpression(node);
            }

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

            if (isSuppressed(api, node, minSdk, mContext)) {
                return;
            }

            Location location = mContext.getUastLocation(node);
            String message = String.format("Cast from %1$s to %2$s requires API level %3$d (current min is %4$d)",
                                           UastLintUtils.getClassName(classType),
                                           UastLintUtils.getClassName(interfaceType), api, minSdk);
            mContext.report(UNSUPPORTED, location, message);
        }

        @Override
        public boolean visitMethod(UMethod method) {
            // API check for default methods
            if (method.getModifierList().hasExplicitModifier(PsiModifier.DEFAULT)) {
                int api = 24; // minSdk for default methods
                int minSdk = getMinSdk(mContext);

                if (!isSuppressed(api, method, minSdk, mContext)) {
                    Location location = mContext.getLocation((PsiElement) method);
                    String message = String.format("Default method requires API level %1$d "
                                                   + "(current min is %2$d)", api, minSdk);
                    mContext.reportUast(UNSUPPORTED, method, location, message);
                }
            }

            return super.visitMethod(method);
        }

        @Override
        public boolean visitClass(UClass aClass) {
            // Check for repeatable annotations
            if (aClass.isAnnotationType()) {
                PsiModifierList modifierList = aClass.getModifierList();
                if (modifierList != null) {
                    for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                        String name = annotation.getQualifiedName();
                        if ("java.lang.annotation.Repeatable".equals(name)) {
                            int api = 24; // minSdk for repeatable annotations
                            int minSdk = getMinSdk(mContext);
                            if (!isSuppressed(api, aClass, minSdk, mContext)) {
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
        public boolean visitCallExpression(UCallExpression expression) {
            PsiMethod method = expression.resolve();
            if (method != null) {
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
                            if (argumentType == null || parameterType.equals(argumentType)
                                || !(argumentType instanceof PsiClassType)) {
                                continue;
                            }
                            checkCast(argument, (PsiClassType) argumentType,
                                      (PsiClassType) parameterType);
                        }
                    }
                }

                PsiModifierList modifierList = method.getModifierList();
                List<UAnnotation> annotations = JavaUAnnotation.wrap(modifierList.getAnnotations());
                if (!checkRequiresApi(expression, method, annotations)) {
                    PsiClass containingClass = method.getContainingClass();
                    if (containingClass != null) {
                        modifierList = containingClass.getModifierList();
                        if (modifierList != null) {
                            checkRequiresApi(expression, method, annotations);
                        }
                    }
                }
            }

            return super.visitCallExpression(expression);
        }

        // Look for @RequiresApi in modifier lists
        private boolean checkRequiresApi(
                UCallExpression expression,
                PsiMethod method,
                List<UAnnotation> annotations) {
            for (UAnnotation annotation : annotations) {
                if (REQUIRES_API_ANNOTATION.equals(annotation.getQualifiedName())) {
                    int api = (int) SupportAnnotationDetector.getLongAttribute(annotation,
                                                                               ATTR_VALUE, -1);
                    if (api <= 1) {
                        // @RequiresApi has two aliasing attributes: api and value
                        api = (int) SupportAnnotationDetector.getLongAttribute(annotation,
                                                                               "api", -1);
                    }
                    int minSdk = getMinSdk(mContext);
                    if (api > minSdk) {
                        int target = getTargetApi(expression);
                        if (target == -1 || api > target) {
                            if (isWithinVersionCheckConditional(expression, api, mContext)) {
                                return true;
                            }
                            if (isPrecededByVersionCheckExit(expression, api, mContext)) {
                                return true;
                            }

                            Location location;
                            location = mContext.getUastLocation(expression);
                            String fqcn = method.getName();
                            String message = String.format(
                                    "Call requires API level %1$d (current min is %2$d): `%3$s`",
                                    api, minSdk, fqcn);
                            mContext.report(UNSUPPORTED, location, message);
                        }
                    }

                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean visitVariable(UVariable node) {
            if (node instanceof ULocalVariable) {
                visitLocalVariable((ULocalVariable) node);
            }
            return super.visitVariable(node);
        }

        private void visitLocalVariable(ULocalVariable variable) {
            UExpression initializer = variable.getUastInitializer();
            if (initializer == null) {
                return;
            }

            PsiType initializerType = initializer.getExpressionType();
            if (!(initializerType instanceof PsiClassType)) {
                return;
            }

            PsiType interfaceType = variable.getType();
            if (initializerType.equals(interfaceType)) {
                return;
            }

            if (!(interfaceType instanceof PsiClassType)) {
                return;
            }

            checkCast(initializer, (PsiClassType)initializerType, (PsiClassType)interfaceType);
        }

        @Override
        public boolean visitBinaryExpression(UBinaryExpression node) {
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
        public boolean visitTryExpression(UTryExpression statement) {
            List<PsiResourceListElement> resourceList = statement.getResources();
            //noinspection VariableNotUsedInsideIf
            if (resourceList != null && !resourceList.isEmpty()) {
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

        /**
         * Checks a Java source field reference. Returns true if the field is known
         * regardless of whether it's an invalid field or not
         */
        private boolean checkField(@NonNull UElement node, @NonNull PsiField field) {
            PsiType type = field.getType();
            // Only look for compile time constants. See JLS 15.28 and JLS 13.4.9.
            if (!(type instanceof PsiPrimitiveType) && !LintUtils.isString(type)) {
                return false;
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
                    mContext.report(INLINED, node, location, message);
                }

                return true;
            }

            return false;
        }
    }

    private static boolean isSuppressed(
            int api, UElement element, int minSdk, JavaContext context) {
        if (api <= minSdk) {
            return true;
        }
        //if (mySeenTargetApi) {
        int target = getTargetApi(element);
        if (target != -1) {
            if (api <= target) {
                return true;
            }
        }
        //}
        // TODO: This MUST BE RESTORED
        //        if (context.getDriver().isSuppressed(UNSUPPORTED, element))
        //        if (/*mySeenSuppress &&*/
        //                (IntellijLintUtils.isSuppressed(element, myFile, UNSUPPORTED) || IntellijLintUtils.isSuppressed(element, myFile, INLINED))) {
        //            return true;
        //        }

        if (isWithinVersionCheckConditional(element, api, context)) {
            return true;
        }
        if (isPrecededByVersionCheckExit(element, api, context)) {
            return true;
        }

        return false;
    }

    public static int getTargetApi(@Nullable UElement scope) {
        while (scope != null) {
            if (scope instanceof PsiModifierListOwner) {
                PsiModifierList modifierList = ((PsiModifierListOwner) scope).getModifierList();
                int targetApi = getTargetApi(modifierList);
                if (targetApi != -1) {
                    return targetApi;
                }
            }
            scope = scope.getContainingElement();
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

    public static int codeNameToApi(@NonNull String text) {
        int dotIndex = text.lastIndexOf('.');
        if (dotIndex != -1) {
            text = text.substring(dotIndex + 1);
        }

        return SdkVersionInfo.getApiByBuildCode(text, true);
    }

    public static int getRequiredVersion(@NonNull Issue issue, @NonNull String errorMessage,
            @NonNull TextFormat format) {
        errorMessage = format.toText(errorMessage);

        if (issue == UNSUPPORTED || issue == INLINED) {
            Pattern pattern = Pattern.compile("\\s(\\d+)\\s"); //$NON-NLS-1$
            Matcher matcher = pattern.matcher(errorMessage);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        }

        return -1;
    }

    private static boolean isWithinSdkConditional(
            @NonNull ClassContext context,
            @NonNull ClassNode classNode,
            @NonNull MethodNode method,
            @NonNull AbstractInsnNode call,
            int requiredApi) {
        assert requiredApi != -1;

        if (!containsSimpleSdkCheck(method)) {
            return false;
        }

        try {
            // Search in the control graph, from beginning, up to the target call
            // node, to see if it's reachable. The call graph is constructed in a
            // special way: we include all control flow edges, *except* those that
            // are satisfied by a SDK_INT version check (where the operand is a version
            // that is at least as high as the one needed for the given call).
            //
            // If we can reach the call, that means that there is a way this call
            // can be reached on some versions, and lint should flag the call/field lookup.
            //
            //
            // Let's say you have code like this:
            //   if (SDK_INT >= LOLLIPOP) {
            //       // Call
            //       return property.hasAdjacentMapping();
            //   }
            //   ...
            //
            // The compiler will turn this into the following byte code:
            //
            //    0:    getstatic #3; //Field android/os/Build$VERSION.SDK_INT:I
            //    3:    bipush 21
            //    5:    if_icmple 17
            //    8:    aload_1
            //    9:    invokeinterface	#4, 1; //InterfaceMethod
            //                       android/view/ViewDebug$ExportedProperty.hasAdjacentMapping:()Z
            //    14:   ifeq 17
            //    17:   ... code after if loop
            //
            // When the call graph is constructed, for an if branch we're called twice; once
            // where the target is the next instruction (the one taken if byte code check is false)
            // and one to the jump label (the one taken if the byte code condition is true).
            //
            // Notice how at the byte code level, the logic is reversed: the >= instruction
            // is turned into "<" and we jump to the code *after* the if clause; otherwise
            // it will just fall through. Therefore, if we take a byte code branch, that means
            // that the SDK check was *not* satisfied, and conversely, the target call is reachable
            // if we don't take the branch.
            //
            // Therefore, when we build the call graph, we will add call graph nodes for an
            // if check if :
            //   (1) it is some other comparison than <, <= or !=.
            //   (2) if the byte code comparison check is *not* satisfied, this means that the the
            //       SDK check was successful and that the call graph should only include
            //       the jump edge
            //   (3) all other edges are added
            //
            // With a flow control graph like that, we can determine whether a target call
            // is guarded by a given SDK check: that will be the case if we cannot reach
            // the target call in the call graph

            ApiCheckGraph graph = new ApiCheckGraph(requiredApi);
            ControlFlowGraph.create(graph, classNode, method);

            // Note: To debug unit tests, you may want to for example do
            //   ControlFlowGraph.Node callNode = graph.getNode(call);
            //   Set<ControlFlowGraph.Node> highlight = Sets.newHashSet(callNode);
            //   Files.write(graph.toDot(highlight), new File("/tmp/graph.gv"), Charsets.UTF_8);
            // This will generate a graphviz file you can visualize with the "dot" utility
            AbstractInsnNode first = method.instructions.get(0);
            return !graph.isConnected(first, call);
        } catch (AnalyzerException e) {
            context.log(e, null);
        }

        return false;
    }

    private static boolean containsSimpleSdkCheck(@NonNull MethodNode method) {
        // Look for a compiled version of "if (Build.VERSION.SDK_INT op N) {"
        InsnList nodes = method.instructions;
        for (int i = 0, n = nodes.size(); i < n; i++) {
            AbstractInsnNode instruction = nodes.get(i);
            if (isSdkVersionLookup(instruction)) {
                AbstractInsnNode bipush = getNextInstruction(instruction);
                if (bipush != null && bipush.getOpcode() == Opcodes.BIPUSH) {
                    AbstractInsnNode ifNode = getNextInstruction(bipush);
                    if (ifNode != null && ifNode.getType() == AbstractInsnNode.JUMP_INSN) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isSdkVersionLookup(@NonNull AbstractInsnNode instruction) {
        if (instruction.getOpcode() == Opcodes.GETSTATIC) {
            FieldInsnNode fieldNode = (FieldInsnNode) instruction;
            return (SDK_INT.equals(fieldNode.name)
                    && ANDROID_OS_BUILD_VERSION.equals(fieldNode.owner));
        }
        return false;
    }

    /**
     * Control flow graph which skips control flow edges that check
     * a given SDK_VERSION requirement that is not met by a given call
     */
    private static class ApiCheckGraph extends ControlFlowGraph {
        private final int mRequiredApi;

        public ApiCheckGraph(int requiredApi) {
            mRequiredApi = requiredApi;
        }

        @Override
        protected void add(@NonNull AbstractInsnNode from, @NonNull AbstractInsnNode to) {
            if (from.getType() == AbstractInsnNode.JUMP_INSN &&
                from.getPrevious() != null &&
                from.getPrevious().getType() == AbstractInsnNode.INT_INSN) {
                IntInsnNode intNode = (IntInsnNode) from.getPrevious();
                if (intNode.getPrevious() != null && isSdkVersionLookup(intNode.getPrevious())) {
                    JumpInsnNode jumpNode = (JumpInsnNode) from;
                    int api = intNode.operand;
                    boolean isJumpEdge = to == jumpNode.label;
                    boolean includeEdge;
                    switch (from.getOpcode()) {
                        case Opcodes.IF_ICMPNE:
                            includeEdge = api < mRequiredApi || isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPLE:
                            includeEdge = api < mRequiredApi - 1 || isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPLT:
                            includeEdge = api < mRequiredApi || isJumpEdge;
                            break;

                        case Opcodes.IF_ICMPGE:
                            includeEdge = api < mRequiredApi || !isJumpEdge;
                            break;
                        case Opcodes.IF_ICMPGT:
                            includeEdge = api < mRequiredApi - 1 || !isJumpEdge;
                            break;
                        default:
                            // unexpected comparison for int API level
                            includeEdge = true;
                    }
                    if (!includeEdge) {
                        return;
                    }
                }
            }

            super.add(from, to);
        }
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
        public boolean visitElement(UElement node) {
            if (mDone) {
                return true;
            }

            if (node.equals(mEndElement)) {
                mDone = true;
            }

            return mDone || !mExpression.equals(node);
        }

        @Override
        public boolean visitIfExpression(UIfExpression ifStatement) {

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

    protected static boolean isPrecededByVersionCheckExit(UElement element, int api,
            JavaContext context) {
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

    private static boolean isUnconditionalReturn(UExpression statement) {
        if (statement instanceof UBlockExpression) {
            List<UExpression> expressions = ((UBlockExpression) statement).getExpressions();
            if (expressions.size() == 1 && expressions.get(0) instanceof UReturnExpression) {
                return true;
            }
        }
        return statement instanceof UReturnExpression;
    }

    public static boolean isWithinVersionCheckConditional(
            UElement element, int api, JavaContext context) {
        UElement current = element.getContainingElement();
        UElement prev = element;
        while (current != null) {
            if (current instanceof UIfExpression) {
                UIfExpression ifStatement = (UIfExpression) current;
                Boolean isConditional = isVersionCheckConditional(api, prev, ifStatement, context);
                if (isConditional != null) {
                    return isConditional;
                }
            } else if (current instanceof UMethod || current instanceof UFile) {
                return false;
            }
            prev = current;
            current = current.getContainingElement();
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
    private static Boolean isVersionCheckConditional(int api,
            @Nullable UElement prev,
            @Nullable UIfExpression ifStatement,
            @NonNull UBinaryExpression binary) {
        UastBinaryOperator tokenType = binary.getOperator();
        if (tokenType == UastBinaryOperator.GREATER || tokenType == UastBinaryOperator.GREATER_OR_EQUAL ||
            tokenType == UastBinaryOperator.LESS_OR_EQUAL || tokenType == UastBinaryOperator.LESS ||
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
                        if (tokenType == UastBinaryOperator.GREATER_OR_EQUAL) {
                            // if (SDK_INT >= ICE_CREAM_SANDWICH) { <call> } else { ... }
                            return level >= api && fromThen;
                        }
                        else if (tokenType == UastBinaryOperator.GREATER) {
                            // if (SDK_INT > ICE_CREAM_SANDWICH) { <call> } else { ... }
                            return level >= api - 1 && fromThen;
                        }
                        else if (tokenType == UastBinaryOperator.LESS_OR_EQUAL) {
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


    public static Location getCatchParametersLocation(JavaContext context, UCatchClause catchClause) {
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

    public static boolean isMultiCatchReflectiveOperationException(UCatchClause catchClause) {
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

    private static boolean isAndedWithConditional(UElement element, int api, @Nullable UElement before) {
        if (element instanceof UBinaryExpression) {
            UBinaryExpression inner = (UBinaryExpression) element;
            if (inner.getOperator() == UastBinaryOperator.LOGICAL_AND) {
                return isAndedWithConditional(inner.getLeftOperand(), api, before) ||
                       inner.getRightOperand() != before && isAndedWithConditional(inner.getRightOperand(), api, before);
            } else  if (inner.getLeftOperand() instanceof UReferenceExpression &&
                        SDK_INT.equals(((UReferenceExpression)inner.getLeftOperand()).getResolvedName())) {
                int level = -1;
                UastOperator tokenType = inner.getOperator();
                UExpression right = inner.getRightOperand();
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
                        level = ((Integer)value).intValue();
                    }
                }
                if (level != -1) {
                    if (tokenType == UastBinaryOperator.GREATER_OR_EQUAL) {
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

    private static boolean isSubclassOfReflectiveOperationException(PsiType type) {
        for (PsiType t : type.getSuperTypes()) {
            if (REFLECTIVE_OPERATION_EXCEPTION.equals(t.getCanonicalText())) {
                return true;
            }
        }
        return false;
    }
}
