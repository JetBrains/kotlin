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
import org.jetbrains.kotlin.js.facade.exceptions.TranslationRuntimeException;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.Collection;

public final class PackageDeclarationTranslator extends AbstractTranslator {
    private final Iterable<KtFile> files;

    public static void translateFiles(@NotNull Collection<KtFile> files, @NotNull TranslationContext context) {
        new PackageDeclarationTranslator(files, context).translate();
    }

    private PackageDeclarationTranslator(@NotNull Iterable<KtFile> files, @NotNull TranslationContext context) {
        super(context);

        this.files = files;
    }

    private void translate() {
        for (KtFile file : files) {
            FileDeclarationVisitor fileVisitor = new FileDeclarationVisitor(context());

            try {
                for (KtDeclaration declaration : file.getDeclarations()) {
                    if (!AnnotationsUtils.isPredefinedObject(BindingUtils.getDescriptorForElement(bindingContext(), declaration))) {
                        declaration.accept(fileVisitor, context());
                    }
                }
            }
            catch (TranslationRuntimeException e) {
                throw e;
            }
            catch (RuntimeException e) {
                throw new TranslationRuntimeException(file, e);
            }
            catch (AssertionError e) {
                throw new TranslationRuntimeException(file, e);
            }
        }
    }
}
