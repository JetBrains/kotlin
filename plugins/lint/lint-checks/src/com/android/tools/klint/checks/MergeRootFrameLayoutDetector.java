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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.ATTR_BACKGROUND;
import static com.android.SdkConstants.ATTR_FOREGROUND;
import static com.android.SdkConstants.ATTR_LAYOUT;
import static com.android.SdkConstants.ATTR_LAYOUT_GRAVITY;
import static com.android.SdkConstants.DOT_JAVA;
import static com.android.SdkConstants.FRAME_LAYOUT;
import static com.android.SdkConstants.LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.R_LAYOUT_RESOURCE_PREFIX;
import static com.android.SdkConstants.VIEW_INCLUDE;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.LayoutDetector;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Location.Handle;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;
import com.android.tools.klint.detector.api.XmlContext;
import com.android.utils.Pair;

import org.jetbrains.uast.UExpression;
import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UQualifiedExpression;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastScanner;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks whether a root FrameLayout can be replaced with a {@code <merge>} tag.
 */
public class MergeRootFrameLayoutDetector extends LayoutDetector implements UastScanner {
    /**
     * Set of layouts that we want to enable the warning for. We only warn for
     * {@code <FrameLayout>}'s that are the root of a layout included from
     * another layout, or directly referenced via a {@code setContentView} call.
     */
    private Set<String> mWhitelistedLayouts;

    /**
     * Set of pending [layout, location] pairs where the given layout is a
     * FrameLayout that perhaps should be replaced by a {@code <merge>} tag (if
     * the layout is included or set as the content view. This must be processed
     * after the whole project has been scanned since the set of includes etc
     * can be encountered after the included layout.
     */
    private List<Pair<String, Location.Handle>> mPending;

    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "MergeRootFrame", //$NON-NLS-1$
            "FrameLayout can be replaced with `<merge>` tag",

            "If a `<FrameLayout>` is the root of a layout and does not provide background " +
            "or padding etc, it can often be replaced with a `<merge>` tag which is slightly " +
            "more efficient. Note that this depends on context, so make sure you understand " +
            "how the `<merge>` tag works before proceeding.",
            Category.PERFORMANCE,
            4,
            Severity.WARNING,
            new Implementation(
                    MergeRootFrameLayoutDetector.class,
                    EnumSet.of(Scope.ALL_RESOURCE_FILES, Scope.SOURCE_FILE)))
            .addMoreInfo(
            "http://android-developers.blogspot.com/2009/03/android-layout-tricks-3-optimize-by.html"); //$NON-NLS-1$

    /** Constructs a new {@link MergeRootFrameLayoutDetector} */
    public MergeRootFrameLayoutDetector() {
    }

    @Override
    @NonNull
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return LintUtils.isXmlFile(file) || LintUtils.endsWith(file.getName(), DOT_JAVA);
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        if (mPending != null && mWhitelistedLayouts != null) {
            // Process all the root FrameLayouts that are eligible, and generate
            // suggestions for <merge> replacements for any layouts that are included
            // from other layouts
            for (Pair<String, Handle> pair : mPending) {
                String layout = pair.getFirst();
                if (mWhitelistedLayouts.contains(layout)) {
                    Handle handle = pair.getSecond();

                    Object clientData = handle.getClientData();
                    if (clientData instanceof Node) {
                        if (context.getDriver().isSuppressed(null, ISSUE, (Node) clientData)) {
                            return;
                        }
                    }

                    Location location = handle.resolve();
                    context.report(ISSUE, location,
                            "This `<FrameLayout>` can be replaced with a `<merge>` tag");
                }
            }
        }
    }

    // Implements XmlScanner

    @Override
    public Collection<String> getApplicableElements() {
        return Arrays.asList(VIEW_INCLUDE, FRAME_LAYOUT);
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        String tag = element.getTagName();
        if (tag.equals(VIEW_INCLUDE)) {
            String layout = element.getAttribute(ATTR_LAYOUT); // NOTE: Not in android: namespace
            if (layout.startsWith(LAYOUT_RESOURCE_PREFIX)) { // Ignore @android:layout/ layouts
                layout = layout.substring(LAYOUT_RESOURCE_PREFIX.length());
                whiteListLayout(layout);
            }
        } else {
            assert tag.equals(FRAME_LAYOUT);
            if (LintUtils.isRootElement(element) &&
                ((isWidthFillParent(element) && isHeightFillParent(element)) ||
                        !element.hasAttributeNS(ANDROID_URI, ATTR_LAYOUT_GRAVITY))
                    && !element.hasAttributeNS(ANDROID_URI, ATTR_BACKGROUND)
                    && !element.hasAttributeNS(ANDROID_URI, ATTR_FOREGROUND)
                    && !hasPadding(element)) {
                String layout = LintUtils.getLayoutName(context.file);
                Handle handle = context.createLocationHandle(element);
                handle.setClientData(element);

                if (!context.getProject().getReportIssues()) {
                    // If this is a library project not being analyzed, ignore it
                    return;
                }

                if (mPending == null) {
                    mPending = new ArrayList<Pair<String,Handle>>();
                }
                mPending.add(Pair.of(layout, handle));
            }
        }
    }

    private void whiteListLayout(String layout) {
        if (mWhitelistedLayouts == null) {
            mWhitelistedLayouts = new HashSet<String>();
        }
        mWhitelistedLayouts.add(layout);
    }

    // Implements UastScanner


    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("setContentView"); //$NON-NLS-1$
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        if (node.getValueArgumentCount() != 1) {
            return;
        }

        List<UExpression> argumentList = node.getValueArguments();
        UExpression argument = argumentList.get(0);
        if (argument instanceof UQualifiedExpression) {
            String expression = argument.renderString();
            if (expression.startsWith(R_LAYOUT_RESOURCE_PREFIX)) {
                whiteListLayout(expression.substring(R_LAYOUT_RESOURCE_PREFIX.length()));
            }
        }
    }
}
