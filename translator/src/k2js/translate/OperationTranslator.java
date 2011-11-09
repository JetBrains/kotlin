package k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsBinaryOperator;
import com.google.dart.compiler.backend.js.ast.JsOperator;
import org.jetbrains.jet.lexer.JetToken;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Talanov Pavel
 */
public final class OperationTranslator {


    private static Map<JetToken, JsOperator> map = new HashMap<JetToken, JsOperator>();

    static {
        map.put(JetTokens.PLUS, JsBinaryOperator.ADD);
        map.put(JetTokens.MINUS, JsBinaryOperator.SUB);
        map.put(JetTokens.MUL, JsBinaryOperator.MUL);
        map.put(JetTokens.DIV, JsBinaryOperator.DIV);
        map.put(JetTokens.EQ, JsBinaryOperator.ASG);
        map.put(JetTokens.GT, JsBinaryOperator.GT);
        map.put(JetTokens.GTEQ, JsBinaryOperator.GTE);
        map.put(JetTokens.LT, JsBinaryOperator.LT);
        map.put(JetTokens.LTEQ, JsBinaryOperator.LTE);
        map.put(JetTokens.EQEQ, JsBinaryOperator.EQ);
    }

    public static JsBinaryOperator getBinaryOperator(JetToken token) {
        assert JetTokens.OPERATIONS.contains(token) : "Token should represent a binary operation!";
        JsOperator result = map.get(token);
        if (result instanceof JsBinaryOperator) {
            return (JsBinaryOperator)result;
        }
        throw new AssertionError("Invalid map: should contain token: " + token.toString());
    }


}
