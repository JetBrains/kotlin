/*
 * Copyright (C) 2015 The Android Open Source Project
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


import static com.android.tools.klint.checks.CutPasteDetector.isReachableFrom;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.JavaEvaluator;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.JavaContext;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLocalVariable;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;

import org.jetbrains.uast.*;
import org.jetbrains.uast.util.UastExpressionUtils;
import org.jetbrains.uast.visitor.AbstractUastVisitor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Checks related to RecyclerView usage.
 */
public class RecyclerViewDetector extends Detector implements Detector.UastScanner {

    public static final Implementation IMPLEMENTATION = new Implementation(
            RecyclerViewDetector.class,
            Scope.JAVA_FILE_SCOPE);

    public static final Issue FIXED_POSITION = Issue.create(
            "RecyclerView", //$NON-NLS-1$
            "RecyclerView Problems",
            "`RecyclerView` will *not* call `onBindViewHolder` again when the position of " +
            "the item changes in the data set unless the item itself is " +
            "invalidated or the new position cannot be determined.\n" +
            "\n" +
            "For this reason, you should *only* use the position parameter " +
            "while acquiring the related data item inside this method, and " +
            "should *not* keep a copy of it.\n" +
            "\n" +
            "If you need the position of an item later on (e.g. in a click " +
            "listener), use `getAdapterPosition()` which will have the updated " +
            "adapter position.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    public static final Issue DATA_BINDER = Issue.create(
            "PendingBindings", //$NON-NLS-1$
            "Missing Pending Bindings",
            "When using a `ViewDataBinding` in a `onBindViewHolder` method, you *must* " +
            "call `executePendingBindings()` before the method exits; otherwise " +
            "the data binding runtime will update the UI in the next animation frame " +
            "causing a delayed update and potential jumps if the item resizes.",
            Category.CORRECTNESS,
            8,
            Severity.ERROR,
            IMPLEMENTATION);

    private static final String VIEW_ADAPTER = "android.support.v7.widget.RecyclerView.Adapter"; //$NON-NLS-1$
    private static final String ON_BIND_VIEW_HOLDER = "onBindViewHolder"; //$NON-NLS-1$

    // ---- Implements UastScanner ----

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        return Collections.singletonList(VIEW_ADAPTER);
    }

    @Override
    public void checkClass(@NonNull JavaContext context, @NonNull UClass declaration) {
        JavaEvaluator evaluator = context.getEvaluator();
        for (PsiMethod method : declaration.findMethodsByName(ON_BIND_VIEW_HOLDER, false)) {
            int size = evaluator.getParameterCount(method);
            if (size == 2 || size == 3) {
                checkMethod(context, method, declaration);
            }
        }
        
        super.checkClass(context, declaration);
    }

    private static void checkMethod(@NonNull JavaContext context,
            @NonNull PsiMethod declaration, @NonNull PsiClass cls) {
        PsiParameter[] parameters = declaration.getParameterList().getParameters();
        PsiParameter viewHolder = parameters[0];
        PsiParameter parameter = parameters[1];

        ParameterEscapesVisitor visitor = new ParameterEscapesVisitor(context, cls, parameter);
        UMethod method = context.getUastContext().getMethod(declaration);
        method.accept(visitor);
        if (visitor.variableEscapes()) {
            reportError(context, viewHolder, parameter);
        }

        // Look for pending data binder calls that aren't executed before the method finishes
        List<UCallExpression> dataBinderReferences = visitor.getDataBinders();
        checkDataBinders(context, method, dataBinderReferences);
    }

    private static void reportError(@NonNull JavaContext context, PsiParameter viewHolder,
            PsiParameter parameter) {
        String variablePrefix = viewHolder.getName();
        if (variablePrefix == null) {
            variablePrefix = "ViewHolder";
        }
        String message = String.format("Do not treat position as fixed; only use immediately "
                + "and call `%1$s.getAdapterPosition()` to look it up later",
                variablePrefix);
        context.report(FIXED_POSITION, parameter, context.getLocation(parameter),
                message);
    }

    private static void checkDataBinders(@NonNull JavaContext context,
            @NonNull UMethod declaration, List<UCallExpression> references) {
        if (references != null && !references.isEmpty()) {
            List<UCallExpression> targets = Lists.newArrayList();
            List<UCallExpression> sources = Lists.newArrayList();
            for (UCallExpression ref : references) {
                if (isExecutePendingBindingsCall(ref)) {
                    targets.add(ref);
                } else {
                    sources.add(ref);
                }
            }

            // Only operate on the last call in each block: ignore siblings with the same parent
            // That way if you have
            //     dataBinder.foo();
            //     dataBinder.bar();
            //     dataBinder.baz();
            // we only flag the *last* of these calls as needing an executePendingBindings
            // afterwards. We do this with a parent map such that we correctly pair
            // elements when they have nested references within (such as if blocks.)
            Map<UElement, UCallExpression> parentToChildren = Maps.newHashMap();
            for (UCallExpression reference : sources) {
                // Note: We're using a map, not a multimap, and iterating forwards:
                // this means that the *last* element will overwrite previous entries,
                // and we end up with the last reference for each parent which is what we
                // want
                UExpression statement = UastUtils.getParentOfType(reference, UExpression.class, true);
                if (statement != null) {
                    parentToChildren.put(statement.getContainingElement(), reference);
                }
            }

            for (UCallExpression source : parentToChildren.values()) {
                UExpression sourceBinderReference = source.getReceiver();
                PsiField sourceDataBinder = getDataBinderReference(sourceBinderReference);
                assert sourceDataBinder != null;

                boolean reachesTarget = false;
                for (UCallExpression target : targets) {
                    if (sourceDataBinder.equals(getDataBinderReference(target.getReceiver()))
                            // TODO: Provide full control flow graph, or at least provide an
                            // isReachable method which can take multiple targets
                            && isReachableFrom(declaration, source, target)) {
                        reachesTarget = true;
                        break;
                    }
                }
                if (!reachesTarget) {
                    String message = String.format(
                            "You must call `%1$s.executePendingBindings()` "
                                + "before the `onBind` method exits, otherwise, the DataBinding "
                                + "library will update the UI in the next animation frame "
                                + "causing a delayed update & potential jumps if the item "
                                + "resizes.",
                            sourceBinderReference.asSourceString());
                    context.report(DATA_BINDER, source, context.getUastLocation(source), message);
                }
            }
        }
    }

    private static boolean isExecutePendingBindingsCall(UCallExpression call) {
        return "executePendingBindings".equals(call.getMethodName());
    }

    @Nullable
    private static PsiField getDataBinderReference(@Nullable UExpression element) {
        if (element instanceof UReferenceExpression) {
            PsiElement resolved = ((UReferenceExpression) element).resolve();
            if (resolved instanceof PsiField) {
                PsiField field = (PsiField) resolved;
                if ("dataBinder".equals(field.getName())) {
                    return field;
                }
            }
        }

        return null;
    }

    /**
     * Determines whether a given variable "escapes" either to a field or to a nested
     * runnable. (We deliberately ignore variables that escape via method calls.)
     */
    private static class ParameterEscapesVisitor extends AbstractUastVisitor {
        protected final JavaContext mContext;
        private final List<PsiVariable> mVariables;
        private final PsiClass mBindClass;
        private boolean mEscapes;
        private boolean mFoundInnerClass;

        private ParameterEscapesVisitor(JavaContext context,
                @NonNull PsiClass bindClass,
                @NonNull PsiParameter variable) {
            mContext = context;
            mVariables = Lists.<PsiVariable>newArrayList(variable);
            mBindClass = bindClass;
        }

        private boolean variableEscapes() {
            return mEscapes;
        }

        @Override
        public boolean visitVariable(UVariable variable) {
            UExpression initializer = variable.getUastInitializer();
            if (initializer instanceof UReferenceExpression) {
                PsiElement resolved = ((UReferenceExpression) initializer).resolve();
                //noinspection SuspiciousMethodCalls
                if (resolved != null && mVariables.contains(resolved)) {
                    if (resolved instanceof PsiLocalVariable) {
                        mVariables.add(variable);
                    } else if (resolved instanceof PsiField) {
                        mEscapes = true;
                    }
                }
            }
            
            return super.visitVariable(variable);
        }

        @Override
        public boolean visitBinaryExpression(UBinaryExpression node) {
            if (node.getOperator() instanceof UastBinaryOperator.AssignOperator) {
                UExpression rhs = node.getRightOperand();
                boolean clearLhs = true;
                if (rhs instanceof UReferenceExpression) {
                    PsiElement resolved = ((UReferenceExpression) rhs).resolve();
                    //noinspection SuspiciousMethodCalls
                    if (resolved != null && mVariables.contains(resolved)) {
                        clearLhs = false;
                        PsiElement resolvedLhs = UastUtils.tryResolve(node.getLeftOperand());
                        if (resolvedLhs instanceof PsiLocalVariable) {
                            PsiLocalVariable variable = (PsiLocalVariable) resolvedLhs;
                            mVariables.add(variable);
                        } else if (resolvedLhs instanceof PsiField) {
                            mEscapes = true;
                        }
                    }
                }
                if (clearLhs) {
                    // If we reassign one of the variables, clear it out
                    PsiElement resolved = UastUtils.tryResolve(node.getLeftOperand());
                    //noinspection SuspiciousMethodCalls
                    if (resolved != null && mVariables.contains(resolved)) {
                        //noinspection SuspiciousMethodCalls
                        mVariables.remove(resolved);
                    }
                }
            }
            return super.visitBinaryExpression(node);
        }

        @Override
        public boolean visitSimpleNameReferenceExpression(USimpleNameReferenceExpression node) {
            if (mFoundInnerClass) {
                // Check to see if this reference is inside the same class as the original
                // onBind (e.g. is this a reference from an inner class, or a reference
                // to a variable assigned from there)
                PsiElement resolved = node.resolve();
                //noinspection SuspiciousMethodCalls
                if (resolved != null && mVariables.contains(resolved)) {
                    PsiClass outer = UastUtils.getParentOfType(node, UClass.class, true);
                    if (!mBindClass.equals(outer)) {
                        mEscapes = true;
                    }
                }
            }
            
            return super.visitSimpleNameReferenceExpression(node);
        }

        @Override
        public boolean visitClass(UClass node) {
            if (node instanceof UAnonymousClass || !node.isStatic()) {
                mFoundInnerClass = true;
            }

            return super.visitClass(node);
        }

        // Also look for data binder references

        private List<UCallExpression> mDataBinders = null;

        @Nullable
        private List<UCallExpression> getDataBinders() {
            return mDataBinders;
        }

        @Override
        public boolean visitCallExpression(UCallExpression expression) {
            if (UastExpressionUtils.isMethodCall(expression)) {
                UExpression methodExpression = expression.getReceiver();
                PsiField dataBinder = getDataBinderReference(methodExpression);
                //noinspection VariableNotUsedInsideIf
                if (dataBinder != null) {
                    if (mDataBinders == null) {
                        mDataBinders = Lists.newArrayList();
                    }
                    mDataBinders.add(expression);
                }
            }
            
            return super.visitCallExpression(expression);
        }
    }
}
