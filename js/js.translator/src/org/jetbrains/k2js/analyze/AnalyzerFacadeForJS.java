/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.k2js.analyze;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.k2js.config.Config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Pavel Talanov
 */
public final class AnalyzerFacadeForJS {

    private AnalyzerFacadeForJS() {
    }

    @NotNull
    public static BindingContext analyzeFilesAndCheckErrors(@NotNull List<JetFile> files,
                                                            @NotNull Config config) {
        BindingContext bindingContext = analyzeFiles(files, config);
        checkForErrors(withJsLibAdded(files, config), bindingContext);
        return bindingContext;
    }

    @NotNull
    public static BindingContext analyzeFiles(@NotNull List<JetFile> files,
                                              @NotNull Config config) {
        Project project = config.getProject();
        return AnalyzingUtils.analyzeFiles(project,
                                           JsConfiguration.jsLibConfiguration(project),
                                           withJsLibAdded(files, config),
                                           notLibFiles(config.getLibFiles()),
                                           JetControlFlowDataTraceFactory.EMPTY);
    }

    private static void checkForErrors(@NotNull List<JetFile> allFiles, @NotNull BindingContext bindingContext) {
        AnalyzingUtils.throwExceptionOnErrors(bindingContext);
        for (JetFile file : allFiles) {
            AnalyzingUtils.checkForSyntacticErrors(file);
        }
    }

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
                @SuppressWarnings("UnnecessaryLocalVariable") boolean notLibFile = !jsLibFiles.contains(file);
                return notLibFile;
            }
        };
    }

    public static BindingContext analyzeNamespace(@NotNull JetFile file) {
        BindingTraceContext bindingTraceContext = new BindingTraceContext();
        Project project = file.getProject();
        AnalyzingUtils.analyzeFilesWithGivenTrace(
                project,
                JsConfiguration.jsLibConfiguration(project),
                Collections.singletonList(file),
                Predicates.<PsiFile>alwaysTrue(),
                JetControlFlowDataTraceFactory.EMPTY,
                bindingTraceContext
        );
        return bindingTraceContext.getBindingContext();
    }

    private static final class JsConfiguration implements ModuleConfiguration {

        @NotNull
        private final Project project;

        public static JsConfiguration jsLibConfiguration(@NotNull Project project) {
            return new JsConfiguration(project);
        }

        private JsConfiguration(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void addDefaultImports(@NotNull WritableScope rootScope,
                                      @NotNull Collection<JetImportDirective> directives) {
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath("js.*")));
            directives.add(JetPsiFactory.createImportDirective(project, new ImportPath(JetStandardClasses.STANDARD_CLASSES_FQNAME, true)));
        }

        @Override
        public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor,
                                         @NotNull WritableScope namespaceMemberScope) {
            DefaultModuleConfiguration.createStandardConfiguration(project).extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
        }

    }
}
