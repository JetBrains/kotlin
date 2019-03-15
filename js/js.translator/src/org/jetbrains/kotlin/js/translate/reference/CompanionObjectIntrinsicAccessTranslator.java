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

package org.jetbrains.kotlin.js.translate.reference;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.js.backend.ast.JsExpression;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.intrinsic.objects.ObjectIntrinsic;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.psi.KtSimpleNameExpression;

import static org.jetbrains.kotlin.js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

public class CompanionObjectIntrinsicAccessTranslator extends AbstractTranslator implements AccessTranslator {
    @NotNull
    /*package*/ static CompanionObjectIntrinsicAccessTranslator newInstance(
            @NotNull KtSimpleNameExpression expression,
            @NotNull TranslationContext context
    ) {
        DeclarationDescriptor referenceDescriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert referenceDescriptor != null : "JetSimpleName expression must reference a descriptor " + expression.getText();
        return new CompanionObjectIntrinsicAccessTranslator(referenceDescriptor, context);
    }

    /*package*/ static boolean isCompanionObjectReference(
            @NotNull KtReferenceExpression expression,
            @NotNull TranslationContext context
    ) {
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return descriptor instanceof ClassDescriptor && context.intrinsics().getObjectIntrinsic((ClassDescriptor) descriptor) != null;
    }

    @NotNull
    private final JsExpression referenceToCompanionObject;

    private CompanionObjectIntrinsicAccessTranslator(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.referenceToCompanionObject = generateReferenceToCompanionObject(descriptor, context);
    }

    @NotNull
    private static JsExpression generateReferenceToCompanionObject(
            @NotNull DeclarationDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        ObjectIntrinsic objectIntrinsic = context.intrinsics().getObjectIntrinsic((ClassDescriptor) descriptor);
        return objectIntrinsic.apply(context);
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        return referenceToCompanionObject;
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        throw new IllegalStateException("companion object can't be set");
    }

    @NotNull
    @Override
    public AccessTranslator getCached() {
        return this;
    }
}
