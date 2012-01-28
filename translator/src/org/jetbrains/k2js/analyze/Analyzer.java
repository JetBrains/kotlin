package org.jetbrains.k2js.analyze;

import com.google.common.base.Predicate;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.Configuration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.k2js.config.Config;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class Analyzer {

    @NotNull
    public static BindingContext analyzeFilesAndCheckErrors(@NotNull List<JetFile> files,
                                                            @NotNull Config config) {
        final List<JetFile> jsLibFiles = config.getLibFiles();
        List<JetFile> allFiles = withJsLibAdded(files, config);
        BindingContext bindingContext = AnalyzingUtils.analyzeFiles(config.getProject(),
                JsConfiguration.jsLibConfiguration(config.getProject()),
                allFiles, notLibFiles(jsLibFiles), JetControlFlowDataTraceFactory.EMPTY);
        checkForErrors(allFiles, bindingContext);
        return bindingContext;
    }

    private static void checkForErrors(@NotNull List<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
            AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        }
    }

    //TODO: use some mechanism to avoid this
    //TODO: move method somewhere
    @NotNull
    public static List<JetFile> withJsLibAdded(@NotNull List<JetFile> files, @NotNull Config config) {
        List<JetFile> allFiles = new ArrayList<JetFile>();
        allFiles.addAll(files);
        allFiles.addAll(config.getLibFiles());
        return allFiles;
    }

    @NotNull
    private static Predicate<PsiFile> notLibFiles(@NotNull final List<JetFile> jsLibFiles) {
        return new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                assert file instanceof JetFile;
                boolean notLibFile = !jsLibFiles.contains(file);
                return notLibFile;
            }
        };
    }

    private static final class JsConfiguration implements Configuration {

        @NotNull
        private Project project;

        public static JsConfiguration jsLibConfiguration(@NotNull Project project) {
            return new JsConfiguration(project);
        }

        private JsConfiguration(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void addDefaultImports(@NotNull BindingTrace trace, @NotNull WritableScope rootScope,
                                      @NotNull Importer importer) {
            ImportsResolver.ImportResolver importResolver = new ImportsResolver.ImportResolver(trace, true);
            importResolver.processImportReference(JetPsiFactory.createImportDirective(project, "js.*"), rootScope, importer);
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor,
                                         @NotNull WritableScope namespaceMemberScope) {
        }
    }

    private Analyzer() {
    }
}
