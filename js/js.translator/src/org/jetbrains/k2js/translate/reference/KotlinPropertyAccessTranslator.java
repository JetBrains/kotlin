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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyGetterDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertySetterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.PropertyGetterDescriptorImpl;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.k2js.translate.context.TranslationContext;

/**
 * For properies /w accessors.
 */
public final class KotlinPropertyAccessTranslator extends PropertyAccessTranslator {

    @Nullable
    private final JsExpression receiver;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;
    @NotNull
    private final ResolvedCall<?> resolvedCall;

    //TODO: too many params in constructor
    /*package*/ KotlinPropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                               @Nullable JsExpression receiver,
                                               @NotNull ResolvedCall<?> resolvedCall,
                                               @NotNull TranslationContext context) {
        super(context);
        this.receiver = receiver;
        this.propertyDescriptor = descriptor.getOriginal();
        this.resolvedCall = resolvedCall;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return translateAsGet(receiver);
    }

    @Override
    @NotNull
    public JsExpression translateAsGet(@Nullable JsExpression receiver) {
        PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
        if (getter == null) {
            //TODO: Temporary hack!!! Rewrite codegen!
            //Now for consistency we don't create default getter for object declaration property descriptor
            PropertyGetterDescriptorImpl getterImpl = DescriptorResolver.createDefaultGetter(propertyDescriptor);
            getterImpl.initialize(propertyDescriptor.getType());
            ((PropertyDescriptorImpl)propertyDescriptor).initialize(getterImpl, null);
            getter = getterImpl;
        }
        assert getter != null : "Getter for kotlin properties should bot be null.";
        return callBuilderForAccessor(receiver)
                .descriptor(getter)
                .translate();
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        return translateAsSet(receiver, toSetTo);
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@Nullable JsExpression receiver, @NotNull JsExpression toSetTo) {
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        assert setter != null : "Setter for kotlin properties should not be null.";
        return callBuilderForAccessor(receiver)
                .args(toSetTo)
                .descriptor(setter)
                .translate();
    }

    @NotNull
    private CallBuilder callBuilderForAccessor(@Nullable JsExpression qualifier) {
        return CallBuilder.build(context())
                .receiver(qualifier)
                .resolvedCall(resolvedCall)
                .type(getCallType());
    }


    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return new CachedPropertyAccessTranslator(receiver, this, context());
    }
}
