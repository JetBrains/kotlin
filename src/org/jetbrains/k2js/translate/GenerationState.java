/*
 * @author max
 */
package org.jetbrains.k2js.translate;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

import java.util.Collections;
import java.util.List;

public class GenerationState {
    private final Project project;


    private BindingContext currentContext;
    private final JetStandardLibrary standardLibrary;

    public GenerationState(Project project, boolean text) {
        this.project = project;
        this.standardLibrary = JetStandardLibrary.getJetStandardLibrary(project);
    }

    public Project getProject() {
        return project;
    }


    public BindingContext getContext() {
        return currentContext;
    }

    private void setContext(BindingContext context) {
        this.currentContext = context;
    }

    public JetStandardLibrary getStandardLibrary() {
        return standardLibrary;
    }

    public void compile(JetFile psiFile) {
        final JetNamespace namespace = psiFile.getRootNamespace();
        final BindingContext bindingContext = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS).analyzeNamespace(namespace, JetControlFlowDataTraceFactory.EMPTY);
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        compileCorrectNamespaces(bindingContext, Collections.singletonList(namespace));
    }

    public JsProgram compileCorrectNamespaces(BindingContext bindingContext, List<JetNamespace> namespaces) {
        this.currentContext = bindingContext;
        //TODO hardcoded
        JsProgram result = new JsProgram("main");
        (new NamespaceTranslator(TranslationContext.rootContext(result, bindingContext), namespaces.get(0))).generateAst();
        return result;
    }

}
