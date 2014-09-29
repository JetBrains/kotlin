// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.util.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.dart.compiler.backend.js.ast.AstPackage.JsObjectScope;

/**
 * A scope is a factory for creating and allocating
 * {@link JsName}s. A JavaScript AST is
 * built in terms of abstract name objects without worrying about obfuscation,
 * keyword/identifier blacklisting, and so on.
 * <p/>
 * <p/>
 * <p/>
 * Scopes are associated with
 * {@link JsFunction}s, but the two are
 * not equivalent. Functions <i>have</i> scopes, but a scope does not
 * necessarily have an associated Function. Examples of this include the
 * {@link JsRootScope} and synthetic
 * scopes that might be created by a client.
 * <p/>
 * <p/>
 * <p/>
 * Scopes can have parents to provide constraints when allocating actual
 * identifiers for names. Specifically, names in child scopes are chosen such
 * that they do not conflict with names in their parent scopes. The ultimate
 * parent is usually the global scope (see
 * {@link JsProgram#getRootScope()}),
 * but parentless scopes are useful for managing names that are always accessed
 * with a qualifier and could therefore never be confused with the global scope
 * hierarchy.
 */
public abstract class JsScope {
    @NotNull
    private final String description;
    private Map<String, JsName> names = Collections.emptyMap();
    private final JsScope parent;
    protected int tempIndex = 0;
    private final String scopeId;

    public JsScope(JsScope parent, @NotNull String description, @Nullable String scopeId) {
        assert (parent != null);
        this.scopeId = scopeId;
        this.description = description;
        this.parent = parent;
    }

    protected JsScope(@NotNull String description) {
        this.description = description;
        parent = null;
        scopeId = null;
    }

    @NotNull
    public JsScope innerObjectScope(@NotNull String scopeName) {
        return JsObjectScope(this, scopeName);
    }

    /**
     * Gets a name object associated with the specified identifier in this scope,
     * creating it if necessary.<br/>
     * If the JsName does not exist yet, a new JsName is created. The identifier,
     * short name, and original name of the newly created JsName are equal to
     * the given identifier.
     *
     * @param identifier An identifier that is unique within this scope.
     */
    @NotNull
    public JsName declareName(@NotNull String identifier) {
        JsName name = findOwnName(identifier);
        return name != null ? name : doCreateName(identifier);
    }

    /**
     * Creates a new variable with an unique ident in this scope.
     * The generated JsName is guaranteed to have an identifier that does not clash with any existing variables in the scope.
     * Future declarations of variables might however clash with the temporary
     * (unless they use this function).
     */
    @NotNull
    public JsName declareFreshName(@NotNull String suggestedName) {
        String name = suggestedName;
        int counter = 0;
        while (hasOwnName(name)) {
            name = suggestedName + '_' + counter++;
        }
        return doCreateName(name);
    }

    private String getNextTempName() {
        // introduced by the compiler
        return "tmp$" + (scopeId != null ? scopeId + "$" : "") + tempIndex++;
    }

    /**
     * Creates a temporary variable with an unique name in this scope.
     * The generated temporary is guaranteed to have an identifier (but not short
     * name) that does not clash with any existing variables in the scope.
     * Future declarations of variables might however clash with the temporary.
     */
    @NotNull
    public JsName declareTemporary() {
        return declareFreshName(getNextTempName());
    }

    /**
     * Attempts to find the name object for the specified ident, searching in this
     * scope, and if not found, in the parent scopes.
     *
     * @return <code>null</code> if the identifier has no associated name
     */
    @Nullable
    public final JsName findName(String ident) {
        JsName name = findOwnName(ident);
        if (name == null && parent != null) {
            return parent.findName(ident);
        }
        return name;
    }

    public boolean hasOwnName(@NotNull String name) {
        return names.containsKey(name);
    }

    /**
     * Returns the parent scope of this scope, or <code>null</code> if this is the
     * root scope.
     */
    public final JsScope getParent() {
        return parent;
    }

    public JsProgram getProgram() {
        assert (parent != null) : "Subclasses must override getProgram() if they do not set a parent";
        return parent.getProgram();
    }

    @Override
    public final String toString() {
        if (parent != null) {
            return description + "->" + parent;
        }
        else {
            return description;
        }
    }

    public void copyOwnNames(JsScope other) {
        names = new HashMap<String, JsName>(names);
        names.putAll(other.names);
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    protected JsName doCreateName(@NotNull String ident) {
        JsName name = new JsName(this, ident);
        names = Maps.put(names, ident, name);
        return name;
    }

    /**
     * Attempts to find the name object for the specified ident, searching in this
     * scope only.
     *
     * @return <code>null</code> if the identifier has no associated name
     */
    protected JsName findOwnName(@NotNull String ident) {
        return names.get(ident);
    }
}
