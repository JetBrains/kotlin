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

import com.google.dart.compiler.backend.js.ast.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetPropertyAccessor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.general.Translation;
import org.jetbrains.k2js.translate.utils.BindingUtils;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.JsAstUtils.*;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.backingFieldReference;

/**
 * @author Pavel Talanov
 *         <p/>
 *         Translates single property /w accessors.
 */
public final class PropertyTranslator extends AbstractTranslator {

    @NotNull
    private final PropertyDescriptor property;
    @NotNull
    private final List<JsPropertyInitializer> accessors = new ArrayList<JsPropertyInitializer>();
    @Nullable
    private final JetProperty declaration;

    static public List<JsPropertyInitializer> translateAccessors(@NotNull PropertyDescriptor descriptor,
                                                                 @NotNull TranslationContext context) {
        PropertyTranslator propertyTranslator = new PropertyTranslator(descriptor, context);
        return propertyTranslator.translate();
    }

    private PropertyTranslator(@NotNull PropertyDescriptor property, @NotNull TranslationContext context) {
        super(context);
        this.property = property;
        this.declaration = BindingUtils.getPropertyForDescriptor(bindingContext(), property);
    }

    @NotNull
    private List<JsPropertyInitializer> translate() {
        addGetter();
        if (property.isVar()) {
            addSetter();
        }
        return accessors;
    }

    private void addGetter() {
        if (hasCustomGetter()) {
            accessors.add(translateCustomAccessor(getCustomGetterDeclaration()));
        }
        else {
            accessors.add(generateDefaultGetter());
        }
    }

    private void addSetter() {
        if (hasCustomSetter()) {
            accessors.add(translateCustomAccessor(getCustomSetterDeclaration()));
        }
        else {
            accessors.add(generateDefaultSetter());
        }
    }

    private boolean hasCustomGetter() {
        return ((declaration != null) && (declaration.getGetter() != null));
    }

    private boolean hasCustomSetter() {
        return ((declaration != null) && (declaration.getSetter() != null));
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
        PropertyGetterDescriptor getterDescriptor = property.getGetter();
        assert getterDescriptor != null : "Getter descriptor should not be null";
        return newNamedMethod(context().getNameForDescriptor(getterDescriptor),
                              generateDefaultGetterFunction(getterDescriptor));
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(@NotNull PropertyGetterDescriptor descriptor) {
        JsReturn returnExpression = new JsReturn(backingFieldReference(context(), property));
        JsFunction getterFunction = context().getFunctionObject(descriptor);
        getterFunction.getBody().getStatements().add(returnExpression);
        return getterFunction;
    }

    @NotNull
    private JsPropertyInitializer generateDefaultSetter() {
        PropertySetterDescriptor setterDescriptor = property.getSetter();
        assert setterDescriptor != null : "Setter descriptor should not be null";
        return newNamedMethod(context().getNameForDescriptor(setterDescriptor),
                              generateDefaultSetterFunction(setterDescriptor));
    }

    @NotNull
    private JsFunction generateDefaultSetterFunction(@NotNull PropertySetterDescriptor propertySetterDescriptor) {
        JsFunction result = context().getFunctionObject(propertySetterDescriptor);
        JsParameter defaultParameter =
                new JsParameter(propertyAccessContext(propertySetterDescriptor).jsScope().declareTemporary());
        JsStatement assignment = assignmentToBackingField(context(), property, defaultParameter.getName().makeRef()).makeStmt();
        setParameters(result, defaultParameter);
        result.getBody().getStatements().add(assignment);
        return result;
    }

    @NotNull
    private TranslationContext propertyAccessContext(@NotNull PropertySetterDescriptor propertySetterDescriptor) {
        return context().newDeclaration(propertySetterDescriptor);
    }

    @NotNull
    private JsPropertyInitializer translateCustomAccessor(@NotNull JetPropertyAccessor expression) {
        return Translation.functionTranslator(expression, context()).translateAsMethod();
    }
}
