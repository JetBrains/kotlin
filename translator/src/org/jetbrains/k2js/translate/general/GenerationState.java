/*
 * @author Talanov Pavel
 */
package org.jetbrains.k2js.translate.general;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;

//TODO: rework this class and the outer API
public final class GenerationState {

    @NotNull
    private final Project project;

    public GenerationState(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public JsProgram compileCorrectNamespaces(@NotNull BindingContext bindingContext,
                                              @NotNull List<JetNamespace> namespaces) {
        //TODO hardcoded
        JetNamespace namespace = namespaces.get(0);
        NamespaceDescriptor descriptor = BindingUtils.getNamespaceDescriptor(bindingContext, namespace);
        return Translation.generateAst(bindingContext, namespace, project);
    }
}
