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

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.*;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiMethod;
import org.jetbrains.uast.*;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import org.jetbrains.uast.visitor.UastVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.android.SdkConstants.RESOURCE_CLZ_ID;

/**
 * Detector looking for cut &amp; paste issues
 */
public class CutPasteDetector extends Detector implements Detector.UastScanner {
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

    private PsiMethod mLastMethod;
    private Map<String, UCallExpression> mIds;
    private Map<String, String> mLhs;
    private Map<String, String> mCallOperands;

    /** Constructs a new {@link CutPasteDetector} check */
    public CutPasteDetector() {
    }

    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("findViewById"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable UastVisitor visitor,
            @NonNull UCallExpression call, @NonNull UMethod calledMethod) {
        String lhs = getLhs(call);
        if (lhs == null) {
            return;
        }

        UMethod method = UastUtils.getParentOfType(call, UMethod.class, false);
        if (method == null) {
            return; // prevent doing the same work for multiple findViewById calls in same method
        } else if (method != mLastMethod) {
            mIds = Maps.newHashMap();
            mLhs = Maps.newHashMap();
            mCallOperands = Maps.newHashMap();
            mLastMethod = method;
        }
        
        String callOperand = call.getReceiver() != null
                             ? call.getReceiver().asSourceString() : "";

        List<UExpression> arguments = call.getValueArguments();
        if (arguments.isEmpty()) {
            return;
        }
        UExpression first = arguments.get(0);
        if (first instanceof UReferenceExpression) {
            UReferenceExpression psiReferenceExpression = (UReferenceExpression) first;
            String id = psiReferenceExpression.getResolvedName();
            UElement operand = (first instanceof UQualifiedReferenceExpression)
                    ? ((UQualifiedReferenceExpression) first).getReceiver()
                    : null;
            
            if (operand instanceof UReferenceExpression) {
                UReferenceExpression type = (UReferenceExpression) operand;
                if (RESOURCE_CLZ_ID.equals(type.getResolvedName())) {
                    if (mIds.containsKey(id)) {
                        if (lhs.equals(mLhs.get(id))) {
                            return;
                        }
                        if (!callOperand.equals(mCallOperands.get(id))) {
                            return;
                        }
                        UCallExpression earlierCall = mIds.get(id);
                        if (!isReachableFrom(method, earlierCall, call)) {
                            return;
                        }
                        Location location = context.getUastLocation(call);
                        Location secondary = context.getUastLocation(earlierCall);
                        secondary.setMessage("First usage here");
                        location.setSecondary(secondary);
                        context.report(ISSUE, call, location, String.format(
                                "The id `%1$s` has already been looked up in this method; possible "
                                        +
                                        "cut & paste error?", first.asSourceString()));
                    } else {
                        mIds.put(id, call);
                        mLhs.put(id, lhs);
                        mCallOperands.put(id, callOperand);
                    }
                }

            }
        }
    }

    @Nullable
    private static String getLhs(@NonNull UCallExpression call) {
        UElement parent = call.getUastParent();
        while (parent != null && !(parent instanceof UBlockExpression)) {
            if (parent instanceof ULocalVariable) {
                return ((ULocalVariable) parent).getName();
            } else if (UastExpressionUtils.isAssignment(parent)) {
                UExpression left = ((UBinaryExpression) parent).getLeftOperand();
                if (left instanceof UReferenceExpression) {
                    return left.asSourceString();
                } else if (left instanceof UArrayAccessExpression) {
                    UArrayAccessExpression aa = (UArrayAccessExpression) left;
                    return aa.getReceiver().asSourceString();
                }
            }
            parent = parent.getUastParent();
        }
        return null;
    }

    static boolean isReachableFrom(
            @NonNull UMethod method,
            @NonNull UElement from,
            @NonNull UElement to) {
        ReachabilityVisitor visitor = new ReachabilityVisitor(from, to);
        method.accept(visitor);
        return visitor.isReachable();
    }

    private static class ReachabilityVisitor extends AbstractUastVisitor {

        private final UElement mFrom;
        private final UElement mTarget;

        private boolean mIsFromReached;
        private boolean mIsTargetReachable;
        private boolean mIsFinished;

        private UExpression mBreakedExpression;
        private UExpression mContinuedExpression;

        ReachabilityVisitor(UElement from, UElement target) {
            mFrom = from;
            mTarget = target;
        }

        @Override
        public boolean visitElement(UElement node) {
            if (mIsFinished || mBreakedExpression != null || mContinuedExpression != null) {
                return true;
            }

            if (node.equals(mFrom)) {
                mIsFromReached = true;
            }

            if (node.equals(mTarget)) {
                mIsFinished = true;
                if (mIsFromReached) {
                    mIsTargetReachable = true;
                }
                return true;
            }

            if (mIsFromReached) {
                if (node instanceof UReturnExpression) {
                    mIsFinished = true;
                } else if (node instanceof UBreakExpression) {
                    mBreakedExpression = getBreakedExpression((UBreakExpression) node);
                } else if (node instanceof UContinueExpression) {
                    UExpression expression = getContinuedExpression((UContinueExpression) node);
                    if (expression != null && UastUtils.isChildOf(mTarget, expression, false)) {
                        mIsTargetReachable = true;
                        mIsFinished = true;
                    } else {
                        mContinuedExpression = expression;
                    }
                } else if (UastUtils.isChildOf(mTarget, node, false)) {
                    mIsTargetReachable = true;
                    mIsFinished = true;
                }
                return true;
            } else {
                if (node instanceof UIfExpression) {
                    UIfExpression ifExpression = (UIfExpression) node;

                    ifExpression.getCondition().accept(this);

                    boolean isFromReached = mIsFromReached;

                    UExpression thenExpression = ifExpression.getThenExpression();
                    if (thenExpression != null) {
                        thenExpression.accept(this);
                    }

                    UExpression elseExpression = ifExpression.getElseExpression();
                    if (elseExpression != null && isFromReached == mIsFromReached) {
                        elseExpression.accept(this);
                    }
                    return true;
                } else if (node instanceof ULoopExpression) {
                    visitLoopExpressionHeader(node);
                    boolean isFromReached = mIsFromReached;

                    ((ULoopExpression) node).getBody().accept(this);

                    if (isFromReached != mIsFromReached
                        && UastUtils.isChildOf(mTarget, node, false)) {
                        mIsTargetReachable = true;
                        mIsFinished = true;
                    }
                    return true;
                }
            }

            return false;
        }

        @Override
        public void afterVisitElement(UElement node) {
            if (node.equals(mBreakedExpression)) {
                mBreakedExpression = null;
            } else if (node.equals(mContinuedExpression)) {
                mContinuedExpression = null;
            }
        }

        private void visitLoopExpressionHeader(UElement node) {
            if (node instanceof UWhileExpression) {
                ((UWhileExpression) node).getCondition().accept(this);
            } else if (node instanceof UDoWhileExpression) {
                ((UDoWhileExpression) node).getCondition().accept(this);
            } else if (node instanceof UForExpression) {
                UForExpression forExpression = (UForExpression) node;

                if (forExpression.getDeclaration() != null) {
                    forExpression.getDeclaration().accept(this);
                }

                if (forExpression.getCondition() != null) {
                    forExpression.getCondition().accept(this);
                }

                if (forExpression.getUpdate() != null) {
                    forExpression.getUpdate().accept(this);
                }
            } else if (node instanceof UForEachExpression) {
                UForEachExpression forEachExpression = (UForEachExpression) node;
                forEachExpression.getForIdentifier().accept(this);
                forEachExpression.getIteratedValue().accept(this);
            }
        }

        private static UExpression getBreakedExpression(UBreakExpression node) {
            UElement parent = node.getUastParent();
            String label = node.getLabel();
            while (parent != null) {
                if (label != null) {
                    if (parent instanceof ULabeledExpression) {
                        ULabeledExpression labeledExpression = (ULabeledExpression) parent;
                        if (labeledExpression.getLabel().equals(label)) {
                            return labeledExpression.getExpression();
                        }
                    }
                } else {
                    if (parent instanceof ULoopExpression || parent instanceof USwitchExpression) {
                        return (UExpression) parent;
                    }
                }
                parent = parent.getUastParent();
            }
            return null;
        }

        private static UExpression getContinuedExpression(UContinueExpression node) {
            UElement parent = node.getUastParent();
            String label = node.getLabel();
            while (parent != null) {
                if (label != null) {
                    if (parent instanceof ULabeledExpression) {
                        ULabeledExpression labeledExpression = (ULabeledExpression) parent;
                        if (labeledExpression.getLabel().equals(label)) {
                            return labeledExpression.getExpression();
                        }
                    }
                } else {
                    if (parent instanceof ULoopExpression) {
                        return (UExpression) parent;
                    }
                }
                parent = parent.getUastParent();
            }
            return null;
        }

        public boolean isReachable() {
            return mIsTargetReachable;
        }
    }
}
