package org.jetbrains.k2js.translate.operation;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.k2js.translate.utils.Namer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class OperatorTable {

    private static Map<JetToken, JsBinaryOperator> binaryOperatorsMap = new HashMap<JetToken, JsBinaryOperator>();
    private static Map<JetToken, JsUnaryOperator> unaryOperatorsMap = new HashMap<JetToken, JsUnaryOperator>();
    private static Map<JetToken, JsNameRef> operatorToFunctionNameReference = new HashMap<JetToken, JsNameRef>();

    static {
        unaryOperatorsMap.put(JetTokens.PLUSPLUS, JsUnaryOperator.INC);     //++
        unaryOperatorsMap.put(JetTokens.MINUSMINUS, JsUnaryOperator.DEC);   //--
        unaryOperatorsMap.put(JetTokens.EXCL, JsUnaryOperator.NOT);         //!
        unaryOperatorsMap.put(JetTokens.MINUS, JsUnaryOperator.NEG);        //-
        unaryOperatorsMap.put(JetTokens.PLUS, JsUnaryOperator.POS);         //+
    }

    //TODO : not all operators
    static {
        binaryOperatorsMap.put(JetTokens.PLUS, JsBinaryOperator.ADD);
        binaryOperatorsMap.put(JetTokens.MINUS, JsBinaryOperator.SUB);
        binaryOperatorsMap.put(JetTokens.MUL, JsBinaryOperator.MUL);
        binaryOperatorsMap.put(JetTokens.DIV, JsBinaryOperator.DIV);
        binaryOperatorsMap.put(JetTokens.EQ, JsBinaryOperator.ASG);
        binaryOperatorsMap.put(JetTokens.GT, JsBinaryOperator.GT);
        binaryOperatorsMap.put(JetTokens.GTEQ, JsBinaryOperator.GTE);
        binaryOperatorsMap.put(JetTokens.LT, JsBinaryOperator.LT);
        binaryOperatorsMap.put(JetTokens.LTEQ, JsBinaryOperator.LTE);
        binaryOperatorsMap.put(JetTokens.EQEQ, JsBinaryOperator.REF_EQ);
        binaryOperatorsMap.put(JetTokens.ANDAND, JsBinaryOperator.AND);
        binaryOperatorsMap.put(JetTokens.EXCLEQ, JsBinaryOperator.NEQ);
        binaryOperatorsMap.put(JetTokens.PERC, JsBinaryOperator.MOD);
        binaryOperatorsMap.put(JetTokens.PLUSEQ, JsBinaryOperator.ASG_ADD);
        binaryOperatorsMap.put(JetTokens.MINUSEQ, JsBinaryOperator.ASG_SUB);
        binaryOperatorsMap.put(JetTokens.DIVEQ, JsBinaryOperator.ASG_DIV);
        binaryOperatorsMap.put(JetTokens.MULTEQ, JsBinaryOperator.ASG_MUL);
        binaryOperatorsMap.put(JetTokens.PERCEQ, JsBinaryOperator.ASG_MOD);
    }

    static {
        operatorToFunctionNameReference.put(JetTokens.IS_KEYWORD, Namer.isOperationReference());
    }

    public static boolean hasCorrespondingBinaryOperator(@NotNull JetToken token) {
        return binaryOperatorsMap.containsKey(token);
    }

    public static boolean hasCorrespondingFunctionInvocation(@NotNull JetToken token) {
        return operatorToFunctionNameReference.containsKey(token);
    }

    @NotNull
    static public JsInvocation getCorrespondingFunctionInvocation(@NotNull JetToken token) {
        JsNameRef functionReference = operatorToFunctionNameReference.get(token);
        assert functionReference != null : "Token should represent a function call!";
        return AstUtil.newInvocation(functionReference);
    }

    @NotNull
    static public JsBinaryOperator getBinaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return binaryOperatorsMap.get(token);
    }

    @NotNull
    static public JsUnaryOperator getUnaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return unaryOperatorsMap.get(token);
    }

    static public boolean isAssignment(JetToken token) {
        return (token == JetTokens.EQ);
    }
}
