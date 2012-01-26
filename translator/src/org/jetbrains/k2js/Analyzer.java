package org.jetbrains.k2js;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class Analyzer {

    //TODO: provide some generic way to access
    private static final List<String> LIB_FILE_NAMES = Arrays.asList(
            "C:\\Dev\\Projects\\Kotlin\\jet\\stdlib\\ktSrc\\jssupport\\JsSupport.jet",
            "C:\\Dev\\Projects\\Kotlin\\jet\\stdlib\\ktSrc\\jssupport\\JsDeclarations.kt"
    );

    @NotNull
    public static List<JetFile> getJsSupportStdLib() {
        List<JetFile> libFiles = new ArrayList<JetFile>();
        for (String libFileName : LIB_FILE_NAMES) {
            libFiles.add(JetFileUtils.loadPsiFile(libFileName));
        }
        return libFiles;
    }

    @NotNull
    public static BindingContext analyzeFiles(@NotNull List<JetFile> files) {
        final List<JetFile> jsLibFiles = getJsSupportStdLib();
        List<JetFile> allFiles = allFiles(files, jsLibFiles);
        Project project = getProject(files);
        return AnalyzingUtils.analyzeFiles(project, JsConfiguration.jsLibConfiguration(project),
                allFiles, notLibFiles(jsLibFiles), JetControlFlowDataTraceFactory.EMPTY);
    }

    @NotNull
    private static Project getProject(@NotNull List<JetFile> files) {
        assert !files.isEmpty();
        return files.iterator().next().getProject();
    }

    @NotNull
    private static List<JetFile> allFiles(@NotNull List<JetFile> files,
                                          @NotNull List<JetFile> jsLibFiles) {
        List<JetFile> allFiles = new ArrayList<JetFile>();
        allFiles.addAll(files);
        allFiles.addAll(jsLibFiles);
        return allFiles;
    }

    @NotNull
    private static Predicate<PsiFile> notLibFiles(@NotNull final List<JetFile> jsLibFiles) {
        return new Predicate<PsiFile>() {
            @Override
            public boolean apply(@Nullable PsiFile file) {
                assert file instanceof JetFile;
                return (!jsLibFiles.contains(file));
            }
        };
    }

    private static class JsConfiguration implements Configuration {

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
