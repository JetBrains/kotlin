package org.jetbrains.k2js;

import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
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

import static org.jetbrains.k2js.translate.utils.BindingUtils.getNamespaceDescriptor;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getNameForNamespace;

//TODO: clean up the code

/**
 * @author Pavel Talanov
 */
public final class K2JSTranslator {

    public static void translatePsiFile(@NotNull JetFile file,
                                        @NotNull String outputFile,
                                        @NotNull Project project) throws Exception {
        (new K2JSTranslator()).generateAndSaveProgram(file, outputFile, project);
    }

    public static void doNothing() {

    }

    public static void translateWithCallToMainAndSaveToFile(@NotNull JetFile file,
                                                            @NotNull String outputPath,
                                                            @NotNull Project project) throws Exception {
        K2JSTranslator translator = new K2JSTranslator();
        String programCode = translator.generateProgramCode(file, project) + "\n";
        String callToMain = translator.generateCallToMain(file, "");
        FileWriter writer = new FileWriter(new File(outputPath));
        writer.write(programCode + callToMain);
        writer.close();
    }

    @Nullable
    private BindingContext bindingContext = null;

    public K2JSTranslator() {
    }

    public void testTranslateFile(@NotNull String inputFile, @NotNull String outputFile) throws Exception {
        JetFile psiFile = JetFileUtils.loadPsiFile(inputFile, null);
        generateAndSaveProgram(psiFile, outputFile, null);
    }

    private void generateAndSaveProgram(@NotNull JetFile psiFile,
                                        @NotNull String outputFile,
                                        @Nullable Project project) throws IOException {
        JsProgram program = generateProgram(Arrays.asList(psiFile), project);
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    public void testTranslateFiles(@NotNull List<String> inputFiles, @NotNull String outputFile) throws Exception {
        List<JetFile> psiFiles = createPsiFileList(inputFiles);
        JsProgram program = generateProgram(psiFiles, null);
        CodeGenerator generator = new CodeGenerator();
        generator.generateToFile(program, new File(outputFile));
    }

    @NotNull
    private List<JetFile> createPsiFileList(@NotNull List<String> inputFiles) {
        List<JetFile> psiFiles = new ArrayList<JetFile>();
        for (String inputFile : inputFiles) {
            psiFiles.add(JetFileUtils.loadPsiFile(inputFile, null));
        }
        return psiFiles;
    }

    @NotNull
    public String translateStringWithCallToMain(@NotNull String programText, @NotNull String argumentsString) {
        JetFile file = JetFileUtils.createPsiFile("test", programText, null);
        String programCode = generateProgramCode(file, null) + "\n";
        String flushOutput = "Kotlin.System.flush();\n";
        String callToMain = generateCallToMain(file, argumentsString);
        String programOutput = "Kotlin.System.output();\n";
        return programCode + flushOutput + callToMain + programOutput;
    }

    @NotNull
    private String generateProgramCode(@NotNull JetFile psiFile,
                                       @Nullable Project project) {
        JsProgram program = generateProgram(Arrays.asList(psiFile), project);
        CodeGenerator generator = new CodeGenerator();
        return generator.generateToString(program);
    }

    @NotNull
    private JsProgram generateProgram(@NotNull List<JetFile> files,
                                      @Nullable Project project) {
        bindingContext = Analyzer.analyzeFiles(files, project);
        for (JetFile file : files) {
            AnalyzingUtils.checkForSyntacticErrors(file);
            AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        }

        assert bindingContext != null;
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
        assert bindingContext != null;
        return getNameForNamespace(getNamespaceDescriptor(bindingContext, psiFile));
    }

}
