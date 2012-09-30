// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.compiler.backend.js.ast;

import com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

import java.util.List;

/**
 * Implemented by nodes that will visit child nodes.
 */
public class JsVisitor {

  protected static final JsContext LVALUE_CONTEXT = new JsContext() {

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return true;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceMe(JsVisitable node) {
      throw new UnsupportedOperationException();
    }
  };

  protected static final JsContext UNMODIFIABLE_CONTEXT = new JsContext() {

    @Override
    public boolean canInsert() {
      return false;
    }

    @Override
    public boolean canRemove() {
      return false;
    }

    @Override
    public void insertAfter(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertBefore(JsVisitable node) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isLvalue() {
      return false;
    }

    @Override
    public void removeMe() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void replaceMe(JsVisitable node) {
      throw new UnsupportedOperationException();
    }
  };

  public final <T extends JsVisitable> T accept(T node) {
    return this.doAccept(node);
  }

  public final <T extends JsVisitable> void acceptList(List<T> collection) {
    doAcceptList(collection);
  }

  public JsExpression acceptLvalue(JsExpression expr) {
    return doAcceptLvalue(expr);
  }

  public final <T extends JsVisitable> void acceptWithInsertRemove(List<T> collection) {
    doAcceptWithInsertRemove(collection);
  }

  public boolean didChange() {
    throw new UnsupportedOperationException();
  }

  public void endVisit(JsArrayAccess x, JsContext ctx) {
  }

  public void endVisit(JsArrayLiteral x, JsContext ctx) {
  }

  public void endVisit(JsBinaryOperation x, JsContext ctx) {
  }

  public void endVisit(JsBlock x, JsContext ctx) {
  }

  public void endVisit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
  }

  public void endVisit(JsBreak x, JsContext ctx) {
  }

  public void endVisit(JsCase x, JsContext ctx) {
  }

  public void endVisit(JsCatch x, JsContext ctx) {
  }

  public void endVisit(JsConditional x, JsContext ctx) {
  }

  public void endVisit(JsContinue x, JsContext ctx) {
  }

  public void endVisit(JsDebugger x, JsContext ctx) {
  }

  public void endVisit(JsDefault x, JsContext ctx) {
  }

  public void endVisit(JsDoWhile x, JsContext ctx) {
  }

  public void endVisit(JsEmpty x, JsContext ctx) {
  }

  public void endVisit(JsExprStmt x, JsContext ctx) {
  }

  public void endVisit(JsFor x, JsContext ctx) {
  }

  public void endVisit(JsForIn x, JsContext ctx) {
  }

  public void endVisit(JsFunction x, JsContext ctx) {
  }

  public void endVisit(JsIf x, JsContext ctx) {
  }

  public void endVisit(JsInvocation x, JsContext ctx) {
  }

  public void endVisit(JsLabel x, JsContext ctx) {
  }

  public void endVisit(JsNameRef x, JsContext ctx) {
  }

  public void endVisit(JsNew x, JsContext ctx) {
  }

  public void endVisit(JsNullLiteral x, JsContext ctx) {
  }

  public void endVisit(JsNumberLiteral x, JsContext ctx) {
  }

  public void endVisit(JsObjectLiteral x, JsContext ctx) {
  }

  public void endVisit(JsParameter x, JsContext ctx) {
  }

  public void endVisit(JsPostfixOperation x, JsContext ctx) {
  }

  public void endVisit(JsPrefixOperation x, JsContext ctx) {
  }

  public void endVisit(JsProgram x, JsContext ctx) {
  }

  public void endVisit(JsProgramFragment x, JsContext ctx) {
  }

  public void endVisit(JsPropertyInitializer x, JsContext ctx) {
  }

  public void endVisit(JsRegExp x, JsContext ctx) {
  }

  public void endVisit(JsReturn x, JsContext ctx) {
  }

  public void endVisit(JsStringLiteral x, JsContext ctx) {
  }

  public void endVisit(JsSwitch x, JsContext ctx) {
  }

  public void endVisit(JsLiteral.JsThisRef x, JsContext ctx) {
  }

  public void endVisit(JsThrow x, JsContext ctx) {
  }

  public void endVisit(JsTry x, JsContext ctx) {
  }

  public void endVisit(JsVar x, JsContext ctx) {
  }

  public void endVisit(JsVars x, JsContext ctx) {
  }

  public void endVisit(JsWhile x, JsContext ctx) {
  }

  public boolean visit(JsArrayAccess x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsArrayLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBinaryOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBlock x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsLiteral.JsBooleanLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsBreak x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsCase x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsCatch x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsConditional x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsContinue x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDebugger x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDefault x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsDoWhile x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsEmpty x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsExprStmt x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsFor x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsForIn x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsFunction x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsIf x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsInvocation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsLabel x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNameRef x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNew x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNullLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNumberLiteral.JsIntLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsNumberLiteral.JsDoubleLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsObjectLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsParameter x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPostfixOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPrefixOperation x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsProgram x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsProgramFragment x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsPropertyInitializer x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsRegExp x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsReturn x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsStringLiteral x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsSwitch x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsLiteral.JsThisRef x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsThrow x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsTry x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsVar x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsVars x, JsContext ctx) {
    return true;
  }

  public boolean visit(JsWhile x, JsContext ctx) {
    return true;
  }

  protected <T extends JsVisitable> T doAccept(T node) {
    doTraverse(node, UNMODIFIABLE_CONTEXT);
    return node;
  }

  protected <T extends JsVisitable> void doAcceptList(List<T> collection) {
    for (T node : collection) {
      doTraverse(node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected JsExpression doAcceptLvalue(JsExpression expr) {
    doTraverse(expr, LVALUE_CONTEXT);
    return expr;
  }

  protected <T extends JsVisitable> void doAcceptWithInsertRemove(List<T> collection) {
    for (T node : collection) {
      doTraverse(node, UNMODIFIABLE_CONTEXT);
    }
  }

  protected void doTraverse(JsVisitable node, JsContext ctx) {
    node.traverse(this, ctx);
  }
}
