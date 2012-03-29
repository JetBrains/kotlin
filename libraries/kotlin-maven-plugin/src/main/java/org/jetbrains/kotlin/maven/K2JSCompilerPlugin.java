package org.jetbrains.kotlin.maven;

import com.intellij.openapi.project.Project;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.CompilerPlugin;
import org.jetbrains.jet.compiler.CompilerPluginContext;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.facade.K2JSTranslator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static kotlin.io.namespace.use;

/**
 * Compiles Kotlin code to JavaScript
 */
public class K2JSCompilerPlugin implements CompilerPlugin {

    private String outFile = "target/js/program.js";

    public void processFiles(CompilerPluginContext context) {
        if (context != null) {
            Project project = context.getProject();
            BindingContext bindingContext = context.getContext();
            List<JetFile> sources = context.getFiles();
            if (bindingContext != null && sources != null && project != null) {
                Config config = new Config(project) {
                    @NotNull
                    @Override
                    public List<JetFile> getLibFiles() {
                        return new ArrayList<JetFile>();
                    }
                };
                K2JSTranslator translator = new K2JSTranslator(config);
                final String code = translator.generateProgramCode(sources);
                File file = new File(outFile);
                File outDir = file.getParentFile();
                if (outDir != null && !outDir.exists()) {
                    outDir.mkdirs();
                }
                try {
                    use(new FileWriter(file), new Function1<FileWriter, Object>() {
                        @Override
                        public Object invoke(FileWriter writer) {
                            try {
                                writer.write(code);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            return null;
                        }
                    });
                } catch (IOException e) {
                    System.out.println("Error: " + e);
                    e.printStackTrace();
                }
            }
        }
    }

    public String getOutFile() {
        return outFile;
    }

    public void setOutFile(String outFile) {
        this.outFile = outFile;
    }
}
