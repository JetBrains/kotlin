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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.utils.TranslationUtils;

import static org.jetbrains.k2js.translate.utils.DescriptorUtils.getExpectedThisDescriptor;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.assignment;
import static org.jetbrains.k2js.translate.utils.JsAstUtils.qualified;

/**
 * @author Pavel Talanov
 *         <p/>
 *         For native apis that use .property notation for access.
 */
//TODO: test this class
public final class NativePropertyAccessTranslator extends PropertyAccessTranslator {

    @Nullable
    private final JsExpression receiver;
    @NotNull
    private final PropertyDescriptor propertyDescriptor;

    /*package*/
    NativePropertyAccessTranslator(@NotNull PropertyDescriptor descriptor,
                                   @Nullable JsExpression receiver,
                                   @NotNull TranslationContext context) {
        super(context);
        this.receiver = receiver;
        this.propertyDescriptor = descriptor.getOriginal();
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        return translateAsGet(getReceiver());
    }

    @NotNull
    @Override
    protected JsExpression translateAsGet(@Nullable JsExpression receiver) {
        JsName nativePropertyName = context().getNameForDescriptor(propertyDescriptor);
        if (receiver != null) {
            return qualified(nativePropertyName, receiver);
        }
        else {
            return nativePropertyName.makeRef();
        }
    }

    @Override
    @NotNull
    protected JsExpression translateAsSet(@Nullable JsExpression receiver, @NotNull JsExpression setTo) {
        assert receiver != null;
        return assignment(translateAsGet(getReceiver()), setTo);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return translateAsSet(getReceiver(), setTo);
    }

    @Nullable
    public JsExpression getReceiver() {
        if (receiver != null) {
            return receiver;
        }
        assert !propertyDescriptor.getReceiverParameter().exists() : "Cant have native extension properties.";
        DeclarationDescriptor expectedThisDescriptor = getExpectedThisDescriptor(propertyDescriptor);
        if (expectedThisDescriptor == null) {
            return null;
        }
        return TranslationUtils.getThisObject(context(), expectedThisDescriptor);
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return new CachedPropertyAccessTranslator(getReceiver(), this, context());
    }
}
