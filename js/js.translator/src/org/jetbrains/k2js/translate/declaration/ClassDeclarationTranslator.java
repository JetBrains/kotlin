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

package org.jetbrains.k2js.translate.declaration;

import com.google.dart.compiler.backend.js.ast.JsExpression;
import com.google.dart.compiler.backend.js.ast.JsNameRef;
import com.google.dart.compiler.backend.js.ast.JsPropertyInitializer;
import gnu.trove.THashMap;
import gnu.trove.TObjectObjectProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import static org.jetbrains.k2js.translate.utils.BindingUtils.getClassDescriptor;

/**
 * Generates a big block where are all the classes(objects representing them) are created.
 */
public final class ClassDeclarationTranslator extends AbstractTranslator {

    private final THashMap<ClassDescriptor, JsNameRef> openClassDescriptorToQualifiedLabel = new THashMap<ClassDescriptor, JsNameRef>();

    private final ClassAliasingMap classDescriptorToQualifiedLabel = new ClassAliasingMap() {
        @NotNull
        @Override
        public JsNameRef get(ClassDescriptor descriptor, ClassDescriptor referencedDescriptor) {
            JsNameRef ref = openClassDescriptorToQualifiedLabel.get(descriptor);
            if (ref != null) {
                return ref;
            }

            // will be resolved later
            ref = new JsNameRef("<unresolved class>");
            openClassDescriptorToQualifiedLabel.put(descriptor, ref);
            return ref;
        }
    };

    public ClassDeclarationTranslator(@NotNull TranslationContext context) {
        super(context);
    }

    public void fixUnresolvedClassReferences() {
        openClassDescriptorToQualifiedLabel.forEachEntry(new TObjectObjectProcedure<ClassDescriptor, JsNameRef>() {
            @Override
            public boolean execute(ClassDescriptor descriptor, JsNameRef ref) {
                if (ref.getName() == null) {
                    // from library
                    ref.resolve(context().getNameForDescriptor(descriptor));
                    ref.setQualifier(context().getQualifierForDescriptor(descriptor));
                }
                return true;
            }
        });
    }

    @Nullable
    public JsPropertyInitializer translate(@NotNull JetClassOrObject declaration, TranslationContext context) {
        ClassDescriptor descriptor = getClassDescriptor(context().bindingContext(), declaration);
        JsExpression value = new ClassTranslator(declaration, classDescriptorToQualifiedLabel, context).translate();

        return new JsPropertyInitializer(context.getNameForDescriptor(descriptor).makeRef(), value);
    }
}
