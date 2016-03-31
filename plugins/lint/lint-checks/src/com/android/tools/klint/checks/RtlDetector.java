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

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_DRAWABLE_END;
import static com.android.SdkConstants.ATTR_DRAWABLE_LEFT;
import static com.android.SdkConstants.ATTR_DRAWABLE_RIGHT;
import static com.android.SdkConstants.ATTR_DRAWABLE_START;
import static com.android.SdkConstants.ATTR_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_END;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_PARENT_START;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_ALIGN_START;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_END;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_LEFT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_RIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_MARGIN_START;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_END_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_LEFT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_RIGHT_OF;
import static com.android.SdkConstants.ATTR_LAYOUT_TO_START_OF;
import static com.android.SdkConstants.ATTR_LIST_PREFERRED_ITEM_PADDING_END;
import static com.android.SdkConstants.ATTR_LIST_PREFERRED_ITEM_PADDING_LEFT;
import static com.android.SdkConstants.ATTR_LIST_PREFERRED_ITEM_PADDING_RIGHT;
import static com.android.SdkConstants.ATTR_LIST_PREFERRED_ITEM_PADDING_START;
import static com.android.SdkConstants.ATTR_PADDING;
import static com.android.SdkConstants.ATTR_PADDING_END;
import static com.android.SdkConstants.ATTR_PADDING_LEFT;
import static com.android.SdkConstants.ATTR_PADDING_RIGHT;
import static com.android.SdkConstants.ATTR_PADDING_START;
import static com.android.SdkConstants.GRAVITY_VALUE_END;
import static com.android.SdkConstants.GRAVITY_VALUE_LEFT;
import static com.android.SdkConstants.GRAVITY_VALUE_RIGHT;
import static com.android.SdkConstants.GRAVITY_VALUE_START;
import static com.android.SdkConstants.TAG_APPLICATION;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

import lombok.ast.AstVisitor;
import lombok.ast.EnumConstant;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.Identifier;
import lombok.ast.ImportDeclaration;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Check which looks for RTL issues (right-to-left support) in layouts
 */
public class RtlDetector extends LayoutDetector implements Detector.JavaScanner {

    @SuppressWarnings("unchecked")
    private static final Implementation IMPLEMENTATION = new Implementation(
            RtlDetector.class,
            EnumSet.of(Scope.RESOURCE_FILE, Scope.JAVA_FILE, Scope.MANIFEST),
            Scope.RESOURCE_FILE_SCOPE,
            Scope.JAVA_FILE_SCOPE,
            Scope.MANIFEST_SCOPE
    );

    public static final Issue USE_START = Issue.create(
        "RtlHardcoded", //$NON-NLS-1$
        "Using left/right instead of start/end attributes",

        "Using `Gravity#LEFT` and `Gravity#RIGHT` can lead to problems when a layout is " +
        "rendered in locales where text flows from right to left. Use `Gravity#START` " +
        "and `Gravity#END` instead. Similarly, in XML `gravity` and `layout_gravity` " +
        "attributes, use `start` rather than `left`.\n" +
        "\n" +
        "For XML attributes such as paddingLeft and `layout_marginLeft`, use `paddingStart` " +
        "and `layout_marginStart`. *NOTE*: If your `minSdkVersion` is less than 17, you should " +
        "add *both* the older left/right attributes *as well as* the new start/right " +
        "attributes. On older platforms, where RTL is not supported and the start/right " +
        "attributes are unknown and therefore ignored, you need the older left/right " +
        "attributes. There is a separate lint check which catches that type of error.\n" +
        "\n" +
        "(Note: For `Gravity#LEFT` and `Gravity#START`, you can use these constants even " +
        "when targeting older platforms, because the `start` bitmask is a superset of the " +
        "`left` bitmask. Therefore, you can use `gravity=\"start\"` rather than " +
        "`gravity=\"left|start\"`.)",

        Category.RTL, 5, Severity.WARNING, IMPLEMENTATION);

    public static final Issue COMPAT = Issue.create(
        "RtlCompat", //$NON-NLS-1$
        "Right-to-left text compatibility issues",

        "API 17 adds a `textAlignment` attribute to specify text alignment. However, " +
        "if you are supporting older versions than API 17, you must *also* specify a " +
        "gravity or layout_gravity attribute, since older platforms will ignore the " +
        "`textAlignment` attribute.",

        Category.RTL, 6, Severity.ERROR, IMPLEMENTATION);

    public static final Issue SYMMETRY = Issue.create(
        "RtlSymmetry", //$NON-NLS-1$
        "Padding and margin symmetry",

        "If you specify padding or margin on the left side of a layout, you should " +
        "probably also specify padding on the right side (and vice versa) for " +
        "right-to-left layout symmetry.",

        Category.RTL, 6, Severity.WARNING, IMPLEMENTATION);


    public static final Issue ENABLED = Issue.create(
        "RtlEnabled", //$NON-NLS-1$
        "Using RTL attributes without enabling RTL support",

        "To enable right-to-left support, when running on API 17 and higher, you must " +
        "set the `android:supportsRtl` attribute in the manifest `<application>` element.\n" +
        "\n" +
        "If you have started adding RTL attributes, but have not yet finished the " +
        "migration, you can set the attribute to false to satisfy this lint check.",

        Category.RTL, 3, Severity.WARNING, IMPLEMENTATION);

    /* TODO:
    public static final Issue FIELD = Issue.create(
        "RtlFieldAccess", //$NON-NLS-1$
        "Accessing margin and padding fields directly",

        "Modifying the padding and margin constants in view objects directly is " +
        "problematic when using RTL support, since it can lead to inconsistent states. You " +
        "*must* use the corresponding setter methods instead (`View#setPadding` etc).",

        Category.RTL, 3, Severity.WARNING, IMPLEMENTATION).setEnabledByDefault(false);

    public static final Issue AWARE = Issue.create(
        "RtlAware", //$NON-NLS-1$
        "View code not aware of RTL APIs",

        "When manipulating views, and especially when implementing custom layouts, " +
        "the code may need to be aware of RTL APIs. This lint check looks for usages of " +
        "APIs that frequently require adjustments for right-to-left text, and warns if it " +
        "does not also see text direction look-ups indicating that the code has already " +
        "been updated to handle RTL layouts.",

        Category.RTL, 3, Severity.WARNING, IMPLEMENTATION).setEnabledByDefault(false);
    */

    private static final String RIGHT_FIELD = "RIGHT";                          //$NON-NLS-1$
    private static final String LEFT_FIELD = "LEFT";                            //$NON-NLS-1$
    private static final String GRAVITY_CLASS = "Gravity";                      //$NON-NLS-1$
    private static final String FQCN_GRAVITY = "android.view.Gravity";          //$NON-NLS-1$
    private static final String FQCN_GRAVITY_PREFIX = "android.view.Gravity.";  //$NON-NLS-1$
    private static final String ATTR_SUPPORTS_RTL = "supportsRtl";              //$NON-NLS-1$
    private static final String ATTR_TEXT_ALIGNMENT = "textAlignment";          //$NON-NLS-1$

    /** API version in which RTL support was added */
    private static final int RTL_API = 17;

    private static final String LEFT = "Left";
    private static final String START = "Start";
    private static final String RIGHT = "Right";
    private static final String END = "End";

    private Boolean mEnabledRtlSupport;
    private boolean mUsesRtlAttributes;

    /** Constructs a new {@link RtlDetector} */
    public RtlDetector() {
    }

    @Override
    @NonNull
    public  Speed getSpeed() {
        return Speed.NORMAL;
    }

    private boolean rtlApplies(@NonNull Context context) {
        Project project = context.getMainProject();
        if  (project.getTargetSdk() < RTL_API) {
            return false;
        }

        int buildTarget = project.getBuildSdk();
        if (buildTarget != -1 && buildTarget < RTL_API) {
            return false;
        }

        //noinspection RedundantIfStatement
        if (mEnabledRtlSupport != null && !mEnabledRtlSupport) {
            return false;
        }

        return true;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mUsesRtlAttributes && mEnabledRtlSupport == null && rtlApplies(context)) {
            List<File> manifestFile = context.getMainProject().getManifestFiles();
            if (!manifestFile.isEmpty()) {
                Location location = Location.create(manifestFile.get(0));
                context.report(ENABLED, location,
                        "The project references RTL attributes, but does not explicitly enable " +
                        "or disable RTL support with `android:supportsRtl` in the manifest");
            }
        }
    }

    // ---- Implements XmlDetector ----

    @VisibleForTesting
    static final String[] ATTRIBUTES = new String[] {
            // Pairs, from left/right constants to corresponding start/end constants
            ATTR_LAYOUT_ALIGN_PARENT_LEFT,          ATTR_LAYOUT_ALIGN_PARENT_START,
            ATTR_LAYOUT_ALIGN_PARENT_RIGHT,         ATTR_LAYOUT_ALIGN_PARENT_END,
            ATTR_LAYOUT_MARGIN_LEFT,                ATTR_LAYOUT_MARGIN_START,
            ATTR_LAYOUT_MARGIN_RIGHT,               ATTR_LAYOUT_MARGIN_END,
            ATTR_PADDING_LEFT,                      ATTR_PADDING_START,
            ATTR_PADDING_RIGHT,                     ATTR_PADDING_END,
            ATTR_DRAWABLE_LEFT,                     ATTR_DRAWABLE_START,
            ATTR_DRAWABLE_RIGHT,                    ATTR_DRAWABLE_END,
            ATTR_LIST_PREFERRED_ITEM_PADDING_LEFT,  ATTR_LIST_PREFERRED_ITEM_PADDING_START,
            ATTR_LIST_PREFERRED_ITEM_PADDING_RIGHT, ATTR_LIST_PREFERRED_ITEM_PADDING_END,

            // RelativeLayout
            ATTR_LAYOUT_TO_LEFT_OF,                 ATTR_LAYOUT_TO_START_OF,
            ATTR_LAYOUT_TO_RIGHT_OF,                ATTR_LAYOUT_TO_END_OF,
            ATTR_LAYOUT_ALIGN_LEFT,                 ATTR_LAYOUT_ALIGN_START,
            ATTR_LAYOUT_ALIGN_RIGHT,                ATTR_LAYOUT_ALIGN_END,
    };
    static {
        if (LintUtils.assertionsEnabled()) {
            for (int i = 0; i < ATTRIBUTES.length; i += 2) {
                String replace = ATTRIBUTES[i];
                String with = ATTRIBUTES[i + 1];
                assert with.equals(convertOldToNew(replace));
                assert replace.equals(convertNewToOld(with));
            }
        }
    }

    public static boolean isRtlAttributeName(@NonNull String attribute) {
        for (int i = 1; i < ATTRIBUTES.length; i += 2) {
            if (attribute.equals(ATTRIBUTES[i])) {
                return true;
            }
        }
        return false;
    }

    @VisibleForTesting
    static String convertOldToNew(String attribute) {
        if (attribute.contains(LEFT)) {
            return attribute.replace(LEFT, START);
        } else {
            return attribute.replace(RIGHT, END);
        }
    }

    @VisibleForTesting
    static String convertNewToOld(String attribute) {
        if (attribute.contains(START)) {
            return attribute.replace(START, LEFT);
        } else {
            return attribute.replace(END, RIGHT);
        }
    }

    @VisibleForTesting
    static String convertToOppositeDirection(String attribute) {
        if (attribute.contains(LEFT)) {
            return attribute.replace(LEFT, RIGHT);
        } else if (attribute.contains(RIGHT)) {
            return attribute.replace(RIGHT, LEFT);
        } else if (attribute.contains(START)) {
            return attribute.replace(START, END);
        } else {
            return attribute.replace(END, START);
        }
    }

    @Nullable
    static String getTextAlignmentToGravity(String attribute) {
        if (attribute.endsWith(START)) { // textStart, viewStart, ...
            return GRAVITY_VALUE_START;
        } else if (attribute.endsWith(END)) { // textEnd, viewEnd, ...
            return GRAVITY_VALUE_END;
        } else {
            return null; // inherit, others
        }
    }

    @Override
    public Collection<String> getApplicableAttributes() {
        int size = ATTRIBUTES.length + 4;
        List<String> attributes = new ArrayList<String>(size);

        // For detecting whether RTL support is enabled
        attributes.add(ATTR_SUPPORTS_RTL);

        // For detecting left/right attributes which should probably be
        // migrated to start/end
        attributes.add(ATTR_GRAVITY);
        attributes.add(ATTR_LAYOUT_GRAVITY);

        // For detecting existing attributes which indicate an attempt to
        // use RTL
        attributes.add(ATTR_TEXT_ALIGNMENT);

        // Add conversion attributes: left/right attributes to nominate
        // attributes that should be added as start/end, and start/end
        // attributes to use to look up elements that should have compatibility
        // left/right ones as well
        Collections.addAll(attributes, ATTRIBUTES);

        assert attributes.size() == size : attributes.size();

        return attributes;
    }

    @Override
    public void visitAttribute(@NonNull XmlContext context, @NonNull Attr attribute) {
        Project project = context.getMainProject();
        String value = attribute.getValue();

        if (!ANDROID_URI.equals(attribute.getNamespaceURI())) {
            // Layout attribute not in the Android namespace (or a custom namespace).
            // This is likely an application error (which should get caught by
            // the MissingPrefixDetector)
            return;
        }

        String name = attribute.getLocalName();
        assert name != null : attribute.getName();

        if (name.equals(ATTR_SUPPORTS_RTL)) {
            mEnabledRtlSupport = Boolean.valueOf(value);
            if (!attribute.getOwnerElement().getTagName().equals(TAG_APPLICATION)) {
                context.report(ENABLED, attribute, context.getLocation(attribute), String.format(
                    "Wrong declaration: `%1$s` should be defined on the `<application>` element",
                        attribute.getName()));
            }
            int targetSdk = project.getTargetSdk();
            if (mEnabledRtlSupport && targetSdk < RTL_API) {
                String message = String.format(
                        "You must set `android:targetSdkVersion` to at least %1$d when "
                                + "enabling RTL support (is %2$d)",
                                RTL_API, project.getTargetSdk());
                context.report(ENABLED, attribute, context.getValueLocation(attribute), message);
            }
            return;
        }

        if (!rtlApplies(context)) {
            return;
        }

        if (name.equals(ATTR_TEXT_ALIGNMENT)) {
            if (context.getProject().getReportIssues()) {
              mUsesRtlAttributes = true;
            }

            Element element = attribute.getOwnerElement();
            final String gravity;
            final Attr gravityNode;
            if (element.hasAttributeNS(ANDROID_URI, ATTR_GRAVITY)) {
                gravityNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_GRAVITY);
                gravity = gravityNode.getValue();
            } else if (element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_GRAVITY)) {
                gravityNode = element.getAttributeNodeNS(ANDROID_URI, ATTR_LAYOUT_GRAVITY);
                gravity = gravityNode.getValue();
            } else if (project.getMinSdk() < RTL_API) {
                int folderVersion = context.getFolderVersion();
                if (folderVersion < RTL_API && context.isEnabled(COMPAT)) {
                    String expectedGravity = getTextAlignmentToGravity(value);
                    if (expectedGravity != null) {
                        String message = String.format(
                                "To support older versions than API 17 (project specifies %1$d) "
                                    + "you must *also* specify `gravity` or `layout_gravity=\"%2$s\"`",
                                project.getMinSdk(), expectedGravity);
                        context.report(COMPAT, attribute,
                                context.getNameLocation(attribute), message);
                    }
                }
                return;
            } else {
                return;
            }

            String expectedGravity = getTextAlignmentToGravity(value);
            if (expectedGravity != null && !gravity.contains(expectedGravity)
                    && context.isEnabled(COMPAT)) {
                String message = String.format("Inconsistent alignment specification between "
                                + "`textAlignment` and `gravity` attributes: was `%1$s`, expected `%2$s`",
                        gravity, expectedGravity);
                Location location = context.getValueLocation(attribute);
                context.report(COMPAT, attribute, location, message);
                Location secondary = context.getValueLocation(gravityNode);
                secondary.setMessage("Incompatible direction here");
                location.setSecondary(secondary);
            }
            return;
        }

        if (name.equals(ATTR_GRAVITY) || name.equals(ATTR_LAYOUT_GRAVITY)) {
            boolean isLeft = value.contains(GRAVITY_VALUE_LEFT);
            boolean isRight = value.contains(GRAVITY_VALUE_RIGHT);
            if (!isLeft && !isRight) {
                if ((value.contains(GRAVITY_VALUE_START) || value.contains(GRAVITY_VALUE_END))
                        && context.getProject().getReportIssues()) {
                    mUsesRtlAttributes = true;
                }
                return;
            }
            String message = String.format(
                    "Use \"`%1$s`\" instead of \"`%2$s`\" to ensure correct behavior in "
                            + "right-to-left locales",
                    isLeft ? GRAVITY_VALUE_START : GRAVITY_VALUE_END,
                    isLeft ? GRAVITY_VALUE_LEFT : GRAVITY_VALUE_RIGHT);
            if (context.isEnabled(USE_START)) {
                context.report(USE_START, attribute, context.getValueLocation(attribute), message);
            }

            return;
        }

        // Some other left/right/start/end attribute
        int targetSdk = project.getTargetSdk();

        // TODO: If attribute is drawableLeft or drawableRight, add note that you might
        // want to consider adding a specialized image in the -ldrtl folder as well

        Element element = attribute.getOwnerElement();
        boolean isPaddingAttribute = isPaddingAttribute(name);
        if (isPaddingAttribute || isMarginAttribute(name)) {
            String opposite = convertToOppositeDirection(name);
            if (element.hasAttributeNS(ANDROID_URI, opposite)) {
                String oldValue = element.getAttributeNS(ANDROID_URI, opposite);
                if (value.equals(oldValue)) {
                    return;
                }
            } else if (isPaddingAttribute
                    && !element.hasAttributeNS(ANDROID_URI,
                    isOldAttribute(opposite) ? convertOldToNew(opposite)
                            : convertNewToOld(opposite)) && context.isEnabled(SYMMETRY)) {
                String message = String.format(
                        "When you define `%1$s` you should probably also define `%2$s` for "
                        + "right-to-left symmetry", name, opposite);
                context.report(SYMMETRY, attribute, context.getNameLocation(attribute), message);
            }
        }

        boolean isOld = isOldAttribute(name);
        if (isOld) {
            if (!context.isEnabled(USE_START)) {
                return;
            }
            String rtl = convertOldToNew(name);
            if (element.hasAttributeNS(ANDROID_URI, rtl)) {
                if (project.getMinSdk() >= RTL_API || context.getFolderVersion() >= RTL_API) {
                    // Warn that left/right isn't needed
                    String message = String.format(
                            "Redundant attribute `%1$s`; already defining `%2$s` with "
                                    + "`targetSdkVersion` %3$s",
                            name, rtl, targetSdk);
                    context.report(USE_START, attribute,
                            context.getNameLocation(attribute), message);
                }
            } else {
                String message;
                if (project.getMinSdk() >= RTL_API || context.getFolderVersion() >= RTL_API) {
                    message = String.format(
                            "Consider replacing `%1$s` with `%2$s:%3$s=\"%4$s\"` to better support "
                                    + "right-to-left layouts",
                            attribute.getName(), attribute.getPrefix(), rtl, value);
                } else {
                    message = String.format(
                            "Consider adding `%1$s:%2$s=\"%3$s\"` to better support "
                                    + "right-to-left layouts",
                            attribute.getPrefix(), rtl, value);
                }
                context.report(USE_START, attribute,
                        context.getNameLocation(attribute), message);
            }
        } else {
            if (project.getMinSdk() >= RTL_API || !context.isEnabled(COMPAT)) {
                // Only supporting 17+: no need to define older attributes
                return;
            }
            int folderVersion = context.getFolderVersion();
            if (folderVersion >= RTL_API) {
                // In a -v17 folder or higher: no need to define older attributes
                return;
            }
            String old = convertNewToOld(name);
            if (element.hasAttributeNS(ANDROID_URI, old)) {
                return;
            }
            String message = String.format(
                    "To support older versions than API 17 (project specifies %1$d) "
                            + "you should *also* add `%2$s:%3$s=\"%4$s\"`",
                    project.getMinSdk(), attribute.getPrefix(), old,
                    convertNewToOld(value));
            context.report(COMPAT, attribute, context.getNameLocation(attribute), message);
        }
    }

    private static boolean isOldAttribute(String name) {
        return name.contains(LEFT) || name.contains(RIGHT);
    }

    private static boolean isMarginAttribute(@NonNull String name) {
        return name.startsWith(ATTR_LAYOUT_MARGIN);
    }

    private static boolean isPaddingAttribute(@NonNull String name) {
        return name.startsWith(ATTR_PADDING);
    }

    // ---- Implements JavaScanner ----

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(Identifier.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        if (rtlApplies(context)) {
            return new IdentifierChecker(context);
        }

        return new ForwardingAstVisitor() { };
    }

    private static class IdentifierChecker extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public IdentifierChecker(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitIdentifier(Identifier node) {
            String identifier = node.astValue();
            boolean isLeft = LEFT_FIELD.equals(identifier);
            boolean isRight = RIGHT_FIELD.equals(identifier);
            if (!isLeft && !isRight) {
                return false;
            }

            Node parent = node.getParent();
            if (parent instanceof ImportDeclaration || parent instanceof EnumConstant
                    || parent instanceof VariableDefinitionEntry) {
                return false;
            }

            JavaParser.ResolvedNode resolved = mContext.resolve(node);
            if (resolved != null) {
                if (!(resolved instanceof JavaParser.ResolvedField)) {
                    return false;
                } else {
                    JavaParser.ResolvedField field = (JavaParser.ResolvedField) resolved;
                    if (!field.getContainingClass().matches(FQCN_GRAVITY)) {
                        return false;
                    }
                }
            } else {
                // Can't resolve types (for example while editing code with errors):
                // rely on heuristics like import statements and class qualifiers
                if (parent instanceof Select &&
                        !(GRAVITY_CLASS.equals(((Select) parent).astOperand().toString()))) {
                    return false;
                }
                if (parent instanceof VariableReference) {
                    // No operand: make sure it's statically imported
                    if (!LintUtils.isImported(mContext.getCompilationUnit(),
                            FQCN_GRAVITY_PREFIX + identifier)) {
                        return false;
                    }
                }
            }

            String message = String.format(
                    "Use \"`Gravity.%1$s`\" instead of \"`Gravity.%2$s`\" to ensure correct "
                            + "behavior in right-to-left locales",
                    (isLeft ? GRAVITY_VALUE_START : GRAVITY_VALUE_END).toUpperCase(Locale.US),
                    (isLeft ? GRAVITY_VALUE_LEFT : GRAVITY_VALUE_RIGHT).toUpperCase(Locale.US));
            Location location = mContext.getLocation(node);
            mContext.report(USE_START, node, location, message);

            return true;
        }
    }
}
