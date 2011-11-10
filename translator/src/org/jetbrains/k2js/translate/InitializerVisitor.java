package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptorVisitor;
import org.jetbrains.jet.lang.psi.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 *
 * This visitor collects all initializers from a given class in a list of statements.
 * Note: we do use this to preserve the order in which initializers are executed.
 */
public class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {

    @Override
    @NotNull
    public List<JsStatement> visitClass(@NotNull JetClass expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @Override
    @NotNull
    public List<JsStatement> visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JetExpression initializer = expression.getInitializer();
        if (initializer == null) {
            return new ArrayList<JsStatement>();
        }
        ExpressionTranslator translator = new ExpressionTranslator(context);
        JsExpression initExpression = AstUtil.convertToExpression(translator.translate(initializer));
        return Arrays.asList(assignmentToBackingField(expression, initExpression, context));
    }

    @NotNull
    JsStatement assignmentToBackingField(@NotNull JetProperty property, @NotNull JsExpression initExpression,
                                                  @NotNull TranslationContext context) {
        JsName backingFieldName = context.classScope()
                .findExistingName(Namer.getBackingFieldNameForProperty(property.getName()));
        assert backingFieldName != null : "Class scope should contain backing field names";
        JsNameRef backingFieldRef = backingFieldName.makeRef();
        backingFieldRef.setQualifier(new JsThisRef());
        return AstUtil.convertToStatement(AstUtil.newAssignment(backingFieldRef, initExpression));
    }

    //TODO : implement
    @Override
    @NotNull
    public List<JsStatement> visitAnonymousInitializer(@NotNull JetClassInitializer initializer,
                                                       @NotNull TranslationContext context) {
        return new ArrayList<JsStatement>();
    }

    @Override
    @NotNull
    // Not interested in other types of declarations, they do not contain initializers.
    public List<JsStatement> visitDeclaration(@NotNull JetDeclaration expression, @NotNull TranslationContext context) {
        return new ArrayList<JsStatement>();
    }

}
