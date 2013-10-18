/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.declaration.DefineInvocation.createDefineInvocation;
import static org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator.createPlace;

final class NamespaceTranslator extends AbstractTranslator {
    @NotNull
    private final NamespaceDescriptor descriptor;

    private final FileDeclarationVisitor visitor;

    private final NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>> definitionPlace;

    NamespaceTranslator(
            @NotNull final NamespaceDescriptor descriptor,
            @NotNull final Map<NamespaceDescriptor, DefineInvocation> descriptorToDefineInvocation,
            @NotNull TranslationContext context
    ) {
        super(context.newDeclaration(descriptor));

        this.descriptor = descriptor;

        visitor = new FileDeclarationVisitor(context());

        definitionPlace = new NotNullLazyValue<Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression>>() {
            @Override
            @NotNull
            public Trinity<List<JsPropertyInitializer>, LabelGenerator, JsExpression> compute() {
                DefineInvocation defineInvocation = descriptorToDefineInvocation.get(descriptor);
                if (defineInvocation == null) {
                    defineInvocation = createDefinitionPlace(null, descriptorToDefineInvocation);
                }

                return createPlace(defineInvocation.getMembers(), context().getQualifiedReference(descriptor));
            }
        };
    }

    public void translate(JetFile file) {
        context().literalFunctionTranslator().setDefinitionPlace(definitionPlace);
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (!AnnotationsUtils.isPredefinedObject(BindingUtils.getDescriptorForElement(bindingContext(), declaration))) {
                declaration.accept(visitor, context());
            }
        }
        context().literalFunctionTranslator().setDefinitionPlace(null);
    }

    private DefineInvocation createDefinitionPlace(
            @Nullable JsExpression initializer,
            Map<NamespaceDescriptor, DefineInvocation> descriptorToDefineInvocation
    ) {
        DefineInvocation place = createDefineInvocation(descriptor, initializer, new JsObjectLiteral(visitor.getResult(), true), context());
        descriptorToDefineInvocation.put(descriptor, place);
        addToParent((NamespaceDescriptor) descriptor.getContainingDeclaration(), getEntry(descriptor, place), descriptorToDefineInvocation);
        return place;
    }

    public void add(@NotNull Map<NamespaceDescriptor, DefineInvocation> descriptorToDefineInvocation) {
        JsExpression initializer = visitor.computeInitializer();

        DefineInvocation defineInvocation = descriptorToDefineInvocation.get(descriptor);
        if (defineInvocation == null) {
            if (initializer != null || !visitor.getResult().isEmpty()) {
                createDefinitionPlace(initializer, descriptorToDefineInvocation);
            }
        }
        else {
            if (initializer != null) {
                assert defineInvocation.getInitializer() == JsLiteral.NULL;
                defineInvocation.setInitializer(initializer);
            }

            List<JsPropertyInitializer> listFromPlace = defineInvocation.getMembers();
            // if equals, so, inner functions was added
            if (listFromPlace != visitor.getResult()) {
                listFromPlace.addAll(visitor.getResult());
            }
        }
    }

    private JsPropertyInitializer getEntry(@NotNull NamespaceDescriptor descriptor, DefineInvocation defineInvocation) {
        return new JsPropertyInitializer(context().getNameForDescriptor(descriptor).makeRef(),
                                         new JsInvocation(context().namer().packageDefinitionMethodReference(), defineInvocation.asList()));
    }

    private boolean addEntryIfParentExists(NamespaceDescriptor parentDescriptor,
            JsPropertyInitializer entry,
            Map<NamespaceDescriptor, DefineInvocation> descriptorToDeclarationPlace) {
        DefineInvocation parentDefineInvocation = descriptorToDeclarationPlace.get(parentDescriptor);
        if (parentDefineInvocation != null) {
            parentDefineInvocation.getMembers().add(entry);
            return true;
        }
        return false;
    }

    private void addToParent(NamespaceDescriptor parentDescriptor,
            JsPropertyInitializer entry,
            Map<NamespaceDescriptor, DefineInvocation> descriptorToDefineInvocation) {
        while (!addEntryIfParentExists(parentDescriptor, entry, descriptorToDefineInvocation)) {
            JsObjectLiteral members = new JsObjectLiteral(new SmartList<JsPropertyInitializer>(entry), true);
            DefineInvocation defineInvocation = createDefineInvocation(parentDescriptor, null, members, context());
            entry = getEntry(parentDescriptor, defineInvocation);

            descriptorToDefineInvocation.put(parentDescriptor, defineInvocation);
            parentDescriptor = (NamespaceDescriptor) parentDescriptor.getContainingDeclaration();
        }
    }

}
