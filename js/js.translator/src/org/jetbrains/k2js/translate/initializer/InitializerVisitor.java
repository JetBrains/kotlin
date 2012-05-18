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

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.declaration.ClassTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.general.Translation.translateAsStatement;
import static org.jetbrains.k2js.translate.utils.BindingUtils.*;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getObjectDeclarationForName;

/**
 * @author Pavel Talanov
 */
public abstract class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {
    static InitializerVisitor create(TranslationContext context) {
        return context.isEcma5() ? new Ecma5InitializerVisitor() : new Ecma3InitializerVisitor();
    }

    @Override
    @NotNull
    public final List<JsStatement> visitProperty(@NotNull JetProperty property, @NotNull TranslationContext context) {
        JetExpression initializer = property.getInitializer();
        if (initializer == null) {
            return Collections.emptyList();
        }
        JsExpression initalizerForProperty = generateInitializerForProperty(getPropertyDescriptor(context.bindingContext(), property),
                                                                            Translation.translateAsExpression(initializer, context),
                                                                            context);
        return JsAstUtils.nullableExpressionToStatementList(initalizerForProperty);
    }

    //TODO: should return JsStatement?
    @Nullable
    protected abstract JsExpression generateInitializerForProperty(@NotNull PropertyDescriptor descriptor,
            @NotNull JsExpression expression, @NotNull TranslationContext context);

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
        JsExpression initializerForProperty = generateInitializerForProperty(propertyDescriptor, objectValue, context);
        return JsAstUtils.nullableExpressionToStatementList(initializerForProperty);
    }

    @NotNull
    protected List<JsStatement> generateInitializerStatements(@NotNull List<JetDeclaration> declarations,
            @NotNull TranslationContext context) {
        List<JsStatement> statements = Lists.newArrayList();
        for (JetDeclaration declaration : declarations) {
            statements.addAll(declaration.accept(this, context));
        }
        return statements;
    }

    @NotNull
    public final List<JsStatement> traverseClass(@NotNull JetClassOrObject expression, @NotNull TranslationContext context) {
        return generateInitializerStatements(expression.getDeclarations(), context);
    }

    @NotNull
    public final List<JsStatement> traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        return generateInitializerStatements(getDeclarationsForNamespace(context.bindingContext(), namespace), context);
    }
}
