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
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TemporaryVariable;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.Collections;
import java.util.List;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getDescriptorForReferenceExpression;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.assignmentToBackingField;
import static org.jetbrains.k2js.translate.utils.TranslationUtils.backingFieldReference;

public final class BackingFieldAccessTranslator extends AbstractTranslator implements CachedAccessTranslator {

    @NotNull
    private final PropertyDescriptor descriptor;

    /*package*/
    public static BackingFieldAccessTranslator newInstance(@NotNull JetSimpleNameExpression expression,
                                                    @NotNull TranslationContext context) {
        DeclarationDescriptor referencedProperty = getDescriptorForReferenceExpression(context.bindingContext(), expression);
        assert referencedProperty instanceof PropertyDescriptor;
        return new BackingFieldAccessTranslator((PropertyDescriptor) referencedProperty, context);
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
    public CachedAccessTranslator getCached() {
        return this;
    }

    @NotNull
    @Override
    public List<TemporaryVariable> declaredTemporaries() {
        return Collections.emptyList();
    }
}
