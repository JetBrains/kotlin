// Copyright 2011, the Dart project authors. All rights reserved.
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above
//    copyright notice, this list of conditions and the following
//    disclaimer in the documentation and/or other materials provided
//    with the distribution.
//  * Neither the name of Google Inc. nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package compiler.backend.js;

import compiler.InternalCompilerException;
import compiler.backend.js.ast.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Implements actual cloning logic. We rely on the JsExpressions to provide
 * traversal logic. The {@link #stack} field is used to accumulate
 * already-cloned JsExpression instances. One gotcha that falls out of this is
 * that argument lists are on the stack in reverse order, so lists should be
 * constructed via inserts, rather than appends.
 */
public class Cloner extends JsVisitor {
  protected final Stack<JsExpression> stack = new Stack<JsExpression>();
  private boolean successful = true;

  /**
   * @param expression
   * @return Return a clone of the expression tree
   */
  public static JsExpression clone(JsExpression expression) {
    Cloner c = new Cloner();
    c.accept(expression);
    return c.getExpression();
  }

  @Override
  public void endVisit(JsArrayAccess x, JsContext ctx) {
    JsArrayAccess newExpression = new JsArrayAccess();
    newExpression.setIndexExpr(stack.pop());
    newExpression.setArrayExpr(stack.pop());
    stack.push(newExpression);
  }

  @Override
  public void endVisit(JsArrayLiteral x, JsContext ctx) {
    JsArrayLiteral toReturn = new JsArrayLiteral();
    List<JsExpression> expressions = toReturn.getExpressions();
    int size = x.getExpressions().size();
    while (size-- > 0) {
      expressions.add(0, stack.pop());
    }
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsBinaryOperation x, JsContext ctx) {
    JsBinaryOperation toReturn = new JsBinaryOperation(x.getOperator());
    toReturn.setArg2(stack.pop());
    toReturn.setArg1(stack.pop());
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsBooleanLiteral x, JsContext ctx) {
    stack.push(x);
  }

  @Override
  public void endVisit(JsConditional x, JsContext ctx) {
    JsConditional toReturn = new JsConditional();
    toReturn.setElseExpression(stack.pop());
    toReturn.setThenExpression(stack.pop());
    toReturn.setTestExpression(stack.pop());
    stack.push(toReturn);
  }

  /**
   * The only functions that would get be visited are those being used as
   * first-class objects.
   */
  @Override
  public void endVisit(JsFunction x, JsContext ctx) {
    // Set a flag to indicate that we cannot continue, and push a null so
    // we don't run out of elements on the stack.
    successful = false;
    stack.push(null);
  }

  /**
   * Cloning the invocation allows us to modify it without damaging other call
   * sites.
   */
  @Override
  public void endVisit(JsInvocation x, JsContext ctx) {
    JsInvocation toReturn = new JsInvocation();
    List<JsExpression> params = toReturn.getArguments();
    int size = x.getArguments().size();
    while (size-- > 0) {
      params.add(0, stack.pop());
    }
    toReturn.setQualifier(stack.pop());
    stack.push(toReturn);
  }

  /**
   * Do a deep clone of a JsNameRef. Because JsNameRef chains are shared
   * throughout the AST, you can't just go and change their qualifiers when
   * re-writing an invocation.
   */
  @Override
  public void endVisit(JsNameRef x, JsContext ctx) {
    JsNameRef toReturn;
    JsName name = x.getName();
    if (name != null) {
      toReturn = new JsNameRef(name);
    } else {
      toReturn = new JsNameRef(x.getIdent());
    }

    if (x.getQualifier() != null) {
      toReturn.setQualifier(stack.pop());
    }
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsNew x, JsContext ctx) {
    int size = x.getArguments().size();
    List<JsExpression> arguments = new ArrayList<JsExpression>(size);
    while (size-- > 0) {
      arguments.add(0, stack.pop());
    }
    JsNew toReturn = new JsNew(stack.pop());
    toReturn.getArguments().addAll(arguments);
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsNullLiteral x, JsContext ctx) {
    stack.push(x);
  }

  @Override
  public void endVisit(JsNumberLiteral x, JsContext ctx) {
    stack.push(x);
  }

  @Override
  public void endVisit(JsObjectLiteral x, JsContext ctx) {
    JsObjectLiteral toReturn = new JsObjectLiteral();
    List<JsPropertyInitializer> inits = toReturn.getPropertyInitializers();

    int size = x.getPropertyInitializers().size();
    while (size-- > 0) {
      /*
       * JsPropertyInitializers are the only non-JsExpression objects that we
       * care about, so we just go ahead and create the objects in the loop,
       * rather than expecting it to be on the stack and having to perform
       * narrowing casts at all stack.pop() invocations.
       */
      JsPropertyInitializer newInit = new JsPropertyInitializer();
      newInit.setValueExpr(stack.pop());
      newInit.setLabelExpr(stack.pop());

      inits.add(0, newInit);
    }
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsPostfixOperation x, JsContext ctx) {
    JsPostfixOperation toReturn = new JsPostfixOperation(x.getOperator());
    toReturn.setArg(stack.pop());
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsPrefixOperation x, JsContext ctx) {
    JsPrefixOperation toReturn = new JsPrefixOperation(x.getOperator());
    toReturn.setArg(stack.pop());
    stack.push(toReturn);
  }

  @Override
  public void endVisit(JsRegExp x, JsContext ctx) {
    stack.push(x);
  }

  @Override
  public void endVisit(JsStringLiteral x, JsContext ctx) {
    stack.push(x);
  }

  @Override
  public void endVisit(JsThisRef x, JsContext ctx) {
    stack.push(new JsThisRef());
  }

  public JsExpression getExpression() {
    return (successful && checkStack()) ? stack.peek() : null;
  }

  private boolean checkStack() {
    if (stack.size() > 1) {
      throw new InternalCompilerException("Too many expressions on stack");
    }

    return stack.size() == 1;
  }
}

