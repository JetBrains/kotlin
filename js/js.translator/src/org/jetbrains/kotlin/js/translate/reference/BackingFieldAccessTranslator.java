/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptorKt;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.assignmentToBackingField;
import static org.jetbrains.kotlin.js.translate.utils.TranslationUtils.backingFieldReference;

public final class BackingFieldAccessTranslator extends AbstractTranslator implements AccessTranslator {

    @NotNull
    private final PropertyDescriptor descriptor;

    /*package*/
    public static BackingFieldAccessTranslator newInstance(@NotNull KtSimpleNameExpression expression,
                                                    @NotNull TranslationContext context) {
        PropertyDescriptor referencedProperty = SyntheticFieldDescriptorKt.getReferencedProperty(
                getDescriptorForReferenceExpression(context.bindingContext(), expression)
        );
        assert referencedProperty != null;
        return new BackingFieldAccessTranslator(referencedProperty, context);
    }

    private BackingFieldAccessTranslator(@NotNull PropertyDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.descriptor = descriptor;
    }

    @NotNull
    @Override
    public JsExpression translateAsGet() {
        return backingFieldReference(context(), descriptor);
    }

    @NotNull
    @Override
    public JsExpression translateAsSet(@NotNull JsExpression setTo) {
        return assignmentToBackingField(context(), descriptor, setTo);
    }

    @NotNull
    @Override
    public AccessTranslator getCached() {
        return this;
    }
}
