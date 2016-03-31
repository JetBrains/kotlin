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

import static com.android.SdkConstants.CLASS_CONTEXT;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.Location;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;
import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Checks for missing {@code recycle} calls on resources that encourage it, and
 * for missing {@code commit} calls on FragmentTransactions, etc.
 */
public class CleanupDetector extends Detector implements UastScanner {

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

    // ---- Implements UastScanner ----


    @Override
    public List<String> getApplicableFunctionNames() {
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
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        if (node.functionNameMatches(BEGIN_TRANSACTION)) {
            checkTransactionCommits(context, node);
        } else {
            checkResourceRecycled(context, node, node.getFunctionName());
        }
    }

    @Override
    public void visitConstructor(UastAndroidContext context, UCallExpression functionCall, UFunction constructor) {
        UClass containingClass = UastUtils.getContainingClass(constructor);
        if (containingClass != null) {
            checkRecycled(context, functionCall, containingClass.getFqName(), RELEASE);
        }
    }

    private static void checkResourceRecycled(@NonNull UastAndroidContext context,
            @NonNull UCallExpression node, @NonNull String name) {
        // Recycle detector
        UFunction method = node.resolve(context);
        if (method == null) {
            return;
        }
        UClass containingClass = UastUtils.getContainingClass(method);
        if (containingClass == null) {
            return;
        }

        if ((OBTAIN.equals(name) || OBTAIN_NO_HISTORY.equals(name)) &&
                containingClass.isSubclassOf(MOTION_EVENT_CLS)) {
            checkRecycled(context, node, MOTION_EVENT_CLS, RECYCLE);
        } else if (OBTAIN.equals(name) && containingClass.isSubclassOf(PARCEL_CLS)) {
            checkRecycled(context, node, PARCEL_CLS, RECYCLE);
        } else if (OBTAIN.equals(name) &&
                containingClass.isSubclassOf(VELOCITY_TRACKER_CLS)) {
            checkRecycled(context, node, VELOCITY_TRACKER_CLS, RECYCLE);
        } else if ((OBTAIN_STYLED_ATTRIBUTES.equals(name)
                || OBTAIN_ATTRIBUTES.equals(name)
                || OBTAIN_TYPED_ARRAY.equals(name)) &&
                (containingClass.isSubclassOf(CLASS_CONTEXT) ||
                        containingClass.isSubclassOf(RESOURCES_CLS))) {
            UType returnType = method.getReturnType();
            if (returnType != null && returnType.matchesFqName(TYPED_ARRAY_CLS)) {
                checkRecycled(context, node, TYPED_ARRAY_CLS, RECYCLE);
            }
        } else if (ACQUIRE_CPC.equals(name) && containingClass.isSubclassOf(
                CONTENT_RESOLVER_CLS)) {
            checkRecycled(context, node, CONTENT_PROVIDER_CLIENT_CLS, RELEASE);
        } else if ((QUERY.equals(name)
                || RAW_QUERY.equals(name)
                || QUERY_WITH_FACTORY.equals(name)
                || RAW_QUERY_WITH_FACTORY.equals(name))
                && (containingClass.isSubclassOf(SQLITE_DATABASE_CLS) ||
                    containingClass.isSubclassOf(CONTENT_RESOLVER_CLS) ||
                    containingClass.isSubclassOf(CONTENT_PROVIDER_CLS) ||
                    containingClass.isSubclassOf(CONTENT_PROVIDER_CLIENT_CLS))) {
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

    private static void checkRecycled(@NonNull final UastAndroidContext context, @NonNull UElement node,
            @NonNull final String recycleType, @NonNull final String recycleName) {
        UVariable boundVariable = getVariable(context, node);
        if (boundVariable == null) {
            return;
        }

        UFunction method = UastUtils.getContainingFunction(node);
        if (method == null) {
            return;
        }

        FinishVisitor visitor = new FinishVisitor(context, boundVariable) {
            @Override
            protected boolean isCleanupCall(@NonNull UCallExpression call) {
                if (!call.functionNameMatches(recycleName)) {
                    return false;
                }
                UDeclaration resolved = call.resolve(mContext);
                if (resolved != null) {
                    UClass containingClass = UastUtils.getContainingClassOrEmpty(resolved);
                    if (containingClass.isSubclassOf(recycleType)) {
                        // Yes, called the right recycle() method; now make sure
                        // we're calling it on the right variable
                        UElement parent = call.getParent();
                        if (parent instanceof UQualifiedExpression) {
                            UExpression operand = ((UQualifiedExpression) parent).getReceiver();
                            resolved = UastUtils.resolveIfCan(operand, mContext);
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
        UElement locationNode = node instanceof UCallExpression ?
                ((UCallExpression) node).getFunctionNameElement() : node;
        Location location = UastAndroidUtils.getLocation(locationNode);
        context.report(RECYCLE_RESOURCE, node, location, message);
    }

    private static boolean checkTransactionCommits(@NonNull UastAndroidContext context,
            @NonNull UCallExpression node) {
        if (isBeginTransaction(context, node)) {
            UVariable boundVariable = getVariable(context, node);
            if (boundVariable == null && isCommittedInChainedCalls(context, node)) {
                return true;
            }

            if (boundVariable != null) {
                UFunction method = UastUtils.getContainingFunction(node);
                if (method == null) {
                    return true;
                }

                FinishVisitor commitVisitor = new FinishVisitor(context, boundVariable) {
                    @Override
                    protected boolean isCleanupCall(@NonNull UCallExpression call) {
                        if (isTransactionCommitMethodCall(mContext, call)) {
                            UExpression operand = UastUtils.getReceiver(call);
                            if (operand instanceof UResolvable) {
                                UDeclaration resolved = ((UResolvable) operand).resolve(mContext);
                                //noinspection SuspiciousMethodCalls
                                if (resolved != null && mVariables.contains(resolved)) {
                                    return true;
                                } else if (resolved instanceof UFunction
                                           && operand instanceof UCallExpression
                                           && isCommittedInChainedCalls(mContext, (UCallExpression) operand)) {
                                    // Check that the target of the committed chains is the
                                    // right variable!
                                    while (operand instanceof UCallExpression) {
                                        operand = UastUtils.getReceiver((UCallExpression)operand);
                                    }
                                    if (operand instanceof USimpleReferenceExpression) {
                                        resolved = ((USimpleReferenceExpression)operand).resolve(mContext);
                                        //noinspection SuspiciousMethodCalls
                                        if (resolved != null && mVariables.contains(resolved)) {
                                            return true;
                                        }
                                    }
                                }
                            }
                        } else if (isShowFragmentMethodCall(mContext, call)) {
                            List<UExpression> arguments = call.getValueArguments();
                            if (arguments.size() == 2) {
                                UExpression first = arguments.get(0);
                                UDeclaration resolved = UastUtils.resolveIfCan(first, mContext);
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
            context.report(COMMIT_FRAGMENT, node, UastAndroidUtils.getLocation(node.getFunctionReference()),
                           message);
        }
        return false;
    }

    private static boolean isCommittedInChainedCalls(@NonNull UastAndroidContext context,
            @NonNull UCallExpression node) {
        // Look for chained calls since the FragmentManager methods all return "this"
        // to allow constructor chaining, e.g.
        //    getFragmentManager().beginTransaction().addToBackStack("test")
        //            .disallowAddToBackStack().hide(mFragment2).setBreadCrumbShortTitle("test")
        //            .show(mFragment2).setCustomAnimations(0, 0).commit();
        UElement parent = node.getParent();
        while (parent instanceof UCallExpression) {
            UCallExpression methodInvocation = (UCallExpression) parent;
            if (isTransactionCommitMethodCall(context, methodInvocation)
                    || isShowFragmentMethodCall(context, methodInvocation)) {
                return true;
            }

            parent = parent.getParent();
        }

        return false;
    }

    private static boolean isTransactionCommitMethodCall(@NonNull UastAndroidContext context,
            @NonNull UCallExpression call) {

        return (call.functionNameMatches(COMMIT) || call.functionNameMatches(COMMIT_ALLOWING_LOSS)) &&
                isMethodOnFragmentClass(context, call,
                        FRAGMENT_TRANSACTION_CLS,
                        FRAGMENT_TRANSACTION_V4_CLS);
    }

    private static boolean isShowFragmentMethodCall(@NonNull UastAndroidContext context,
            @NonNull UCallExpression call) {
        return call.functionNameMatches(SHOW)
                && isMethodOnFragmentClass(context, call,
                DIALOG_FRAGMENT, DIALOG_V4_FRAGMENT);
    }

    private static boolean isMethodOnFragmentClass(
            @NonNull UastAndroidContext context,
            @NonNull UCallExpression call,
            @NonNull String fragmentClass,
            @NonNull String v4FragmentClass) {
        UFunction resolved = call.resolve(context);
        if (resolved != null) {
            UClass containingClass = UastUtils.getContainingClassOrEmpty(resolved);
            return containingClass.isSubclassOf(fragmentClass) ||
                    containingClass.isSubclassOf(v4FragmentClass);
        }

        return false;
    }

    @Nullable
    public static UVariable getVariable(@NonNull UastAndroidContext context,
            @NonNull UElement expression) {
        UElement parent = expression.getParent();
        if (parent instanceof UBinaryExpression) {
            UBinaryExpression binaryExpression = (UBinaryExpression) parent;
            if (binaryExpression.getOperator() == UastBinaryOperator.ASSIGN) {
                UExpression lhs = binaryExpression.getLeftOperand();
                if (lhs instanceof UResolvable) {
                    UDeclaration resolved = ((UResolvable) lhs).resolve(context);
                    if (resolved instanceof UVariable) {
                        return (UVariable) resolved;
                    }
                }
            }
        } else if (parent instanceof UVariable) {
            return (UVariable) parent;
        }

        return null;
    }

    private static boolean isBeginTransaction(@NonNull UastAndroidContext context,
            @NonNull UCallExpression node) {
        assert node.functionNameMatches(BEGIN_TRANSACTION) : node.renderString();
        if (node.functionNameMatches(BEGIN_TRANSACTION)) {
            UFunction method = node.resolve(context);
            if (method != null) {
                UClass containingClass = UastUtils.getContainingClassOrEmpty(method);
                if (containingClass.isSubclassOf(FRAGMENT_MANAGER_CLS)
                        || containingClass.isSubclassOf(FRAGMENT_MANAGER_V4_CLS)) {
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
    private abstract static class FinishVisitor extends UastVisitor {
        protected final UastAndroidContext mContext;
        protected final List<UVariable> mVariables;
        private boolean mContainsCleanup;
        private boolean mEscapes;

        public FinishVisitor(UastAndroidContext context, @NonNull UVariable variable) {
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
        public boolean visitElement(@NotNull UElement node) {
            return mContainsCleanup || super.visitElement(node);
        }

        protected abstract boolean isCleanupCall(@NonNull UCallExpression call);

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression call) {
            if (mContainsCleanup) {
                return true;
            }

            // Look for escapes
            if (!mEscapes) {
                for (UExpression expression : call.getValueArguments()) {
                    if (expression instanceof USimpleReferenceExpression) {
                        UDeclaration resolved = ((USimpleReferenceExpression) expression).resolve(mContext);
                        //noinspection SuspiciousMethodCalls
                        if (resolved != null && mVariables.contains(resolved)) {
                            mEscapes = true;

                            // Special case: MotionEvent.obtain(MotionEvent): passing in an
                            // event here does not recycle the event, and we also know it
                            // doesn't escape
                            if (OBTAIN.equals(call.getFunctionName())) {
                                UFunction method = call.resolve(mContext);
                                if (method != null) {
                                    UClass cls = UastUtils.getContainingClassOrEmpty(method);
                                    if (cls.matchesFqName(MOTION_EVENT_CLS)) {
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
        public boolean visitVariable(@NotNull UVariable node) {
            UExpression initializer = node.getInitializer();
            if (initializer instanceof USimpleReferenceExpression) {
                UDeclaration resolved = ((USimpleReferenceExpression) initializer).resolve(mContext);
                //noinspection SuspiciousMethodCalls
                if (resolved != null && mVariables.contains(resolved)) {
                    if (node.getKind() == UastVariableKind.LOCAL_VARIABLE) {
                        mVariables.add(node);
                    } else if (node.getKind() == UastVariableKind.MEMBER) {
                        mEscapes = true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean visitBinaryExpression(@NotNull UBinaryExpression node) {
            if (node.getOperator() == UastBinaryOperator.ASSIGN) {
                UExpression rhs = node.getRightOperand();
                if (rhs instanceof USimpleReferenceExpression) {
                    UDeclaration resolved = ((USimpleReferenceExpression) rhs).resolve(mContext);
                    UExpression leftOperand = node.getLeftOperand();
                    //noinspection SuspiciousMethodCalls
                    if (resolved != null && mVariables.contains(resolved) && leftOperand instanceof UResolvable) {
                        UDeclaration resolvedLhs = ((UResolvable) leftOperand).resolve(mContext);
                        if (resolvedLhs instanceof UVariable) {
                            UVariable variable = (UVariable) resolvedLhs;
                            if (variable.getKind() == UastVariableKind.LOCAL_VARIABLE) {
                                mVariables.add(variable);
                            } else if (variable.getKind() == UastVariableKind.MEMBER) {
                                mEscapes = true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        @Override
        public boolean visitSpecialExpressionList(@NotNull USpecialExpressionList node) {
            if (node.getKind() == UastSpecialExpressionKind.RETURN) {
                UExpression value = node.firstOrNull();
                if (value instanceof USimpleReferenceExpression) {
                    UDeclaration resolved = ((USimpleReferenceExpression) value).resolve(mContext);
                    //noinspection SuspiciousMethodCalls
                    if (resolved != null && mVariables.contains(resolved)) {
                        mEscapes = true;
                    }
                }
            }

            return false;
        }
    }
}
