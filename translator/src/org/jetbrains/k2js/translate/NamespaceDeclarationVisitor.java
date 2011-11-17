package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;

/**
 * @author Talanov Pavel
 */
//TODO: rework the class
public class NamespaceDeclarationVisitor extends TranslatorVisitor<JsStatement> {

    @NotNull
    @Override
    //TODO method too long
    public JsStatement visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JsName propertyName = context.declareLocalName(getPropertyName(expression));
        JsNameRef jsPropertyNameReference = context.getNamespaceQualifiedReference(propertyName);
        JsExpression jsInitExpression = translateInitializerForProperty(expression, context);
        JsExpression result;
        if (jsInitExpression != null) {
            result = AstUtil.newAssignment(jsPropertyNameReference, jsInitExpression);
        } else {
            result = jsPropertyNameReference;
        }
        return AstUtil.convertToStatement(result);
    }

    @NotNull
    @Override
    public JsStatement visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        return Translation.classTranslator(context).translateClass(expression);
    }

    @NotNull
    @Override
    public JsStatement visitNamedFunction(@NotNull JetNamedFunction expression, @NotNull TranslationContext context) {
        return AstUtil.convertToStatement(Translation.functionTranslator(context).translateAsFunction(expression));
    }

}
