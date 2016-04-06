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
import com.android.tools.klint.detector.api.Category;
import com.android.tools.klint.detector.api.Context;
import com.android.tools.klint.detector.api.Detector;
import com.android.tools.klint.detector.api.Implementation;
import com.android.tools.klint.detector.api.Issue;
import com.android.tools.klint.detector.api.LintUtils;
import com.android.tools.klint.detector.api.Scope;
import com.android.tools.klint.detector.api.Severity;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;
import org.jetbrains.uast.check.UastAndroidUtils;
import org.jetbrains.uast.check.UastScanner;
import org.jetbrains.uast.java.JavaSpecialExpressionKinds;
import org.jetbrains.uast.visitor.UastVisitor;

/**
 * Detector looking for SharedPreferences.edit() calls without a corresponding
 * commit() or apply() call
 */
public class SharedPrefsDetector extends Detector implements UastScanner {
    /** The main issue discovered by this detector */
    public static final Issue ISSUE = Issue.create(
            "CommitPrefEdits", //$NON-NLS-1$
            "Missing `commit()` on `SharedPreference` editor",

            "After calling `edit()` on a `SharedPreference`, you must call `commit()` " +
            "or `apply()` on the editor to save the results.",

            Category.CORRECTNESS,
            6,
            Severity.WARNING,
            new Implementation(
                    SharedPrefsDetector.class,
                    Scope.JAVA_FILE_SCOPE));

    public static final String ANDROID_CONTENT_SHARED_PREFERENCES =
            "android.content.SharedPreferences"; //$NON-NLS-1$
    private static final String ANDROID_CONTENT_SHARED_PREFERENCES_EDITOR =
            "android.content.SharedPreferences.Editor"; //$NON-NLS-1$

    /** Constructs a new {@link SharedPrefsDetector} check */
    public SharedPrefsDetector() {
    }

    @Override
    public boolean appliesTo(@NonNull Context context, @NonNull File file) {
        return true;
    }


    // ---- Implements UastScanner ----

    @Override
    public List<String> getApplicableFunctionNames() {
        return Collections.singletonList("edit"); //$NON-NLS-1$
    }

    @Override
    public void visitFunctionCall(UastAndroidContext context, UCallExpression node) {
        assert "edit".equals(node.getFunctionName());

        boolean verifiedType = false;
        UFunction resolve = node.resolve(context);
        if (resolve != null) {
            UType returnType = resolve.getReturnType();
            if (returnType == null ||
                !returnType.matchesFqName(ANDROID_CONTENT_SHARED_PREFERENCES_EDITOR)) {
                return;
            }
            verifiedType = true;
        }

        UElement parent = node.getParent();
        if (!(parent instanceof UQualifiedExpression)) {
            return;
        }
        UElement operand = ((UQualifiedExpression)parent).getReceiver();

        // Looking for the specific pattern where you assign the edit() result
        // to a local variable; this means we won't recognize some other usages
        // of the API (e.g. assigning it to a previously declared variable) but
        // is needed until we have type attribution in the AST itself.

        UVariable definition = getLhs(parent);
        boolean allowCommitBeforeTarget;
        if (definition == null) {
            if (operand instanceof USimpleReferenceExpression) {
                UDeclaration resolvedDeclaration = (((USimpleReferenceExpression)operand).resolve(context));
                if (resolvedDeclaration instanceof UVariable) {
                    String type = ((UVariable)resolvedDeclaration).getType().getName();
                    if (!type.equals("SharedPreferences")) { //$NON-NLS-1$
                        return;
                    }
                }
                allowCommitBeforeTarget = true;
            } else {
                return;
            }
        } else {
            if (!verifiedType) {
                UType type = definition.getType();
                String possiblefqName = type.resolveOrEmpty(context).getFqNameOrName();
                if (possiblefqName.endsWith("SharedPreferences.Editor")) { //$NON-NLS-1$
                    if (!type.matchesFqName("Editor") ||                  //$NON-NLS-1$
                        !LintUtils.isImported(context.getLintContext().getCompilationUnit(),
                                              ANDROID_CONTENT_SHARED_PREFERENCES_EDITOR)) {
                        return;
                    }
                }
            }
            allowCommitBeforeTarget = false;
        }

        UFunction method = UastUtils.getContainingFunction(parent);
        if (method == null) {
            return;
        }

        CommitFinder finder = new CommitFinder(context, node, allowCommitBeforeTarget);
        finder.process(method);
        if (!finder.isCommitCalled()) {
            context.report(ISSUE, method, UastAndroidUtils.getLocation(node),
                           "`SharedPreferences.edit()` without a corresponding `commit()` or `apply()` call");
        }
    }

    @Nullable
    private static UVariable getLhs(@NonNull UElement node) {
        while (node != null) {
            if (node instanceof UFunction) {
                return null;
            }
            if (node instanceof UVariable) {
                return ((UVariable)node);
            }

            node = node.getParent();
        }

        return null;
    }

    private static class CommitFinder extends UastVisitor {
        /** The target edit call */
        private final UCallExpression mTarget;
        /** whether it allows the commit call to be seen before the target node */
        private final boolean mAllowCommitBeforeTarget;

        private final UastAndroidContext mContext;

        /** Whether we've found one of the commit/cancel methods */
        private boolean mFound;
        /** Whether we've seen the target edit node yet */
        private boolean mSeenTarget;

        private CommitFinder(UastAndroidContext context, UCallExpression target,
                             boolean allowCommitBeforeTarget) {
            mContext = context;
            mTarget = target;
            mAllowCommitBeforeTarget = allowCommitBeforeTarget;
        }

        @Override
        public boolean visitCallExpression(@NotNull UCallExpression node) {
            UElement qualifiedElement = UastUtils.getQualifiedCallElement(node);

            if (node == mTarget) {
                mSeenTarget = true;
            } else if (mAllowCommitBeforeTarget || mSeenTarget ||
                    (qualifiedElement instanceof UQualifiedExpression &&
                    ((UQualifiedExpression)qualifiedElement).getReceiver() == mTarget)) {
                String name = node.getFunctionName();
                boolean isCommit = "commit".equals(name);
                if (isCommit || "apply".equals(name)) { //$NON-NLS-1$ //$NON-NLS-2$
                    // TODO: Do more flow analysis to see whether we're really calling commit/apply
                    // on the right type of object?
                    mFound = true;

                    UFunction method = node.resolve(mContext);
                    if (method != null) {
                        UClass clz = UastUtils.getContainingClassOrEmpty(method);
                        if (clz.isSubclassOf("android.content.SharedPreferences.Editor")
                            && mContext.getLintContext().getProject().getMinSdkVersion().getApiLevel() >= 9) {
                            // See if the return value is read: can only replace commit with
                            // apply if the return value is not considered
                            boolean returnValueIgnored = false;
                            if (qualifiedElement instanceof UFunction ||
                                qualifiedElement instanceof UClass ||
                                qualifiedElement instanceof UBlockExpression) {
                                returnValueIgnored = true;
                            } else if (qualifiedElement instanceof UIfExpression) {
                                returnValueIgnored = ((UIfExpression) qualifiedElement).getCondition() != node;
                            } else if (qualifiedElement instanceof USpecialExpressionList &&
                                       ((USpecialExpressionList)qualifiedElement).getKind() == UastSpecialExpressionKind.RETURN) {
                                returnValueIgnored = false;
                            } else if (qualifiedElement instanceof UVariable) {
                                returnValueIgnored = false;
                            } else if (qualifiedElement instanceof UForExpression) {
                                returnValueIgnored = ((UForExpression) qualifiedElement).getCondition() != node;
                            } else if (qualifiedElement instanceof UWhileExpression) {
                                returnValueIgnored = ((UWhileExpression) qualifiedElement).getCondition() != node;
                            } else if (qualifiedElement instanceof UDoWhileExpression) {
                                returnValueIgnored = ((UDoWhileExpression) qualifiedElement).getCondition() != node;
                            } else if (qualifiedElement instanceof UExpressionSwitchClauseExpression) {
                                returnValueIgnored = ((UExpressionSwitchClauseExpression) qualifiedElement).getCaseValue() != node;
                            } else if (qualifiedElement instanceof USpecialExpressionList &&
                                       ((USpecialExpressionList)qualifiedElement).getKind() == JavaSpecialExpressionKinds.ASSERT) {
                                returnValueIgnored = !((USpecialExpressionList) qualifiedElement).getExpressions().contains(node);
                            } else {
                                returnValueIgnored = true;
                            }
                            if (returnValueIgnored && isCommit) {
                                String message = "Consider using `apply()` instead; `commit` writes "
                                                 + "its data to persistent storage immediately, whereas "
                                                 + "`apply` will handle it in the background";
                                mContext.report(ISSUE, node, UastAndroidUtils.getLocation(node), message);
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
                if (node.getExpressions().contains(mTarget)) {
                    mFound = true;
                }
            }

            return false;
        }

        boolean isCommitCalled() {
            return mFound;
        }
    }
}
