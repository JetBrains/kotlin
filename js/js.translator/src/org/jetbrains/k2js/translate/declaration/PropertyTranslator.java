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
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.expression.FunctionTranslator;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.JsDescriptorUtils;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import java.util.List;

import static org.jetbrains.k2js.translate.context.Namer.getDelegateNameRef;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

/**
 * Translates single property /w accessors.
 */
public final class PropertyTranslator extends AbstractTranslator {
    @NotNull
    private final PropertyDescriptor descriptor;
    @Nullable
    private final JetProperty declaration;

    public static void translateAccessors(@NotNull PropertyDescriptor descriptor, @NotNull List<JsPropertyInitializer> result, @NotNull TranslationContext context) {
        translateAccessors(descriptor, null, result, context);
    }

    public static void translateAccessors(@NotNull PropertyDescriptor descriptor,
            @Nullable JetProperty declaration,
            @NotNull List<JsPropertyInitializer> result,
            @NotNull TranslationContext context) {
        if (!JsDescriptorUtils.isSimpleFinalProperty(descriptor)) {
            new PropertyTranslator(descriptor, declaration, context).translate(result);
        }
    }

    private PropertyTranslator(@NotNull PropertyDescriptor descriptor, @Nullable JetProperty declaration, @NotNull TranslationContext context) {
        super(context);

        this.descriptor = descriptor;
        this.declaration = declaration;
    }

    private void translate(@NotNull List<JsPropertyInitializer> result) {
        List<JsPropertyInitializer> to;
        if (!JsDescriptorUtils.isExtension(descriptor)) {
            to = new SmartList<JsPropertyInitializer>();
            result.add(new JsPropertyInitializer(context().getNameForDescriptor(descriptor).makeRef(), new JsObjectLiteral(to, true)));
        }
        else {
            to = result;
        }

        to.add(generateGetter());
        if (descriptor.isVar()) {
            to.add(generateSetter());
        }
    }

    private JsPropertyInitializer generateGetter() {
        if (hasCustomGetter()) {
            return translateCustomAccessor(getCustomGetterDeclaration());
        }
        else {
            return generateDefaultGetter();
        }
    }

    private JsPropertyInitializer generateSetter() {
        if (hasCustomSetter()) {
            return translateCustomAccessor(getCustomSetterDeclaration());
        }
        else {
            return generateDefaultSetter();
        }
    }

    private boolean hasCustomGetter() {
        return declaration != null && declaration.getGetter() != null && getCustomGetterDeclaration().hasBody();
    }

    private boolean hasCustomSetter() {
        return declaration != null && declaration.getSetter() != null && getCustomSetterDeclaration().hasBody();
    }

    @NotNull
    private JetPropertyAccessor getCustomGetterDeclaration() {
        assert declaration != null;
        JetPropertyAccessor getterDeclaration = declaration.getGetter();
        assert getterDeclaration != null;
        return getterDeclaration;
    }

    @NotNull
    private JetPropertyAccessor getCustomSetterDeclaration() {
        assert declaration != null;
        JetPropertyAccessor setter = declaration.getSetter();
        assert setter != null;
        return setter;
    }

    @NotNull
    private JsPropertyInitializer generateDefaultGetter() {
        PropertyGetterDescriptor getterDescriptor = descriptor.getGetter();
        assert getterDescriptor != null : "Getter descriptor should not be null";
        return generateDefaultAccessor(getterDescriptor, generateDefaultGetterFunction(getterDescriptor));
    }

    private String getPropertyName() {
        return descriptor.getName().asString();
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(@NotNull PropertyGetterDescriptor getterDescriptor) {
        JsExpression value;
        ResolvedCall<FunctionDescriptor> delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, getterDescriptor);
        if (delegatedCall != null) {
            value = CallTranslator.INSTANCE$.translate(context(), delegatedCall, getDelegateNameRef(getPropertyName()));
        } else {
            value = backingFieldReference(context(), this.descriptor);
        }
        return simpleReturnFunction(context().getScopeForDescriptor(getterDescriptor.getContainingDeclaration()), value);
    }

    @NotNull
    private JsPropertyInitializer generateDefaultSetter() {
        PropertySetterDescriptor setterDescriptor = descriptor.getSetter();
        assert setterDescriptor != null : "Setter descriptor should not be null";
        return generateDefaultAccessor(setterDescriptor, generateDefaultSetterFunction(setterDescriptor));
    }

    @NotNull
    private JsFunction generateDefaultSetterFunction(@NotNull PropertySetterDescriptor setterDescriptor) {
        JsFunction fun = new JsFunction(context().getScopeForDescriptor(setterDescriptor.getContainingDeclaration()));

        assert setterDescriptor.getValueParameters().size() == 1 : "Setter must have 1 parameter";
        ValueParameterDescriptor valueParameterDescriptor = setterDescriptor.getValueParameters().get(0);
        JsParameter defaultParameter = new JsParameter(fun.getScope().declareTemporary());
        JsNameRef defaultParameterRef = defaultParameter.getName().makeRef();

        fun.getParameters().add(defaultParameter);
        TranslationContext contextWithAliased = context().innerContextWithAliased(valueParameterDescriptor, defaultParameterRef);

        JsExpression setExpression;
        ResolvedCall<FunctionDescriptor> delegatedCall = bindingContext().get(BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL,
                                                                              setterDescriptor);
        if (delegatedCall != null) {
            setExpression = CallTranslator.INSTANCE$.translate(contextWithAliased, delegatedCall, getDelegateNameRef(getPropertyName()));
        } else {
            setExpression = assignmentToBackingField(contextWithAliased, descriptor, defaultParameterRef);
        }
        fun.setBody(new JsBlock(setExpression.makeStmt()));
        return fun;
    }

    @NotNull
    private JsPropertyInitializer generateDefaultAccessor(@NotNull PropertyAccessorDescriptor accessorDescriptor,
            @NotNull JsFunction function) {
        return TranslationUtils.translateFunctionAsEcma5PropertyDescriptor(function, accessorDescriptor, context());
    }

    @NotNull
    private JsPropertyInitializer translateCustomAccessor(@NotNull JetPropertyAccessor expression) {
        FunctionTranslator translator = Translation.functionTranslator(expression, context());
        return translator.translateAsEcma5PropertyDescriptor();
    }
}
