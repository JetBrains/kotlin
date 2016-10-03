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

package org.jetbrains.kotlin.js.translate.declaration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;

final class PackageTranslator extends AbstractTranslator {
    static PackageTranslator create(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        TranslationContext newContext = context.newDeclaration(descriptor);
        FileDeclarationVisitor visitor = new FileDeclarationVisitor(newContext);
        return new PackageTranslator(newContext, visitor);
    }

    private final FileDeclarationVisitor visitor;

    private PackageTranslator(
            @NotNull TranslationContext context,
            @NotNull FileDeclarationVisitor visitor
    ) {
        super(context);
        this.visitor = visitor;
    }
    
    public void translate(KtFile file) {
        for (KtDeclaration declaration : file.getDeclarations()) {
            if (!AnnotationsUtils.isPredefinedObject(BindingUtils.getDescriptorForElement(bindingContext(), declaration))) {
                declaration.accept(visitor, context());
            }
        }
    }
}
