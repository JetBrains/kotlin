package org.jetbrains.k2js;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.JetTestUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Talanov Pavel
 */
public final class K2JSTranslator {

    private final JetCoreEnvironment myEnvironment = new JetCoreEnvironment(new Disposable() {

        @Override
        public void dispose() {
        }
    });

    public K2JSTranslator() {
    }

    public void translateFile(@NotNull String inputFile, @NotNull String outputFile) throws Exception {

        JetFile PsiFile = loadPsiFile(inputFile);

        JsProgram program = generateProgram(PsiFile);

        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    @NotNull
    public String translateString(@NotNull String programText) {
        JetFile PsiFile = createPsiFile("test", programText);
        JsProgram program = generateProgram(PsiFile);

        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    private JsProgram generateProgram(@NotNull JetFile psiFile) {
        final File rtJar = CompileEnvironment.findRtJar(true);
        myEnvironment.addToClasspath(rtJar);

        JetNamespace namespace = psiFile.getRootNamespace();

        BindingContext bindingContext = JetTestUtils.analyzeNamespace(namespace,
                JetControlFlowDataTraceFactory.EMPTY);

        return Translation.generateAst(bindingContext, namespace, myEnvironment.getProject());
    }

    @NotNull
    protected String loadFile(@NotNull String path) throws IOException {
        return doLoadFile(path);
    }

    @NotNull
    protected String doLoadFile(@NotNull String path) throws IOException {
        String text = FileUtil.loadFile(new File(path), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    @NotNull
    protected JetFile createPsiFile(@NotNull String name, @NotNull String text) {
        return (JetFile) createFile(name + ".jet", text);
    }

    @NotNull
    protected JetFile loadPsiFile(@NotNull String name) {
        try {
            return createPsiFile(name, loadFile(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return ((PsiFileFactoryImpl) PsiFileFactory.getInstance(myEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }

}
