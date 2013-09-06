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
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.LabelGenerator;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.initializer.InitializerUtils;
import org.jetbrains.k2js.translate.initializer.InitializerVisitor;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.translate.declaration.DefineInvocation.createDefineInvocation;
import static org.jetbrains.k2js.translate.expression.LiteralFunctionTranslator.createPlace;
import static org.jetbrains.k2js.translate.initializer.InitializerUtils.generateInitializerForDelegate;
import static org.jetbrains.k2js.translate.initializer.InitializerUtils.generateInitializerForProperty;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptor;

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

        visitor = new FileDeclarationVisitor();

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

    public void add(@NotNull Map<NamespaceDescriptor, DefineInvocation> descriptorToDefineInvocation,
            @NotNull List<JsStatement> initializers) {
        JsExpression initializer;
        if (visitor.initializerStatements.isEmpty()) {
            initializer = null;
        }
        else {
            initializer = visitor.initializer;
            if (!context().isEcma5()) {
                initializers.add(new JsInvocation(Namer.getFunctionCallRef(initializer),
                                                  context().getQualifiedReference(descriptor)).makeStmt());
            }
        }

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

    private class FileDeclarationVisitor extends DeclarationBodyVisitor {
        private final JsFunction initializer;
        private final TranslationContext initializerContext;
        private final List<JsStatement> initializerStatements;
        private final InitializerVisitor initializerVisitor;

        private FileDeclarationVisitor() {
            initializer = JsAstUtils.createFunctionWithEmptyBody(context().scope());
            initializerContext = context().contextWithScope(initializer);
            initializerStatements = initializer.getBody().getStatements();
            initializerVisitor = new InitializerVisitor(initializerStatements);
        }

        @Override
        public Void visitClass(@NotNull JetClass declaration, @NotNull TranslationContext context) {
            JsPropertyInitializer entry = context.classDeclarationTranslator().translate(declaration, context);
            if (entry != null) {
                result.add(entry);
            }
            return null;
        }

        @Override
        public Void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, @NotNull TranslationContext context) {
            InitializerUtils.generateObjectInitializer(declaration, initializerStatements, context);
            return null;
        }

        @Override
        public Void visitProperty(@NotNull JetProperty property, @NotNull TranslationContext context) {
            super.visitProperty(property, context);
            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                JsExpression value = Translation.translateAsExpression(initializer, initializerContext);
                PropertyDescriptor propertyDescriptor = getPropertyDescriptor(context.bindingContext(), property);
                initializerStatements.add(generateInitializerForProperty(context, propertyDescriptor, value));
            }

            JsStatement delegate = generateInitializerForDelegate(context, property);
            if (delegate != null) initializerStatements.add(delegate);

            return null;
        }

        @Override
        public Void visitAnonymousInitializer(@NotNull JetClassInitializer expression, @NotNull TranslationContext context) {
            expression.accept(initializerVisitor, initializerContext);
            return null;
        }
    }
}
