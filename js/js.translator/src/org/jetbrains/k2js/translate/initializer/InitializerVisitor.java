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

package org.jetbrains.k2js.translate.initializer;

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.general.Translation.translateAsStatement;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDeclarationsForNamespace;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForObjectDeclaration;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getObjectDeclarationForName;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;

/**
 * @author Pavel Talanov
 */
class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {
    static InitializerVisitor create(TranslationContext context) {
        return context.isEcma5() ? new InitializerEcma5Visitor() : new InitializerVisitor();
    }

    @Override
    @NotNull
    public final List<JsStatement> visitProperty(@NotNull JetProperty property, @NotNull TranslationContext context) {
        JetExpression initializer = property.getInitializer();
        if (initializer == null) {
            return Collections.emptyList();
        }
        return toStatements(defineMember(context, getPropertyDescriptor(context.bindingContext(), property),
                                         Translation.translateAsExpression(initializer, context)));
    }

    @Nullable
    protected JsExpression defineMember(TranslationContext context, PropertyDescriptor propertyDescriptor, JsExpression value) {
        return assignmentToBackingField(context, propertyDescriptor, value);
    }

    @Override
    @NotNull
    public List<JsStatement> visitAnonymousInitializer(@NotNull JetClassInitializer initializer,
                                                       @NotNull TranslationContext context) {
        return Arrays.asList(translateAsStatement(initializer.getBody(), context));
    }

    @Override
    @NotNull
    // Not interested in other types of declarations, they do not contain initializers.
    public List<JsStatement> visitDeclaration(@NotNull JetDeclaration expression, @NotNull TranslationContext context) {
        return Collections.emptyList();
    }

    @Override
    @NotNull
    public final List<JsStatement> visitObjectDeclarationName(@NotNull JetObjectDeclarationName objectName,
            @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptor = getPropertyDescriptorForObjectDeclaration(context.bindingContext(), objectName);
        JetObjectDeclaration objectDeclaration = getObjectDeclarationForName(objectName);
        JsInvocation objectValue = ClassTranslator.generateClassCreationExpression(objectDeclaration, context);
        return toStatements(defineMember(context, propertyDescriptor, objectValue));
    }

    private static List<JsStatement> toStatements(@Nullable JsExpression expression) {
        return expression == null ? Collections.<JsStatement>emptyList() : Collections.<JsStatement>singletonList(expression.makeStmt());
    }

    protected List<JsStatement> createStatements(List<JetDeclaration> declarations, TranslationContext context) {
        if (declarations.isEmpty()) {
            return Collections.emptyList();
        }

        List<JsStatement> statements = new ArrayList<JsStatement>(declarations.size());
        for (JetDeclaration declaration : declarations) {
            statements.addAll(declaration.accept(this, context));
        }
        return statements;
    }

    @NotNull
    public final List<JsStatement> traverseClass(@NotNull JetClassOrObject expression, @NotNull TranslationContext context) {
        return createStatements(expression.getDeclarations(), context);
    }

    @NotNull
    public final List<JsStatement> traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        return createStatements(getDeclarationsForNamespace(context.bindingContext(), namespace), context);
    }
}
