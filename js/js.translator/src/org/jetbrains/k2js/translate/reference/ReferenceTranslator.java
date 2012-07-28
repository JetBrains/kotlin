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
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.k2js.translate.context.TranslationContext;

import static org.jetbrains.k2js.translate.utils.PsiUtils.isBackingFieldReference;

/**
 * @author Pavel Talanov
 */
public final class ReferenceTranslator {

    private ReferenceTranslator() {
    }

    @NotNull
    public static JsExpression translateSimpleName(@NotNull JetSimpleNameExpression expression,
            @NotNull TranslationContext context) {
        return getAccessTranslator(expression, context).translateAsGet();
    }

    @NotNull
    public static JsExpression translateAsFQReference(@NotNull DeclarationDescriptor referencedDescriptor,
            @NotNull TranslationContext context) {
        JsExpression qualifier = context.getQualifierForDescriptor(referencedDescriptor);
        if (qualifier == null) {
            return translateAsLocalNameReference(referencedDescriptor, context);
        }
        JsName referencedName = context.getNameForDescriptor(referencedDescriptor);
        return new JsNameRef(referencedName, qualifier);
    }

    @NotNull
    public static JsExpression translateAsLocalNameReference(@NotNull DeclarationDescriptor referencedDescriptor,
            @NotNull TranslationContext context) {
        //Preconditions.checkNotNull(referencedDescriptor, "No referencedDescriptor available");
        return context.getNameForDescriptor(referencedDescriptor).makeRef();
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetSimpleNameExpression referenceExpression,
            @NotNull TranslationContext context) {
        return getAccessTranslator(referenceExpression, null, context);
    }

    @NotNull
    public static AccessTranslator getAccessTranslator(@NotNull JetSimpleNameExpression referenceExpression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context) {
        if (isBackingFieldReference(referenceExpression)) {
            return BackingFieldAccessTranslator.newInstance(referenceExpression, context);
        }
        if (PropertyAccessTranslator.canBePropertyAccess(referenceExpression, context)) {
            return PropertyAccessTranslator.newInstance(referenceExpression, receiver, CallType.NORMAL, context);
        }
        return ReferenceAccessTranslator.newInstance(referenceExpression, context);
    }
}
