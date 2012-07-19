package org.jetbrains.k2js.translate.expression;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.k2js.translate.context.TraceableThisAliasProvider;
import org.jetbrains.k2js.translate.context.TranslationContext;

class InnerObjectTranslator extends InnerDeclarationTranslator {
    public InnerObjectTranslator(@NotNull JetElement declaration, @NotNull TranslationContext context, @NotNull JsFunction fun) {
        super(declaration, context, fun);
    }

    @Override
    protected JsExpression createExpression(JsNameRef nameRef, JsExpression self) {
        return createInvocation(nameRef, self);
    }

    @Override
    @NotNull
    public JsExpression translate(@NotNull JsNameRef nameRef) {
        return super.translate(nameRef, thisAliasProvider().getRefIfWasCaptured());
    }

    private TraceableThisAliasProvider thisAliasProvider() {
        return ((TraceableThisAliasProvider) context.thisAliasProvider());
    }

    @Override
    protected JsInvocation createInvocation(JsNameRef nameRef, JsExpression self) {
        JsInvocation invocation = new JsInvocation(nameRef);
        if (thisAliasProvider().wasThisCaptured()) {
            fun.getParameters().add(new JsParameter(((JsNameRef) self).getName()));
            invocation.getArguments().add(JsLiteral.THIS);
        }
        return invocation;
    }
}
