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

package org.jetbrains.k2js.translate.declaration;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.List;
import java.util.Set;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getAllNonNativeNamespaceDescriptors;
import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getAllClassesDefinedInNamespace;

/**
 * @author Pavel Talanov
 */
public final class NamespaceDeclarationTranslator extends AbstractTranslator {

    public static List<JsStatement> translateFiles(@NotNull List<JetFile> files, @NotNull TranslationContext context) {
        Set<NamespaceDescriptor> namespaceDescriptorSet = getAllNonNativeNamespaceDescriptors(context.bindingContext(), files);
        return (new NamespaceDeclarationTranslator(Lists.newArrayList(namespaceDescriptorSet), context)).translate();
    }

    @NotNull
    private final ClassDeclarationTranslator classDeclarationTranslator;
    @NotNull
    private final List<NamespaceDescriptor> namespaceDescriptors;

    private NamespaceDeclarationTranslator(@NotNull List<NamespaceDescriptor> namespaceDescriptors,
                                           @NotNull TranslationContext context) {
        super(context);
        this.namespaceDescriptors = namespaceDescriptors;
        this.classDeclarationTranslator = new ClassDeclarationTranslator(getAllClasses(), context);
    }

    @NotNull
    private List<ClassDescriptor> getAllClasses() {
        List<ClassDescriptor> result = Lists.newArrayList();
        for (NamespaceDescriptor namespaceDescriptor : namespaceDescriptors) {
            result.addAll(getAllClassesDefinedInNamespace(namespaceDescriptor));
        }
        return result;
    }

    @NotNull
    private List<JsStatement> translate() {
        List<JsStatement> result = classesDeclarations();
        result.addAll(namespacesDeclarations());
        return result;
    }

    @NotNull
    private List<JsStatement> classesDeclarations() {
        List<JsStatement> result = Lists.newArrayList();
        classDeclarationTranslator.generateDeclarations();
        result.add(classDeclarationTranslator.getDeclarationsStatement());
        return result;
    }

    @NotNull
    private List<JsStatement> namespacesDeclarations() {
        List<JsStatement> result = Lists.newArrayList();
        List<NamespaceTranslator> namespaceTranslators = getTranslatorsForNonEmptyNamespaces();
        result.addAll(declarationStatements(namespaceTranslators));
        result.addAll(initializeStatements(namespaceTranslators));
        return result;
    }

    @NotNull
    private List<NamespaceTranslator> getTranslatorsForNonEmptyNamespaces() {
        List<NamespaceTranslator> namespaceTranslators = Lists.newArrayList();
        for (NamespaceDescriptor descriptor : namespaceDescriptors) {
            if (!DescriptorUtils.isNamespaceEmpty(descriptor)) {
                namespaceTranslators.add(new NamespaceTranslator(descriptor, classDeclarationTranslator, context()));
            }
        }
        return namespaceTranslators;
    }

    @NotNull
    private static List<JsStatement> declarationStatements(@NotNull List<NamespaceTranslator> namespaceTranslators) {
        List<JsStatement> result = Lists.newArrayList();
        for (NamespaceTranslator translator : namespaceTranslators) {
            result.add(translator.getDeclarationStatement());
        }
        return result;
    }

    @NotNull
    private static List<JsStatement> initializeStatements(@NotNull List<NamespaceTranslator> namespaceTranslators) {
        List<JsStatement> result = Lists.newArrayList();
        for (NamespaceTranslator translator : namespaceTranslators) {
            result.add(translator.getInitializeStatement());
        }
        return result;
    }
}
