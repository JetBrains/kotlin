/*
 * @author Talanov Pavel
 */
package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.k2js.declarations.DeclarationExtractor;

import javax.print.attribute.standard.MediaSize;
import javax.xml.ws.Binding;
import java.beans.beancontext.BeanContext;
import java.util.Collections;
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
