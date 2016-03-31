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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.ANDROID_PREFIX;
import static com.android.SdkConstants.ANDROID_THEME_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_CLASS;
import static com.android.SdkConstants.ATTR_ID;
import static com.android.SdkConstants.ATTR_LABEL_FOR;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PADDING_START;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_TARGET_API;
import static com.android.SdkConstants.ATTR_TEXT_IS_SELECTABLE;
import static com.android.SdkConstants.BUTTON;
import static com.android.SdkConstants.CHECK_BOX;
import static com.android.SdkConstants.CLASS_CONSTRUCTOR;
import static com.android.SdkConstants.CONSTRUCTOR_NAME;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.R_CLASS;
import static com.android.SdkConstants.SWITCH;
import static com.android.SdkConstants.TAG;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.TARGET_API;
import static com.android.SdkConstants.TOOLS_URI;
import static com.android.SdkConstants.VIEW_TAG;
import static com.android.tools.lint.detector.api.ClassContext.getFqcn;
import static com.android.tools.lint.detector.api.ClassContext.getInternalName;
import static com.android.tools.lint.detector.api.LintUtils.getNextInstruction;
import static com.android.tools.lint.detector.api.Location.SearchDirection.BACKWARD;
import static com.android.tools.lint.detector.api.Location.SearchDirection.FORWARD;
import static com.android.tools.lint.detector.api.Location.SearchDirection.NEAREST;
import static com.android.utils.SdkUtils.getResourceFieldName;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.resources.ResourceFolderType;
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.SdkVersionInfo;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.DefaultPosition;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Location.SearchHints;
import com.android.tools.lint.detector.api.Position;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.TextFormat;
import com.android.tools.lint.detector.api.XmlContext;
import com.android.utils.Pair;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.ast.Annotation;
import lombok.ast.AnnotationElement;
import lombok.ast.AnnotationValue;
import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.Case;
import lombok.ast.Catch;
import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.If;
import lombok.ast.ImportDeclaration;
import lombok.ast.InlineIfExpression;
import lombok.ast.IntegralLiteral;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Modifiers;
import lombok.ast.Select;
import lombok.ast.StrictListAccessor;
import lombok.ast.StringLiteral;
import lombok.ast.SuperConstructorInvocation;
import lombok.ast.Switch;
import lombok.ast.Try;
import lombok.ast.TypeReference;
import lombok.ast.VariableDefinition;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Looks for usages of APIs that are not supported in all the versions targeted
 * by this application (according to its minimum API requirement in the manifest).
 */
public class ApiDetector extends ResourceXmlDetector
        implements Detector.ClassScanner, Detector.JavaScanner {

    /**
     * Whether we flag variable, field, parameter and return type declarations of a type
     * not yet available. It appears Dalvik is very forgiving and doesn't try to preload
     * classes until actually needed, so there is no need to flag these, and in fact,
     * patterns used for supporting new and old versions sometimes declares these methods
     * and only conditionally end up actually accessing methods and fields, so only check
     * method and field accesses.
     */
    private static final boolean CHECK_DECLARATIONS = false;

    private static final boolean AOSP_BUILD = System.getenv("ANDROID_BUILD_TOP") != null; //$NON-NLS-1$

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

    /** Accessing an unsupported API */
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

    /** Accessing an inlined API on older platforms */
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
    private static final String SWITCH_TABLE_PREFIX = "$SWITCH_TABLE$";  //$NON-NLS-1$
    private static final String ORDINAL_METHOD = "ordinal"; //$NON-NLS-1$
    public static final String ENUM_SWITCH_PREFIX = "$SwitchMap$";  //$NON-NLS-1$

    private static final String TAG_RIPPLE = "ripple";
    private static final String TAG_VECTOR = "vector";
    private static final String TAG_ANIMATED_VECTOR = "animated-vector";
    private static final String TAG_ANIMATED_SELECTOR = "animated-selector";

    private static final String SDK_INT = "SDK_INT";
    private static final String ANDROID_OS_BUILD_VERSION = "android/os/Build$VERSION";

    protected ApiLookup mApiDatabase;
    private boolean mWarnedMissingDb;
    private int mMinApi = -1;
    private Map<String, List<Pair<String, Location>>> mPendingFields;

    /** Constructs a new API check */
    public ApiDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.SLOW;
    }

    @Override
    public void beforeCheckProject(@NonNull Context context) {
        mApiDatabase = ApiLookup.get(context.getClient());
        // We can't look up the minimum API required by the project here:
        // The manifest file hasn't been processed yet in the -before- project hook.
        // For now it's initialized lazily in getMinSdk(Context), but the
        // lint infrastructure should be fixed to parse manifest file up front.

        if (mApiDatabase == null && !mWarnedMissingDb) {
            mWarnedMissingDb = true;
            context.report(IssueRegistry.LINT_ERROR, Location.create(context.file),
                        "Can't find API database; API check not performed");
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
                    if (RtlDetector.isRtlAttributeName(name)) {
                        // No need to warn for example that
                        //  "layout_alignParentEnd will only be used in API level 17 and higher"
                        // since we have a dedicated RTL lint rule dealing with those attributes

                        // However, paddingStart in particular is known to cause crashes
                        // when used on TextViews (and subclasses of TextViews), on some
                        // devices, because vendor specific attributes conflict with the
                        // later-added framework resources, and these are apparently read
                        // by the text views:
                        if (name.equals(ATTR_PADDING_START) &&
                                viewMayExtendTextView(attribute.getOwnerElement())) {
                            Location location = context.getLocation(attribute);
                            String message = String.format(
                                    "Attribute `%1$s` referenced here can result in a crash on "
                                            + "some specific devices older than API %2$d "
                                            + "(current min is %3$d)",
                                    attribute.getLocalName(), attributeApiLevel, minSdk);
                            context.report(UNSUPPORTED, attribute, location, message);
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
        if (tag.equals(SdkConstants.VIEW_TAG)) {
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
        return ATTR_LABEL_FOR.equals(name) || ATTR_TEXT_IS_SELECTABLE.equals(name);

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
                checkElement(context, element, TAG_RIPPLE, 21, UNSUPPORTED);
                checkElement(context, element, TAG_VECTOR, 21, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_SELECTOR, 21, UNSUPPORTED);
                checkElement(context, element, TAG_ANIMATED_VECTOR, 21, UNSUPPORTED);
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
                            String owner = "android/R$"    //$NON-NLS-1$
                                    + text.substring(ANDROID_PREFIX.length(), index);
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
        } else {
            if (VIEW_TAG.equals(tag)) {
                tag = element.getAttribute(ATTR_CLASS);
                if (tag == null || tag.isEmpty()) {
                    return;
                }
            } else {
                // TODO: Complain if <tag> is used at the root level!
                checkElement(context, element, TAG, 21, UNUSED);
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
    private void checkElement(@NonNull XmlContext context, @NonNull Element element,
            @NonNull String tag, int api, @NonNull Issue issue) {
        if (tag.equals(element.getTagName())) {
            int minSdk = getMinSdk(context);
            if (api > minSdk && api > context.getFolderVersion()
                    && api > getLocalMinSdk(element)) {
                Location location = context.getLocation(element);
                String message;
                if (issue == UNSUPPORTED) {
                    message = String.format(
                            "`<%1$s>` requires API level %2$d (current min is %3$d)", tag, api,
                            minSdk);
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
                        fqcn = "new " + ClassContext.getFqcn(owner); //$NON-NLS-1$
                    } else {
                        fqcn = ClassContext.getFqcn(owner) + '#' + name;
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
                            // "Lpackage/Class;" => "package/Bar"
                            String className = desc.substring(1, desc.length() - 1);
                            int api = mApiDatabase.getClassVersion(className);
                            if (api > minSdk) {
                                String fqcn = ClassContext.getFqcn(className);
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
                            String fqcn = ClassContext.getFqcn(type);
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
                                fqcn = "new " + ClassContext.getFqcn(owner); //$NON-NLS-1$
                            } else {
                                fqcn = ClassContext.getFqcn(owner) + '#' + name;
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
                                    api, minSdk, ClassContext.getFqcn(owner));
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
                            if (owner.startsWith("android/support/")) { //$NON-NLS-1$
                                owner = context.getDriver().getSuperClass(owner);
                            } else {
                                owner = null;
                            }
                        } else if (owner.startsWith("java/")) {    //$NON-NLS-1$
                            if (owner.equals(LocaleDetector.DATE_FORMAT_OWNER)) {
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

                        String fqcn = ClassContext.getFqcn(owner) + '#' + name;
                        if (mPendingFields != null) {
                            mPendingFields.remove(fqcn);
                        }
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
                            String fqcn = ClassContext.getFqcn(className);
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
            String fqcn = ClassContext.getFqcn(signature);
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
                                String fqcn = ClassContext.getFqcn(owner) + '#' + name;
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
                            if (key.equals("value")) {  //$NON-NLS-1$
                                Object value = annotation.values.get(i + 1);
                                if (value instanceof Integer) {
                                    return (Integer) value;
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
        while (element != null) {
            String targetApi = element.getAttributeNS(TOOLS_URI, ATTR_TARGET_API);
            if (targetApi != null && !targetApi.isEmpty()) {
                if (Character.isDigit(targetApi.charAt(0))) {
                    try {
                        return Integer.parseInt(targetApi);
                    } catch (NumberFormatException nufe) {
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

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mPendingFields != null) {
            for (List<Pair<String, Location>> list : mPendingFields.values()) {
                for (Pair<String, Location> pair : list) {
                    String message = pair.getFirst();
                    Location location = pair.getSecond();
                    context.report(INLINED, location, message);
                }
            }
        }

        super.afterCheckProject(context);
    }

// ---- Implements JavaScanner ----

    @Nullable
    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        if (mApiDatabase == null) {
            return new ForwardingAstVisitor() {
            };
        }
        return new ApiVisitor(context);
    }

    @Nullable
    @Override
    public List<Class<? extends lombok.ast.Node>> getApplicableNodeTypes() {
        List<Class<? extends lombok.ast.Node>> types =
                new ArrayList<Class<? extends lombok.ast.Node>>(2);
        types.add(ImportDeclaration.class);
        types.add(Select.class);
        types.add(MethodDeclaration.class);
        types.add(ConstructorDeclaration.class);
        types.add(VariableDefinitionEntry.class);
        types.add(VariableReference.class);
        types.add(Try.class);
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
            @Nullable lombok.ast.Node node,
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
        lombok.ast.Node curr = node.getParent();
        while (curr != null) {
            Class<? extends lombok.ast.Node> nodeType = curr.getClass();
            if (nodeType == Case.class) {
                Case caseStatement = (Case) curr;
                Expression condition = caseStatement.astCondition();
                return condition != null && isAncestor(condition, node);
            } else if (nodeType == If.class) {
                If ifStatement = (If) curr;
                Expression condition = ifStatement.astCondition();
                return condition != null && isAncestor(condition, node);
            } else if (nodeType == InlineIfExpression.class) {
                InlineIfExpression ifStatement = (InlineIfExpression) curr;
                Expression condition = ifStatement.astCondition();
                return condition != null && isAncestor(condition, node);
            }
            curr = curr.getParent();
        }

        return false;
    }

    private static boolean isAncestor(
            @NonNull lombok.ast.Node ancestor,
            @Nullable lombok.ast.Node node) {
        while (node != null) {
            if (node == ancestor) {
                return true;
            }
            node = node.getParent();
        }

        return false;
    }

    private final class ApiVisitor extends ForwardingAstVisitor {
        private JavaContext mContext;
        private Map<String, String> mClassToImport = Maps.newHashMap();
        private List<String> mStarImports;
        private Set<String> mLocalVars;
        private lombok.ast.Node mCurrentMethod;
        private Set<String> mFields;
        private List<String> mStaticStarImports;

        private ApiVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitImportDeclaration(ImportDeclaration node) {
            if (node.astStarImport()) {
                // Similarly, if you're inheriting from a constants class, figure out
                // how that works... :=(
                String fqcn = node.asFullyQualifiedName();
                int strip = fqcn.lastIndexOf('*');
                if (strip != -1) {
                    strip = fqcn.lastIndexOf('.', strip);
                    if (strip != -1) {
                        String pkgName = getInternalName(fqcn.substring(0, strip));
                        if (ApiLookup.isRelevantOwner(pkgName)) {
                            if (node.astStaticImport()) {
                                if (mStaticStarImports == null) {
                                    mStaticStarImports = Lists.newArrayList();
                                }
                                mStaticStarImports.add(pkgName);
                            } else {
                                if (mStarImports == null) {
                                    mStarImports = Lists.newArrayList();
                                }
                                mStarImports.add(pkgName);
                            }
                        }
                    }
                }
            } else if (node.astStaticImport()) {
                String fqcn = node.asFullyQualifiedName();
                String fieldName = getInternalName(fqcn);
                int index = fieldName.lastIndexOf('$');
                if (index != -1) {
                    String owner = fieldName.substring(0, index);
                    String name = fieldName.substring(index + 1);
                    checkField(node, name, owner);
                }
            } else {
                // Store in map -- if it's "one of ours"
                // Use override detector's map for that purpose
                String fqcn = node.asFullyQualifiedName();

                int last = fqcn.lastIndexOf('.');
                if (last != -1) {
                    String className = fqcn.substring(last + 1);
                    mClassToImport.put(className, fqcn);
                }
            }

            return super.visitImportDeclaration(node);
        }

        @Override
        public boolean visitSelect(Select node) {
            boolean result = super.visitSelect(node);

            if (node.getParent() instanceof Select) {
                // We only want to look at the leaf expressions; e.g. if you have
                // "foo.bar.baz" we only care about the select foo.bar.baz, not foo.bar
                return result;
            }

            // See if this corresponds to a field reference. We assume it's a field if
            // it's a select (x.y) and either the identifier y is capitalized (e.g.
            // foo.VIEW_MASK) or if it's a member of an R class (R.id.foo).
            String name = node.astIdentifier().astValue();
            boolean isField = Character.isUpperCase(name.charAt(0));
            if (!isField) {
                // See if there's an R class
                Select current = node;
                while (current != null) {
                    Expression operand = current.astOperand();
                    if (operand instanceof Select) {
                        current = (Select) operand;
                        if (R_CLASS.equals(current.astIdentifier().astValue())) {
                            isField = true;
                            break;
                        }
                    } else if (operand instanceof VariableReference) {
                        VariableReference reference = (VariableReference) operand;
                        if (R_CLASS.equals(reference.astIdentifier().astValue())) {
                            isField = true;
                        }
                        break;
                    } else {
                        break;
                    }
                }
            }

            if (isField) {
                Expression operand = node.astOperand();
                if (operand.getClass() == Select.class) {
                    // Possibly a fully qualified name in place
                    String cls = operand.toString();

                    // See if it's an imported class with an inner class
                    // (e.g. Manifest.permission.FIELD)
                    if (Character.isUpperCase(cls.charAt(0))) {
                        int firstDot = cls.indexOf('.');
                        if (firstDot != -1) {
                            String base = cls.substring(0, firstDot);
                            String fqcn = mClassToImport.get(base);
                            if (fqcn != null) {
                                // Yes imported
                                String owner = getInternalName(fqcn + cls.substring(firstDot));
                                checkField(node, name, owner);
                                return result;
                            }

                            // Might be a star import: have to iterate and check here
                            if (mStarImports != null) {
                                for (String packagePrefix : mStarImports) {
                                    String owner = getInternalName(packagePrefix + '/' + cls);
                                    if (checkField(node, name, owner)) {
                                        mClassToImport.put(name, owner);
                                        return result;
                                    }
                                }
                            }
                        }
                    }

                    // See if it's a fully qualified reference in place
                    String owner = getInternalName(cls);
                    checkField(node, name, owner);
                    return result;
                } else if (operand.getClass() == VariableReference.class) {
                    String className = ((VariableReference) operand).astIdentifier().astValue();
                    // Not a FQCN that we care about: look in imports
                    String fqcn = mClassToImport.get(className);
                    if (fqcn != null) {
                        // Yes imported
                        String owner = getInternalName(fqcn);
                        checkField(node, name, owner);
                        return result;
                    }

                    if (Character.isUpperCase(className.charAt(0))) {
                        // Might be a star import: have to iterate and check here
                        if (mStarImports != null) {
                            for (String packagePrefix : mStarImports) {
                                String owner = getInternalName(packagePrefix) + '/' + className;
                                if (checkField(node, name, owner)) {
                                    mClassToImport.put(name, owner);
                                    return result;
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }

        @Override
        public boolean visitVariableReference(VariableReference node) {
            boolean result = super.visitVariableReference(node);

            if (node.getParent() != null) {
                lombok.ast.Node parent = node.getParent();
                Class<? extends lombok.ast.Node> parentClass = parent.getClass();
                if (parentClass == Select.class
                        || parentClass == Switch.class // look up on the switch expression type
                        || parentClass == Case.class
                        || parentClass == ConstructorInvocation.class
                        || parentClass == SuperConstructorInvocation.class
                        || parentClass == AnnotationElement.class) {
                    return result;
                }

                if (parent instanceof MethodInvocation &&
                        ((MethodInvocation) parent).astOperand() == node) {
                    return result;
                } else if (parent instanceof BinaryExpression) {
                    BinaryExpression expression = (BinaryExpression) parent;
                    if (expression.astLeft() == node) {
                        return result;
                    }
                }
            }

            String name = node.astIdentifier().astValue();
            if (Character.isUpperCase(name.charAt(0))
                    && (mLocalVars == null || !mLocalVars.contains(name))
                    && (mFields == null || !mFields.contains(name))) {
                // Potential field reference: check it
                if (mStaticStarImports != null) {
                    for (String owner : mStaticStarImports) {
                        if (checkField(node, name, owner)) {
                            break;
                        }
                    }
                }
            }

            return result;
        }

        @Override
        public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
            if (mCurrentMethod != null) {
                if (mLocalVars == null) {
                    mLocalVars = Sets.newHashSet();
                }
                mLocalVars.add(node.astName().astValue());
            } else {
                if (mFields == null) {
                    mFields = Sets.newHashSet();
                }
                mFields.add(node.astName().astValue());
            }
            return super.visitVariableDefinitionEntry(node);
        }

        @Override
        public boolean visitMethodDeclaration(MethodDeclaration node) {
            mLocalVars = null;
            mCurrentMethod = node;
            return super.visitMethodDeclaration(node);
        }

        @Override
        public boolean visitConstructorDeclaration(ConstructorDeclaration node) {
            mLocalVars = null;
            mCurrentMethod = node;
            return super.visitConstructorDeclaration(node);
        }

        @Override
        public boolean visitTry(Try node) {
            Object nativeNode = node.getNativeNode();
            if (nativeNode != null && nativeNode.getClass().getName().equals(
                    "org.eclipse.jdt.internal.compiler.ast.TryStatement")) {
                boolean isTryWithResources = false;
                try {
                    Field field = nativeNode.getClass().getDeclaredField("resources");
                    Object value = field.get(nativeNode);
                    if (value instanceof Object[]) {
                        Object[] resources = (Object[]) value;
                        isTryWithResources = resources.length > 0;
                    }
                } catch (NoSuchFieldException e) {
                    // Unexpected: ECJ parser internals have changed; can't detect try block
                } catch (IllegalAccessException e) {
                    // Unexpected: ECJ parser internals have changed; can't detect try block
                }
                if (isTryWithResources) {
                    int minSdk = getMinSdk(mContext);
                    int api = 19;  // minSdk for try with resources
                    if (api > minSdk && api > getLocalMinSdk(node)) {
                        Location location = mContext.getLocation(node);
                        String message = String.format("Try-with-resources requires "
                                + "API level %1$d (current min is %2$d)", api, minSdk);
                        LintDriver driver = mContext.getDriver();
                        if (!driver.isSuppressed(mContext, UNSUPPORTED, node)) {
                            mContext.report(UNSUPPORTED, location, message);
                        }
                    }
                } else {
                    // Special case: check types of catch block variables; these apparently
                    // need to be available at runtime even if there are no explicit calls
                    for (Catch c : node.astCatches()) {
                        VariableDefinition variableDefinition = c.astExceptionDeclaration();
                        TypeReference typeReference = variableDefinition.astTypeReference();
                        String fqcn = null;
                        JavaParser.ResolvedNode resolved = mContext.resolve(typeReference);
                        if (resolved != null) {
                            fqcn = resolved.getSignature();
                        } else if (typeReference.getTypeName().equals(
                                "ReflectiveOperationException")) {
                            fqcn = "java.lang.ReflectiveOperationException";
                        }
                        if (fqcn != null) {
                            String owner = getInternalName(fqcn);
                            int api = mApiDatabase.getClassVersion(owner);
                            int minSdk = getMinSdk(mContext);
                            if (api > minSdk && api > getLocalMinSdk(typeReference)) {
                                Location location = mContext.getLocation(typeReference);
                                String message = String.format(
                                    "Class requires API level %1$d (current min is %2$d): `%3$s`",
                                    api, minSdk, fqcn);
                                LintDriver driver = mContext.getDriver();
                                if (!driver.isSuppressed(mContext, UNSUPPORTED, typeReference)) {
                                    mContext.report(UNSUPPORTED, location, message);
                                }
                            }
                        }
                    }
                }
            }

            return super.visitTry(node);
        }

        @Override
        public void endVisit(lombok.ast.Node node) {
            if (node == mCurrentMethod) {
                mCurrentMethod = null;
            }
            super.endVisit(node);
        }

        /**
         * Checks a Java source field reference. Returns true if the field is known
         * regardless of whether it's an invalid field or not
         */
        private boolean checkField(
                @NonNull lombok.ast.Node node,
                @NonNull String name,
                @NonNull String owner) {
            int api = mApiDatabase.getFieldVersion(owner, name);
            if (api != -1) {
                int minSdk = getMinSdk(mContext);
                if (api > minSdk
                        && api > getLocalMinSdk(node)) {
                    if (isBenignConstantUsage(node, name, owner)) {
                        return true;
                    }

                    Location location = mContext.getLocation(node);
                    String fqcn = getFqcn(owner) + '#' + name;

                    if (node instanceof ImportDeclaration) {
                        // Replace import statement location range with just
                        // the identifier part
                        ImportDeclaration d = (ImportDeclaration) node;
                        int startOffset = d.astParts().first().getPosition().getStart();
                        Position start = location.getStart();
                        int startColumn = start.getColumn();
                        int startLine = start.getLine();
                        start = new DefaultPosition(startLine,
                                startColumn + startOffset - start.getOffset(), startOffset);
                        int fqcnLength = fqcn.length();
                        Position end = new DefaultPosition(startLine,
                                start.getColumn() + fqcnLength,
                                start.getOffset() + fqcnLength);
                        location = Location.create(location.getFile(), start, end);
                    }

                    String message = String.format(
                            "Field requires API level %1$d (current min is %2$d): `%3$s`",
                            api, minSdk, fqcn);

                    LintDriver driver = mContext.getDriver();
                    if (driver.isSuppressed(mContext, INLINED, node)) {
                        return true;
                    }

                    // Also allow to suppress these issues with NewApi, since some
                    // fields used to get identified that way
                    if (driver.isSuppressed(mContext, UNSUPPORTED, node)) {
                        return true;
                    }

                    // We can't report the issue right away; we don't yet know if
                    // this is an actual inlined (static primitive or String) yet.
                    // So just make a note of it, and report these after the project
                    // checking has finished; any fields that aren't inlined will be
                    // cleared when they're noticed by the class check.
                    if (mPendingFields == null) {
                        mPendingFields = Maps.newHashMapWithExpectedSize(20);
                    }
                    List<Pair<String, Location>> list = mPendingFields.get(fqcn);
                    if (list == null) {
                        list = new ArrayList<Pair<String, Location>>();
                        mPendingFields.put(fqcn, list);
                    } else {
                        // See if this location already exists. This can happen if
                        // we have multiple references to an inlined field on the same
                        // line. Since the class file only gives us line information, we
                        // can't distinguish between these in the client as separate usages,
                        // so they end up being identical errors.
                        for (Pair<String, Location> pair : list) {
                            Location existingLocation = pair.getSecond();
                            if (location.getFile().equals(existingLocation.getFile())) {
                                Position start = location.getStart();
                                Position existingStart = existingLocation.getStart();
                                if (start != null && existingStart != null
                                        && start.getLine() == existingStart.getLine()) {
                                    return true;
                                }
                            }
                        }
                    }
                    list.add(Pair.of(message, location));
                }

                return true;
            }

            return false;
        }

        /**
         * Returns the minimum SDK to use according to the given AST node, or null
         * if no {@code TargetApi} annotations were found
         *
         * @return the API level to use for this node, or -1
         */
        public int getLocalMinSdk(@Nullable lombok.ast.Node scope) {
            while (scope != null) {
                Class<? extends lombok.ast.Node> type = scope.getClass();
                // The Lombok AST uses a flat hierarchy of node type implementation classes
                // so no need to do instanceof stuff here.
                if (type == VariableDefinition.class) {
                    // Variable
                    VariableDefinition declaration = (VariableDefinition) scope;
                    int targetApi = getTargetApi(declaration.astModifiers());
                    if (targetApi != -1) {
                        return targetApi;
                    }
                } else if (type == MethodDeclaration.class) {
                    // Method
                    // Look for annotations on the method
                    MethodDeclaration declaration = (MethodDeclaration) scope;
                    int targetApi = getTargetApi(declaration.astModifiers());
                    if (targetApi != -1) {
                        return targetApi;
                    }
                } else if (type == ConstructorDeclaration.class) {
                    // Constructor
                    // Look for annotations on the method
                    ConstructorDeclaration declaration = (ConstructorDeclaration) scope;
                    int targetApi = getTargetApi(declaration.astModifiers());
                    if (targetApi != -1) {
                        return targetApi;
                    }
                } else if (type == ClassDeclaration.class) {
                    // Class
                    ClassDeclaration declaration = (ClassDeclaration) scope;
                    int targetApi = getTargetApi(declaration.astModifiers());
                    if (targetApi != -1) {
                        return targetApi;
                    }
                }

                scope = scope.getParent();
            }

            return -1;
        }
    }

    /**
     * Returns the API level for the given AST node if specified with
     * an {@code @TargetApi} annotation.
     *
     * @param modifiers the modifier to check
     * @return the target API level, or -1 if not specified
     */
    public static int getTargetApi(@Nullable Modifiers modifiers) {
        if (modifiers == null) {
            return -1;
        }
        StrictListAccessor<Annotation, Modifiers> annotations = modifiers.astAnnotations();
        if (annotations == null) {
            return -1;
        }

        for (Annotation annotation : annotations) {
            TypeReference t = annotation.astAnnotationTypeReference();
            String typeName = t.getTypeName();
            if (typeName.endsWith(TARGET_API)) {
                StrictListAccessor<AnnotationElement, Annotation> values =
                        annotation.astElements();
                if (values != null) {
                    for (AnnotationElement element : values) {
                        AnnotationValue valueNode = element.astValue();
                        if (valueNode == null) {
                            continue;
                        }
                        if (valueNode instanceof IntegralLiteral) {
                            IntegralLiteral literal = (IntegralLiteral) valueNode;
                            return literal.astIntValue();
                        } else if (valueNode instanceof StringLiteral) {
                            String value = ((StringLiteral) valueNode).astValue();
                            return SdkVersionInfo.getApiByBuildCode(value, true);
                        } else if (valueNode instanceof Select) {
                            Select select = (Select) valueNode;
                            String codename = select.astIdentifier().astValue();
                            return SdkVersionInfo.getApiByBuildCode(codename, true);
                        } else if (valueNode instanceof VariableReference) {
                            VariableReference reference = (VariableReference) valueNode;
                            String codename = reference.astIdentifier().astValue();
                            return SdkVersionInfo.getApiByBuildCode(codename, true);
                        }
                    }
                }
            }
        }

        return -1;
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
}
