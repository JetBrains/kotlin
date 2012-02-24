// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.common.SourceInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A JavaScript <code>var</code> statement.
 */
public class JsVars extends JsStatement implements Iterable<JsVars.JsVar> {

  /**
   * A var declared using the JavaScript <code>var</code> statement.
   */
  public static class JsVar extends JsNode implements HasName {

    private final JsName name;
    private JsExpression initExpr;

    public JsVar(JsName name) {
      this.name = name;
    }

    public JsVar(JsName name, JsExpression initExpr) {
      this.name = name;
      this.initExpr = initExpr;
    }

    public JsExpression getInitExpr() {
      return initExpr;
    }

    @Override
    public JsName getName() {
      return name;
    }

    public void setInitExpr(JsExpression initExpr) {
      this.initExpr = initExpr;
    }

    @Override
    public void traverse(JsVisitor v, JsContext ctx) {
      if (v.visit(this, ctx)) {
        if (initExpr != null) {
          initExpr = v.accept(initExpr);
        }
      }
      v.endVisit(this, ctx);
    }

    @Override
    public JsVar setSourceRef(SourceInfo info) {
      super.setSourceRef(info);
      return this;
    }

    @Override
    public NodeKind getKind() {
      return NodeKind.VAR;
    }
  }

  private final List<JsVar> vars = new ArrayList<JsVar>();

  public JsVars() {
  }

  public void add(JsVar var) {
    vars.add(var);
  }

  public int getNumVars() {
    return vars.size();
  }

  public void insert(JsVar var) {
    vars.add(var);
  }

  public boolean isEmpty() {
    return vars.isEmpty();
  }

  // Iterator returns JsVar objects
  @Override
  public Iterator<JsVar> iterator() {
    return vars.iterator();
  }

  @Override
  public void traverse(JsVisitor v, JsContext ctx) {
    if (v.visit(this, ctx)) {
      v.acceptWithInsertRemove(vars);
    }
    v.endVisit(this, ctx);
  }

  @Override
  public NodeKind getKind() {
    return NodeKind.VARS;
  }
}
