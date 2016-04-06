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

package com.android.tools.klint.checks;

import static com.android.SdkConstants.*;

import com.android.annotations.NonNull;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.android.tools.klint.detector.api.Speed;
import com.google.common.collect.Lists;

import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Looks for ListView scrolling performance: should use view holder pattern
 */
public class ViewHolderDetector extends Detector implements UastScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            ViewHolderDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Using a view inflater unconditionally in an AdapterView */
    public static final Issue ISSUE = Issue.create(
            "ViewHolder", //$NON-NLS-1$
            "View Holder Candidates",

            "When implementing a view Adapter, you should avoid unconditionally inflating a " +
            "new layout; if an available item is passed in for reuse, you should try to " +
            "use that one instead. This helps make for example ListView scrolling much " +
            "smoother.",

            Category.PERFORMANCE,
            5,
            Severity.WARNING,
            IMPLEMENTATION)
            .addMoreInfo(
            "http://developer.android.com/training/improving-layouts/smooth-scrolling.html#ViewHolder");

    private static final String GET_VIEW = "getView";  //$NON-NLS-1$
    static final String INFLATE = "inflate";           //$NON-NLS-1$

    /** Constructs a new {@link ViewHolderDetector} check */
    public ViewHolderDetector() {
    }

    @NonNull
    @Override
    public Speed getSpeed() {
        return Speed.NORMAL;
    }

    // ---- Implements UastScanner ----

    @Override
    public UastVisitor createUastVisitor(UastAndroidContext context) {
        return new ViewAdapterVisitor(context);
    }

    private static class ViewAdapterVisitor extends UastVisitor {
        private final UastAndroidContext mContext;

        public ViewAdapterVisitor(UastAndroidContext context) {
            mContext = context;
        }

        @Override
        public boolean visitFunction(@NotNull UFunction node) {
            if (isViewAdapterMethod(node)) {
                InflationVisitor visitor = new InflationVisitor(mContext);
                visitor.process(node);
                visitor.finish();
            }
            return false;
        }

        /**
         * Returns true if this method looks like it's overriding android.widget.Adapter's getView
         * method: getView(int position, View convertView, ViewGroup parent)
         */
        private static boolean isViewAdapterMethod(UFunction node) {
            if (GET_VIEW.equals(node.getName())) {
                List<UVariable> parameters = node.getValueParameters();
                if (parameters.size() == 3) {
                    Iterator<UVariable> iterator = parameters.iterator();
                    if (!iterator.hasNext()) {
                        return false;
                    }

                    UVariable first = iterator.next();
                    if (!first.getType().isInt()) {
                        return false;
                    }

                    if (!iterator.hasNext()) {
                        return false;
                    }

                    UVariable second = iterator.next();
                    if (!second.getType().matchesFqName(CLASS_VIEW)) {
                        return false;
                    }

                    if (!iterator.hasNext()) {
                        return false;
                    }

                    UVariable third = iterator.next();
                    //noinspection RedundantIfStatement
                    if (!third.getType().matchesFqName(CLASS_VIEWGROUP)) {
                        return false;
                    }

                    return true;
                }
            }

            return false;
        }
    }

    private static class InflationVisitor extends UastVisitor {
        private final UastAndroidContext mContext;
        private List<UElement> mNodes;
        private boolean mHaveConditional;

        public InflationVisitor(UastAndroidContext context) {
            mContext = context;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            UElement parent = node.getParent();
            if (parent instanceof UQualifiedExpression) {
                String methodName = node.getFunctionName();
                if (INFLATE.equals(methodName) && node.getValueArgumentCount() >= 1) {
                    // See if we're inside a conditional
                    boolean insideIf = false;
                    UElement p = parent.getParent();
                    while (p != null) {
                        if (p instanceof UIfExpression || p instanceof USwitchExpression) {
                            insideIf = true;
                            mHaveConditional = true;
                            break;
                        } else if (p == node) {
                            break;
                        }
                        p = p.getParent();
                    }
                    if (!insideIf) {
                        // Rather than reporting immediately, we only report if we didn't
                        // find any conditionally executed inflate statements in the method.
                        // This is because there are cases where getView method is complicated
                        // and inflates not just the top level layout but also children
                        // of the view, and we don't want to flag these. (To be more accurate
                        // should perform flow analysis and only report unconditional inflation
                        // of layouts that wind up flowing to the return value; that requires
                        // more work, and this simple heuristic is good enough for nearly all test
                        // cases I've come across.
                        if (mNodes == null) {
                            mNodes = Lists.newArrayList();
                        }
                        mNodes.add(node);
                    }
                }
            }

            return false;
        }

        public void finish() {
            if (!mHaveConditional && mNodes != null) {
                for (UElement node : mNodes) {
                    String message = "Unconditional layout inflation from view adapter: "
                            + "Should use View Holder pattern (use recycled view passed "
                            + "into this method as the second parameter) for smoother "
                            + "scrolling";
                    mContext.report(ISSUE, node, UastAndroidUtils.getLocation(node), message);
                }
            }
        }
    }
}
