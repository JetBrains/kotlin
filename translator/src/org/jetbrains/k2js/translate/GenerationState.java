/*
 * @author Talanov Pavel
 */
package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.DeclarationExtractor;

import java.util.List;

public final class GenerationState {


    public GenerationState() {
    }

    //TODO method too long
    public JsProgram compileCorrectNamespaces(BindingContext bindingContext, List<JetNamespace> namespaces) {
        //TODO hardcoded
        JsProgram result = new JsProgram("main");
        JetNamespace namespace = namespaces.get(0);
        NamespaceDescriptor descriptor = BindingUtils.getNamespaceDescriptor(bindingContext, namespace);
        DeclarationExtractor extractor = new DeclarationExtractor();
        extractor.extractDeclarations(descriptor, result.getRootScope());
        (new NamespaceTranslator(TranslationContext.rootContext(result, bindingContext, extractor)))
                .generateAst(namespace);
        return result;
    }


}
