package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jaxen.expr.Expr;
import org.jetbrains.jet.lang.psi.*;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel.Talanov
 */
public class NamespaceTranslator extends AbstractTranslator {

    private final JetNamespace namespace;

    NamespaceTranslator(TranslationContext context, JetNamespace namespace) {
        super(context);
        this.namespace = namespace;
    }

    JsProgram generateAst() {
        translate(namespace);
        return program();
    }

    public JsBlock translate(JetNamespace namespace) {
        // TODO write namespace change logic
        JsBlock block = program().getFragmentBlock(0);
        JsFunction dummyFunc = newNamespace(namespace.getName());
        JsBlock namespaceBlock = new JsBlock();
        dummyFunc.setBody(namespaceBlock);
        TranslationContext newContext = translationContext()
                .newBlock(namespaceBlock).newScope(dummyFunc.getScope()).newNamespace(new JsNameRef(getJSName(namespace.getName())));
        DeclarationTranslator declarationTranslator = new DeclarationTranslator(newContext);
        for (JetDeclaration declaration : namespace.getDeclarations()) {
            namespaceBlock.addStatement(declarationTranslator.translateDeclaration(declaration));
        }
        return block;
    }

    public JsFunction newNamespace(String namespaceId) {
        JsFunction result = new JsFunction(scope());
        List<JsParameter> params = new ArrayList<JsParameter>();
        params.add(new JsParameter(getJSName(namespaceId)));
        result.setParameters(params);
        JsExpression invocationParam = new JsNameRef(namespaceId);
        block().addStatement(AstUtil.convertToStatement(AstUtil.newInvocation((result), invocationParam)));
        return result;
    }
}
