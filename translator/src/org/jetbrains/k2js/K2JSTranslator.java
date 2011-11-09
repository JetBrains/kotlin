package org.jetbrains.k2js;

import com.google.common.base.Predicates;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.core.JavaCoreEnvironment;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.sampullara.cli.Argument;
import org.jetbrains.k2js.generate.CodeGenerator;
import org.jetbrains.k2js.translate.GenerationState;
import org.jetbrains.jet.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithTextRange;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDefaultImports;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

/**
 * @author Pavel.Talanov
 */

public class K2JSTranslator {
    public static class Arguments {
        @Argument(value = "output", description = "output directory")
        public String outputDir;
        @Argument(value = "src", description = "source file or directory")
        public String src;
    }

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        Arguments arguments = new Arguments();
        arguments.src = "test_files\\test_cases\\test.kt";
        arguments.outputDir = null;
        translate(arguments);
    }

    public static void translate(Arguments arguments) {
        Disposable root = new Disposable() {
            @Override
            public void dispose() {
            }
        };

        JetCoreEnvironment environment = new JetCoreEnvironment(root);

        File rtJar = initJdk();
        if (rtJar == null) return;
        environment.addToClasspath(rtJar);

        VirtualFile vFile = environment.getLocalFileSystem().findFileByPath(arguments.src);
        if (vFile == null) {
            System.out.print("File/directory not found: " + arguments.src);
            return;
        }

        Project project = environment.getProject();
        GenerationState generationState = new GenerationState();
        List<JetNamespace> namespaces = Lists.newArrayList();
        if(vFile.isDirectory())  {
            File dir = new File(vFile.getPath());
            addFiles(environment, project, namespaces, dir);
        }
        else {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
            if (psiFile instanceof JetFile) {
                namespaces.add(((JetFile) psiFile).getRootNamespace());
            }
            else {
                System.out.print("Not a Kotlin file: " + vFile.getPath());
                return;
            }
        }

        BindingContext bindingContext = AnalyzingUtils.getInstance(JavaDefaultImports.JAVA_DEFAULT_IMPORTS)
                .analyzeNamespaces(project, namespaces, Predicates.<PsiFile>alwaysTrue(), JetControlFlowDataTraceFactory.EMPTY);

        ErrorCollector errorCollector = new ErrorCollector(bindingContext);
        errorCollector.report();

        if (!errorCollector.hasErrors) {
            // Translate generated psi
            CodeGenerator generator = new CodeGenerator();
            JsProgram program = generationState.compileCorrectNamespaces(bindingContext, namespaces);
            if (arguments.outputDir == null) {
                generator.generateToConsole(program);
            } else {
                File outputFile = new File(arguments.outputDir);
                try {
                    generator.generateToFile(program, outputFile);
                }
                catch (IOException e) {
                    System.out.println("Failed to write to specified file");
                }
            }
        }

    }

    private static class ErrorCollector {
        Multimap<PsiFile,DiagnosticWithTextRange> maps = LinkedHashMultimap.<PsiFile, DiagnosticWithTextRange>create();

        boolean hasErrors;

        public ErrorCollector(BindingContext bindingContext) {
            for (Diagnostic diagnostic : bindingContext.getDiagnostics()) {
                report(diagnostic);
            }
        }

        private void report(Diagnostic diagnostic) {
            hasErrors |= diagnostic.getSeverity() == Severity.ERROR;
            if(diagnostic instanceof DiagnosticWithTextRange) {
                DiagnosticWithTextRange diagnosticWithTextRange = (DiagnosticWithTextRange) diagnostic;
                maps.put(diagnosticWithTextRange.getPsiFile(), diagnosticWithTextRange);
            }
            else {
                System.out.println(diagnostic.getSeverity().toString() + ": " + diagnostic.getMessage());
            }
        }

        void report() {
            if(!maps.isEmpty()) {
                for (PsiFile psiFile : maps.keySet()) {
                    System.out.println(psiFile.getVirtualFile().getPath());
                    Collection<DiagnosticWithTextRange> diagnosticWithTextRanges = maps.get(psiFile);
                    for (DiagnosticWithTextRange diagnosticWithTextRange : diagnosticWithTextRanges) {
                        String position = DiagnosticUtils.formatPosition(diagnosticWithTextRange);
                        System.out.println("\t" + diagnosticWithTextRange.getSeverity().toString() + ": " + position + " " + diagnosticWithTextRange.getMessage());
                    }
                }
            }
        }
    }

    private static void addFiles(JavaCoreEnvironment environment, Project project, List<JetNamespace> namespaces, File dir) {
        for(File file : dir.listFiles()) {
            if(!file.isDirectory()) {
                VirtualFile virtualFile = environment.getLocalFileSystem().findFileByPath(file.getAbsolutePath());
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile instanceof JetFile) {
                    namespaces.add(((JetFile) psiFile).getRootNamespace());
                }
            }
            else {
                addFiles(environment, project, namespaces, file);
            }
        }
    }

    private static File initJdk() {
        String javaHome = System.getenv("JAVA_HOME");
        File rtJar = null;
        if (javaHome == null) {
            ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
            if(systemClassLoader instanceof URLClassLoader) {
                URLClassLoader loader = (URLClassLoader) systemClassLoader;
                for(URL url: loader.getURLs()) {
                    if("file".equals(url.getProtocol())) {
                        if(url.getFile().endsWith("/lib/rt.jar")) {
                            rtJar = new File(url.getFile());
                            break;
                        }
                        if(url.getFile().endsWith("/Classes/classes.jar")) {
                            rtJar = new File(url.getFile()).getAbsoluteFile();
                            break;
                        }
                    }
                }
            }

            if(rtJar == null) {
                System.out.println("JAVA_HOME environment variable needs to be defined");
                return null;
            }
        }
        else {
            rtJar = findRtJar(javaHome);
        }

        if (rtJar == null || !rtJar.exists()) {
            System.out.print("No rt.jar found under JAVA_HOME=" + javaHome);
            return null;
        }
        return rtJar;
    }

    private static File findRtJar(String javaHome) {
        File rtJar = new File(javaHome, "jre/lib/rt.jar");
        if (rtJar.exists()) {
            return rtJar;
        }
        return null;
    }

}

