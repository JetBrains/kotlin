package org.jetbrains.k2js;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.utils.GenerationUtils;
import org.jetbrains.k2js.utils.JetFileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import static org.jetbrains.k2js.utils.JetFileUtils.createPsiFileList;

//TODO: clean up the code

/**
 * @author Pavel Talanov
 */
public final class K2JSTranslator {

    public static void translateWithCallToMainAndSaveToFile(@NotNull JetFile file,
                                                            @NotNull String outputPath,
                                                            @NotNull Project project) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(new IDEAConfig(project));
        String programCode = translator.generateProgramCode(file) + "\n";
        String callToMain = translator.generateCallToMain(file, "");
        FileWriter writer = new FileWriter(new File(outputPath));
        writer.write(programCode + callToMain);
        writer.close();
    }

    public static void testTranslateFile(@NotNull String inputFile, @NotNull String outputFile) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(new TestConfig());
        JetFile psiFile = JetFileUtils.loadPsiFile(inputFile, translator.getProject());
        translator.generateAndSaveProgram(psiFile, outputFile);
    }

    @NotNull
    private Config config;

    public K2JSTranslator(@NotNull Config config) {
        this.config = config;
    }


    private void generateAndSaveProgram(@NotNull JetFile psiFile,
                                        @NotNull String outputFile) throws IOException {
        JsProgram program = generateProgram(Arrays.asList(psiFile));
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    public static void testTranslateFiles(@NotNull List<String> inputFiles, @NotNull String outputFile)
            throws Exception {
        K2JSTranslator translator = new K2JSTranslator(new TestConfig());
        List<JetFile> psiFiles = createPsiFileList(inputFiles, translator.getProject());
        JsProgram program = translator.generateProgram(psiFiles);
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) {
        JetFile file = JetFileUtils.createPsiFile("test", programText, getProject());
        String programCode = generateProgramCode(file) + "\n";
        String flushOutput = "Kotlin.System.flush();\n";
        String callToMain = generateCallToMain(file, argumentsString);
        String programOutput = "Kotlin.System.output();\n";
        return programCode + flushOutput + callToMain + programOutput;
    }

    @NotNull
    private String generateProgramCode(@NotNull JetFile psiFile) {
        JsProgram program = generateProgram(Arrays.asList(psiFile));
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    private JsProgram generateProgram(@NotNull List<JetFile> files) {
        BindingContext bindingContext = Analyzer.analyzeFiles(files, config.getProject());
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
            AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        }

        JetFile file = files.iterator().next();
        return Translation.generateAst(bindingContext, file, file.getProject());
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
        JetNamespaceHeader namespaceHeader = psiFile.getNamespaceHeader();
        String name = namespaceHeader.getName();
        assert name != null : "NamespaceHeader must have a name";
        return name;
    }

    @NotNull
    private Project getProject() {
        return config.getProject();
    }

}
