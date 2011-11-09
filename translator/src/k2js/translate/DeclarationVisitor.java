package k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * @author Talanov Pavel
 */
public class DeclarationVisitor extends TranslatorVisitor<JsStatement> {

    @NotNull
    @Override
    //TODO method too long
    public JsStatement visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JsName propertyName = context.declareLocalName(getPropertyName(expression));
        JsNameRef jsPropertyNameReference = context.getNamespaceQualifiedReference(propertyName);
        JsExpression jsInitExpression = translateInitializer(expression, context);
        JsExpression result;
        if (jsInitExpression != null) {
            result = AstUtil.newAssignment(jsPropertyNameReference, jsInitExpression);
        }
        else {
            result = jsPropertyNameReference;
        }
        return AstUtil.convertToStatement(result);
    }

    //TODO duplicate code translateInitializer 1
    @Nullable
    private JsExpression translateInitializer(@NotNull JetProperty declaration, @NotNull TranslationContext context) {
        JsExpression jsInitExpression = null;
        JetExpression initializer = declaration.getInitializer();
        if (initializer != null) {
            jsInitExpression = AstUtil.convertToExpression(
                (new ExpressionTranslator(context)).translate(initializer));
        }
        return jsInitExpression;
    }

    @NotNull
    @Override
    public JsStatement visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        ClassTranslator translator = new ClassTranslator(context);
        return translator.translateClass(expression);
    }

    @NotNull
    @Override
    public JsStatement visitNamedFunction(@NotNull JetNamedFunction expression, @NotNull TranslationContext context) {
        FunctionTranslator functionTranslator = new FunctionTranslator(context);
        return AstUtil.convertToStatement(functionTranslator.translateFunction(expression));
    }

}
