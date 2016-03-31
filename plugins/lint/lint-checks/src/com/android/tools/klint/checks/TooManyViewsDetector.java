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
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LayoutDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;

import org.w3c.dom.Element;

import java.util.Collection;

/**
 * Checks whether a view hierarchy has too many views or has a suspiciously deep hierarchy
 */
public class TooManyViewsDetector extends LayoutDetector {

    private static final Implementation IMPLEMENTATION = new Implementation(
            TooManyViewsDetector.class,
            Scope.RESOURCE_FILE_SCOPE);

    /** Issue of having too many views in a single layout */
    public static final Issue TOO_MANY = Issue.create(
            "TooManyViews", //$NON-NLS-1$
            "Layout has too many views",
            "Using too many views in a single layout is bad for " +
            "performance. Consider using compound drawables or other tricks for " +
            "reducing the number of views in this layout.\n" +
            "\n" +
            "The maximum view count defaults to 80 but can be configured with the " +
            "environment variable `ANDROID_LINT_MAX_VIEW_COUNT`.",
            Category.PERFORMANCE,
            1,
            Severity.WARNING,
            IMPLEMENTATION);

    /** Issue of having too deep hierarchies in layouts */
    public static final Issue TOO_DEEP = Issue.create(
            "TooDeepLayout", //$NON-NLS-1$
            "Layout hierarchy is too deep",
            "Layouts with too much nesting is bad for performance. " +
            "Consider using a flatter layout (such as `RelativeLayout` or `GridLayout`)." +
            "The default maximum depth is 10 but can be configured with the environment " +
            "variable `ANDROID_LINT_MAX_DEPTH`.",
            Category.PERFORMANCE,
            1,
            Severity.WARNING,
            IMPLEMENTATION);

    private static final int MAX_VIEW_COUNT;
    private static final int MAX_DEPTH;
    static {
        int maxViewCount = 0;
        int maxDepth = 0;

        String countValue = System.getenv("ANDROID_LINT_MAX_VIEW_COUNT"); //$NON-NLS-1$
        if (countValue != null) {
            try {
                maxViewCount = Integer.parseInt(countValue);
            } catch (NumberFormatException e) {
                // pass: set to default below
            }
        }
        String depthValue = System.getenv("ANDROID_LINT_MAX_DEPTH"); //$NON-NLS-1$
        if (depthValue != null) {
            try {
                maxDepth = Integer.parseInt(depthValue);
            } catch (NumberFormatException e) {
                // pass: set to default below
            }
        }
        if (maxViewCount == 0) {
            maxViewCount = 80;
        }
        if (maxDepth == 0) {
            maxDepth = 10;
        }

        MAX_VIEW_COUNT = maxViewCount;
        MAX_DEPTH = maxDepth;
    }

    private int mViewCount;
    private int mDepth;
    private boolean mWarnedAboutDepth;

    /** Constructs a new {@link TooManyViewsDetector} */
    public TooManyViewsDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void beforeCheckFile(@NonNull Context context) {
        mViewCount = mDepth = 0;
        mWarnedAboutDepth = false;
    }

    @Override
    public Collection<String> getApplicableElements() {
        return ALL;
    }

    @Override
    public void visitElement(@NonNull XmlContext context, @NonNull Element element) {
        mViewCount++;
        mDepth++;

        if (mDepth == MAX_DEPTH && !mWarnedAboutDepth) {
            // Have to record whether or not we've warned since we could have many siblings
            // at the max level and we'd warn for each one. No need to do the same thing
            // for the view count error since we'll only have view count exactly equal the
            // max just once.
            mWarnedAboutDepth = true;
            String msg = String.format("`%1$s` has more than %2$d levels, bad for performance",
                    context.file.getName(), MAX_DEPTH);
            context.report(TOO_DEEP, element, context.getLocation(element), msg);
        }
        if (mViewCount == MAX_VIEW_COUNT) {
            String msg = String.format("`%1$s` has more than %2$d views, bad for performance",
                    context.file.getName(), MAX_VIEW_COUNT);
            context.report(TOO_MANY, element, context.getLocation(element), msg);
        }
    }

    @Override
    public void visitElementAfter(@NonNull XmlContext context, @NonNull Element element) {
        mDepth--;
    }
}
