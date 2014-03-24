/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.js;

import com.google.gwt.dev.jjs.Correlation.Literal;
import com.google.gwt.dev.jjs.SourceInfo;
import com.google.gwt.dev.jjs.SourceOrigin;
import com.google.gwt.dev.js.ast.JsArrayAccess;
import com.google.gwt.dev.js.ast.JsArrayLiteral;
import com.google.gwt.dev.js.ast.JsBinaryOperation;
import com.google.gwt.dev.js.ast.JsBinaryOperator;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsBooleanLiteral;
import com.google.gwt.dev.js.ast.JsBreak;
import com.google.gwt.dev.js.ast.JsCase;
import com.google.gwt.dev.js.ast.JsCatch;
import com.google.gwt.dev.js.ast.JsConditional;
import com.google.gwt.dev.js.ast.JsContinue;
import com.google.gwt.dev.js.ast.JsDebugger;
import com.google.gwt.dev.js.ast.JsDefault;
import com.google.gwt.dev.js.ast.JsDoWhile;
import com.google.gwt.dev.js.ast.JsEmpty;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFor;
import com.google.gwt.dev.js.ast.JsForIn;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsIf;
import com.google.gwt.dev.js.ast.JsInvocation;
import com.google.gwt.dev.js.ast.JsLabel;
import com.google.gwt.dev.js.ast.JsName;
import com.google.gwt.dev.js.ast.JsNameRef;
import com.google.gwt.dev.js.ast.JsNew;
import com.google.gwt.dev.js.ast.JsNode;
import com.google.gwt.dev.js.ast.JsNullLiteral;
import com.google.gwt.dev.js.ast.JsNumberLiteral;
import com.google.gwt.dev.js.ast.JsObjectLiteral;
import com.google.gwt.dev.js.ast.JsParameter;
import com.google.gwt.dev.js.ast.JsPostfixOperation;
import com.google.gwt.dev.js.ast.JsPrefixOperation;
import com.google.gwt.dev.js.ast.JsPropertyInitializer;
import com.google.gwt.dev.js.ast.JsRegExp;
import com.google.gwt.dev.js.ast.JsReturn;
import com.google.gwt.dev.js.ast.JsRootScope;
import com.google.gwt.dev.js.ast.JsScope;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsStringLiteral;
import com.google.gwt.dev.js.ast.JsSwitch;
import com.google.gwt.dev.js.ast.JsThisRef;
import com.google.gwt.dev.js.ast.JsThrow;
import com.google.gwt.dev.js.ast.JsTry;
import com.google.gwt.dev.js.ast.JsUnaryOperator;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsWhile;
import com.google.gwt.dev.js.rhino.Context;
import com.google.gwt.dev.js.rhino.ErrorReporter;
import com.google.gwt.dev.js.rhino.EvaluatorException;
import com.google.gwt.dev.js.rhino.IRFactory;
import com.google.gwt.dev.js.rhino.Node;
import com.google.gwt.dev.js.rhino.Parser;
import com.google.gwt.dev.js.rhino.TokenStream;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Parses JavaScript source.
 */
public class JsParser {

  public static List<JsStatement> parse(SourceInfo rootSourceInfo,
      JsScope scope, Reader r) throws IOException, JsParserException {
    return new JsParser().parseImpl(rootSourceInfo, scope, r);
  }

  public static void parseInto(SourceInfo rootSourceInfo, JsScope scope,
      JsBlock block, Reader r) throws IOException, JsParserException {
    List<JsStatement> childStmts = parse(rootSourceInfo, scope, r);
    List<JsStatement> parentStmts = block.getStatements();
    parentStmts.addAll(childStmts);
  }

  private final Stack<JsScope> scopeStack = new Stack<JsScope>();
  private final Stack<SourceInfo> sourceInfoStack = new Stack<SourceInfo>();

  private JsParser() {
  }

  List<JsStatement> parseImpl(final SourceInfo rootSourceInfo, JsScope scope,
      Reader r) throws JsParserException, IOException {
    // Create a custom error handler so that we can throw our own exceptions.
    Context.enter().setErrorReporter(new ErrorReporter() {
      @Override
      public void error(String msg, String loc, int ln, String src, int col) {
        throw new UncheckedJsParserException(new JsParserException(msg, ln,
            src, col, rootSourceInfo.getFileName()));
      }

      @Override
      public EvaluatorException runtimeError(String msg, String loc, int ln,
          String src, int col) {
        // Never called, but just in case.
        throw new UncheckedJsParserException(new JsParserException(msg, ln,
            src, col, rootSourceInfo.getFileName()));
      }

      @Override
      public void warning(String msg, String loc, int ln, String src, int col) {
        // Ignore warnings.
      }
    });
    try {
      // Parse using the Rhino parser.
      //
      TokenStream ts = new TokenStream(r, rootSourceInfo.getFileName(),
          rootSourceInfo.getStartLine());
      Parser parser = new Parser(new IRFactory(ts));
      Node topNode = (Node) parser.parse(ts);

      // Map the Rhino AST to ours.
      pushScope(scope, rootSourceInfo);
      List<JsStatement> stmts = mapStatements(topNode);
      popScope();
      return stmts;
    } catch (UncheckedJsParserException e) {
      throw e.getParserException();
    } finally {
      Context.exit();
    }
  }

  private JsParserException createParserException(String msg, Node offender) {
    return new JsParserException(msg, offender.getLineno(), null, 0,
        sourceInfoStack.peek().getFileName());
  }

  private JsScope getScope() {
    return scopeStack.peek();
  }

  private SourceInfo makeSourceInfo(Node node) {
    SourceInfo parent = sourceInfoStack.peek();
    int lineno = node.getLineno();
    if (lineno == -1) {
      // Rhino only reports line numbers for statement nodes, not expressions
      return parent;
    }
    return parent.makeChild(SourceOrigin.create(lineno, parent.getFileName()));
  }

  /**
   * Force a distinct child to be created, so correlations can be added.
   */
  private SourceInfo makeSourceInfoDistinct(Node node) {
    SourceInfo parent = sourceInfoStack.peek();
    int lineno = node.getLineno();
    if (lineno == -1) {
      // Rhino only reports line numbers for statement nodes, not expressions
      lineno = parent.getStartLine();
    }
    return parent.makeChild(SourceOrigin.create(lineno, parent.getFileName()));
  }

  private JsNode map(Node node) throws JsParserException {

    switch (node.getType()) {
      case TokenStream.SCRIPT: {
        JsBlock block = new JsBlock(makeSourceInfo(node));
        mapStatements(block.getStatements(), node);
        return block;
      }

      case TokenStream.DEBUGGER:
        return mapDebuggerStatement(node);

      case TokenStream.VOID:
        // VOID = nothing was parsed for this node
        return null;

      case TokenStream.EXPRSTMT:
        return mapExprStmt(node);

      case TokenStream.REGEXP:
        return mapRegExp(node);

      case TokenStream.ADD:
        return mapBinaryOperation(JsBinaryOperator.ADD, node);

      case TokenStream.SUB:
        return mapBinaryOperation(JsBinaryOperator.SUB, node);

      case TokenStream.MUL:
        return mapBinaryOperation(JsBinaryOperator.MUL, node);

      case TokenStream.DIV:
        return mapBinaryOperation(JsBinaryOperator.DIV, node);

      case TokenStream.MOD:
        return mapBinaryOperation(JsBinaryOperator.MOD, node);

      case TokenStream.AND:
        return mapBinaryOperation(JsBinaryOperator.AND, node);

      case TokenStream.OR:
        return mapBinaryOperation(JsBinaryOperator.OR, node);

      case TokenStream.BITAND:
        return mapBinaryOperation(JsBinaryOperator.BIT_AND, node);

      case TokenStream.BITOR:
        return mapBinaryOperation(JsBinaryOperator.BIT_OR, node);

      case TokenStream.BITXOR:
        return mapBinaryOperation(JsBinaryOperator.BIT_XOR, node);

      case TokenStream.ASSIGN:
        return mapAssignmentVariant(node);

      case TokenStream.RELOP:
        return mapRelationalVariant(node);

      case TokenStream.EQOP:
        return mapEqualityVariant(node);

      case TokenStream.SHOP:
        return mapShiftVariant(node);

      case TokenStream.UNARYOP:
        return mapUnaryVariant(node);

      case TokenStream.INC:
        return mapIncDecFixity(JsUnaryOperator.INC, node);

      case TokenStream.DEC:
        return mapIncDecFixity(JsUnaryOperator.DEC, node);

      case TokenStream.HOOK:
        return mapConditional(node);

      case TokenStream.STRING: {
        SourceInfo info = makeSourceInfoDistinct(node);
        info.addCorrelation(info.getCorrelator().by(Literal.STRING));
        return new JsStringLiteral(info, node.getString());
      }

      case TokenStream.NUMBER:
        return mapNumber(node);

      case TokenStream.CALL:
        return mapCall(node);

      case TokenStream.GETPROP:
        return mapGetProp(node);

      case TokenStream.SETPROP:
        return mapSetProp(node);

      case TokenStream.DELPROP:
        return mapDeleteProp(node);

      case TokenStream.IF:
        return mapIfStatement(node);

      case TokenStream.WHILE:
        return mapDoOrWhileStatement(true, node);

      case TokenStream.DO:
        return mapDoOrWhileStatement(false, node);

      case TokenStream.FOR:
        return mapForStatement(node);

      case TokenStream.WITH:
        return mapWithStatement(node);

      case TokenStream.GETELEM:
        return mapGetElem(node);

      case TokenStream.SETELEM:
        return mapSetElem(node);

      case TokenStream.FUNCTION:
        return mapFunction(node);

      case TokenStream.BLOCK:
        return mapBlock(node);

      case TokenStream.SETNAME:
        return mapBinaryOperation(JsBinaryOperator.ASG, node);

      case TokenStream.NAME:
      case TokenStream.BINDNAME:
        return mapName(node);

      case TokenStream.RETURN:
        return mapReturn(node);

      case TokenStream.BREAK:
        return mapBreak(node);

      case TokenStream.CONTINUE:
        return mapContinue(node);

      case TokenStream.OBJLIT:
        return mapObjectLit(node);

      case TokenStream.ARRAYLIT:
        return mapArrayLit(node);

      case TokenStream.VAR:
        return mapVar(node);

      case TokenStream.PRIMARY:
        return mapPrimary(node);

      case TokenStream.COMMA:
        return mapBinaryOperation(JsBinaryOperator.COMMA, node);

      case TokenStream.NEW:
        return mapNew(node);

      case TokenStream.THROW:
        return mapThrowStatement(node);

      case TokenStream.TRY:
        return mapTryStatement(node);

      case TokenStream.SWITCH:
        return mapSwitchStatement(node);

      case TokenStream.LABEL:
        return mapLabel(node);

      default:
        int tokenType = node.getType();
        throw createParserException("Unexpected top-level token type: "
            + tokenType, node);
    }
  }

  private JsArrayLiteral mapArrayLit(Node node) throws JsParserException {
    JsArrayLiteral toLit = new JsArrayLiteral(makeSourceInfo(node));
    Node from = node.getFirstChild();
    while (from != null) {
      toLit.getExpressions().add(mapExpression(from));
      from = from.getNext();
    }
    return toLit;
  }

  /**
   * Produces a {@link JsNameRef}.
   */
  private JsNameRef mapAsPropertyNameRef(Node nameRefNode)
      throws JsParserException {
    JsNode unknown = map(nameRefNode);
    // This is weird, but for "a.b", the rhino AST calls "b" a string literal.
    // However, since we know it's for a PROPGET, we can unstringliteralize it.
    //
    if (unknown instanceof JsStringLiteral) {
      JsStringLiteral lit = (JsStringLiteral) unknown;
      String litName = lit.getValue();
      return new JsNameRef(makeSourceInfo(nameRefNode), litName);
    } else {
      throw createParserException("Expecting a name reference", nameRefNode);
    }
  }

  private JsExpression mapAssignmentVariant(Node asgNode)
      throws JsParserException {
    switch (asgNode.getIntDatum()) {
      case TokenStream.NOP:
        return mapBinaryOperation(JsBinaryOperator.ASG, asgNode);

      case TokenStream.ADD:
        return mapBinaryOperation(JsBinaryOperator.ASG_ADD, asgNode);

      case TokenStream.SUB:
        return mapBinaryOperation(JsBinaryOperator.ASG_SUB, asgNode);

      case TokenStream.MUL:
        return mapBinaryOperation(JsBinaryOperator.ASG_MUL, asgNode);

      case TokenStream.DIV:
        return mapBinaryOperation(JsBinaryOperator.ASG_DIV, asgNode);

      case TokenStream.MOD:
        return mapBinaryOperation(JsBinaryOperator.ASG_MOD, asgNode);

      case TokenStream.BITAND:
        return mapBinaryOperation(JsBinaryOperator.ASG_BIT_AND, asgNode);

      case TokenStream.BITOR:
        return mapBinaryOperation(JsBinaryOperator.ASG_BIT_OR, asgNode);

      case TokenStream.BITXOR:
        return mapBinaryOperation(JsBinaryOperator.ASG_BIT_XOR, asgNode);

      case TokenStream.LSH:
        return mapBinaryOperation(JsBinaryOperator.ASG_SHL, asgNode);

      case TokenStream.RSH:
        return mapBinaryOperation(JsBinaryOperator.ASG_SHR, asgNode);

      case TokenStream.URSH:
        return mapBinaryOperation(JsBinaryOperator.ASG_SHRU, asgNode);

      default:
        throw createParserException("Unknown assignment operator variant: "
            + asgNode.getIntDatum(), asgNode);
    }
  }

  private JsExpression mapBinaryOperation(JsBinaryOperator op, Node node)
      throws JsParserException {
    Node from1 = node.getFirstChild();
    Node from2 = from1.getNext();

    JsExpression to1 = mapExpression(from1);
    JsExpression to2 = mapExpression(from2);

    return new JsBinaryOperation(makeSourceInfo(node), op, to1, to2);
  }

  private JsBlock mapBlock(Node nodeStmts) throws JsParserException {
    SourceInfo info = makeSourceInfo(nodeStmts);
    JsBlock block = new JsBlock(info);
    pushSourceInfo(info);
    mapStatements(block.getStatements(), nodeStmts);
    popSourceInfo();
    return block;
  }

  private JsBreak mapBreak(Node breakNode) {
    Node fromLabel = breakNode.getFirstChild();
    if (fromLabel != null) {
      return new JsBreak(makeSourceInfo(breakNode), mapName(fromLabel));
    } else {
      return new JsBreak(makeSourceInfo(breakNode));
    }
  }

  private JsInvocation mapCall(Node callNode) throws JsParserException {
    JsInvocation invocation = new JsInvocation(makeSourceInfo(callNode));

    // Map the target expression.
    //
    Node from = callNode.getFirstChild();
    JsExpression to = mapExpression(from);
    invocation.setQualifier(to);

    // Iterate over and map the arguments.
    //
    List<JsExpression> args = invocation.getArguments();
    from = from.getNext();
    while (from != null) {
      to = mapExpression(from);
      args.add(to);
      from = from.getNext();
    }

    return invocation;
  }

  private JsExpression mapConditional(Node condNode) throws JsParserException {
    JsConditional toCond = new JsConditional(makeSourceInfo(condNode));

    Node fromTest = condNode.getFirstChild();
    toCond.setTestExpression(mapExpression(fromTest));

    Node fromThen = fromTest.getNext();
    toCond.setThenExpression(mapExpression(fromThen));

    Node fromElse = fromThen.getNext();
    toCond.setElseExpression(mapExpression(fromElse));

    return toCond;
  }

  private JsContinue mapContinue(Node contNode) {
    Node fromLabel = contNode.getFirstChild();
    if (fromLabel != null) {
      return new JsContinue(makeSourceInfo(contNode), mapName(fromLabel));
    } else {
      return new JsContinue(makeSourceInfo(contNode));
    }
  }

  private JsStatement mapDebuggerStatement(Node node) {
    // Calls an optional method to invoke the debugger.
    //
    return new JsDebugger(makeSourceInfo(node));
  }

  private JsExpression mapDeleteProp(Node node) throws JsParserException {
    Node from = node.getFirstChild();
    JsExpression to = mapExpression(from);
    if (to instanceof JsNameRef) {
      return new JsPrefixOperation(makeSourceInfo(node),
          JsUnaryOperator.DELETE, to);
    } else if (to instanceof JsArrayAccess) {
      return new JsPrefixOperation(makeSourceInfo(node),
          JsUnaryOperator.DELETE, to);
    } else {
      throw createParserException(
          "'delete' can only operate on property names and array elements",
          from);
    }
  }

  private JsStatement mapDoOrWhileStatement(boolean isWhile, Node ifNode)
      throws JsParserException {

    // Pull out the pieces we want to map.
    //
    Node fromTestExpr;
    Node fromBody;
    if (isWhile) {
      fromTestExpr = ifNode.getFirstChild();
      fromBody = ifNode.getFirstChild().getNext();
    } else {
      fromBody = ifNode.getFirstChild();
      fromTestExpr = ifNode.getFirstChild().getNext();
    }

    SourceInfo info = makeSourceInfo(ifNode);
    pushSourceInfo(info);

    // Map the test expression.
    //
    JsExpression toTestExpr = mapExpression(fromTestExpr);

    // Map the body block.
    //
    JsStatement toBody = mapStatement(fromBody);

    popSourceInfo();

    // Create and attach the "while" or "do" statement we're mapping to.
    //
    if (isWhile) {
      return new JsWhile(info, toTestExpr, toBody);
    } else {
      return new JsDoWhile(info, toTestExpr, toBody);
    }
  }

  private JsExpression mapEqualityVariant(Node eqNode) throws JsParserException {
    switch (eqNode.getIntDatum()) {
      case TokenStream.EQ:
        return mapBinaryOperation(JsBinaryOperator.EQ, eqNode);

      case TokenStream.NE:
        return mapBinaryOperation(JsBinaryOperator.NEQ, eqNode);

      case TokenStream.SHEQ:
        return mapBinaryOperation(JsBinaryOperator.REF_EQ, eqNode);

      case TokenStream.SHNE:
        return mapBinaryOperation(JsBinaryOperator.REF_NEQ, eqNode);

      case TokenStream.LT:
        return mapBinaryOperation(JsBinaryOperator.LT, eqNode);

      case TokenStream.LE:
        return mapBinaryOperation(JsBinaryOperator.LTE, eqNode);

      case TokenStream.GT:
        return mapBinaryOperation(JsBinaryOperator.GT, eqNode);

      case TokenStream.GE:
        return mapBinaryOperation(JsBinaryOperator.GTE, eqNode);

      default:
        throw createParserException("Unknown equality operator variant: "
            + eqNode.getIntDatum(), eqNode);
    }
  }

  private JsExpression mapExpression(Node exprNode) throws JsParserException {
    JsNode unknown = map(exprNode);
    if (unknown instanceof JsExpression) {
      return (JsExpression) unknown;
    } else {
      throw createParserException("Expecting an expression", exprNode);
    }
  }

  private JsExprStmt mapExprStmt(Node node) throws JsParserException {
    pushSourceInfo(makeSourceInfo(node));
    JsExpression expr = mapExpression(node.getFirstChild());
    popSourceInfo();
    return expr.makeStmt();
  }

  private JsStatement mapForStatement(Node forNode) throws JsParserException {
    Node fromInit = forNode.getFirstChild();
    Node fromTest = fromInit.getNext();
    Node fromIncr = fromTest.getNext();
    Node fromBody = fromIncr.getNext();

    SourceInfo info = makeSourceInfo(forNode);
    if (fromBody == null) {
      // This could be a "for...in" structure.
      // We could based on the different child layout.
      //
      Node fromIter = forNode.getFirstChild();
      Node fromObjExpr = fromIter.getNext();
      fromBody = fromObjExpr.getNext();

      JsForIn toForIn;
      if (fromIter.getType() == TokenStream.VAR) {
        // A named iterator var.
        //
        Node fromIterVarName = fromIter.getFirstChild();
        String fromName = fromIterVarName.getString();
        JsName toName = getScope().declareName(fromName);
        toForIn = new JsForIn(info, toName);
        Node fromIterInit = fromIterVarName.getFirstChild();
        if (fromIterInit != null) {
          // That has an initializer expression (useful only for side effects).
          //
          toForIn.setIterExpr(mapOptionalExpression(fromIterInit));
        }
      } else {
        // An unnamed iterator var.
        //
        toForIn = new JsForIn(info);
        toForIn.setIterExpr(mapExpression(fromIter));
      }
      toForIn.setObjExpr(mapExpression(fromObjExpr));

      // The body stmt.
      //
      JsStatement bodyStmt = mapStatement(fromBody);
      if (bodyStmt != null) {
        toForIn.setBody(bodyStmt);
      } else {
        toForIn.setBody(new JsEmpty(info));
      }

      return toForIn;
    } else {
      // Regular ol' for loop.
      //
      JsFor toFor = new JsFor(info);

      // The first item is either an expression or a JsVars.
      JsNode initThingy = map(fromInit);
      if (initThingy != null) {
        if (initThingy instanceof JsVars) {
          toFor.setInitVars((JsVars) initThingy);
        } else {
          assert (initThingy instanceof JsExpression);
          toFor.setInitExpr((JsExpression) initThingy);
        }
      }
      toFor.setCondition(mapOptionalExpression(fromTest));
      toFor.setIncrExpr(mapOptionalExpression(fromIncr));

      JsStatement bodyStmt = mapStatement(fromBody);
      if (bodyStmt != null) {
        toFor.setBody(bodyStmt);
      } else {
        toFor.setBody(new JsEmpty(info));
      }
      return toFor;
    }
  }

  private JsExpression mapFunction(Node fnNode) throws JsParserException {

    Node fromFnNameNode = fnNode.getFirstChild();
    Node fromParamNode = fnNode.getFirstChild().getNext().getFirstChild();
    Node fromBodyNode = fnNode.getFirstChild().getNext().getNext();

    // Decide the function's name, if any.
    //
    String fromFnName = fromFnNameNode.getString();
    JsName toFnName = null;
    if (fromFnName != null && fromFnName.length() > 0) {
      toFnName = getScope().declareName(fromFnName);
    }

    // Create it, and set the params.
    //
    SourceInfo fnSourceInfo = makeSourceInfo(fnNode);
    JsFunction toFn = new JsFunction(fnSourceInfo, getScope(), toFnName);

    // Creating a function also creates a new scope, which we push onto
    // the scope stack.
    //
    pushScope(toFn.getScope(), fnSourceInfo);

    while (fromParamNode != null) {
      String fromParamName = fromParamNode.getString();
      // should this be unique? I think not since you can have dup args.
      JsName paramName = toFn.getScope().declareName(fromParamName);
      toFn.getParameters().add(new JsParameter(fnSourceInfo, paramName));
      fromParamNode = fromParamNode.getNext();
    }

    // Map the function's body.
    //
    JsBlock toBody = mapBlock(fromBodyNode);
    toFn.setBody(toBody);

    // Pop the new function's scope off of the scope stack.
    //
    popScope();

    return toFn;
  }

  private JsArrayAccess mapGetElem(Node getElemNode) throws JsParserException {
    Node from1 = getElemNode.getFirstChild();
    Node from2 = from1.getNext();

    JsExpression to1 = mapExpression(from1);
    JsExpression to2 = mapExpression(from2);

    return new JsArrayAccess(makeSourceInfo(getElemNode), to1, to2);
  }

  private JsNameRef mapGetProp(Node getPropNode) throws JsParserException {
    Node from1 = getPropNode.getFirstChild();
    Node from2 = from1.getNext();

    JsExpression toQualifier = mapExpression(from1);
    JsNameRef toNameRef;
    if (from2 != null) {
      toNameRef = mapAsPropertyNameRef(from2);
    } else {
      // Special properties don't have a second expression.
      //
      Object obj = getPropNode.getProp(Node.SPECIAL_PROP_PROP);
      assert (obj instanceof String);
      toNameRef = new JsNameRef(makeSourceInfo(getPropNode), (String) obj);
    }
    toNameRef.setQualifier(toQualifier);

    return toNameRef;
  }

  private JsIf mapIfStatement(Node ifNode) throws JsParserException {

    // Pull out the pieces we want to map.
    //
    Node fromTestExpr = ifNode.getFirstChild();
    Node fromThenBlock = ifNode.getFirstChild().getNext();
    Node fromElseBlock = ifNode.getFirstChild().getNext().getNext();

    // Create the "if" statement we're mapping to.
    //
    JsIf toIf = new JsIf(makeSourceInfo(ifNode));

    // Map the test expression.
    //
    JsExpression toTestExpr = mapExpression(fromTestExpr);
    toIf.setIfExpr(toTestExpr);

    // Map the "then" block.
    //
    toIf.setThenStmt(mapStatement(fromThenBlock));

    // Map the "else" block.
    //
    if (fromElseBlock != null) {
      toIf.setElseStmt(mapStatement(fromElseBlock));
    }

    return toIf;
  }

  private JsExpression mapIncDecFixity(JsUnaryOperator op, Node node)
      throws JsParserException {
    switch (node.getIntDatum()) {
      case TokenStream.PRE:
        return mapPrefixOperation(op, node);
      case TokenStream.POST:
        return mapPostfixOperation(op, node);
      default:
        throw createParserException(
            "Unknown prefix/postfix variant: " + node.getIntDatum(), node);
    }
  }

  private JsLabel mapLabel(Node labelNode) throws JsParserException {
    String fromName = labelNode.getFirstChild().getString();
    JsName toName = getScope().declareName(fromName);
    Node fromStmt = labelNode.getFirstChild().getNext();
    JsLabel toLabel = new JsLabel(makeSourceInfo(labelNode), toName);
    toLabel.setStmt(mapStatement(fromStmt));
    return toLabel;
  }

  /**
   * Creates a reference to a name that may or may not be obfuscatable, based on
   * whether it matches a known name in the scope.
   */
  private JsNameRef mapName(Node node) {
    String ident = node.getString();
    return new JsNameRef(makeSourceInfo(node), ident);
  }

  private JsNew mapNew(Node newNode) throws JsParserException {
    // Map the constructor expression, which is often just the name of
    // some lambda.
    //
    Node fromCtorExpr = newNode.getFirstChild();
    JsNew newExpr = new JsNew(makeSourceInfo(newNode),
        mapExpression(fromCtorExpr));

    // Iterate over and map the arguments.
    //
    List<JsExpression> args = newExpr.getArguments();
    Node fromArg = fromCtorExpr.getNext();
    while (fromArg != null) {
      args.add(mapExpression(fromArg));
      fromArg = fromArg.getNext();
    }

    return newExpr;
  }

  private JsExpression mapNumber(Node numberNode) {
    return new JsNumberLiteral(makeSourceInfo(numberNode),
        numberNode.getDouble());
  }

  private JsExpression mapObjectLit(Node objLitNode) throws JsParserException {
    JsObjectLiteral toLit = new JsObjectLiteral(makeSourceInfo(objLitNode));
    Node fromPropInit = objLitNode.getFirstChild();
    while (fromPropInit != null) {

      Node fromLabelExpr = fromPropInit;
      JsExpression toLabelExpr = mapExpression(fromLabelExpr);

      // Advance to the initializer expression.
      //
      fromPropInit = fromPropInit.getNext();
      Node fromValueExpr = fromPropInit;
      if (fromValueExpr == null) {
        throw createParserException("Expected an init expression for: "
            + toLabelExpr, objLitNode);
      }
      JsExpression toValueExpr = mapExpression(fromValueExpr);

      JsPropertyInitializer toPropInit = new JsPropertyInitializer(
          makeSourceInfo(fromLabelExpr), toLabelExpr, toValueExpr);
      toLit.getPropertyInitializers().add(toPropInit);

      // Begin the next property initializer, if there is one.
      //
      fromPropInit = fromPropInit.getNext();
    }

    return toLit;
  }

  private JsExpression mapOptionalExpression(Node exprNode)
      throws JsParserException {
    JsNode unknown = map(exprNode);
    if (unknown != null) {
      if (unknown instanceof JsExpression) {
        return (JsExpression) unknown;
      } else {
        throw createParserException("Expecting an expression or null", exprNode);
      }
    }
    return null;
  }

  private JsExpression mapPostfixOperation(JsUnaryOperator op, Node node)
      throws JsParserException {
    Node from = node.getFirstChild();
    JsExpression to = mapExpression(from);
    return new JsPostfixOperation(makeSourceInfo(node), op, to);
  }

  private JsExpression mapPrefixOperation(JsUnaryOperator op, Node node)
      throws JsParserException {
    Node from = node.getFirstChild();
    JsExpression to = mapExpression(from);
    return new JsPrefixOperation(makeSourceInfo(node), op, to);
  }

  private JsExpression mapPrimary(Node node) throws JsParserException {
    switch (node.getIntDatum()) {
      case TokenStream.THIS:
        return new JsThisRef(makeSourceInfo(node));

      case TokenStream.TRUE:
        return JsBooleanLiteral.TRUE;

      case TokenStream.FALSE:
        return JsBooleanLiteral.FALSE;

      case TokenStream.NULL:
        return JsNullLiteral.INSTANCE;

      case TokenStream.UNDEFINED:
        return new JsNameRef(makeSourceInfo(node),
            JsRootScope.INSTANCE.getUndefined());

      default:
        throw createParserException("Unknown primary: " + node.getIntDatum(),
            node);
    }
  }

  private JsNode mapRegExp(Node regExpNode) {
    JsRegExp toRegExp = new JsRegExp(makeSourceInfo(regExpNode));

    Node fromPattern = regExpNode.getFirstChild();
    toRegExp.setPattern(fromPattern.getString());

    Node fromFlags = fromPattern.getNext();
    if (fromFlags != null) {
      toRegExp.setFlags(fromFlags.getString());
    }

    return toRegExp;
  }

  private JsExpression mapRelationalVariant(Node relNode)
      throws JsParserException {
    switch (relNode.getIntDatum()) {
      case TokenStream.LT:
        return mapBinaryOperation(JsBinaryOperator.LT, relNode);

      case TokenStream.LE:
        return mapBinaryOperation(JsBinaryOperator.LTE, relNode);

      case TokenStream.GT:
        return mapBinaryOperation(JsBinaryOperator.GT, relNode);

      case TokenStream.GE:
        return mapBinaryOperation(JsBinaryOperator.GTE, relNode);

      case TokenStream.INSTANCEOF:
        return mapBinaryOperation(JsBinaryOperator.INSTANCEOF, relNode);

      case TokenStream.IN:
        return mapBinaryOperation(JsBinaryOperator.INOP, relNode);

      default:
        throw createParserException("Unknown relational operator variant: "
            + relNode.getIntDatum(), relNode);
    }
  }

  private JsReturn mapReturn(Node returnNode) throws JsParserException {
    SourceInfo info = makeSourceInfo(returnNode);
    JsReturn toReturn = new JsReturn(info);
    pushSourceInfo(info);
    Node from = returnNode.getFirstChild();
    if (from != null) {
      JsExpression to = mapExpression(from);
      toReturn.setExpr(to);
    }

    popSourceInfo();
    return toReturn;
  }

  private JsExpression mapSetElem(Node setElemNode) throws JsParserException {
    // Reuse the get elem code.
    //
    JsArrayAccess lhs = mapGetElem(setElemNode);

    // Map the RHS.
    //
    Node fromRhs = setElemNode.getFirstChild().getNext().getNext();
    JsExpression toRhs = mapExpression(fromRhs);

    return new JsBinaryOperation(makeSourceInfo(setElemNode),
        JsBinaryOperator.ASG, lhs, toRhs);
  }

  private JsExpression mapSetProp(Node getPropNode) throws JsParserException {
    // Reuse the get prop code.
    //
    JsNameRef lhs = mapGetProp(getPropNode);

    // Map the RHS.
    //
    Node fromRhs = getPropNode.getFirstChild().getNext().getNext();
    JsExpression toRhs = mapExpression(fromRhs);

    return new JsBinaryOperation(makeSourceInfo(getPropNode),
        JsBinaryOperator.ASG, lhs, toRhs);
  }

  private JsExpression mapShiftVariant(Node shiftNode) throws JsParserException {
    switch (shiftNode.getIntDatum()) {
      case TokenStream.LSH:
        return mapBinaryOperation(JsBinaryOperator.SHL, shiftNode);

      case TokenStream.RSH:
        return mapBinaryOperation(JsBinaryOperator.SHR, shiftNode);

      case TokenStream.URSH:
        return mapBinaryOperation(JsBinaryOperator.SHRU, shiftNode);

      default:
        throw createParserException("Unknown equality operator variant: "
            + shiftNode.getIntDatum(), shiftNode);
    }
  }

  private JsStatement mapStatement(Node nodeStmt) throws JsParserException {
    JsNode unknown = map(nodeStmt);
    if (unknown != null) {
      if (unknown instanceof JsStatement) {
        return (JsStatement) unknown;
      } else if (unknown instanceof JsExpression) {
        return ((JsExpression) unknown).makeStmt();
      } else {
        throw createParserException("Expecting a statement", nodeStmt);
      }
    } else {
      // When map() returns null, we return an empty statement.
      //
      return new JsEmpty(makeSourceInfo(nodeStmt));
    }
  }

  private void mapStatements(List<JsStatement> stmts, Node nodeStmts)
      throws JsParserException {
    Node curr = nodeStmts.getFirstChild();
    while (curr != null) {
      JsStatement stmt = mapStatement(curr);
      if (stmt != null) {
        stmts.add(stmt);
      } else {
        // When mapStatement() returns null, we just ignore it.
        //
      }
      curr = curr.getNext();
    }
  }

  private List<JsStatement> mapStatements(Node nodeStmts)
      throws JsParserException {
    List<JsStatement> stmts = new ArrayList<JsStatement>();
    mapStatements(stmts, nodeStmts);
    return stmts;
  }

  private JsSwitch mapSwitchStatement(Node switchNode) throws JsParserException {
    SourceInfo info = makeSourceInfo(switchNode);
    JsSwitch toSwitch = new JsSwitch(info);
    pushSourceInfo(info);

    // The switch expression.
    //
    Node fromSwitchExpr = switchNode.getFirstChild();
    toSwitch.setExpr(mapExpression(fromSwitchExpr));

    // The members.
    //
    Node fromMember = fromSwitchExpr.getNext();
    while (fromMember != null) {
      if (fromMember.getType() == TokenStream.CASE) {
        JsCase toCase = new JsCase(makeSourceInfo(fromMember));

        // Set the case expression. In JS, this can be any expression.
        //
        Node fromCaseExpr = fromMember.getFirstChild();
        toCase.setCaseExpr(mapExpression(fromCaseExpr));

        // Set the case statements.
        //
        Node fromCaseBlock = fromCaseExpr.getNext();
        mapStatements(toCase.getStmts(), fromCaseBlock);

        // Attach the case to the switch.
        //
        toSwitch.getCases().add(toCase);
      } else {
        // This should be the only default statement.
        // If more than one is present, we keep the last one.
        //
        assert (fromMember.getType() == TokenStream.DEFAULT);
        JsDefault toDefault = new JsDefault(makeSourceInfo(fromMember));

        // Set the default statements.
        //
        Node fromDefaultBlock = fromMember.getFirstChild();
        mapStatements(toDefault.getStmts(), fromDefaultBlock);

        // Attach the default to the switch.
        //
        toSwitch.getCases().add(toDefault);
      }
      fromMember = fromMember.getNext();
    }

    popSourceInfo();
    return toSwitch;
  }

  private JsThrow mapThrowStatement(Node throwNode) throws JsParserException {
    SourceInfo info = makeSourceInfo(throwNode);
    pushSourceInfo(info);

    // Create, map, and attach.
    //
    Node fromExpr = throwNode.getFirstChild();
    JsThrow toThrow = new JsThrow(info, mapExpression(fromExpr));

    popSourceInfo();
    return toThrow;
  }

  private JsTry mapTryStatement(Node tryNode) throws JsParserException {
    JsTry toTry = new JsTry(makeSourceInfo(tryNode));

    // Map the "try" body.
    //
    Node fromTryBody = tryNode.getFirstChild();
    toTry.setTryBlock(mapBlock(fromTryBody));

    // Map zero or more catch blocks.
    //
    Node fromCatchNodes = fromTryBody.getNext();
    Node fromCatchNode = fromCatchNodes.getFirstChild();
    while (fromCatchNode != null) {
      assert (fromCatchNode.getType() == TokenStream.CATCH);
      // Map the catch variable.
      //
      Node fromCatchVarName = fromCatchNode.getFirstChild();
      JsCatch catchBlock = new JsCatch(makeSourceInfo(fromCatchNode),
          getScope(), fromCatchVarName.getString());

      // Pre-advance to the next catch block, if any.
      // We do this here to decide whether or not this is the last one.
      //
      fromCatchNode = fromCatchNode.getNext();

      // Map the condition, with a little fixup based on whether or not
      // this is the last catch block.
      //
      Node fromCondition = fromCatchVarName.getNext();
      JsExpression toCondition = mapExpression(fromCondition);
      catchBlock.setCondition(toCondition);
      if (fromCatchNode == null) {
        if (toCondition instanceof JsBooleanLiteral) {
          if (((JsBooleanLiteral) toCondition).getValue()) {
            // Actually, this is an unconditional catch block.
            // Indicate that by nulling the condition.
            //
            catchBlock.setCondition(null);
          }
        }
      }

      // Map the catch body.
      //
      Node fromCatchBody = fromCondition.getNext();
      pushScope(catchBlock.getScope(), catchBlock.getSourceInfo());
      catchBlock.setBody(mapBlock(fromCatchBody));
      popScope();

      // Attach it.
      //
      toTry.getCatches().add(catchBlock);
    }

    Node fromFinallyNode = fromCatchNodes.getNext();
    if (fromFinallyNode != null) {
      toTry.setFinallyBlock(mapBlock(fromFinallyNode));
    }

    return toTry;
  }

  private JsExpression mapUnaryVariant(Node unOp) throws JsParserException {
    switch (unOp.getIntDatum()) {
      case TokenStream.SUB:
        return mapPrefixOperation(JsUnaryOperator.NEG, unOp);

      case TokenStream.NOT:
        return mapPrefixOperation(JsUnaryOperator.NOT, unOp);

      case TokenStream.BITNOT:
        return mapPrefixOperation(JsUnaryOperator.BIT_NOT, unOp);

      case TokenStream.TYPEOF:
        return mapPrefixOperation(JsUnaryOperator.TYPEOF, unOp);

      case TokenStream.ADD:
        if (unOp.getFirstChild().getType() != TokenStream.NUMBER) {
          return mapPrefixOperation(JsUnaryOperator.POS, unOp);
        } else {
          // Pretend we didn't see it.
          return mapExpression(unOp.getFirstChild());
        }

      case TokenStream.VOID:
        return mapPrefixOperation(JsUnaryOperator.VOID, unOp);

      default:
        throw createParserException(
            "Unknown unary operator variant: " + unOp.getIntDatum(), unOp);
    }
  }

  private JsVars mapVar(Node varNode) throws JsParserException {
    SourceInfo info = makeSourceInfo(varNode);
    pushSourceInfo(info);
    JsVars toVars = new JsVars(info);
    Node fromVar = varNode.getFirstChild();
    while (fromVar != null) {
      // Use a conservative name allocation strategy that allocates all names
      // from the function's scope, even the names of properties in field
      // literals.
      //
      String fromName = fromVar.getString();
      JsName toName = getScope().declareName(fromName);
      JsVars.JsVar toVar = new JsVars.JsVar(makeSourceInfo(fromVar), toName);

      Node fromInit = fromVar.getFirstChild();
      if (fromInit != null) {
        JsExpression toInit = mapExpression(fromInit);
        toVar.setInitExpr(toInit);
      }
      toVars.add(toVar);

      fromVar = fromVar.getNext();
    }

    popSourceInfo();
    return toVars;
  }

  private JsNode mapWithStatement(Node withNode) throws JsParserException {
    // The "with" statement is unsupported because it introduces ambiguity
    // related to whether or not a name is obfuscatable that we cannot resolve
    // statically. This is modified in our copy of the Rhino Parser to provide
    // detailed source & line info. So, this method should never actually be
    // called.
    //
    throw createParserException("Internal error: unexpected token 'with'",
        withNode);
  }

  private void popScope() {
    scopeStack.pop();
    sourceInfoStack.pop();
  }

  private void popSourceInfo() {
    sourceInfoStack.pop();
  }

  private void pushScope(JsScope scope, SourceInfo sourceInfo) {
    scopeStack.push(scope);
    sourceInfoStack.push(sourceInfo);
  }

  /**
   * This should be called when processing any Rhino statement Node that has
   * line number data so that enclosed expressions will have a useful source
   * location.
   * 
   * @see Node#hasLineno
   */
  private void pushSourceInfo(SourceInfo sourceInfo) {
    assert sourceInfo.getStartLine() >= 0 : "Bad SourceInfo line number";
    sourceInfoStack.push(sourceInfo);
  }
}
