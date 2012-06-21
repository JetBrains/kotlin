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
import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.JsAstUtils;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getAllNonNativeNamespaceDescriptors;
import static org.jetbrains.k2js.translate.utils.JsDescriptorUtils.getAllClassesDefinedInNamespace;

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
            result.addAll(getAllClassesDefinedInNamespace(namespaceDescriptor, context().bindingContext()));
        }
        return result;
    }

    @NotNull
    private List<JsStatement> translate() {
        List<JsStatement> result = new ArrayList<JsStatement>();
        classesDeclarations(result);
        namespacesDeclarations(result);
        return result;
    }

    private void classesDeclarations(List<JsStatement> statements) {
        classDeclarationTranslator.generateDeclarations();
        statements.add(classDeclarationTranslator.getDeclarationsStatement());
    }

    private void namespacesDeclarations(List<JsStatement> statements) {
        List<NamespaceTranslator> namespaceTranslators = getTranslatorsForNonEmptyNamespaces();
        declarationStatements(namespaceTranslators, statements);
        initializeStatements(namespaceTranslators, statements);
    }

    @NotNull
    private List<NamespaceTranslator> getTranslatorsForNonEmptyNamespaces() {
        List<NamespaceTranslator> namespaceTranslators = Lists.newArrayList();
        for (NamespaceDescriptor descriptor : filterNonEmptyNamespaces(filterTopLevelAndRootNamespaces(namespaceDescriptors))) {
            namespaceTranslators.add(new NamespaceTranslator(descriptor, classDeclarationTranslator, context()));
        }
        return namespaceTranslators;
    }

    private void declarationStatements(@NotNull List<NamespaceTranslator> namespaceTranslators,
            @NotNull List<JsStatement> statements) {
        JsObjectLiteral objectLiteral = new JsObjectLiteral();
        JsNameRef packageMapNameRef = context().jsScope().declareName("_").makeRef();
        JsExpression packageMapValue;
        if (context().isNotEcma3()) {
            packageMapValue = AstUtil.newInvocation(JsAstUtils.CREATE_OBJECT, context().program().getNullLiteral(), objectLiteral);
        }
        else {
            packageMapValue = objectLiteral;
        }
        statements.add(JsAstUtils.newVar(packageMapNameRef.getName(), packageMapValue));

        for (NamespaceTranslator translator : namespaceTranslators) {
            translator.addNamespaceDeclaration(objectLiteral.getPropertyInitializers());
        }
    }

    private static void initializeStatements(@NotNull List<NamespaceTranslator> namespaceTranslators,
            @NotNull List<JsStatement> statements) {
        for (NamespaceTranslator translator : namespaceTranslators) {
            for (JsExpression expression : translator.getInitializers()) {
                statements.add(expression.makeStmt());
            }
        }
    }

    @NotNull
    private static List<NamespaceDescriptor> filterTopLevelAndRootNamespaces(@NotNull List<NamespaceDescriptor> namespaceDescriptors) {
        List<NamespaceDescriptor> result = Lists.newArrayList();
        for (NamespaceDescriptor descriptor : namespaceDescriptors) {
            if (DescriptorUtils.isTopLevelNamespace(descriptor) || DescriptorUtils.isRootNamespace(descriptor)) {
                result.add(descriptor);
            }
        }
        return result;
    }

    @NotNull
    private List<NamespaceDescriptor> filterNonEmptyNamespaces(@NotNull List<NamespaceDescriptor> namespaceDescriptors) {
        List<NamespaceDescriptor> result = Lists.newArrayList();
        for (NamespaceDescriptor descriptor : namespaceDescriptors) {
            if (!JsDescriptorUtils.isNamespaceEmpty(descriptor, context().bindingContext())) {
                result.add(descriptor);
            }
        }
        return result;
    }
}
