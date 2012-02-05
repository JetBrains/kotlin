package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.util.AstUtil;
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
import java.util.Arrays;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.TranslationUtils.*;

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
        this.declaration = BindingUtils.getPropertyForDescriptor(context().bindingContext(), property);
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
        } else {
            accessors.add(generateDefaultGetter());
        }
    }

    private void addSetter() {
        if (hasCustomSetter()) {
            accessors.add(translateCustomAccessor(getCustomSetterDeclaration()));
        } else {
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
        return AstUtil.newNamedMethod(context().getNameForDescriptor(getterDescriptor),
                generateDefaultGetterFunction(getterDescriptor));
    }

    @NotNull
    private JsFunction generateDefaultGetterFunction(@NotNull PropertyGetterDescriptor descriptor) {
        JsReturn returnExpression = new JsReturn(backingFieldReference(context(), property));
        JsFunction getterFunction = functionWithScope(context().getScopeForDescriptor(descriptor));
        getterFunction.setBody(AstUtil.convertToBlock(returnExpression));
        return getterFunction;
    }

    @NotNull
    private JsPropertyInitializer generateDefaultSetter() {
        PropertySetterDescriptor setterDescriptor = property.getSetter();
        assert setterDescriptor != null : "Setter descriptor should not be null";
        return AstUtil.newNamedMethod(context().getNameForDescriptor(setterDescriptor),
                generateDefaultSetterFunction(setterDescriptor));
    }

    @NotNull
    private JsFunction generateDefaultSetterFunction(@NotNull PropertySetterDescriptor propertySetterDescriptor) {
        JsFunction result = functionWithScope(context().getScopeForDescriptor(propertySetterDescriptor));
        JsParameter defaultParameter =
                new JsParameter(propertyAccessContext(propertySetterDescriptor).jsScope().declareTemporary());
        JsStatement assignment = assignmentToBackingField(context(), property, defaultParameter.getName().makeRef());
        result.setParameters(Arrays.asList(defaultParameter));
        result.setBody(AstUtil.convertToBlock(assignment));
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
