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

import static com.android.SdkConstants.RESOURCE_CLZ_ID;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Maps;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.ast.ArrayAccess;
import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.Cast;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.If;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Select;
import lombok.ast.Statement;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Detector looking for cut & paste issues
 */
public class CutPasteDetector extends Detector implements Detector.JavaScanner {
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

    private Node mLastMethod;
    private Map<String, MethodInvocation> mIds;
    private Map<String, String> mLhs;
    private Map<String, String> mCallOperands;

    /** Constructs a new {@link CutPasteDetector} check */
    public CutPasteDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }

    // ---- Implements JavaScanner ----

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("findViewById"); //$NON-NLS-1$
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation call) {
        String lhs = getLhs(call);
        if (lhs == null) {
            return;
        }

        Node method = JavaContext.findSurroundingMethod(call);
        if (method == null) {
            return;
        } else if (method != mLastMethod) {
            mIds = Maps.newHashMap();
            mLhs = Maps.newHashMap();
            mCallOperands = Maps.newHashMap();
            mLastMethod = method;
        }

        String callOperand = call.astOperand() != null ? call.astOperand().toString() : "";

        Expression first = call.astArguments().first();
        if (first instanceof Select) {
            Select select = (Select) first;
            String id = select.astIdentifier().astValue();
            Expression operand = select.astOperand();
            if (operand instanceof Select) {
                Select type = (Select) operand;
                if (type.astIdentifier().astValue().equals(RESOURCE_CLZ_ID)) {
                    if (mIds.containsKey(id)) {
                        if (lhs.equals(mLhs.get(id))) {
                            return;
                        }
                        if (!callOperand.equals(mCallOperands.get(id))) {
                            return;
                        }
                        MethodInvocation earlierCall = mIds.get(id);
                        if (!isReachableFrom(method, earlierCall, call)) {
                            return;
                        }
                        Location location = context.getLocation(call);
                        Location secondary = context.getLocation(earlierCall);
                        secondary.setMessage("First usage here");
                        location.setSecondary(secondary);
                        context.report(ISSUE, call, location, String.format(
                            "The id `%1$s` has already been looked up in this method; possible " +
                            "cut & paste error?", first.toString()));
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
    private static String getLhs(@NonNull MethodInvocation call) {
        Node parent = call.getParent();
        if (parent instanceof Cast) {
            parent = parent.getParent();
        }

        if (parent instanceof VariableDefinitionEntry) {
            VariableDefinitionEntry vde = (VariableDefinitionEntry) parent;
            return vde.astName().astValue();
        } else if (parent instanceof BinaryExpression) {
            BinaryExpression be = (BinaryExpression) parent;
            Expression left = be.astLeft();
            if (left instanceof VariableReference || left instanceof Select) {
                return be.astLeft().toString();
            } else if (left instanceof ArrayAccess) {
                ArrayAccess aa = (ArrayAccess) left;
                return aa.astOperand().toString();
            }
        }

        return null;
    }

    private static boolean isReachableFrom(
            @NonNull Node method,
            @NonNull MethodInvocation from,
            @NonNull MethodInvocation to) {
        ReachableVisitor visitor = new ReachableVisitor(from, to);
        method.accept(visitor);

        return visitor.isReachable();
    }

    private static class ReachableVisitor extends ForwardingAstVisitor {
        @NonNull private final MethodInvocation mFrom;
        @NonNull private final MethodInvocation mTo;
        private boolean mReachable;
        private boolean mSeenEnd;

        public ReachableVisitor(@NonNull MethodInvocation from, @NonNull MethodInvocation to) {
            mFrom = from;
            mTo = to;
        }

        boolean isReachable() {
            return mReachable;
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (node == mFrom) {
                mReachable = true;
            } else if (node == mTo) {
                mSeenEnd = true;

            }
            return super.visitMethodInvocation(node);
        }

        @Override
        public boolean visitIf(If node) {
            Expression condition = node.astCondition();
            Statement body = node.astStatement();
            Statement elseBody = node.astElseStatement();
            if (condition != null) {
                condition.accept(this);
            }
            if (body != null) {
                boolean wasReachable = mReachable;
                body.accept(this);
                mReachable = wasReachable;
            }
            if (elseBody != null) {
                boolean wasReachable = mReachable;
                elseBody.accept(this);
                mReachable = wasReachable;
            }

            endVisit(node);

            return false;
        }

        @Override
        public boolean visitNode(Node node) {
            return mSeenEnd;
        }
    }
}
