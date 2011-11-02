// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.util.Lists;
import com.google.dart.compiler.util.Maps;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A scope is a factory for creating and allocating
 * {@link com.google.dart.compiler.backend.js.ast.JsName}s. A JavaScript AST is
 * built in terms of abstract name objects without worrying about obfuscation,
 * keyword/identifier blacklisting, and so on.
 *
 * <p>
 *
 * Scopes are associated with
 * {@link com.google.dart.compiler.backend.js.ast.JsFunction}s, but the two are
 * not equivalent. Functions <i>have</i> scopes, but a scope does not
 * necessarily have an associated Function. Examples of this include the
 * {@link com.google.dart.compiler.backend.js.ast.JsRootScope} and synthetic
 * scopes that might be created by a client.
 *
 * <p>
 *
 * Scopes can have parents to provide constraints when allocating actual
 * identifiers for names. Specifically, names in child scopes are chosen such
 * that they do not conflict with names in their parent scopes. The ultimate
 * parent is usually the global scope (see
 * {@link com.google.dart.compiler.backend.js.ast.JsProgram#getRootScope()}),
 * but parentless scopes are useful for managing names that are always accessed
 * with a qualifier and could therefore never be confused with the global scope
 * hierarchy.
 */
public class JsScope implements Serializable {

  private List<JsScope> children = Collections.emptyList();
  private final String description;
  private Map<String, JsName> names = Collections.emptyMap();
  private JsScope parent;
  protected int tempIndex = 0;
  private final String scopeId;

 /* 
  * Create a scope with parent.
  */
 public JsScope(JsScope parent, String description) {
   this(parent, description, null);
 }
 
  /**
   * Create a scope with parent.
   */
  public JsScope(JsScope parent, String description, String scopeId) {
    assert (parent != null);
    this.scopeId = scopeId;
    this.description = description;
    this.parent = parent;
    parent.children = Lists.add(parent.children, this);
  }

  /**
   * Rebase the function to a new scope.
   * @param newParent The scope to add the function to.
   */
  public void rebase(JsScope newParent) {
    detachFromParent();
    parent = newParent;
    parent.children = Lists.add(parent.children, this);
  }

  /**
   * Rebase the function's children to a new scope.
   * @param newParent
   */
  public void rebaseChildScopes(JsScope newParent) {
    if (newParent == this) {
      return;
    }
    parent.children = Lists.addAll(parent.children, children);
    for (JsScope child : children) {
      child.parent = newParent;
    }
    children = Collections.emptyList();
  }

  /**
   * Subclasses can detach and become parentless.
   */
  protected void detachFromParent() {
    JsScope oldParent = parent;

    oldParent.children = Lists.remove(
        parent.children, oldParent.children.indexOf(this));

    parent = null;
  }

  /**
   * Subclasses can be parentless.
   */
  protected JsScope(String description) {
    this.description = description;
    this.parent = null;
    this.scopeId = null;
  }

  /**
   * Gets a name object associated with the specified ident in this scope,
   * creating it if necessary.<br/>
   * If the JsName does not exist yet, a new JsName is created. The ident,
   * short name, and original name of the newly created JsName are equal to
   * the given ident.
   *
   * @param ident An identifier that is unique within this scope.
   */
  public JsName declareName(String ident) {
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      return name;
    }
    return doCreateName(ident, ident, ident);
  }

  /**
   * Creates a new variable with an unique ident in this scope.
   * The generated JsName is guaranteed to have an identifier (but not short
   * name) that does not clash with any existing variables in the scope.
   * Future declarations of variables might however clash with the temporary
   * (unless they use this function).
   */
  public JsName declareFreshName(String shortName) {
    String ident = shortName;
    int counter = 0;
    while (findExistingNameNoRecurse(ident) != null) {
      ident = shortName + "_" + counter++;
    }
    return doCreateName(ident, shortName, shortName);
  }

  String getNextTempName() {
    // TODO(ngeoffray): Decide on a convention for temporary variables
    // introduced by the compiler.
    return "tmp$" + (scopeId != null ? scopeId + "$" : "") + tempIndex++;
  }

  /**
   * Creates a temporary variable with an unique name in this scope.
   * The generated temporary is guaranteed to have an identifier (but not short
   * name) that does not clash with any existing variables in the scope.
   * Future declarations of variables might however clash with the temporary.
   */
  public JsName declareTemporary() {
    return declareFreshName(getNextTempName());
  }

  /**
   * Gets a name object associated with the specified ident in this scope,
   * creating it if necessary.<br/>
   * If the JsName does not exist yet, a new JsName is created with the given
   * ident, short name and original name.
   *
   * @param ident An identifier that is unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @throws IllegalArgumentException if ident already exists in this scope but
   *           the requested short name does not match the existing short name.
   */
  public JsName declareName(String ident, String shortIdent) {
    return declareName(ident, shortIdent, ident);
  }

  /**
   * Gets a name object associated with the specified ident in this scope,
   * creating it if necessary.<br/>
   * If the JsName does not exist yet, a new JsName is created. The original
   * name stored in the JsName is equal to the (unmangled) specified originalName.
   *
   * @param ident An identifier that is unique within this scope.
   * @param shortIdent A "pretty" name that does not have to be unique.
   * @param originalName The original name in the source.
   * @throws IllegalArgumentException if ident already exists in this scope but
   *           the requested short name does not match the existing short name,
   *           or the original name does not match the existing original name.
   */
  public JsName declareName(String ident, String shortIdent, String originalName) {
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null) {
      if (!name.getShortIdent().equals(shortIdent)
          || !nullableEquals(name.getOriginalName(), originalName)) {
        throw new IllegalArgumentException("Requested short name " + shortIdent
            + " conflicts with preexisting short name " + name.getShortIdent() + " for identifier "
            + ident);
      }
      return name;
    }
    return doCreateName(ident, shortIdent, originalName);
  }

  boolean nullableEquals(String s1, String s2) {
    return (s1 == null) ? (s2 == null) : s1.equals(s2);
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this
   * scope, and if not found, in the parent scopes.
   *
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingName(String ident) {
    JsName name = findExistingNameNoRecurse(ident);
    if (name == null && parent != null) {
      return parent.findExistingName(ident);
    }
    return name;
  }

  /**
   * Attempts to find an unobfuscatable name object for the specified ident,
   * searching in this scope, and if not found, in the parent scopes.
   *
   * @return <code>null</code> if the identifier has no associated name
   */
  public final JsName findExistingUnobfuscatableName(String ident) {
    JsName name = findExistingNameNoRecurse(ident);
    if (name != null && name.isObfuscatable()) {
      name = null;
    }
    if (name == null && parent != null) {
      return parent.findExistingUnobfuscatableName(ident);
    }
    return name;
  }

  /**
   * Returns an iterator for all the names defined by this scope.
   */
  public Iterator<JsName> getAllNames() {
    return names.values().iterator();
  }

  /**
   * Returns a list of this scope's child scopes.
   */
  public final List<JsScope> getChildren() {
    return children;
  }

  /**
   * Returns the parent scope of this scope, or <code>null</code> if this is the
   * root scope.
   */
  public final JsScope getParent() {
    return parent;
  }

  /**
   * Returns the associated program.
   */
  public JsProgram getProgram() {
    assert (parent != null) : "Subclasses must override getProgram() if they do not set a parent";
    return parent.getProgram();
  }

  @Override
  public final String toString() {
    if (parent != null) {
      return description + "->" + parent;
    } else {
      return description;
    }
  }

  /**
   * Creates a new name in this scope.
   */
  protected JsName doCreateName(String ident, String shortIdent, String originalName) {
    JsName name = new JsName(this, ident, shortIdent, originalName);
    names = Maps.putOrdered(names, ident, name);
    return name;
  }

  /**
   * Attempts to find the name object for the specified ident, searching in this
   * scope only.
   *
   * @return <code>null</code> if the identifier has no associated name
   */
  protected JsName findExistingNameNoRecurse(String ident) {
    return names.get(ident);
  }
}
