package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsUnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class OperatorTable {


    private static Map<JetToken, JsBinaryOperator> binaryOperatorsMap = new HashMap<JetToken, JsBinaryOperator>();
    private static Map<JetToken, JsUnaryOperator> unaryOperatorsMap = new HashMap<JetToken, JsUnaryOperator>();

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
        binaryOperatorsMap.put(JetTokens.EQEQ, JsBinaryOperator.EQ);
        binaryOperatorsMap.put(JetTokens.ANDAND, JsBinaryOperator.AND);
        binaryOperatorsMap.put(JetTokens.EXCLEQ, JsBinaryOperator.NEQ);
    }

    @NotNull
    public static JsBinaryOperator getBinaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return binaryOperatorsMap.get(token);
    }

    @NotNull
    public static JsUnaryOperator getUnaryOperator(@NotNull JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent an operation!";
        return unaryOperatorsMap.get(token);
    }

    public static boolean isAssignment(JetToken token) {
        return (token == JetTokens.EQ);
    }

}
