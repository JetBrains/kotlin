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

import static com.android.SdkConstants.CLASS_CONTEXT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.JavaParser.ResolvedField;
import com.android.tools.lint.client.api.JavaParser.ResolvedMethod;
import com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import com.android.tools.lint.client.api.JavaParser.ResolvedVariable;
import com.android.tools.lint.client.api.JavaParser.TypeDescriptor;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.JavaScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import lombok.ast.AstVisitor;
import lombok.ast.BinaryExpression;
import lombok.ast.BinaryOperator;
import lombok.ast.ConstructorInvocation;
import lombok.ast.Expression;
import lombok.ast.ForwardingAstVisitor;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Return;
import lombok.ast.StrictListAccessor;
import lombok.ast.VariableDefinitionEntry;
import lombok.ast.VariableReference;

/**
 * Checks for missing {@code recycle} calls on resources that encourage it, and
 * for missing {@code commit} calls on FragmentTransactions, etc.
 */
public class CleanupDetector extends Detector implements JavaScanner {

    private static final Implementation IMPLEMENTATION = new Implementation(
            CleanupDetector.class,
            Scope.JAVA_FILE_SCOPE);

    /** Problems with missing recycle calls */
    public static final Issue RECYCLE_RESOURCE = Issue.create(
        "Recycle", //$NON-NLS-1$
        "Missing `recycle()` calls",

        "Many resources, such as TypedArrays, VelocityTrackers, etc., " +
        "should be recycled (with a `recycle()` call) after use. This lint check looks " +
        "for missing `recycle()` calls.",

        Category.PERFORMANCE,
        7,
        Severity.WARNING,
            IMPLEMENTATION);

    /** Problems with missing commit calls. */
    public static final Issue COMMIT_FRAGMENT = Issue.create(
            "CommitTransaction", //$NON-NLS-1$
            "Missing `commit()` calls",

            "After creating a `FragmentTransaction`, you typically need to commit it as well",

            Category.CORRECTNESS,
            7,
            Severity.WARNING,
            IMPLEMENTATION);

    // Target method names
    private static final String RECYCLE = "recycle";                                  //$NON-NLS-1$
    private static final String RELEASE = "release";                                  //$NON-NLS-1$
    private static final String OBTAIN = "obtain";                                    //$NON-NLS-1$
    private static final String SHOW = "show";                                        //$NON-NLS-1$
    private static final String ACQUIRE_CPC = "acquireContentProviderClient";         //$NON-NLS-1$
    private static final String OBTAIN_NO_HISTORY = "obtainNoHistory";                //$NON-NLS-1$
    private static final String OBTAIN_ATTRIBUTES = "obtainAttributes";               //$NON-NLS-1$
    private static final String OBTAIN_TYPED_ARRAY = "obtainTypedArray";              //$NON-NLS-1$
    private static final String OBTAIN_STYLED_ATTRIBUTES = "obtainStyledAttributes";  //$NON-NLS-1$
    private static final String BEGIN_TRANSACTION = "beginTransaction";               //$NON-NLS-1$
    private static final String COMMIT = "commit";                                    //$NON-NLS-1$
    private static final String COMMIT_ALLOWING_LOSS = "commitAllowingStateLoss";     //$NON-NLS-1$
    private static final String QUERY = "query";                                      //$NON-NLS-1$
    private static final String RAW_QUERY = "rawQuery";                               //$NON-NLS-1$
    private static final String QUERY_WITH_FACTORY = "queryWithFactory";              //$NON-NLS-1$
    private static final String RAW_QUERY_WITH_FACTORY = "rawQueryWithFactory";       //$NON-NLS-1$
    private static final String CLOSE = "close";                                      //$NON-NLS-1$

    private static final String MOTION_EVENT_CLS = "android.view.MotionEvent";        //$NON-NLS-1$
    private static final String RESOURCES_CLS = "android.content.res.Resources";      //$NON-NLS-1$
    private static final String PARCEL_CLS = "android.os.Parcel";                     //$NON-NLS-1$
    private static final String TYPED_ARRAY_CLS = "android.content.res.TypedArray";   //$NON-NLS-1$
    private static final String VELOCITY_TRACKER_CLS = "android.view.VelocityTracker";//$NON-NLS-1$
    private static final String DIALOG_FRAGMENT = "android.app.DialogFragment";       //$NON-NLS-1$
    private static final String DIALOG_V4_FRAGMENT =
            "android.support.v4.app.DialogFragment";                                  //$NON-NLS-1$
    private static final String FRAGMENT_MANAGER_CLS = "android.app.FragmentManager"; //$NON-NLS-1$
    private static final String FRAGMENT_MANAGER_V4_CLS =
            "android.support.v4.app.FragmentManager";                                 //$NON-NLS-1$
    private static final String FRAGMENT_TRANSACTION_CLS =
            "android.app.FragmentTransaction";                                        //$NON-NLS-1$
    private static final String FRAGMENT_TRANSACTION_V4_CLS =
            "android.support.v4.app.FragmentTransaction";                             //$NON-NLS-1$

    public static final String SURFACE_CLS = "android.view.Surface";
    public static final String SURFACE_TEXTURE_CLS = "android.graphics.SurfaceTexture";

    public static final String CONTENT_PROVIDER_CLIENT_CLS
            = "android.content.ContentProviderClient";

    public static final String CONTENT_RESOLVER_CLS = "android.content.ContentResolver";
    public static final String CONTENT_PROVIDER_CLS = "android.content.ContentProvider";
    @SuppressWarnings("SpellCheckingInspection")
    public static final String SQLITE_DATABASE_CLS = "android.database.sqlite.SQLiteDatabase";
    public static final String CURSOR_CLS = "android.database.Cursor";

    /** Constructs a new {@link CleanupDetector} */
    public CleanupDetector() {
    }

    // ---- Implements JavaScanner ----

    @Nullable
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList(
                // FragmentManager commit check
                BEGIN_TRANSACTION,

                // Recycle check
                OBTAIN, OBTAIN_NO_HISTORY,
                OBTAIN_STYLED_ATTRIBUTES,
                OBTAIN_ATTRIBUTES,
                OBTAIN_TYPED_ARRAY,

                // Release check
                ACQUIRE_CPC,

                // Cursor close check
                QUERY, RAW_QUERY, QUERY_WITH_FACTORY, RAW_QUERY_WITH_FACTORY
        );
    }

    @Nullable
    @Override
    public List<String> getApplicableConstructorTypes() {
        return Arrays.asList(SURFACE_TEXTURE_CLS, SURFACE_CLS);
    }

    @Override
    public void visitMethod(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull MethodInvocation node) {

        String name = node.astName().astValue();
        if (BEGIN_TRANSACTION.equals(name)) {
            checkTransactionCommits(context, node);
        } else {
            checkResourceRecycled(context, node, name);
        }
    }

    @Override
    public void visitConstructor(@NonNull JavaContext context, @Nullable AstVisitor visitor,
            @NonNull ConstructorInvocation node, @NonNull ResolvedMethod constructor) {
        checkRecycled(context, node, constructor.getContainingClass().getSignature(), RELEASE);
    }

    private static void checkResourceRecycled(@NonNull JavaContext context,
            @NonNull MethodInvocation node, @NonNull String name) {
        // Recycle detector
        ResolvedNode resolved = context.resolve(node);
        if (!(resolved instanceof ResolvedMethod)) {
            return;
        }
        ResolvedMethod method = (ResolvedMethod) resolved;
        ResolvedClass containingClass = method.getContainingClass();
        if ((OBTAIN.equals(name) || OBTAIN_NO_HISTORY.equals(name)) &&
                containingClass.isSubclassOf(MOTION_EVENT_CLS, false)) {
            checkRecycled(context, node, MOTION_EVENT_CLS, RECYCLE);
        } else if (OBTAIN.equals(name) && containingClass.isSubclassOf(PARCEL_CLS, false)) {
            checkRecycled(context, node, PARCEL_CLS, RECYCLE);
        } else if (OBTAIN.equals(name) &&
                containingClass.isSubclassOf(VELOCITY_TRACKER_CLS, false)) {
            checkRecycled(context, node, VELOCITY_TRACKER_CLS, RECYCLE);
        } else if ((OBTAIN_STYLED_ATTRIBUTES.equals(name)
                || OBTAIN_ATTRIBUTES.equals(name)
                || OBTAIN_TYPED_ARRAY.equals(name)) &&
                (containingClass.isSubclassOf(CLASS_CONTEXT, false) ||
                        containingClass.isSubclassOf(RESOURCES_CLS, false))) {
            TypeDescriptor returnType = method.getReturnType();
            if (returnType != null && returnType.matchesSignature(TYPED_ARRAY_CLS)) {
                checkRecycled(context, node, TYPED_ARRAY_CLS, RECYCLE);
            }
        } else if (ACQUIRE_CPC.equals(name) && containingClass.isSubclassOf(
                CONTENT_RESOLVER_CLS, false)) {
            checkRecycled(context, node, CONTENT_PROVIDER_CLIENT_CLS, RELEASE);
        } else if ((QUERY.equals(name)
                || RAW_QUERY.equals(name)
                || QUERY_WITH_FACTORY.equals(name)
                || RAW_QUERY_WITH_FACTORY.equals(name))
                && (containingClass.isSubclassOf(SQLITE_DATABASE_CLS, false) ||
                    containingClass.isSubclassOf(CONTENT_RESOLVER_CLS, false) ||
                    containingClass.isSubclassOf(CONTENT_PROVIDER_CLS, false) ||
                    containingClass.isSubclassOf(CONTENT_PROVIDER_CLIENT_CLS, false))) {
            // Other potential cursors-returning methods that should be tracked:
            //    android.app.DownloadManager#query
            //    android.content.ContentProviderClient#query
            //    android.content.ContentResolver#query
            //    android.database.sqlite.SQLiteQueryBuilder#query
            //    android.provider.Browser#getAllBookmarks
            //    android.provider.Browser#getAllVisitedUrls
            //    android.provider.DocumentsProvider#queryChildDocuments
            //    android.provider.DocumentsProvider#qqueryDocument
            //    android.provider.DocumentsProvider#queryRecentDocuments
            //    android.provider.DocumentsProvider#queryRoots
            //    android.provider.DocumentsProvider#querySearchDocuments
            //    android.provider.MediaStore$Images$Media#query
            //    android.widget.FilterQueryProvider#runQuery
            checkRecycled(context, node, CURSOR_CLS, CLOSE);
        }
    }

    private static void checkRecycled(@NonNull final JavaContext context, @NonNull Node node,
            @NonNull final String recycleType, @NonNull final String recycleName) {
        ResolvedVariable boundVariable = getVariable(context, node);
        if (boundVariable == null) {
            return;
        }

        Node method = JavaContext.findSurroundingMethod(node);
        if (method == null) {
            return;
        }

        FinishVisitor visitor = new FinishVisitor(context, boundVariable) {
            @Override
            protected boolean isCleanupCall(@NonNull MethodInvocation call) {
                String methodName = call.astName().astValue();
                if (!recycleName.equals(methodName)) {
                    return false;
                }
                ResolvedNode resolved = mContext.resolve(call);
                if (resolved instanceof ResolvedMethod) {
                    ResolvedClass containingClass = ((ResolvedMethod) resolved).getContainingClass();
                    if (containingClass.isSubclassOf(recycleType, false)) {
                        // Yes, called the right recycle() method; now make sure
                        // we're calling it on the right variable
                        Expression operand = call.astOperand();
                        if (operand != null) {
                            resolved = mContext.resolve(operand);
                            //noinspection SuspiciousMethodCalls
                            if (resolved != null && mVariables.contains(resolved)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        };

        method.accept(visitor);
        if (visitor.isCleanedUp() || visitor.variableEscapes()) {
            return;
        }

        String className = recycleType.substring(recycleType.lastIndexOf('.') + 1);
        String message;
        if (RECYCLE.equals(recycleName)) {
            message = String.format(
                    "This `%1$s` should be recycled after use with `#recycle()`", className);
        } else {
            message = String.format(
                    "This `%1$s` should be freed up after use with `#%2$s()`", className,
                    recycleName);
        }
        Node locationNode = node instanceof MethodInvocation ?
                ((MethodInvocation) node).astName() : node;
        Location location = context.getLocation(locationNode);
        context.report(RECYCLE_RESOURCE, node, location, message);
    }

    private static boolean checkTransactionCommits(@NonNull JavaContext context,
            @NonNull MethodInvocation node) {
        if (isBeginTransaction(context, node)) {
            ResolvedVariable boundVariable = getVariable(context, node);
            if (boundVariable == null && isCommittedInChainedCalls(context, node)) {
                return true;
            }

            if (boundVariable != null) {
                Node method = JavaContext.findSurroundingMethod(node);
                if (method == null) {
                    return true;
                }

                FinishVisitor commitVisitor = new FinishVisitor(context, boundVariable) {
                    @Override
                    protected boolean isCleanupCall(@NonNull MethodInvocation call) {
                        if (isTransactionCommitMethodCall(mContext, call)) {
                            Expression operand = call.astOperand();
                            if (operand != null) {
                                ResolvedNode resolved = mContext.resolve(operand);
                                //noinspection SuspiciousMethodCalls
                                if (resolved != null && mVariables.contains(resolved)) {
                                    return true;
                                } else if (resolved instanceof ResolvedMethod
                                        && operand instanceof MethodInvocation
                                        && isCommittedInChainedCalls(mContext,(MethodInvocation) operand)) {
                                    // Check that the target of the committed chains is the
                                    // right variable!
                                    while (operand instanceof MethodInvocation) {
                                        operand = ((MethodInvocation)operand).astOperand();
                                    }
                                    if (operand instanceof VariableReference) {
                                        resolved = mContext.resolve(operand);
                                        //noinspection SuspiciousMethodCalls
                                        if (resolved != null && mVariables.contains(resolved)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        } else if (isShowFragmentMethodCall(mContext, call)) {
                            StrictListAccessor<Expression, MethodInvocation> arguments =
                                    call.astArguments();
                            if (arguments.size() == 2) {
                                Expression first = arguments.first();
                                ResolvedNode resolved = mContext.resolve(first);
                                //noinspection SuspiciousMethodCalls
                                if (resolved != null && mVariables.contains(resolved)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                };

                method.accept(commitVisitor);
                if (commitVisitor.isCleanedUp() || commitVisitor.variableEscapes()) {
                    return true;
                }
            }

            String message = "This transaction should be completed with a `commit()` call";
            context.report(COMMIT_FRAGMENT, node, context.getLocation(node.astName()),
                    message);
        }
        return false;
    }

    private static boolean isCommittedInChainedCalls(@NonNull JavaContext context,
            @NonNull MethodInvocation node) {
        // Look for chained calls since the FragmentManager methods all return "this"
        // to allow constructor chaining, e.g.
        //    getFragmentManager().beginTransaction().addToBackStack("test")
        //            .disallowAddToBackStack().hide(mFragment2).setBreadCrumbShortTitle("test")
        //            .show(mFragment2).setCustomAnimations(0, 0).commit();
        Node parent = node.getParent();
        while (parent instanceof MethodInvocation) {
            MethodInvocation methodInvocation = (MethodInvocation) parent;
            if (isTransactionCommitMethodCall(context, methodInvocation)
                    || isShowFragmentMethodCall(context, methodInvocation)) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }

    private static boolean isTransactionCommitMethodCall(@NonNull JavaContext context,
            @NonNull MethodInvocation call) {

        String methodName = call.astName().astValue();
        return (COMMIT.equals(methodName) || COMMIT_ALLOWING_LOSS.equals(methodName)) &&
                isMethodOnFragmentClass(context, call,
                        FRAGMENT_TRANSACTION_CLS,
                        FRAGMENT_TRANSACTION_V4_CLS);
    }

    private static boolean isShowFragmentMethodCall(@NonNull JavaContext context,
            @NonNull MethodInvocation call) {
        String methodName = call.astName().astValue();
        return SHOW.equals(methodName)
                && isMethodOnFragmentClass(context, call,
                DIALOG_FRAGMENT, DIALOG_V4_FRAGMENT);
    }

    private static boolean isMethodOnFragmentClass(
            @NonNull JavaContext context,
            @NonNull MethodInvocation call,
            @NonNull String fragmentClass,
            @NonNull String v4FragmentClass) {
        ResolvedNode resolved = context.resolve(call);
        if (resolved instanceof ResolvedMethod) {
            ResolvedClass containingClass = ((ResolvedMethod) resolved).getContainingClass();
            return containingClass.isSubclassOf(fragmentClass, false) ||
                    containingClass.isSubclassOf(v4FragmentClass, false);
        }

        return false;
    }

    @Nullable
    public static ResolvedVariable getVariable(@NonNull JavaContext context,
            @NonNull Node expression) {
        Node parent = expression.getParent();
        if (parent instanceof BinaryExpression) {
            BinaryExpression binaryExpression = (BinaryExpression) parent;
            if (binaryExpression.astOperator() == BinaryOperator.ASSIGN) {
                Expression lhs = binaryExpression.astLeft();
                ResolvedNode resolved = context.resolve(lhs);
                if (resolved instanceof ResolvedVariable) {
                    return (ResolvedVariable) resolved;
                }
            }
        } else if (parent instanceof VariableDefinitionEntry) {
            ResolvedNode resolved = context.resolve(parent);
            if (resolved instanceof ResolvedVariable) {
                return (ResolvedVariable) resolved;
            }
        }

        return null;
    }

    private static boolean isBeginTransaction(@NonNull JavaContext context,
            @NonNull MethodInvocation node) {
        String methodName = node.astName().astValue();
        assert methodName.equals(BEGIN_TRANSACTION) : methodName;
        if (BEGIN_TRANSACTION.equals(methodName)) {
            ResolvedNode resolved = context.resolve(node);
            if (resolved instanceof ResolvedMethod) {
                ResolvedMethod method = (ResolvedMethod) resolved;
                ResolvedClass containingClass = method.getContainingClass();
                if (containingClass.isSubclassOf(FRAGMENT_MANAGER_CLS, false)
                        || containingClass.isSubclassOf(FRAGMENT_MANAGER_V4_CLS,
                        false)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Visitor which checks whether an operation is "finished"; in the case
     * of a FragmentTransaction we're looking for a "commit" call; in the
     * case of a TypedArray we're looking for a "recycle", call, in the
     * case of a database cursor we're looking for a "close" call, etc.
     */
    private abstract static class FinishVisitor extends ForwardingAstVisitor {
        protected final JavaContext mContext;
        protected final List<ResolvedVariable> mVariables;
        private boolean mContainsCleanup;
        private boolean mEscapes;

        public FinishVisitor(JavaContext context, @NonNull ResolvedVariable variable) {
            mContext = context;
            mVariables = Lists.newArrayList(variable);
        }

        public boolean isCleanedUp() {
            return mContainsCleanup;
        }

        public boolean variableEscapes() {
            return mEscapes;
        }

        @Override
        public boolean visitNode(Node node) {
            return mContainsCleanup || super.visitNode(node);
        }

        protected abstract boolean isCleanupCall(@NonNull MethodInvocation call);

        @Override
        public boolean visitMethodInvocation(MethodInvocation call) {
            if (mContainsCleanup) {
                return true;
            }

            super.visitMethodInvocation(call);

            // Look for escapes
            if (!mEscapes) {
                for (Expression expression : call.astArguments()) {
                    if (expression instanceof VariableReference) {
                        ResolvedNode resolved = mContext.resolve(expression);
                        //noinspection SuspiciousMethodCalls
                        if (resolved != null && mVariables.contains(resolved)) {
                            mEscapes = true;

                            // Special case: MotionEvent.obtain(MotionEvent): passing in an
                            // event here does not recycle the event, and we also know it
                            // doesn't escape
                            if (OBTAIN.equals(call.astName().astValue())) {
                                ResolvedNode r = mContext.resolve(call);
                                if (r instanceof ResolvedMethod) {
                                    ResolvedMethod method = (ResolvedMethod) r;
                                    ResolvedClass cls = method.getContainingClass();
                                    if (cls.matches(MOTION_EVENT_CLS)) {
                                        mEscapes = false;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isCleanupCall(call)) {
                mContainsCleanup = true;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public boolean visitVariableDefinitionEntry(VariableDefinitionEntry node) {
            Expression initializer = node.astInitializer();
            if (initializer instanceof VariableReference) {
                ResolvedNode resolved = mContext.resolve(initializer);
                //noinspection SuspiciousMethodCalls
                if (resolved != null && mVariables.contains(resolved)) {
                    ResolvedNode resolvedVariable = mContext.resolve(node);
                    if (resolvedVariable instanceof ResolvedVariable) {
                        ResolvedVariable variable = (ResolvedVariable) resolvedVariable;
                        mVariables.add(variable);
                    } else if (resolvedVariable instanceof ResolvedField) {
                        mEscapes = true;
                    }
                }
            }
            return super.visitVariableDefinitionEntry(node);
        }

        @Override
        public boolean visitBinaryExpression(BinaryExpression node) {
            if (node.astOperator() == BinaryOperator.ASSIGN) {
                Expression rhs = node.astRight();
                if (rhs instanceof VariableReference) {
                    ResolvedNode resolved = mContext.resolve(rhs);
                    //noinspection SuspiciousMethodCalls
                    if (resolved != null && mVariables.contains(resolved)) {
                        ResolvedNode resolvedLhs = mContext.resolve(node.astLeft());
                        if (resolvedLhs instanceof ResolvedVariable) {
                            ResolvedVariable variable = (ResolvedVariable) resolvedLhs;
                            mVariables.add(variable);
                        } else if (resolvedLhs instanceof ResolvedField) {
                            mEscapes = true;
                        }
                    }
                }
            }
            return super.visitBinaryExpression(node);
        }

        @Override
        public boolean visitReturn(Return node) {
            Expression value = node.astValue();
            if (value instanceof VariableReference) {
                ResolvedNode resolved = mContext.resolve(value);
                //noinspection SuspiciousMethodCalls
                if (resolved != null && mVariables.contains(resolved)) {
                    mEscapes = true;
                }
            }

            return super.visitReturn(node);
        }
    }
}
