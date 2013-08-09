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
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.AnnotationsUtils;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.reference.ReferenceTranslator.translateAsFQReference;
import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;

public class ClassObjectAccessTranslator extends AbstractTranslator implements CachedAccessTranslator {
    @NotNull
    /*package*/ static ClassObjectAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        DeclarationDescriptor referenceDescriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert referenceDescriptor != null : "JetSimpleName expression must reference a descriptor " + expression.getText();
        return new ClassObjectAccessTranslator(referenceDescriptor, context);
    }

    /*package*/ static boolean isClassObjectReference(@NotNull JetReferenceExpression expression,
            @NotNull TranslationContext context) {
        DeclarationDescriptor descriptor = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        return descriptor instanceof ClassDescriptor && !AnnotationsUtils.isNativeObject(descriptor);
    }

    @NotNull
    private final JsExpression referenceToClassObject;

    private ClassObjectAccessTranslator(@NotNull DeclarationDescriptor descriptor, @NotNull TranslationContext context) {
        super(context);
        this.referenceToClassObject = Namer.getClassObjectAccessor(translateAsFQReference(descriptor, context()));
    }

    @Override
    @NotNull
    public JsExpression translateAsGet() {
        return referenceToClassObject;
    }

    @Override
    @NotNull
    public JsExpression translateAsSet(@NotNull JsExpression toSetTo) {
        throw new IllegalStateException("Class object can't set");
    }

    @NotNull
    @Override
    public CachedAccessTranslator getCached() {
        return this;
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {
        return Collections.emptyList();
    }
}
