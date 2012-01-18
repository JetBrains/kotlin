package org.jetbrains.k2js;

import com.google.common.base.Predicate;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.GenerationUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getNamespaceDescriptor;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getNameForNamespace;

/**
 * @author Pavel Talanov
 */
public final class K2JSTranslator {

    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "C:\\Dev\\Projects\\Kotlin\\jet\\stdlib\\ktSrc\\jssupport\\JsCollectionSupport.jet",
            "C:\\Dev\\Projects\\Kotlin\\jet\\stdlib\\ktSrc\\jssupport\\JsSupport.jet"
    );

    @NotNull
    private JetCoreEnvironment environment = new JetCoreEnvironment(new Disposable() {

        @Override
        public void dispose() {
        }
    });

    public void setEnvironment(JetCoreEnvironment env) {
        environment = env;
    }

    @Nullable
    private BindingContext bindingContext = null;

    public K2JSTranslator() {
    }

    @NotNull
    public List<JetFile> getJsSupportStdLib() {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            libFiles.add(loadPsiFile(libFileName));
        }
        return libFiles;
    }

    public void translateFile(@NotNull String inputFile, @NotNull String outputFile) throws Exception {
        JetFile PsiFile = loadPsiFile(inputFile);
        // includeRtJar();
        JsProgram program = generateProgram(PsiFile);
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) {
        JetFile file = createPsiFile("test", programText);
        String programCode = generateProgramCode(file) + "\n";
        String flushOutput = "Kotlin.System.flush();\n";
        String callToMain = generateCallToMain(file, argumentsString);
        String programOutput = "Kotlin.System.output();\n";
        return programCode + flushOutput + callToMain + programOutput;
    }

    @NotNull
    private String generateProgramCode(@NotNull JetFile psiFile) {
        JsProgram program = generateProgram(psiFile);
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    private JsProgram generateProgram(@NotNull JetFile psiFile) {

//        bindingContext = analyzeNamespace(psiFile,
//                JetControlFlowDataTraceFactory.EMPTY);
        List<JetFile> files = getJsSupportStdLib();
        files.add(psiFile);
        bindingContext = AnalyzerFacade.analyzeFilesWithJavaIntegration(psiFile.getProject(), files, new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                for (String libFileName : LIB_FILE_NAMES) {
                    if (libFileName.contains(file.getName().substring(0, file.getName().lastIndexOf('.')))) {
                        return false;
                    }
                }
                return true;
            }
        }, JetControlFlowDataTraceFactory.EMPTY);
        assert bindingContext != null;
        AnalyzingUtils.checkForSyntacticErrors(psiFile);
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);

        return Translation.generateAst(bindingContext, psiFile, environment.getProject());
    }

    private void includeRtJar() {
        final File rtJar = CompileEnvironment.findRtJar(true);
        environment.addToClasspath(rtJar);
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
    protected PsiFile createFile(@NotNull String name, @NotNull String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return ((PsiFileFactoryImpl) PsiFileFactory.getInstance(environment.getProject()))
                .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }


    @NotNull
    public String generateCallToMain(@NotNull JetFile file, @NotNull String argumentString) {
        String namespaceName = getRootNamespaceName(file);

        List<String> arguments = parseString(argumentString);
        return GenerationUtils.generateCallToMain(namespaceName, arguments);
    }

    @NotNull
    private List<String> parseString(@NotNull String argumentString) {
        List<String> result = new ArrayList<String>();
        StringTokenizer stringTokenizer = new StringTokenizer(argumentString);
        while (stringTokenizer.hasMoreTokens()) {
            result.add(stringTokenizer.nextToken());
        }
        return result;
    }

    @NotNull
    private String getRootNamespaceName(@NotNull JetFile psiFile) {
        assert bindingContext != null;
        return getNameForNamespace(getNamespaceDescriptor(bindingContext, psiFile));
    }

}
