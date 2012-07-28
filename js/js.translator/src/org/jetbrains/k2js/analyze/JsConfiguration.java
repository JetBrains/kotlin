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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.DefaultModuleConfiguration;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.isRootNamespace;

/**
* @author Pavel Talanov
*/
public final class JsConfiguration implements ModuleConfiguration {

    @NotNull
    private static final List<ImportPath> DEFAULT_IMPORT_PATHS = Arrays.asList(new ImportPath("js.*"), new ImportPath("java.lang.*"),
                                                                              new ImportPath(JetStandardClasses.STANDARD_CLASSES_FQNAME,
                                                                                             true),
                                                                              new ImportPath("kotlin.*"));
    @NotNull
    private final Project project;
    /*
    * Adds a possibility to inject some preanalyzed files to speed up tests.
    * */
    @Nullable
    private final BindingContext preanalyzedContext;

    JsConfiguration(@NotNull Project project, @Nullable BindingContext preanalyzedContext) {
        this.project = project;
        this.preanalyzedContext = preanalyzedContext;
    }

    @Override
    public void addDefaultImports(@NotNull Collection<JetImportDirective> directives) {
        for (ImportPath path : DEFAULT_IMPORT_PATHS) {
            directives.add(JetPsiFactory.createImportDirective(project, path));
        }
    }

    @Override
    public void extendNamespaceScope(@NotNull BindingTrace trace, @NotNull NamespaceDescriptor namespaceDescriptor,
            @NotNull WritableScope namespaceMemberScope) {
        DefaultModuleConfiguration.createStandardConfiguration(project, BuiltinsScopeExtensionMode.ALL)
                .extendNamespaceScope(trace, namespaceDescriptor, namespaceMemberScope);
        if (hasPreanalyzedContextForTests()) {
            extendScopeWithPreAnalyzedContextForTests(namespaceDescriptor, namespaceMemberScope);
        }
    }

    private boolean hasPreanalyzedContextForTests() {
        return preanalyzedContext != null;
    }

    /*NOTE: this code is wrong. Check it if you have tests failing for frontend reasons*/
    @SuppressWarnings("ConstantConditions")
    private void extendScopeWithPreAnalyzedContextForTests(@NotNull NamespaceDescriptor namespaceDescriptor,
            @NotNull WritableScope namespaceMemberScope) {
        if (isNamespaceImportedByDefault(namespaceDescriptor) || isRootNamespace(namespaceDescriptor)) {
            FqName descriptorName = DescriptorUtils.getFQName(namespaceDescriptor).toSafe();
            NamespaceDescriptor alreadyAnalyzedNamespace = preanalyzedContext.get(BindingContext.FQNAME_TO_NAMESPACE_DESCRIPTOR, descriptorName);
            namespaceMemberScope.importScope(alreadyAnalyzedNamespace.getMemberScope());
        }
    }

    private static boolean isNamespaceImportedByDefault(@NotNull NamespaceDescriptor namespaceDescriptor) {
        for (ImportPath path : DEFAULT_IMPORT_PATHS) {
            if (path.fqnPart().equals(DescriptorUtils.getFQName(namespaceDescriptor).toSafe())) {
                return true;
            }
        }
        return false;
    }
}
