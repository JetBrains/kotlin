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
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.DescriptorUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.newObjectLiteral;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.newVar;

/**
 * @author Pavel.Talanov
 *         <p/>
 *         Genereate code for a single descriptor.
 */
public final class NamespaceTranslator extends AbstractTranslator {

    @NotNull
    private final NamespaceDescriptor descriptor;
    @NotNull
    private final JsName namespaceName;
    @NotNull
    private final ClassDeclarationTranslator classDeclarationTranslator;

    /*package*/ NamespaceTranslator(@NotNull NamespaceDescriptor descriptor,
                                    @NotNull ClassDeclarationTranslator classDeclarationTranslator,
                                    @NotNull TranslationContext context) {
        super(context.newDeclaration(descriptor));
        this.descriptor = descriptor;
        this.namespaceName = context.getNameForDescriptor(descriptor);
        this.classDeclarationTranslator = classDeclarationTranslator;
    }


    @NotNull
    public JsStatement getDeclarationAsVar() {
        return newVar(namespaceName, getNamespaceDeclaration());
    }

    @NotNull
    public JsPropertyInitializer getDeclarationAsInitializer() {
        return new JsPropertyInitializer(namespaceName.makeRef(), getNamespaceDeclaration());
    }

    @NotNull
    private JsInvocation getNamespaceDeclaration() {
        JsInvocation namespaceDeclaration = namespaceCreateMethodInvocation();
        namespaceDeclaration.getArguments().add(translateNamespaceMemberDeclarations());
        namespaceDeclaration.getArguments().add(getClassesAndNestedNamespaces());
        return namespaceDeclaration;
    }

    @NotNull
    private JsInvocation namespaceCreateMethodInvocation() {
        return AstUtil.newInvocation(context().namer().namespaceCreationMethodReference());
    }

    @NotNull
    private JsObjectLiteral getClassesAndNestedNamespaces() {
        JsObjectLiteral classesAndNestedNamespaces = new JsObjectLiteral();
        classesAndNestedNamespaces.getPropertyInitializers()
            .addAll(getClassesDefined());
        classesAndNestedNamespaces.getPropertyInitializers()
            .addAll(getNestedNamespaceDeclarations());
        return classesAndNestedNamespaces;
    }

    @NotNull
    private List<JsPropertyInitializer> getClassesDefined() {
        return classDeclarationTranslator.classDeclarationsForNamespace(descriptor);
    }

    @NotNull
    private List<JsPropertyInitializer> getNestedNamespaceDeclarations() {
        List<JsPropertyInitializer> result = Lists.newArrayList();
        List<NamespaceDescriptor> nestedNamespaces = DescriptorUtils.getNestedNamespaces(descriptor);
        for (NamespaceDescriptor nestedNamespace : nestedNamespaces) {
            NamespaceTranslator nestedNamespaceTranslator = new NamespaceTranslator(nestedNamespace, classDeclarationTranslator, context());
            result.add(nestedNamespaceTranslator.getDeclarationAsInitializer());
        }
        return result;
    }

    @NotNull
    private JsObjectLiteral translateNamespaceMemberDeclarations() {
        List<JsPropertyInitializer> propertyList = new ArrayList<JsPropertyInitializer>();
        propertyList.add(Translation.generateNamespaceInitializerMethod(descriptor, context()));
        propertyList.addAll(new DeclarationBodyVisitor().traverseNamespace(descriptor, context()));
        return newObjectLiteral(propertyList);
    }
}
