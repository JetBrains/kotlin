/*
 * @author Talanov Pavel
 */
package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.declarations.Declarations;

import java.util.List;

public final class GenerationState {


    public GenerationState() {
    }

    //TODO method too long
    @NotNull
    public JsProgram compileCorrectNamespaces(@NotNull BindingContext bindingContext, @NotNull List<JetNamespace> namespaces) {
        //TODO hardcoded
        JsProgram result = new JsProgram("main");
        JetNamespace namespace = namespaces.get(0);
        NamespaceDescriptor descriptor = BindingUtils.getNamespaceDescriptor(bindingContext, namespace);
        Declarations declarations = Declarations.extractDeclarations(descriptor, result.getRootScope());
        Translation.generateAst(result, bindingContext, declarations, namespace);
        return result;
    }


}
