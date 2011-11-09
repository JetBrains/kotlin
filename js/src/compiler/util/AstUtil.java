// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package compiler.util;

import compiler.InternalCompilerException;
import compiler.common.SourceInfo;
import compiler.backend.js.ast.*;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author johnlenz@google.com (John Lenz)
 */
public class AstUtil {

    public static JsInvocation newInvocation(
            JsExpression target, JsExpression... params) {
        JsInvocation invoke = new JsInvocation();
        invoke.setQualifier(target);
        for (JsExpression expr : params) {
            invoke.getArguments().add(expr);
        }
        return invoke;
    }

    public static JsInvocation newInvocation(
            JsExpression target, List<JsExpression> params) {
        JsInvocation invoke = new JsInvocation();
        invoke.setQualifier(target);
        for (JsExpression expr : params) {
            invoke.getArguments().add(expr);
        }
        return invoke;
    }

    public static JsNameRef newQualifiedNameRef(String name) {
        JsNameRef node = null;
        int endPos = -1;
        int startPos = 0;
        do {
            endPos = name.indexOf('.', startPos);
            String part = (endPos == -1
                    ? name.substring(startPos)
                    : name.substring(startPos, endPos));
            node = newNameRef(node, part);
            startPos = endPos + 1;
        } while (endPos != -1);

        return node;
    }

    public static JsNameRef newNameRef(JsExpression qualifier, String prop) {
        JsNameRef nameRef = new JsNameRef(prop);
        if (qualifier != null) {
            nameRef.setQualifier(qualifier);
        }
        return nameRef;
    }

    public static JsNameRef nameref(SourceInfo info, JsName qualifier, JsName prop) {
        JsNameRef nameRef = new JsNameRef(prop);
        if (qualifier != null) {
            nameRef.setQualifier(qualifier.makeRef());
        }
        nameRef.setSourceRef(info);
        return nameRef;
    }

    public static JsNameRef newNameRef(JsExpression qualifier, JsName prop) {
        JsNameRef nameRef = new JsNameRef(prop);
        if (qualifier != null) {
            nameRef.setQualifier(qualifier);
        }
        return nameRef;
    }

    public static JsNameRef newPrototypeNameRef(JsExpression qualifier) {
        return newNameRef(qualifier, "prototype");
    }

    public static JsArrayAccess newArrayAccess(JsExpression target, JsExpression key) {
        JsArrayAccess arr = new JsArrayAccess();
        arr.setArrayExpr(target);
        arr.setIndexExpr(key);
        return arr;
    }

    public static JsBlock newBlock(JsStatement... stmts) {
        JsBlock jsBlock = new JsBlock();
        for (JsStatement stmt : stmts) {
            jsBlock.getStatements().add(stmt);
        }
        return jsBlock;
    }

    /**
     * Returns a sequence of expressions (using the binary sequence operator).
     *
     * @param exprs - expressions to add to sequence
     * @return a sequence of expressions.
     */
    public static JsBinaryOperation newSequence(JsExpression... exprs) {
        if (exprs.length < 2) {
            throw new InternalCompilerException("newSequence expects at least two arguments");
        }
        JsExpression result = exprs[exprs.length - 1];
        for (int i = exprs.length - 2; i >= 0; i--) {
            result = new JsBinaryOperation(JsBinaryOperator.COMMA, exprs[i], result);
        }
        return (JsBinaryOperation) result;
    }

    // Ensure a valid LHS
    public static JsBinaryOperation newAssignment(
            JsNameRef nameRef, JsExpression expr) {
        return new JsBinaryOperation(JsBinaryOperator.ASG, nameRef, expr);
    }

    public static JsBinaryOperation newAssignment(
            JsArrayAccess target, JsExpression expr) {
        return new JsBinaryOperation(JsBinaryOperator.ASG, target, expr);
    }

    public static JsVars newVar(SourceInfo info, JsName name, JsExpression expr) {
        JsVars.JsVar var = new JsVars.JsVar(name).setSourceRef(info);
        var.setInitExpr(expr);
        JsVars vars = new JsVars();
        vars.add(var);
        return vars;
    }

    public static JsVars newVar(JsName name, JsExpression expr) {
        JsVars.JsVar var = new JsVars.JsVar(name);
        if (expr != null) {
            var.setInitExpr(expr);
        }
        JsVars vars = new JsVars();
        vars.add(var);
        return vars;
    }

    public static JsSwitch newSwitch(
            JsExpression expr, JsSwitchMember... cases) {
        JsSwitch jsSwitch = new JsSwitch();
        jsSwitch.setExpr(expr);
        for (JsSwitchMember jsCase : cases) {
            jsSwitch.getCases().add(jsCase);
        }
        return jsSwitch;
    }

    public static JsCase newCase(JsExpression expr, JsStatement... stmts) {
        JsCase jsCase = new JsCase();
        jsCase.setCaseExpr(expr);
        for (JsStatement stmt : stmts) {
            jsCase.getStmts().add(stmt);
        }
        return jsCase;
    }

    public static JsDefault newDefaultCase(JsStatement... stmts) {
        JsDefault jsCase = new JsDefault();
        for (JsStatement stmt : stmts) {
            jsCase.getStmts().add(stmt);
        }
        return jsCase;
    }

    public static JsFunction newFunction(
            JsScope scope, @Nullable JsName name, List<JsParameter> params, JsBlock body) {
        JsFunction fn = new JsFunction(scope);
        if (name != null) {
            fn.setName(name);
        }
        if (params != null) {
            for (JsParameter param : params) {
                fn.getParameters().add(param);
            }
        }
        fn.setBody(body);
        return fn;
    }

    public static JsStatement convertToStatement(JsNode jsNode) {
        if (jsNode instanceof JsExpression) {
            return new JsExprStmt((JsExpression) jsNode);
        }
        if (jsNode instanceof JsStatement) {
            return (JsStatement) jsNode;
        }
        throw new RuntimeException("Cannot convert JsNode to JsStatement: Unexpected node");
    }

    public static JsBlock convertToBlock(JsNode jsNode) {
        if (jsNode instanceof JsBlock) {
            return (JsBlock)jsNode;
        }
        JsStatement jsStatement = convertToStatement(jsNode);
        return new JsBlock(jsStatement);
    }

    public static JsExpression convertToExpression(JsNode jsNode) {
        if (jsNode instanceof JsExpression) {
            return (JsExpression)jsNode;
        }
        throw new RuntimeException("Cannot convert JsNode to JsExpression: Unexpected node");
    }

    public static JsInvocation call(SourceInfo src, JsExpression target, JsExpression... params) {
        return (JsInvocation) newInvocation(target, params).setSourceRef(src);
    }

    public static JsBinaryOperation comma(SourceInfo src, JsExpression op1, JsExpression op2) {
        return (JsBinaryOperation) new JsBinaryOperation(JsBinaryOperator.COMMA, op1, op2)
                .setSourceRef(src);
    }

    public static JsNameRef nameref(SourceInfo src, String name) {
        return (JsNameRef) new JsNameRef(name).setSourceRef(src);
    }

    public static JsNameRef nameref(SourceInfo src, JsName qualifier, String prop) {
        return nameref(src, qualifier.makeRef().setSourceRef(src), prop);
    }

    public static JsNameRef nameref(SourceInfo src, JsExpression qualifier, String prop) {
        return (JsNameRef) newNameRef(qualifier, prop).setSourceRef(src);
    }

    public static JsExpression assign(SourceInfo src, JsNameRef op1, JsExpression op2) {
        return newAssignment(op1, op2).setSourceRef(src);
    }

    public static JsExpression neq(SourceInfo src, JsExpression op1, JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.NEQ, op1, op2).setSourceRef(src);
    }

    public static JsExpression not(SourceInfo src, JsExpression op1) {
        return new JsPrefixOperation(JsUnaryOperator.NOT, op1).setSourceRef(src);
    }

    public static JsExpression preinc(SourceInfo src, JsExpression op1) {
        return new JsPrefixOperation(JsUnaryOperator.INC, op1).setSourceRef(src);
    }

    public static JsExpression and(SourceInfo src, JsExpression op1, JsExpression op2) {
        return new JsBinaryOperation(JsBinaryOperator.AND, op1, op2).setSourceRef(src);
    }

    public static JsExpression in(SourceInfo src, JsExpression propName, JsExpression obj) {
        return new JsBinaryOperation(JsBinaryOperator.INOP, propName, obj).setSourceRef(src);
    }

    public static JsPropertyInitializer newNamedMethod(JsName name, JsFunction function) {
        JsNameRef methodName = name.makeRef();
        return new JsPropertyInitializer(methodName, function);
    }
}
