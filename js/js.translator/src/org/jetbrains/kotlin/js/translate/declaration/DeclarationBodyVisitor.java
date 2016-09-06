/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.Translation;
import org.jetbrains.kotlin.js.translate.general.TranslatorVisitor;
import org.jetbrains.kotlin.js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.js.translate.utils.TranslationUtils;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getFunctionDescriptor;
import static org.jetbrains.kotlin.js.translate.utils.JsDescriptorUtils.getSupertypesWithoutFakes;

public class DeclarationBodyVisitor extends TranslatorVisitor<Void> {
    protected final List<JsPropertyInitializer> result;
    private final List<JsPropertyInitializer> staticResult;
    private final List<JsPropertyInitializer> enumEntryList = new SmartList<JsPropertyInitializer>();

    @NotNull
    private final JsScope scope;

    @Nullable
    private List<JsStatement> initializerStatements;

    public DeclarationBodyVisitor(
            @NotNull List<JsPropertyInitializer> result, @NotNull List<JsPropertyInitializer> staticResult,
            @NotNull JsScope scope
    ) {
        this.result = result;
        this.staticResult = staticResult;
        this.scope = scope;
    }

    @NotNull
    public List<JsPropertyInitializer> getResult() {
        return result;
    }

    public List<JsPropertyInitializer> getEnumEntryList() {
        return enumEntryList;
    }

    @Override
    protected Void emptyResult(@NotNull TranslationContext context) {
        return null;
    }

    @Override
    public Void visitClassOrObject(@NotNull KtClassOrObject declaration, TranslationContext context) {
        staticResult.addAll(ClassTranslator.translate(declaration, context).getProperties());

        if (declaration instanceof KtObjectDeclaration) {
            KtObjectDeclaration objectDeclaration = (KtObjectDeclaration) declaration;
            if (objectDeclaration.isCompanion()) {
                DeclarationDescriptor descriptor = BindingUtils.getDescriptorForElement(context.bindingContext(), declaration);
                addInitializerStatement(context.getQualifiedReference(descriptor).makeStmt());
            }
        }

        return null;
    }

    @Override
    public Void visitEnumEntry(@NotNull KtEnumEntry enumEntry, TranslationContext data) {
        ClassDescriptor descriptor = getClassDescriptor(data.bindingContext(), enumEntry);
        List<KotlinType> supertypes = getSupertypesWithoutFakes(descriptor);
        if (enumEntry.getBody() != null || supertypes.size() > 1) {
            enumEntryList.addAll(ClassTranslator.translate(enumEntry, data).getProperties());
        } else {
            assert supertypes.size() == 1 : "Simple Enum entry must have one supertype";
            JsExpression jsEnumEntryCreation = new ClassInitializerTranslator(enumEntry, data)
                    .generateEnumEntryInstanceCreation(supertypes.get(0));
            jsEnumEntryCreation = TranslationUtils.simpleReturnFunction(data.scope(), jsEnumEntryCreation);
            enumEntryList.add(new JsPropertyInitializer(data.getNameForDescriptor(descriptor).makeRef(), jsEnumEntryCreation));
        }
        return null;
    }

    @Override
    public Void visitNamedFunction(@NotNull KtNamedFunction expression, TranslationContext context) {
        FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
        if (descriptor.getModality() == Modality.ABSTRACT) {
            return null;
        }

        context = context.newDeclaration(descriptor, context.getDefinitionPlace());
        JsPropertyInitializer methodAsPropertyInitializer = Translation.functionTranslator(expression, context).translateAsMethod();
        result.add(methodAsPropertyInitializer);
        return null;
    }

    @Override
    public Void visitProperty(@NotNull KtProperty expression, TranslationContext context) {
        PropertyDescriptor propertyDescriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), expression);
        context = context.newDeclaration(propertyDescriptor, context.getDefinitionPlace());
        PropertyTranslatorKt.translateAccessors(propertyDescriptor, expression, result, context);
        return null;
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull KtAnonymousInitializer expression, TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return null;
    }

    @Override
    public Void visitSecondaryConstructor(@NotNull KtSecondaryConstructor constructor, TranslationContext data) {
        return null;
    }

    private void addInitializerStatement(@NotNull JsStatement statement) {
        if (initializerStatements == null) {
            initializerStatements = new ArrayList<JsStatement>();
            JsFunction initializerFunction = new JsFunction(scope, new JsBlock(initializerStatements), "class initializer");
            staticResult.add(new JsPropertyInitializer(new JsNameRef("object_initializer$"), initializerFunction));
        }
        initializerStatements.add(statement);
    }

    @Override
    public Void visitTypeAlias(@NotNull KtTypeAlias typeAlias, TranslationContext data) {
        // Resolved by front-end, not used by backend
        return null;
    }
}
