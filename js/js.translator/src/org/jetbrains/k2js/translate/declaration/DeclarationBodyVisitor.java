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

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsFunction;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import com.intellij.util.SmartList;
import jet.runtime.typeinfo.KotlinSignature;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.general.TranslatorVisitor;
import org.jetbrains.k2js.translate.initializer.ClassInitializerTranslator;
import org.jetbrains.k2js.translate.utils.BindingUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.k2js.translate.initializer.InitializerUtils.createClassObjectInitializer;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getFunctionDescriptor;

public class DeclarationBodyVisitor extends TranslatorVisitor<Void> {
    @KotlinSignature("val result: MutableList<JsPropertyInitializer>")
    protected final List<JsPropertyInitializer> result;
    protected final List<JsPropertyInitializer> staticResult;
    protected final List<JsPropertyInitializer> enumEntryList = new SmartList<JsPropertyInitializer>();

    public DeclarationBodyVisitor() {
        this(new SmartList<JsPropertyInitializer>(), new SmartList<JsPropertyInitializer>());
    }

    public DeclarationBodyVisitor(@NotNull List<JsPropertyInitializer> result, @NotNull List<JsPropertyInitializer> staticResult) {
        this.result = result;
        this.staticResult = staticResult;
    }

    @NotNull
    public List<JsPropertyInitializer> getResult() {
        return result;
    }

    public List<JsPropertyInitializer> getEnumEntryList() {
        return enumEntryList;
    }

    @Override
    public Void visitClass(@NotNull JetClass expression, TranslationContext context) {
        return null;
    }

    @Override
    public Void visitEnumEntry(@NotNull JetEnumEntry enumEntry, TranslationContext data) {
        JsExpression jsEnumEntryCreation;
        ClassDescriptor descriptor = getClassDescriptor(data.bindingContext(), enumEntry);
        Collection<JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
        if (enumEntry.getBody() != null || supertypes.size() > 1) {
            jsEnumEntryCreation = ClassTranslator.generateClassCreation(enumEntry, descriptor, data);
        } else {
            assert supertypes.size() == 1 : "Simple Enum entry must have one supertype";
            jsEnumEntryCreation = new ClassInitializerTranslator(enumEntry, data).generateEnumEntryInstanceCreation(supertypes.iterator().next());
        }
        enumEntryList.add(new JsPropertyInitializer(data.getNameForDescriptor(descriptor).makeRef(), jsEnumEntryCreation));
        return null;
    }

    @Override
    public Void visitClassObject(@NotNull JetClassObject classObject, TranslationContext context) {
        JetObjectDeclaration declaration = classObject.getObjectDeclaration();
        assert declaration != null : "Declaration for class object must be not null";
        ClassDescriptor descriptor = getClassDescriptor(context.bindingContext(), declaration);
        JsExpression value = ClassTranslator.generateClassCreation(declaration, descriptor, context);

        JsFunction fun = TranslationUtils.simpleReturnFunction(context.getScopeForDescriptor(descriptor), value);
        staticResult.add(createClassObjectInitializer(fun, context));
        return null;
    }

    @Override
    public Void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration, TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return null;
    }

    @Override
    public Void visitNamedFunction(@NotNull JetNamedFunction expression, TranslationContext context) {
        FunctionDescriptor descriptor = getFunctionDescriptor(context.bindingContext(), expression);
        if (descriptor.getModality() == Modality.ABSTRACT) {
            return null;
        }

        JsPropertyInitializer methodAsPropertyInitializer = Translation.functionTranslator(expression, context).translateAsMethod();
        result.add(methodAsPropertyInitializer);
        return null;
    }

    @Override
    public Void visitProperty(@NotNull JetProperty expression, TranslationContext context) {
        PropertyDescriptor propertyDescriptor = BindingUtils.getPropertyDescriptor(context.bindingContext(), expression);
        PropertyTranslator.translateAccessors(propertyDescriptor, expression, result, context);
        return null;
    }

    @Override
    public Void visitAnonymousInitializer(@NotNull JetClassInitializer expression, TranslationContext context) {
        // parsed it in initializer visitor => no additional actions are needed
        return null;
    }
}
