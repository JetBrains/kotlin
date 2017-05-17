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

import com.google.gwt.dev.js.parserExceptions.JsParserException;
import com.google.gwt.dev.js.rhino.CodePosition;
import com.google.gwt.dev.js.rhino.Node;
import com.google.gwt.dev.js.rhino.TokenStream;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.js.backend.ast.*;

import java.util.ArrayList;
import java.util.List;

public class JsAstMapper {

    private final ScopeContext scopeContext;

    @NotNull
    private final String fileName;

    public JsAstMapper(@NotNull JsScope scope, @NotNull String fileName) {
        scopeContext = new ScopeContext(scope);
        this.fileName = fileName;
    }

    private static JsParserException createParserException(String msg, Node offender) {
        return new JsParserException("Parser encountered internal error: " + msg, offender.getPosition());
    }

    private JsNode map(Node node) throws JsParserException {
        return withLocation(mapWithoutLocation(node), node);
    }

    private JsNode mapWithoutLocation(Node node) throws JsParserException {
        switch (node.getType()) {
            case TokenStream.SCRIPT: {
                JsBlock block = new JsBlock();
                mapStatements(block.getStatements(), node);
                return block;
            }

            case TokenStream.DEBUGGER:
                return mapDebuggerStatement(node);

            case TokenStream.VOID:
                // VOID = nothing was parsed for this node
                return null;

            case TokenStream.EXPRSTMT:
                return mapExpressionStatement(node);

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

            case TokenStream.STRING:
                return new JsStringLiteral(node.getString());

            case TokenStream.NUMBER_INT:
                return mapIntNumber(node);

            case TokenStream.NUMBER:
                return mapDoubleNumber(node);

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
                return scopeContext.globalNameFor(node.getString()).makeRef();

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
        JsArrayLiteral toLit = new JsArrayLiteral();
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
            return scopeContext.referenceFor(lit.getValue());
        }
        else {
            throw createParserException("Expecting a name reference", nameRefNode);
        }
    }

    private JsExpression mapAssignmentVariant(Node asgNode)
            throws JsParserException {
        switch (asgNode.getOperation()) {
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
                                            + asgNode.getOperation(), asgNode);
        }
    }

    private JsExpression mapBinaryOperation(JsBinaryOperator op, Node node)
            throws JsParserException {
        Node from1 = node.getFirstChild();
        Node from2 = from1.getNext();

        JsExpression to1 = mapExpression(from1);
        JsExpression to2 = mapExpression(from2);

        return new JsBinaryOperation(op, to1, to2);
    }

    private JsBlock mapBlock(Node nodeStmts) throws JsParserException {
        JsBlock block = new JsBlock();
        mapStatements(block.getStatements(), nodeStmts);
        return block;
    }

    private JsBreak mapBreak(Node breakNode) {
        return new JsBreak(getTargetLabel(breakNode));
    }

    @Nullable
    private JsNameRef getTargetLabel(@NotNull Node statementWithLabel) {
        int type = statementWithLabel.getType();
        if (type != TokenStream.BREAK && type != TokenStream.CONTINUE) {
            String tokenTypeName = TokenStream.tokenToName(statementWithLabel.getType());
            throw new AssertionError("Unexpected node type with label: " + tokenTypeName);
        }

        Node label = statementWithLabel.getFirstChild();
        if (label == null) return null;

        String identifier = label.getString();
        assert identifier != null: "If label exists identifier should not be null";

        JsName labelName = scopeContext.labelFor(identifier);
        assert labelName != null: "Unknown label name: " + identifier;

        return labelName.makeRef();
    }

    private JsInvocation mapCall(Node callNode) throws JsParserException {
        // Map the target expression.
        //
        Node from = callNode.getFirstChild();
        JsExpression qualifier = mapExpression(from);

        // Iterate over and map the arguments.
        //
        List<JsExpression> arguments = new SmartList<>();
        from = from.getNext();
        while (from != null) {
            arguments.add(mapExpression(from));
            from = from.getNext();
        }

        return new JsInvocation(qualifier, arguments);
    }

    private JsExpression mapConditional(Node condNode) throws JsParserException {
        JsConditional toCond = new JsConditional();

        Node fromTest = condNode.getFirstChild();
        toCond.setTestExpression(mapExpression(fromTest));

        Node fromThen = fromTest.getNext();
        toCond.setThenExpression(mapExpression(fromThen));

        Node fromElse = fromThen.getNext();
        toCond.setElseExpression(mapExpression(fromElse));

        return toCond;
    }

    private JsContinue mapContinue(Node contNode) {
        return new JsContinue(getTargetLabel(contNode));
    }

    private JsStatement mapDebuggerStatement(Node node) {
        // Calls an optional method to invoke the debugger.
        //
        return new JsDebugger();
    }

    private JsExpression mapDeleteProp(Node node) throws JsParserException {
        Node from = node.getFirstChild();
        JsExpression to = mapExpression(from);
        if (to instanceof JsNameRef) {
            return new JsPrefixOperation(
                    JsUnaryOperator.DELETE, to);
        }
        else if (to instanceof JsArrayAccess) {
            return new JsPrefixOperation(
                    JsUnaryOperator.DELETE, to);
        }
        else {
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
        }
        else {
            fromBody = ifNode.getFirstChild();
            fromTestExpr = ifNode.getFirstChild().getNext();
        }

        // Map the test expression.
        //
        JsExpression toTestExpr = mapExpression(fromTestExpr);

        // Map the body block.
        //
        JsStatement toBody = mapStatement(fromBody);

        // Create and attach the "while" or "do" statement we're mapping to.
        //
        if (isWhile) {
            return new JsWhile(toTestExpr, toBody);
        }
        else {
            return new JsDoWhile(toTestExpr, toBody);
        }
    }

    private JsExpression mapEqualityVariant(Node eqNode) throws JsParserException {
        switch (eqNode.getOperation()) {
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
                                            + eqNode.getOperation(), eqNode);
        }
    }

    private JsExpression mapExpression(Node exprNode) throws JsParserException {
        JsNode unknown = map(exprNode);

        if (unknown instanceof JsExpression) {
            return (JsExpression) unknown;
        }
        else {
            throw createParserException("Expecting an expression", exprNode);
        }
    }

    private JsStatement mapExpressionStatement(Node node) throws JsParserException {
        JsExpression expr = mapExpression(node.getFirstChild());
        return expr.makeStmt();
    }

    private JsStatement mapForStatement(Node forNode) throws JsParserException {
        Node fromInit = forNode.getFirstChild();
        Node fromTest = fromInit.getNext();
        Node fromIncr = fromTest.getNext();
        Node fromBody = fromIncr.getNext();

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
                JsName toName = scopeContext.localNameFor(fromName);
                toForIn = new JsForIn(toName);
                Node fromIterInit = fromIterVarName.getFirstChild();
                if (fromIterInit != null) {
                    // That has an initializer expression (useful only for side effects).
                    //
                    toForIn.setIterExpression(mapOptionalExpression(fromIterInit));
                }
            }
            else {
                // An unnamed iterator var.
                //
                toForIn = new JsForIn();
                toForIn.setIterExpression(mapExpression(fromIter));
            }
            toForIn.setObjectExpression(mapExpression(fromObjExpr));

            // The body stmt.
            //
            JsStatement bodyStmt = mapStatement(fromBody);
            if (bodyStmt != null) {
                toForIn.setBody(bodyStmt);
            }
            else {
                toForIn.setBody(JsEmpty.INSTANCE);
            }

            return toForIn;
        }
        else {
            // Regular ol' for loop.
            //
            JsFor toFor;

            // The first item is either an expression or a JsVars.
            JsNode init = map(fromInit);
            JsExpression condition = mapOptionalExpression(fromTest);
            JsExpression increment = mapOptionalExpression(fromIncr);
            assert (init != null);
            if (init instanceof JsVars) {
                toFor = new JsFor((JsVars) init, condition, increment);
            }
            else {
                assert (init instanceof JsExpression);
                toFor = new JsFor((JsExpression) init, condition, increment);
            }

            JsStatement bodyStmt = mapStatement(fromBody);
            if (bodyStmt != null) {
                toFor.setBody(bodyStmt);
            }
            else {
                toFor.setBody(JsEmpty.INSTANCE);
            }
            return toFor;
        }
    }

    public JsFunction mapFunction(Node fnNode) throws JsParserException {
        int nodeType = fnNode.getType();
        assert nodeType == TokenStream.FUNCTION: "Expected function node, got: " + TokenStream.tokenToName(nodeType);
        Node fromFnNameNode = fnNode.getFirstChild();
        Node fromParamNode = fnNode.getFirstChild().getNext().getFirstChild();
        Node fromBodyNode = fnNode.getFirstChild().getNext().getNext();
        JsFunction toFn = scopeContext.enterFunction();

        // Decide the function's name, if any.
        //
        String fnNameIdent = fromFnNameNode.getString();
        if (fnNameIdent != null && fnNameIdent.length() > 0) {
            toFn.setName(scopeContext.globalNameFor(fnNameIdent));
        }

        while (fromParamNode != null) {
            String fromParamName = fromParamNode.getString();
            JsName name = scopeContext.localNameFor(fromParamName);
            toFn.getParameters().add(new JsParameter(name));
            fromParamNode = fromParamNode.getNext();
        }

        // Map the function's body.
        //
        JsBlock toBody = mapBlock(fromBodyNode);
        toFn.setBody(toBody);

        scopeContext.exitFunction();
        return toFn;
    }

    private JsArrayAccess mapGetElem(Node getElemNode) throws JsParserException {
        Node from1 = getElemNode.getFirstChild();
        Node from2 = from1.getNext();

        JsExpression to1 = mapExpression(from1);
        JsExpression to2 = mapExpression(from2);

        return new JsArrayAccess(to1, to2);
    }

    private JsNameRef mapGetProp(Node getPropNode) throws JsParserException {
        Node from1 = getPropNode.getFirstChild();
        Node from2 = from1.getNext();

        JsExpression toQualifier = mapExpression(from1);
        JsNameRef toNameRef;
        toNameRef = mapAsPropertyNameRef(from2);

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
        JsIf toIf = new JsIf(mapExpression(fromTestExpr), mapStatement(fromThenBlock));

        // Map the "else" block.
        //
        if (fromElseBlock != null) {
            toIf.setElseStatement(mapStatement(fromElseBlock));
        }

        return toIf;
    }

    private JsExpression mapIncDecFixity(JsUnaryOperator op, Node node)
            throws JsParserException {
        switch (node.getOperation()) {
            case TokenStream.PRE:
                return mapPrefixOperation(op, node);
            case TokenStream.POST:
                return mapPostfixOperation(op, node);
            default:
                throw createParserException(
                        "Unknown prefix/postfix variant: " + node.getOperation(), node);
        }
    }

    private JsLabel mapLabel(Node labelNode) throws JsParserException {
        String fromName = labelNode.getFirstChild().getString();

        JsName toName = scopeContext.enterLabel(fromName, fromName);

        Node fromStmt = labelNode.getFirstChild().getNext();
        JsLabel toLabel = new JsLabel(toName);
        toLabel.setStatement(mapStatement(fromStmt));

        scopeContext.exitLabel();

        return toLabel;
    }

    private JsNew mapNew(Node newNode) throws JsParserException {
        // Map the constructor expression, which is often just the name of
        // some lambda.
        //
        Node fromCtorExpr = newNode.getFirstChild();
        JsNew newExpr = new JsNew(
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

    private static JsExpression mapIntNumber(Node numberNode) {
        return new JsIntLiteral((int) numberNode.getDouble());
    }

    private static JsExpression mapDoubleNumber(Node numberNode) {
        return new JsDoubleLiteral(numberNode.getDouble());
    }

    private JsExpression mapObjectLit(Node objLitNode) throws JsParserException {
        JsObjectLiteral toLit = new JsObjectLiteral();
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
                    toLabelExpr, toValueExpr);
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
            }
            else {
                throw createParserException("Expecting an expression or null", exprNode);
            }
        }
        return null;
    }

    private JsExpression mapPostfixOperation(JsUnaryOperator op, Node node)
            throws JsParserException {
        Node from = node.getFirstChild();
        JsExpression to = mapExpression(from);
        return new JsPostfixOperation(op, to);
    }

    private JsExpression mapPrefixOperation(JsUnaryOperator op, Node node)
            throws JsParserException {
        Node from = node.getFirstChild();
        JsExpression to = mapExpression(from);
        return new JsPrefixOperation(op, to);
    }

    private static JsExpression mapPrimary(Node node) throws JsParserException {
        switch (node.getOperation()) {
            case TokenStream.THIS:
                return new JsThisRef();

            case TokenStream.TRUE:
                return new JsBooleanLiteral(true);

            case TokenStream.FALSE:
                return new JsBooleanLiteral(false);

            case TokenStream.NULL:
                return new JsNullLiteral();

            case TokenStream.UNDEFINED:
                return new JsNameRef("undefined");

            default:
                throw createParserException("Unknown primary: " + node.getOperation(),
                                            node);
        }
    }

    private JsNode mapRegExp(Node regExpNode) {
        JsRegExp toRegExp = new JsRegExp();

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
        switch (relNode.getOperation()) {
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
                                            + relNode.getOperation(), relNode);
        }
    }

    private JsReturn mapReturn(Node returnNode) throws JsParserException {
        JsReturn toReturn = new JsReturn();
        Node from = returnNode.getFirstChild();
        if (from != null) {
            JsExpression to = mapExpression(from);
            toReturn.setExpression(to);
        }

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

        return new JsBinaryOperation(
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

        return new JsBinaryOperation(
                JsBinaryOperator.ASG, lhs, toRhs);
    }

    private JsExpression mapShiftVariant(Node shiftNode) throws JsParserException {
        switch (shiftNode.getOperation()) {
            case TokenStream.LSH:
                return mapBinaryOperation(JsBinaryOperator.SHL, shiftNode);

            case TokenStream.RSH:
                return mapBinaryOperation(JsBinaryOperator.SHR, shiftNode);

            case TokenStream.URSH:
                return mapBinaryOperation(JsBinaryOperator.SHRU, shiftNode);

            default:
                throw createParserException("Unknown equality operator variant: "
                                            + shiftNode.getOperation(), shiftNode);
        }
    }

    private JsStatement mapStatement(Node nodeStmt) throws JsParserException {
        JsNode unknown = map(nodeStmt);

        if (unknown != null) {
            if (unknown instanceof JsStatement) {
                return (JsStatement) unknown;
            }
            else if (unknown instanceof JsExpression) {
                return ((JsExpression) unknown).makeStmt();
            }
            else {
                throw createParserException("Expecting a statement", nodeStmt);
            }
        }
        else {
            // When map() returns null, we return an empty statement.
            //
            return JsEmpty.INSTANCE;
        }
    }

    private void mapStatements(List<JsStatement> stmts, Node nodeStmts)
            throws JsParserException {
        Node curr = nodeStmts.getFirstChild();
        while (curr != null) {
            JsStatement stmt = mapStatement(curr);
            if (stmt != null) {
                stmts.add(stmt);
            }
            else {
                // When mapStatement() returns null, we just ignore it.
                //
            }
            curr = curr.getNext();
        }
    }

    public List<JsStatement> mapStatements(Node nodeStmts)
            throws JsParserException {
        List<JsStatement> stmts = new ArrayList<>();
        mapStatements(stmts, nodeStmts);
        return stmts;
    }

    private JsSwitch mapSwitchStatement(Node switchNode) throws JsParserException {
        JsSwitch toSwitch = new JsSwitch();

        // The switch expression.
        //
        Node fromSwitchExpr = switchNode.getFirstChild();
        toSwitch.setExpression(mapExpression(fromSwitchExpr));

        // The members.
        //
        Node fromMember = fromSwitchExpr.getNext();
        while (fromMember != null) {
            if (fromMember.getType() == TokenStream.CASE) {
                JsCase toCase = new JsCase();

                // Set the case expression. In JS, this can be any expression.
                //
                Node fromCaseExpr = fromMember.getFirstChild();
                toCase.setCaseExpression(mapExpression(fromCaseExpr));

                // Set the case statements.
                //
                Node fromCaseBlock = fromCaseExpr.getNext();
                mapStatements(toCase.getStatements(), fromCaseBlock);

                // Attach the case to the switch.
                //
                toSwitch.getCases().add(toCase);
            }
            else {
                // This should be the only default statement.
                // If more than one is present, we keep the last one.
                //
                assert (fromMember.getType() == TokenStream.DEFAULT);
                JsDefault toDefault = new JsDefault();

                // Set the default statements.
                //
                Node fromDefaultBlock = fromMember.getFirstChild();
                mapStatements(toDefault.getStatements(), fromDefaultBlock);

                // Attach the default to the switch.
                //
                toSwitch.getCases().add(toDefault);
            }
            fromMember = fromMember.getNext();
        }

        return toSwitch;
    }

    private JsThrow mapThrowStatement(Node throwNode) throws JsParserException {
        // Create, map, and attach.
        //
        Node fromExpr = throwNode.getFirstChild();
        JsThrow toThrow = new JsThrow(mapExpression(fromExpr));

        return toThrow;
    }

    private JsTry mapTryStatement(Node tryNode) throws JsParserException {
        JsTry toTry = new JsTry();

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
            JsCatch catchBlock = scopeContext.enterCatch(fromCatchVarName.getString());

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
            catchBlock.setBody(mapBlock(fromCatchBody));

            // Attach it.
            //
            toTry.getCatches().add(catchBlock);
            scopeContext.exitCatch();
        }

        Node fromFinallyNode = fromCatchNodes.getNext();
        if (fromFinallyNode != null) {
            toTry.setFinallyBlock(mapBlock(fromFinallyNode));
        }

        return toTry;
    }

    private JsExpression mapUnaryVariant(Node unOp) throws JsParserException {
        switch (unOp.getOperation()) {
            case TokenStream.SUB:
                return mapPrefixOperation(JsUnaryOperator.NEG, unOp);

            case TokenStream.NOT:
                return mapPrefixOperation(JsUnaryOperator.NOT, unOp);

            case TokenStream.BITNOT:
                return mapPrefixOperation(JsUnaryOperator.BIT_NOT, unOp);

            case TokenStream.TYPEOF:
                return mapPrefixOperation(JsUnaryOperator.TYPEOF, unOp);

            case TokenStream.ADD:
                if (!isJsNumber(unOp.getFirstChild())) {
                    return mapPrefixOperation(JsUnaryOperator.POS, unOp);
                }
                else {
                    // Pretend we didn't see it.
                    return mapExpression(unOp.getFirstChild());
                }

            case TokenStream.VOID:
                return mapPrefixOperation(JsUnaryOperator.VOID, unOp);

            default:
                throw createParserException(
                        "Unknown unary operator variant: " + unOp.getOperation(), unOp);
        }
    }

    private JsVars mapVar(Node varNode) throws JsParserException {
        JsVars toVars = new JsVars();
        Node fromVar = varNode.getFirstChild();
        while (fromVar != null) {
            // Use a conservative name allocation strategy that allocates all names
            // from the function's scope, even the names of properties in field
            // literals.
            //
            String fromName = fromVar.getString();
            JsName toName = scopeContext.localNameFor(fromName);
            JsVars.JsVar toVar = withLocation(new JsVars.JsVar(toName), fromVar);

            Node fromInit = fromVar.getFirstChild();
            if (fromInit != null) {
                JsExpression toInit = mapExpression(fromInit);
                toVar.setInitExpression(toInit);
            }
            toVars.add(toVar);

            fromVar = fromVar.getNext();
        }

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

    private boolean isJsNumber(Node jsNode) {
        int type = jsNode.getType();
        return type == TokenStream.NUMBER || type == TokenStream.NUMBER;
    }

    private <T extends JsNode> T withLocation(T astNode, Node node) {
        CodePosition location = node.getPosition();
        if (location != null) {
            JsLocation jsLocation = new JsLocation(fileName, location.getLine(), location.getOffset());
            if (astNode instanceof SourceInfoAwareJsNode) {
                astNode.setSource(jsLocation);
            }
            else if (astNode instanceof JsExpressionStatement) {
                ((JsExpressionStatement) astNode).getExpression().setSource(jsLocation);
            }
        }
        return astNode;
    }
}
