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

import static com.android.SdkConstants.ANDROID_NS_NAME_PREFIX;
import static com.android.SdkConstants.ANDROID_STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LAYOUT_HEIGHT;
import static com.android.SdkConstants.ATTR_LAYOUT_WIDTH;
import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ATTR_PARENT;
import static com.android.SdkConstants.ATTR_STYLE;
import static com.android.SdkConstants.AUTO_URI;
import static com.android.SdkConstants.FD_RES_LAYOUT;
import static com.android.SdkConstants.FQCN_GRID_LAYOUT_V7;
import static com.android.SdkConstants.GRID_LAYOUT;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.REQUEST_FOCUS;
import static com.android.SdkConstants.STYLE_RESOURCE_PREFIX;
import static com.android.SdkConstants.TABLE_LAYOUT;
import static com.android.SdkConstants.TABLE_ROW;
import static com.android.SdkConstants.TAG_DATA;
import static com.android.SdkConstants.TAG_IMPORT;
import static com.android.SdkConstants.TAG_ITEM;
import static com.android.SdkConstants.TAG_LAYOUT;
import static com.android.SdkConstants.TAG_STYLE;
import static com.android.SdkConstants.TAG_VARIABLE;
import static com.android.SdkConstants.VIEW_INCLUDE;
import static com.android.SdkConstants.VIEW_MERGE;
import static com.android.resources.ResourceFolderType.LAYOUT;
import static com.android.resources.ResourceFolderType.VALUES;
import static com.android.tools.klint.detector.api.LintUtils.getLayoutName;
import static com.android.tools.klint.detector.api.LintUtils.isNullLiteral;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.annotations.VisibleForTesting;
import com.android.resources.ResourceUrl;
import com.android.resources.ResourceFolderType;
import com.android.resources.ResourceType;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Detector.JavaPsiScanner;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.LayoutDetector;
import com.android.tools.klint.detector.api.ResourceEvaluator;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.XmlContext;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastLiteralUtils;
import org.jetbrains.uast.visitor.UastVisitor;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ensures that layout width and height attributes are specified
 */
public class RequiredAttributeDetector extends LayoutDetector implements Detector.UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "RequiredSize", //$NON-NLS-1$
            "Missing `layout_width` or `layout_height` attributes",

            "All views must specify an explicit `layout_width` and `layout_height` attribute. " +
            "There is a runtime check for this, so if you fail to specify a size, an exception " +
            "is thrown at runtime.\n" +
            "\n" +
            "It's possible to specify these widths via styles as well. GridLayout, as a special " +
            "case, does not require you to specify a size.",
            Category.CORRECTNESS,
            4,
            Severity.ERROR,
            new Implementation(
                    RequiredAttributeDetector.class,
                    EnumSet.of(Scope.JAVA_FILE, Scope.ALL_RESOURCE_FILES)));

    public static final String PERCENT_RELATIVE_LAYOUT
            = "android.support.percent.PercentRelativeLayout";
    public static final String ATTR_LAYOUT_WIDTH_PERCENT = "layout_widthPercent";
    public static final String ATTR_LAYOUT_HEIGHT_PERCENT = "layout_heightPercent";

    /** Map from each style name to parent style */
    @Nullable private Map<String, String> mStyleParents;

    /** Set of style names where the style sets the layout width */
    @Nullable private Set<String> mWidthStyles;

    /** Set of style names where the style sets the layout height */
    @Nullable private Set<String> mHeightStyles;

    /** Set of layout names for layouts that are included by an {@code <include>} tag
     * where the width is set on the include */
    @Nullable private Set<String> mIncludedWidths;

    /** Set of layout names for layouts that are included by an {@code <include>} tag
     * where the height is set on the include */
    @Nullable private Set<String> mIncludedHeights;

    /** Set of layout names for layouts that are included by an {@code <include>} tag
     * where the width is <b>not</b> set on the include */
    @Nullable private Set<String> mNotIncludedWidths;

    /** Set of layout names for layouts that are included by an {@code <include>} tag
     * where the height is <b>not</b> set on the include */
    @Nullable private Set<String> mNotIncludedHeights;

    /** Whether the width was set in a theme definition */
    private boolean mSetWidthInTheme;

    /** Whether the height was set in a theme definition */
    private boolean mSetHeightInTheme;

    /** Constructs a new {@link RequiredAttributeDetector} */
    public RequiredAttributeDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull ResourceFolderType folderType) {
        return folderType == LAYOUT || folderType == VALUES;
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        // Process checks in two phases:
        // Phase 1: Gather styles and includes (styles are encountered after the layouts
        // so we can't do it in a single phase, and includes can be affected by includes from
        // layouts we haven't seen yet)
        // Phase 2: Process layouts, using gathered style and include data, and mark layouts
        // not known.
        //
        if (context.getPhase() == 1) {
            checkSizeSetInTheme();

            context.requestRepeat(this, Scope.RESOURCE_FILE_SCOPE);
        }
    }

    private boolean isWidthStyle(String style) {
        return isSizeStyle(style, mWidthStyles);
    }

    private boolean isHeightStyle(String style) {
        return isSizeStyle(style, mHeightStyles);
    }

    private boolean isSizeStyle(String style, Set<String> sizeStyles) {
        if (isFrameworkSizeStyle(style)) {
            return true;
        }
        if (sizeStyles == null) {
            return false;
        }
        return isSizeStyle(stripStylePrefix(style), sizeStyles, 0);
    }

    private static boolean isFrameworkSizeStyle(String style) {
        // The styles Widget.TextView.ListSeparator (and several theme variations, such as
        // Widget.Holo.TextView.ListSeparator, Widget.Holo.Light.TextView.ListSeparator, etc)
        // define layout_width and layout_height.
        // These are exposed through the listSeparatorTextViewStyle style.
        if (style.equals("?android:attr/listSeparatorTextViewStyle")      //$NON-NLS-1$
                || style.equals("?android/listSeparatorTextViewStyle")) { //$NON-NLS-1$
            return true;
        }

        // It's also set on Widget.QuickContactBadge and Widget.QuickContactBadgeSmall
        // These are exposed via a handful of attributes with a common prefix
        if (style.startsWith("?android:attr/quickContactBadgeStyle")) { //$NON-NLS-1$
            return true;
        }

        // Finally, the styles are set on MediaButton and Widget.Holo.Tab (and
        // Widget.Holo.Light.Tab) but these are not exposed via attributes.

        return false;
    }

    private boolean isSizeStyle(
            @NonNull String style,
            @NonNull Set<String> sizeStyles, int depth) {
        if (depth == 30) {
            // Cycle between local and framework attribute style missed
            // by the fact that we're stripping the distinction between framework
            // and local styles here
            return false;
        }

        assert !style.startsWith(STYLE_RESOURCE_PREFIX)
                && !style.startsWith(ANDROID_STYLE_RESOURCE_PREFIX);

        if (sizeStyles.contains(style)) {
            return true;
        }

        if (mStyleParents != null) {
            String parentStyle = mStyleParents.get(style);
            if (parentStyle != null) {
                parentStyle = stripStylePrefix(parentStyle);
                if (isSizeStyle(parentStyle, sizeStyles, depth + 1)) {
                    return true;
                }
            }
        }

        int index = style.lastIndexOf('.');
        if (index > 0) {
            return isSizeStyle(style.substring(0, index), sizeStyles, depth + 1);
        }

        return false;
    }

    private void checkSizeSetInTheme() {
        // Look through the styles and determine whether each style is a theme
        if (mStyleParents == null) {
            return;
        }

        Map<String, Boolean> isTheme = Maps.newHashMap();
        for (String style : mStyleParents.keySet()) {
            if (isTheme(stripStylePrefix(style), isTheme, 0)) {
                mSetWidthInTheme = true;
                mSetHeightInTheme = true;
                break;
            }
        }
    }

    private boolean isTheme(String style, Map<String, Boolean> isTheme, int depth) {
        if (depth == 30) {
            // Cycle between local and framework attribute style missed
            // by the fact that we're stripping the distinction between framework
            // and local styles here
            return false;
        }

        assert !style.startsWith(STYLE_RESOURCE_PREFIX)
                && !style.startsWith(ANDROID_STYLE_RESOURCE_PREFIX);

        Boolean known = isTheme.get(style);
        if (known != null) {
            return known;
        }

        if (style.contains("Theme")) { //$NON-NLS-1$
            isTheme.put(style, true);
            return true;
        }

        if (mStyleParents != null) {
            String parentStyle = mStyleParents.get(style);
            if (parentStyle != null) {
                parentStyle = stripStylePrefix(parentStyle);
                if (isTheme(parentStyle, isTheme, depth + 1)) {
                    isTheme.put(style, true);
                    return true;
                }
            }
        }

        int index = style.lastIndexOf('.');
        if (index > 0) {
            String parentStyle = style.substring(0, index);
            boolean result = isTheme(parentStyle, isTheme, depth + 1);
            isTheme.put(style, result);
            return result;
        }

        return false;
    }

    @VisibleForTesting
    static boolean hasLayoutVariations(File file) {
        File parent = file.getParentFile();
        if (parent == null) {
            return false;
        }
        File res = parent.getParentFile();
        if (res == null) {
            return false;
        }
        String name = file.getName();
        File[] folders = res.listFiles();
        if (folders == null) {
            return false;
        }
        for (File folder : folders) {
            if (!folder.getName().startsWith(FD_RES_LAYOUT)) {
                continue;
            }
            if (folder.equals(parent)) {
                continue;
            }
            File other = new File(folder, name);
            if (other.exists()) {
                return true;
            }
        }

        return false;
    }

    private static String stripStylePrefix(@NonNull String style) {
        if (style.startsWith(STYLE_RESOURCE_PREFIX)) {
            style = style.substring(STYLE_RESOURCE_PREFIX.length());
        } else if (style.startsWith(ANDROID_STYLE_RESOURCE_PREFIX)) {
            style = style.substring(ANDROID_STYLE_RESOURCE_PREFIX.length());
        }

        return style;
    }

    private static boolean isRootElement(@NonNull Node node) {
        return node == node.getOwnerDocument().getDocumentElement();
    }

    // ---- Implements XmlScanner ----

    @Override
    public Collection<String> getApplicableElements() {
        return ALL;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        ResourceFolderType folderType = context.getResourceFolderType();
        int phase = context.getPhase();
        if (phase == 1 && folderType == VALUES) {
            String tag = element.getTagName();
            if (TAG_STYLE.equals(tag)) {
                String parent = element.getAttribute(ATTR_PARENT);
                if (parent != null && !parent.isEmpty()) {
                    String name = element.getAttribute(ATTR_NAME);
                    if (name != null && !name.isEmpty()) {
                        if (mStyleParents == null) {
                            mStyleParents = Maps.newHashMap();
                        }
                        mStyleParents.put(name, parent);
                    }
                }
            } else if (TAG_ITEM.equals(tag)
                    && TAG_STYLE.equals(element.getParentNode().getNodeName())) {
                String name = element.getAttribute(ATTR_NAME);
                if (name.endsWith(ATTR_LAYOUT_WIDTH) &&
                        name.equals(ANDROID_NS_NAME_PREFIX + ATTR_LAYOUT_WIDTH)) {
                    if (mWidthStyles == null) {
                        mWidthStyles = Sets.newHashSet();
                    }
                    String styleName = ((Element) element.getParentNode()).getAttribute(ATTR_NAME);
                    mWidthStyles.add(styleName);
                }
                if (name.endsWith(ATTR_LAYOUT_HEIGHT) &&
                        name.equals(ANDROID_NS_NAME_PREFIX + ATTR_LAYOUT_HEIGHT)) {
                    if (mHeightStyles == null) {
                        mHeightStyles = Sets.newHashSet();
                    }
                    String styleName = ((Element) element.getParentNode()).getAttribute(ATTR_NAME);
                    mHeightStyles.add(styleName);
                }
            }
        } else if (folderType == LAYOUT) {
            if (phase == 1) {
                // Gather includes
                if (element.getTagName().equals(VIEW_INCLUDE)) {
                    String layout = element.getAttribute(ATTR_LAYOUT);
                    if (layout != null && !layout.isEmpty()) {
                        recordIncludeWidth(layout,
                                element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH));
                        recordIncludeHeight(layout,
                                element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT));
                    }
                }
            } else {
                assert phase == 2; // Check everything using style data and include data
                boolean hasWidth = element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_WIDTH);
                boolean hasHeight = element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_HEIGHT);

                if (mSetWidthInTheme) {
                    hasWidth = true;
                }

                if (mSetHeightInTheme) {
                    hasHeight = true;
                }

                if (hasWidth && hasHeight) {
                    return;
                }

                String tag = element.getTagName();
                if (VIEW_MERGE.equals(tag)
                        || VIEW_INCLUDE.equals(tag)
                        || REQUEST_FOCUS.equals(tag)) {
                    return;
                }

                // Data binding: these tags shouldn't specify width/height
                if (tag.equals(TAG_LAYOUT)
                        || tag.equals(TAG_VARIABLE)
                        || tag.equals(TAG_DATA)
                        || tag.equals(TAG_IMPORT)) {
                    return;
                }

                String parentTag = element.getParentNode() != null
                        ?  element.getParentNode().getNodeName() : "";
                if (TABLE_LAYOUT.equals(parentTag)
                        || TABLE_ROW.equals(parentTag)
                        || GRID_LAYOUT.equals(parentTag)
                        || FQCN_GRID_LAYOUT_V7.equals(parentTag)) {
                    return;
                }

                // PercentRelativeLayout or PercentFrameLayout?
                boolean isPercent = parentTag.startsWith("android.support.percent.Percent");
                if (isPercent) {
                    hasWidth |= element.hasAttributeNS(AUTO_URI, ATTR_LAYOUT_WIDTH_PERCENT);
                    hasHeight |= element.hasAttributeNS(AUTO_URI, ATTR_LAYOUT_HEIGHT_PERCENT);
                    if (hasWidth && hasHeight) {
                        return;
                    }
                }

                if (!context.getProject().getReportIssues()) {
                    // If this is a library project not being analyzed, ignore it
                    return;
                }

                boolean certain = true;
                boolean isRoot = isRootElement(element);
                if (isRoot || isRootElement(element.getParentNode())
                        && VIEW_MERGE.equals(parentTag)) {
                    String name = LAYOUT_RESOURCE_PREFIX + getLayoutName(context.file);
                    if (!hasWidth && mIncludedWidths != null) {
                        hasWidth = mIncludedWidths.contains(name);
                        // If the layout is *also* included in a context where the width
                        // was not set, we're not certain; it's possible that
                        if (mNotIncludedWidths != null && mNotIncludedWidths.contains(name)) {
                            hasWidth = false;
                            // If we only have a single layout we know that this layout isn't
                            // always included with layout_width or layout_height set, but
                            // if there are multiple layouts, it's possible that at runtime
                            // we only load the size-less layout by the tag which includes
                            // the size
                            certain = !hasLayoutVariations(context.file);
                        }
                    }
                    if (!hasHeight && mIncludedHeights != null) {
                        hasHeight = mIncludedHeights.contains(name);
                        if (mNotIncludedHeights != null && mNotIncludedHeights.contains(name)) {
                            hasHeight = false;
                            certain = !hasLayoutVariations(context.file);
                        }
                    }
                    if (hasWidth && hasHeight) {
                        return;
                    }
                }

                if (!hasWidth || !hasHeight) {
                    String style = element.getAttribute(ATTR_STYLE);
                    if (style != null && !style.isEmpty()) {
                        if (!hasWidth) {
                            hasWidth = isWidthStyle(style);
                        }
                        if (!hasHeight) {
                            hasHeight = isHeightStyle(style);
                        }
                    }
                    if (hasWidth && hasHeight) {
                        return;
                    }
                }

                String message;
                if (!(hasWidth || hasHeight)) {
                    if (certain) {
                        message = "The required `layout_width` and `layout_height` attributes " +
                                "are missing";
                    } else {
                        message = "The required `layout_width` and `layout_height` attributes " +
                                "*may* be missing";
                    }
                } else {
                    String attribute = hasWidth ? ATTR_LAYOUT_HEIGHT : ATTR_LAYOUT_WIDTH;
                    if (certain) {
                        message = String.format("The required `%1$s` attribute is missing",
                                attribute);
                    } else {
                        message = String.format("The required `%1$s` attribute *may* be missing",
                                attribute);
                    }
                }
                if (isPercent) {
                    String escapedLayoutWidth = '`' + ATTR_LAYOUT_WIDTH + '`';
                    String escapedLayoutHeight = '`' + ATTR_LAYOUT_HEIGHT + '`';
                    String escapedLayoutWidthPercent = '`' + ATTR_LAYOUT_WIDTH_PERCENT + '`';
                    String escapedLayoutHeightPercent = '`' + ATTR_LAYOUT_HEIGHT_PERCENT + '`';
                    message = message.replace(escapedLayoutWidth, escapedLayoutWidth + " or "
                            + escapedLayoutWidthPercent).replace(escapedLayoutHeight,
                            escapedLayoutHeight + " or " + escapedLayoutHeightPercent);
                }
                context.report(ISSUE, element, context.getLocation(element),
                        message);
            }
        }
    }

    private void recordIncludeWidth(String layout, boolean providesWidth) {
        if (providesWidth) {
            if (mIncludedWidths == null) {
                mIncludedWidths = Sets.newHashSet();
            }
            mIncludedWidths.add(layout);
        } else {
            if (mNotIncludedWidths == null) {
                mNotIncludedWidths = Sets.newHashSet();
            }
            mNotIncludedWidths.add(layout);
        }
    }

    private void recordIncludeHeight(String layout, boolean providesHeight) {
        if (providesHeight) {
            if (mIncludedHeights == null) {
                mIncludedHeights = Sets.newHashSet();
            }
            mIncludedHeights.add(layout);
        } else {
            if (mNotIncludedHeights == null) {
                mNotIncludedHeights = Sets.newHashSet();
            }
            mNotIncludedHeights.add(layout);
        }
    }

    // ---- Implements UastScanner ----

    @Override
    @Nullable
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("inflate"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod method) {
        // Handle
        //    View#inflate(Context context, int resource, ViewGroup root)
        //    LayoutInflater#inflate(int resource, ViewGroup root)
        //    LayoutInflater#inflate(int resource, ViewGroup root, boolean attachToRoot)
        List<UExpression> args = call.getValueArguments();

        String layout = null;
        int index = 0;
        ResourceEvaluator evaluator = new ResourceEvaluator(context);
        for (UExpression expression : args) {
            ResourceUrl url = evaluator.getResource(expression);
            if (url != null && url.type == ResourceType.LAYOUT) {
                layout = url.toString();
                break;
            }
            index++;
        }

        if (layout == null) {
            // Flow analysis didn't succeed
            return;
        }

        // In all the applicable signatures, the view root argument is immediately after
        // the layout resource id.
        int viewRootPos = index + 1;
        if (viewRootPos < args.size()) {
            UExpression viewRoot = args.get(viewRootPos);
            if (UastLiteralUtils.isNullLiteral(viewRoot)) {
                // Yep, this one inflates the given view with a null parent:
                // Tag it as such. For now just use the include data structure since
                // it has the same net effect
                recordIncludeWidth(layout, true);
                recordIncludeHeight(layout, true);
            }
        }
    }
}
