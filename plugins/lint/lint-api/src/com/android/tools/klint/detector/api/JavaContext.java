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

package com.android.tools.lint.detector.api;

import static com.android.SdkConstants.CLASS_CONTEXT;
import static com.android.tools.lint.client.api.JavaParser.ResolvedNode;
import static com.android.tools.lint.client.api.JavaParser.TypeDescriptor;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.client.api.JavaParser;
import com.android.tools.lint.client.api.JavaParser.ResolvedClass;
import com.android.tools.lint.client.api.LintDriver;

import java.io.File;
import java.util.Iterator;

import lombok.ast.ClassDeclaration;
import lombok.ast.ConstructorDeclaration;
import lombok.ast.Expression;
import lombok.ast.MethodDeclaration;
import lombok.ast.MethodInvocation;
import lombok.ast.Node;
import lombok.ast.Position;

/**
 * A {@link Context} used when checking Java files.
 * <p/>
 * <b>NOTE: This is not a public or final API; if you rely on this be prepared
 * to adjust your code for the next tools release.</b>
 */
public class JavaContext extends Context {
    static final String SUPPRESS_COMMENT_PREFIX = "//noinspection "; //$NON-NLS-1$

    /** The parse tree */
    private Node mCompilationUnit;

    /** The parser which produced the parse tree */
    private final JavaParser mParser;

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
            @NonNull File file,
            @NonNull JavaParser parser) {
        super(driver, project, main, file);
        mParser = parser;
    }

    /**
     * Returns a location for the given node
     *
     * @param node the AST node to get a location for
     * @return a location for the given node
     */
    @NonNull
    public Location getLocation(@NonNull Node node) {
        return mParser.getLocation(this, node);
    }

    @NonNull
    public JavaParser getParser() {
        return mParser;
    }

    @Nullable
    public Node getCompilationUnit() {
        return mCompilationUnit;
    }

    /**
     * Sets the compilation result. Not intended for client usage; the lint infrastructure
     * will set this when a context has been processed
     *
     * @param compilationUnit the parse tree
     */
    public void setCompilationUnit(@Nullable Node compilationUnit) {
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
    public void report(
            @NonNull Issue issue,
            @Nullable Node scope,
            @Nullable Location location,
            @NonNull String message) {
        if (scope != null && mDriver.isSuppressed(this, issue, scope)) {
            return;
        }
        super.report(issue, location, message);
    }

    /**
     * Report an error.
     * Like {@link #report(Issue, Node, Location, String)} but with
     * a now-unused data parameter at the end.
     *
     * @deprecated Use {@link #report(Issue, Node, Location, String)} instead;
     *    this method is here for custom rule compatibility
     */
    @SuppressWarnings("UnusedDeclaration") // Potentially used by external existing custom rules
    @Deprecated
    public void report(
            @NonNull Issue issue,
            @Nullable Node scope,
            @Nullable Location location,
            @NonNull String message,
            @SuppressWarnings("UnusedParameters") @Nullable Object data) {
        report(issue, scope, location, message);
    }

    @Nullable
    public static Node findSurroundingMethod(Node scope) {
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == MethodDeclaration.class || type == ConstructorDeclaration.class) {
                return scope;
            }

            scope = scope.getParent();
        }

        return null;
    }

    @Nullable
    public static ClassDeclaration findSurroundingClass(@Nullable Node scope) {
        while (scope != null) {
            Class<? extends Node> type = scope.getClass();
            // The Lombok AST uses a flat hierarchy of node type implementation classes
            // so no need to do instanceof stuff here.
            if (type == ClassDeclaration.class) {
                return (ClassDeclaration) scope;
            }

            scope = scope.getParent();
        }

        return null;
    }

    @Override
    @Nullable
    protected String getSuppressCommentPrefix() {
        return SUPPRESS_COMMENT_PREFIX;
    }

    public boolean isSuppressedWithComment(@NonNull Node scope, @NonNull Issue issue) {
        // Check whether there is a comment marker
        String contents = getContents();
        assert contents != null; // otherwise we wouldn't be here
        Position position = scope.getPosition();
        if (position == null) {
            return false;
        }

        int start = position.getStart();
        return isSuppressedWithComment(start, issue);
    }

    @NonNull
    public Location.Handle createLocationHandle(@NonNull Node node) {
        return mParser.createLocationHandle(this, node);
    }

    @Nullable
    public ResolvedNode resolve(@NonNull Node node) {
        return mParser.resolve(this, node);
    }

    @Nullable
    public ResolvedClass findClass(@NonNull String fullyQualifiedName) {
        return mParser.findClass(this, fullyQualifiedName);
    }

    @Nullable
    public TypeDescriptor getType(@NonNull Node node) {
        return mParser.getType(this, node);
    }

    /**
     * Returns true if the given method invocation node corresponds to a call on a
     * {@code android.content.Context}
     *
     * @param node the method call node
     * @return true iff the method call is on a class extending context
     */
    public boolean isContextMethod(@NonNull MethodInvocation node) {
        // Method name used in many other contexts where it doesn't have the
        // same semantics; only use this one if we can resolve types
        // and we're certain this is the Context method
        ResolvedNode resolved = resolve(node);
        if (resolved instanceof JavaParser.ResolvedMethod) {
            JavaParser.ResolvedMethod method = (JavaParser.ResolvedMethod) resolved;
            ResolvedClass containingClass = method.getContainingClass();
            if (containingClass.isSubclassOf(CLASS_CONTEXT, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first ancestor node of the given type
     *
     * @param element the element to search from
     * @param clz     the target node type
     * @param <T>     the target node type
     * @return the nearest ancestor node in the parent chain, or null if not found
     */
    @Nullable
    public static <T extends Node> T getParentOfType(
            @Nullable Node element,
            @NonNull Class<T> clz) {
        return getParentOfType(element, clz, true);
    }

    /**
     * Returns the first ancestor node of the given type
     *
     * @param element the element to search from
     * @param clz     the target node type
     * @param strict  if true, do not consider the element itself, only its parents
     * @param <T>     the target node type
     * @return the nearest ancestor node in the parent chain, or null if not found
     */
    @Nullable
    public static <T extends Node> T getParentOfType(
            @Nullable Node element,
            @NonNull Class<T> clz,
            boolean strict) {
        if (element == null) {
            return null;
        }

        if (strict) {
            element = element.getParent();
        }

        while (element != null) {
            if (clz.isInstance(element)) {
                //noinspection unchecked
                return (T) element;
            }
            element = element.getParent();
        }

        return null;
    }

    /**
     * Returns the first ancestor node of the given type, stopping at the given type
     *
     * @param element     the element to search from
     * @param clz         the target node type
     * @param strict      if true, do not consider the element itself, only its parents
     * @param terminators optional node types to terminate the search at
     * @param <T>         the target node type
     * @return the nearest ancestor node in the parent chain, or null if not found
     */
    @Nullable
    public static <T extends Node> T getParentOfType(@Nullable Node element,
            @NonNull Class<T> clz,
            boolean strict,
            @NonNull Class<? extends Node>... terminators) {
        if (element == null) {
            return null;
        }
        if (strict) {
            element = element.getParent();
        }

        while (element != null && !clz.isInstance(element)) {
            for (Class<?> terminator : terminators) {
                if (terminator.isInstance(element)) {
                    return null;
                }
            }
            element = element.getParent();
        }

        //noinspection unchecked
        return (T) element;
    }

    /**
     * Returns the first sibling of the given node that is of the given class
     *
     * @param sibling the sibling to search from
     * @param clz     the type to look for
     * @param <T>     the type
     * @return the first sibling of the given type, or null
     */
    @Nullable
    public static <T extends Node> T getNextSiblingOfType(@Nullable Node sibling,
            @NonNull Class<T> clz) {
        if (sibling == null) {
            return null;
        }
        Node parent = sibling.getParent();
        if (parent == null) {
            return null;
        }

        Iterator<Node> iterator = parent.getChildren().iterator();
        while (iterator.hasNext()) {
            if (iterator.next() == sibling) {
                break;
            }
        }

        while (iterator.hasNext()) {
            Node child = iterator.next();
            if (clz.isInstance(child)) {
                //noinspection unchecked
                return (T) child;
            }

        }

        return null;
    }


    /**
     * Returns the given argument of the given call
     *
     * @param call the call containing arguments
     * @param index the index of the target argument
     * @return the argument at the given index
     * @throws IllegalArgumentException if index is outside the valid range
     */
    @NonNull
    public static Node getArgumentNode(@NonNull MethodInvocation call, int index) {
        int i = 0;
        for (Expression parameter : call.astArguments()) {
            if (i == index) {
                return parameter;
            }
            i++;
        }
        throw new IllegalArgumentException(Integer.toString(index));
    }
}
