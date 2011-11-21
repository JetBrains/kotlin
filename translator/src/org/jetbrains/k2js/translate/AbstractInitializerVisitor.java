package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsScope;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassInitializer;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Talanov Pavel
 */
public abstract class AbstractInitializerVisitor extends TranslatorVisitor<List<JsStatement>> {

    @NotNull
    protected final JsScope initializerMethodScope;

    @NotNull
    protected final TranslationContext initializerMethodContext;

    protected AbstractInitializerVisitor(@NotNull TranslationContext enclosingContext, @NotNull JsScope newScope) {
        this.initializerMethodContext = enclosingContext.newEnclosingScope(newScope);
        this.initializerMethodScope = newScope;
    }

    @NotNull
    abstract protected JsFunction generate();

    @Override
    @NotNull
    public List<JsStatement> visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JetExpression initializer = expression.getInitializer();
        if (initializer == null) {
            return new ArrayList<JsStatement>();
        }
        return Arrays.asList(translateInitializer(expression, context, initializer));
    }

    @NotNull
    private JsStatement translateInitializer(@NotNull JetProperty property, @NotNull TranslationContext context,
                                             @NotNull JetExpression initializer) {
        JsExpression initExpression = Translation.translateAsExpression(initializer, context);
        return assignmentToBackingField(property, initExpression, context);
    }

    @NotNull
    JsStatement assignmentToBackingField(@NotNull JetProperty property, @NotNull JsExpression initExpression,
                                         @NotNull TranslationContext context) {

        return AstUtil.newAssignmentStatement(backingFieldReference(property, context), initExpression);
    }

    @Override
    @NotNull
    public List<JsStatement> visitAnonymousInitializer(@NotNull JetClassInitializer initializer,
                                                       @NotNull TranslationContext context) {
        return Arrays.asList(Translation.translateAsStatement(initializer.getBody(), context));
    }

    @Override
    @NotNull
    // Not interested in other types of declarations, they do not contain initializers.
    public List<JsStatement> visitDeclaration(@NotNull JetDeclaration expression, @NotNull TranslationContext context) {
        return new ArrayList<JsStatement>();
    }
}
