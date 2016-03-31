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

package com.android.tools.lint.checks;

import static com.android.SdkConstants.VIEW;
import static com.android.SdkConstants.VIEW_GROUP;
import static com.android.tools.lint.client.api.JavaParser.TYPE_INT;

import com.android.annotations.NonNull;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.If;
import lombok.ast.InlineIfExpression;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.StrictListAccessor;
import lombok.ast.Switch;
import lombok.ast.VariableDefinition;

/**
 * Looks for ListView scrolling performance: should use view holder pattern
 */
public class ViewHolderDetector extends Detector implements Detector.JavaScanner {

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

    // ---- Implements JavaScanner ----

    @Override
    public List<Class<? extends Node>> getApplicableNodeTypes() {
        return Collections.<Class<? extends Node>>singletonList(MethodDeclaration.class);
    }

    @Override
    public AstVisitor createJavaVisitor(@NonNull JavaContext context) {
        return new ViewAdapterVisitor(context);
    }

    private static class ViewAdapterVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;

        public ViewAdapterVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitMethodDeclaration(MethodDeclaration node) {
            if (isViewAdapterMethod(node)) {
                InflationVisitor visitor = new InflationVisitor(mContext);
                node.accept(visitor);
                visitor.finish();
            }
            return super.visitMethodDeclaration(node);
        }

        /**
         * Returns true if this method looks like it's overriding android.widget.Adapter's getView
         * method: getView(int position, View convertView, ViewGroup parent)
         */
        private static boolean isViewAdapterMethod(MethodDeclaration node) {
            if (GET_VIEW.equals(node.astMethodName().astValue())) {
                StrictListAccessor<VariableDefinition, MethodDeclaration> parameters =
                        node.astParameters();
                if (parameters != null && parameters.size() == 3) {
                    Iterator<VariableDefinition> iterator = parameters.iterator();
                    if (!iterator.hasNext()) {
                        return false;
                    }

                    VariableDefinition first = iterator.next();
                    if (!first.astTypeReference().astParts().last().getTypeName().equals(
                            TYPE_INT)) {
                        return false;
                    }

                    if (!iterator.hasNext()) {
                        return false;
                    }

                    VariableDefinition second = iterator.next();
                    if (!second.astTypeReference().astParts().last().getTypeName().equals(
                            VIEW)) {
                        return false;
                    }

                    if (!iterator.hasNext()) {
                        return false;
                    }

                    VariableDefinition third = iterator.next();
                    //noinspection RedundantIfStatement
                    if (!third.astTypeReference().astParts().last().getTypeName().equals(
                            VIEW_GROUP)) {
                        return false;
                    }

                    return true;
                }
            }

            return false;
        }
    }

    private static class InflationVisitor extends ForwardingAstVisitor {
        private final JavaContext mContext;
        private List<Node> mNodes;
        private boolean mHaveConditional;

        public InflationVisitor(JavaContext context) {
            mContext = context;
        }

        @Override
        public boolean visitMethodInvocation(MethodInvocation node) {
            if (node.astOperand() != null) {
                String methodName = node.astName().astValue();
                if (methodName.equals(INFLATE) && node.astArguments().size() >= 1) {
                    // See if we're inside a conditional
                    boolean insideIf = false;
                    Node p = node.getParent();
                    while (p != null) {
                        if (p instanceof If || p instanceof InlineIfExpression
                                || p instanceof Switch) {
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

            return super.visitMethodInvocation(node);
        }

        public void finish() {
            if (!mHaveConditional && mNodes != null) {
                for (Node node : mNodes) {
                    String message = "Unconditional layout inflation from view adapter: "
                            + "Should use View Holder pattern (use recycled view passed "
                            + "into this method as the second parameter) for smoother "
                            + "scrolling";
                    mContext.report(ISSUE, node, mContext.getLocation(node), message);
                }
            }
        }
    }
}
