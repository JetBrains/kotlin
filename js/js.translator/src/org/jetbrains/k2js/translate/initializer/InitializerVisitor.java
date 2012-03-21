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

import com.google.dart.compiler.backend.js.ast.JsExprStmt;
import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsInvocation;
import com.google.dart.compiler.backend.js.ast.JsStatement;
import org.jetbrains.annotations.NotNull;
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
import static org.jetbrains.k2js.translate.utils.BindingUtils.getPropertyDescriptorForObjectDeclaration;
import static org.jetbrains.k2js.translate.utils.PsiUtils.getObjectDeclarationForName;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;

/**
 * @author Pavel Talanov
 */
public final class InitializerVisitor extends TranslatorVisitor<List<JsStatement>> {

    /*package*/ InitializerVisitor() {
    }

    @Override
    @NotNull
    public List<JsStatement> visitProperty(@NotNull JetProperty expression, @NotNull TranslationContext context) {
        JetExpression initializer = expression.getInitializer();
        if (initializer == null) {
            return new ArrayList<JsStatement>();
        }
        return Arrays.asList(translateInitializer(expression, context, initializer));
    }

    @NotNull
    private static JsStatement translateInitializer(@NotNull JetProperty property, @NotNull TranslationContext context,
                                                    @NotNull JetExpression initializer) {
        JsExpression initExpression = Translation.translateAsExpression(initializer, context);
        return assignmentToBackingField(context, property, initExpression).makeStmt();
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
    public List<JsStatement> visitObjectDeclarationName(@NotNull JetObjectDeclarationName objectName,
                                                        @NotNull TranslationContext context) {
        PropertyDescriptor propertyDescriptorForObjectDeclaration
            = getPropertyDescriptorForObjectDeclaration(context.bindingContext(), objectName);
        JetObjectDeclaration objectDeclaration = getObjectDeclarationForName(objectName);
        JsInvocation objectValue = ClassTranslator.generateClassCreationExpression(objectDeclaration, context);
        JsExprStmt assignment = assignmentToBackingField(context, propertyDescriptorForObjectDeclaration, objectValue)
            .makeStmt();
        return Collections.<JsStatement>singletonList(assignment);
    }

    @NotNull
    public List<JsStatement> traverseClass(@NotNull JetClassOrObject expression, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : expression.getDeclarations()) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }

    @NotNull
    public List<JsStatement> traverseNamespace(@NotNull NamespaceDescriptor namespace, @NotNull TranslationContext context) {
        List<JsStatement> initializerStatements = new ArrayList<JsStatement>();
        for (JetDeclaration declaration : getDeclarationsForNamespace(context.bindingContext(), namespace)) {
            initializerStatements.addAll(declaration.accept(this, context));
        }
        return initializerStatements;
    }
}
