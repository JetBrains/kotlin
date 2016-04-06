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

package com.android.tools.klint.detector.api;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.client.api.LintDriver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.uast.*;
import org.jetbrains.uast.check.UastAndroidContext;

import java.io.File;
import java.util.List;

import static com.android.SdkConstants.CLASS_CONTEXT;

/**
 * A {@link Context} used when checking Java files.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class JavaContext extends Context implements UastAndroidContext {
    static final String SUPPRESS_COMMENT_PREFIX = "//noinspection "; //$NON-NLS-1$

    /** The parse tree */
    private UFile mCompilationUnit;

    /**
     * Constructs a {@link JavaContext} for running lint on the given file, with
     * the given scope, in the given project reporting errors to the given
     * client.
     *
     * @param driver the driver running through the checks
     * @param project the project to run lint on which contains the given file
     * @param main the main project if this project is a library project, or
     *            null if this is not a library project. The main project is
     *            the root project of all library projects, not necessarily the
     *            directly including project.
     * @param file the file to be analyzed
     * @param parser the parser to use
     */
    public JavaContext(
            @NonNull LintDriver driver,
            @NonNull Project project,
            @Nullable Project main,
            @NonNull File file) {
        super(driver, project, main, file);
    }

    /**
     * Returns a location for the given node
     *
     * @param node the AST node to get a location for
     * @return a location for the given node
     */
    @Override
    public Location getLocation(@NotNull UElement element) {
        return UastAndroidContext.DefaultImpls.getLocation(this, element);
    }

    @Nullable
    public UFile getCompilationUnit() {
        return mCompilationUnit;
    }

    /**
     * Sets the compilation result. Not intended for client usage; the lint infrastructure
     * will set this when a context has been processed
     *
     * @param compilationUnit the parse tree
     */
    public void setCompilationUnit(@Nullable UFile compilationUnit) {
        mCompilationUnit = compilationUnit;
    }

    @Override
    public void report(@NonNull Issue issue, @Nullable Location location,
            @NonNull String message) {
        if (mDriver.isSuppressed(this, issue, mCompilationUnit)) {
            return;
        }
        super.report(issue, location, message);
    }

    /**
     * Reports an issue applicable to a given AST node. The AST node is used as the
     * scope to check for suppress lint annotations.
     *
     * @param issue the issue to report
     * @param scope the AST node scope the error applies to. The lint infrastructure
     *    will check whether there are suppress annotations on this node (or its enclosing
     *    nodes) and if so suppress the warning without involving the client.
     * @param location the location of the issue, or null if not known
     * @param message the message for this warning
     */
    @Override
    public void report(
      @NonNull Issue issue,
      @Nullable UElement scope,
      @Nullable Location location,
      @NonNull String message) {
        if (scope != null && mDriver.isSuppressed(this, issue, scope)) {
            return;
        }
        super.report(issue, location, message);
    }

    @Override
    @Nullable
    protected String getSuppressCommentPrefix() {
        return SUPPRESS_COMMENT_PREFIX;
    }

    /**
     * Returns true if the given method invocation node corresponds to a call on a
     * {@code android.content.Context}
     *
     * @param node the method call node
     * @return true iff the method call is on a class extending context
     */
    public boolean isContextMethod(@NonNull UCallExpression node) {
        // Method name used in many other contexts where it doesn't have the
        // same semantics; only use this one if we can resolve types
        // and we're certain this is the Context method
        UFunction resolved = node.resolve(this);
        UClass containingClass = UastUtils.getContainingClass(resolved);
        if (resolved != null && containingClass != null) {
            if (containingClass.isSubclassOf(CLASS_CONTEXT)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public JavaContext getLintContext() {
        return this;
    }

    @NotNull
    @Override
    public List<UastConverter> getConverters() {
        return getClient().getConverters();
    }

    @org.jetbrains.annotations.Nullable
    @Override
    public UElement convert(@Nullable Object element) {
        return UastContext.DefaultImpls.convert(this, element);
    }
}
