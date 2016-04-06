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

import static com.android.SdkConstants.RESOURCE_CLZ_ID;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Detector looking for cut & paste issues
 */
public class CutPasteDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "CutPasteId", //$NON-NLS-1$
            "Likely cut & paste mistakes",

            "This lint check looks for cases where you have cut & pasted calls to " +
            "`findViewById` but have forgotten to update the R.id field. It's possible " +
            "that your code is simply (redundantly) looking up the field repeatedly, " +
            "but lint cannot distinguish that from a case where you for example want to " +
            "initialize fields `prev` and `next` and you cut & pasted `findViewById(R.id.prev)` " +
            "and forgot to update the second initialization to `R.id.next`.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    CutPasteDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    private UFunction mLastMethod;
    private Map<String, UCallExpression> mIds;
    private Map<String, String> mLhs;
    private Map<String, String> mCallOperands;

    /** Constructs a new {@link CutPasteDetector} check */
    public CutPasteDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("findViewById"); //$NON-NLS-1$
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        String lhs = getLhs(node);
        if (lhs == null) {
            return;
        }

        UFunction method = UastUtils.getContainingFunction(node);
        if (method == null) {
            return;
        } else if (method != mLastMethod) {
            mIds = Maps.newHashMap();
            mLhs = Maps.newHashMap();
            mCallOperands = Maps.newHashMap();
            mLastMethod = method;
        }

        UElement parent = node.getParent();
        String callOperand = "";
        if (parent instanceof UQualifiedExpression) {
            callOperand = ((UQualifiedExpression)parent).getReceiver().renderString();
        }

        UExpression first = node.getValueArguments().get(0);
        if (first instanceof UQualifiedExpression) {
            UQualifiedExpression select = (UQualifiedExpression) first;
            String id = select.getSelector().renderString();
            UExpression operand = select.getReceiver();
            if (operand instanceof UQualifiedExpression) {
                UQualifiedExpression type = (UQualifiedExpression) operand;
                if (type.getSelector().renderString().equals(RESOURCE_CLZ_ID)) {
                    if (mIds.containsKey(id)) {
                        if (lhs.equals(mLhs.get(id))) {
                            return;
                        }
                        if (!callOperand.equals(mCallOperands.get(id))) {
                            return;
                        }
                        UCallExpression earlierCall = mIds.get(id);
                        if (!isReachableFrom(method, earlierCall, node)) {
                            return;
                        }
                        Location location = UastAndroidUtils.getLocation(node);
                        Location secondary = UastAndroidUtils.getLocation(earlierCall);
                        if (location != null && secondary != null) {
                            secondary.setMessage("First usage here");
                            location.setSecondary(secondary);
                            context.report(ISSUE, node, location, String.format(
                              "The id `%1$s` has already been looked up in this method; possible " +
                              "cut & paste error?", first.toString()));
                        }
                    } else {
                        mIds.put(id, node);
                        mLhs.put(id, lhs);
                        mCallOperands.put(id, callOperand);
                    }
                }
            }
        }

    }
    
    @Nullable
    private static String getLhs(@NonNull UCallExpression call) {
        UElement parent = call.getParent();
        if (UastBinaryExpressionWithTypeUtils.isTypeCast(parent)) {
            assert parent != null;
            parent = parent.getParent();
        }

        if (parent instanceof UVariable) {
            UVariable vde = (UVariable) parent;
            return vde.getName();
        } else if (parent instanceof UBinaryExpression) {
            UBinaryExpression be = (UBinaryExpression) parent;
            UExpression left = be.getLeftOperand();
            if (left instanceof USimpleReferenceExpression || left instanceof UQualifiedExpression) {
                return be.getLeftOperand().toString();
            } else if (left instanceof UArrayAccessExpression) {
                UArrayAccessExpression aa = (UArrayAccessExpression) left;
                return aa.getReceiver().toString();
            }
        }

        return null;
    }

    private static boolean isReachableFrom(
            @NonNull UElement method,
            @NonNull UCallExpression from,
            @NonNull UCallExpression to) {
        ReachableVisitor visitor = new ReachableVisitor(from, to);
        visitor.process(method);
        return visitor.isReachable();
    }

    private static class ReachableVisitor extends UastVisitor {
        @NonNull private final UCallExpression mFrom;
        @NonNull private final UCallExpression mTo;
        private boolean mReachable;
        private boolean mSeenEnd;

        public ReachableVisitor(@NonNull UCallExpression from, @NonNull UCallExpression to) {
            mFrom = from;
            mTo = to;
        }

        boolean isReachable() {
            return mReachable;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            if (node == mFrom) {
                mReachable = true;
            } else if (node == mTo) {
                mSeenEnd = true;

            }
            return false;
        }

        @Override
        public boolean visitIfExpression(@NotNull UIfExpression node) {
            UExpression condition = node.getCondition();
            UExpression body = node.getThenBranch();
            UElement elseBody = node.getElseBranch();
            process(condition);

            if (body != null) {
                boolean wasReachable = mReachable;
                process(body);
                mReachable = wasReachable;
            }
            if (elseBody != null) {
                boolean wasReachable = mReachable;
                process(elseBody);
                mReachable = wasReachable;
            }

            return false;
        }

        @Override
        public void process(@NotNull UElement element) {
            if (!mSeenEnd) {
                super.process(element);
            }
        }
    }
}
