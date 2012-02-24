// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js;

import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.google.dart.compiler.backend.js.ast.JsRootScope;
import com.google.dart.compiler.backend.js.ast.JsScope;

import java.util.*;

/**
 * A namer that uses short, readable idents to maximize reability.
 */
public class JsPrettyNamer implements JsNamer {

  public JsPrettyNamer() {
    this.program = null;
  }

  @Override
  public void exec(JsProgram program) {
    new JsPrettyNamer(program).execImpl();
  }

  /**
   * Communicates to a parent scope all the idents used by all child scopes.
   */
  private Set<String> childIdents = null;

  private final JsProgram program;

  /**
   * A map containing the next integer to try as an identifier suffix for a
   * given JsScope.
   */
  private IdentityHashMap<JsScope, HashMap<String, Integer>> startIdentForScope =
      new IdentityHashMap<JsScope, HashMap<String, Integer>>();

  protected JsPrettyNamer(JsProgram program) {
    this.program = program;
  }

  private void execImpl() {
    visit(program.getRootScope());
  }

  private boolean isLegal(JsScope scope, Set<String> childIdents, String newIdent) {
    if (JsReservedIdentifiers.isKeyword(newIdent)) {
      return false;
    }
    if (childIdents.contains(newIdent)) {
      // one of my children already claimed this ident
      return false;
    }
    /*
     * Never obfuscate a name into an identifier that conflicts with an existing
     * unobfuscatable name! It's okay if it conflicts with an existing
     * obfuscatable name; that name will get obfuscated out of the way.
     */
    return (scope.findExistingUnobfuscatableName(newIdent) == null);
  }

  private void visit(JsScope scope) {
    HashMap<String, Integer> startIdent = startIdentForScope.get(scope);
    if (startIdent == null) {
      startIdent = new HashMap<String, Integer>();
      startIdentForScope.put(scope, startIdent);
    }

    // Save off the childIdents which is currently being computed for my parent.
    Set<String> myChildIdents = childIdents;

    /*
     * Visit my children first. Reset childIdents so that my children will get a
     * clean slate: I do not communicate to my children.
     */
    childIdents = new HashSet<String>();
    List<JsScope> children = scope.getChildren();
    for (Iterator<JsScope> it = children.iterator(); it.hasNext();) {
      visit(it.next());
    }

    JsRootScope rootScope = program.getRootScope();
    if (scope == rootScope) {
      return;
    }

    // Visit all my idents.
    for (Iterator<JsName> it = scope.getAllNames(); it.hasNext();) {
      JsName name = it.next();
      if (!name.isObfuscatable()) {
        // Unobfuscatable names become themselves.
        name.setShortIdent(name.getIdent());
        continue;
      }

      String newIdent = name.getShortIdent();
      if (!isLegal(scope, childIdents, newIdent)) {
        String checkIdent;

        // Start searching using a suffix hint stored in the scope.
        // We still do a search in case there is a collision with
        // a user-provided identifier
        Integer s = startIdent.get(newIdent);
        int suffix = (s == null) ? 0 : s.intValue();
        do {
          checkIdent = newIdent + "_" + suffix++;
        } while (!isLegal(scope, childIdents, checkIdent));
        startIdent.put(newIdent, suffix);
        name.setShortIdent(checkIdent);
      } else {
        // nothing to do; the short name is already good
      }
      childIdents.add(name.getShortIdent());
    }
    myChildIdents.addAll(childIdents);
    childIdents = myChildIdents;
  }
}
