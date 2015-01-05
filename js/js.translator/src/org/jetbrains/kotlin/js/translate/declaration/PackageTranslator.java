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

import com.google.dart.compiler.backend.js.ast.*;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.js.translate.utils.AnnotationsUtils;
import org.jetbrains.kotlin.js.translate.context.DefinitionPlace;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;
import org.jetbrains.kotlin.js.translate.utils.BindingUtils;

import java.util.List;
import java.util.Map;

final class PackageTranslator extends AbstractTranslator {
    static PackageTranslator create(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull TranslationContext context
    ) {
        JsScope scope = context.getScopeForDescriptor(descriptor);
        JsNameRef reference = context.getQualifiedReference(descriptor);
        SmartList<JsPropertyInitializer> properties = new SmartList<JsPropertyInitializer>();

        DefinitionPlace definitionPlace = new DefinitionPlace((JsObjectScope) scope, reference, properties);

        TranslationContext newContext = context.newDeclaration(descriptor, definitionPlace);
        FileDeclarationVisitor visitor = new FileDeclarationVisitor(newContext, definitionPlace.getProperties());
        return new PackageTranslator(descriptor, newContext, visitor);
    }

    @NotNull
    private final PackageFragmentDescriptor descriptor;

    private final FileDeclarationVisitor visitor;

    private PackageTranslator(
            @NotNull PackageFragmentDescriptor descriptor,
            @NotNull TranslationContext context,
            @NotNull FileDeclarationVisitor visitor
    ) {
        super(context);
        this.descriptor = descriptor;
        this.visitor = visitor;
    }
    
    public void translate(JetFile file) {
        for (JetDeclaration declaration : file.getDeclarations()) {
            if (!AnnotationsUtils.isPredefinedObject(BindingUtils.getDescriptorForElement(bindingContext(), declaration))) {
                declaration.accept(visitor, context());
            }
        }
    }

    private void createDefinitionPlace(
            @Nullable JsExpression initializer,
            Map<FqName, DefineInvocation> packageFqNameToDefineInvocation
    ) {
        FqName fqName = descriptor.getFqName();
        DefineInvocation place = DefineInvocation.create(fqName, initializer, new JsObjectLiteral(visitor.getResult(), true), context());
        packageFqNameToDefineInvocation.put(fqName, place);
        addToParent(fqName.parent(), getEntry(fqName, place), packageFqNameToDefineInvocation);
    }

    public void add(@NotNull Map<FqName, DefineInvocation> packageFqNameToDefineInvocation) {
        JsExpression initializer = visitor.computeInitializer();

        DefineInvocation defineInvocation = packageFqNameToDefineInvocation.get(descriptor.getFqName());
        if (defineInvocation == null) {
            if (initializer != null || !visitor.getResult().isEmpty()) {
                createDefinitionPlace(initializer, packageFqNameToDefineInvocation);
            }
        }
        else {
            if (initializer != null) {
                assert defineInvocation.getInitializer() == JsLiteral.NULL;
                defineInvocation.setInitializer(initializer);
            }

            List<JsPropertyInitializer> listFromPlace = defineInvocation.getMembers();
            // if equals, so, inner functions was added
            if (listFromPlace != visitor.getResult()) {
                listFromPlace.addAll(visitor.getResult());
            }
        }
    }

    private JsPropertyInitializer getEntry(@NotNull FqName fqName, DefineInvocation defineInvocation) {
        return new JsPropertyInitializer(context().getNameForPackage(fqName).makeRef(),
                                         new JsInvocation(context().namer().packageDefinitionMethodReference(), defineInvocation.asList()));
    }

    private static boolean addEntryIfParentExists(
            FqName parentFqName,
            JsPropertyInitializer entry,
            Map<FqName, DefineInvocation> packageFqNameToDeclarationPlace
    ) {
        DefineInvocation parentDefineInvocation = packageFqNameToDeclarationPlace.get(parentFqName);
        if (parentDefineInvocation != null) {
            parentDefineInvocation.getMembers().add(entry);
            return true;
        }
        return false;
    }

    private void addToParent(@NotNull FqName parentFqName,
            JsPropertyInitializer entry,
            Map<FqName, DefineInvocation> packageFqNameToDefineInvocation) {
        while (!addEntryIfParentExists(parentFqName, entry, packageFqNameToDefineInvocation)) {
            JsObjectLiteral members = new JsObjectLiteral(new SmartList<JsPropertyInitializer>(entry), true);
            DefineInvocation defineInvocation = DefineInvocation.create(parentFqName, null, members, context());
            entry = getEntry(parentFqName, defineInvocation);

            packageFqNameToDefineInvocation.put(parentFqName, defineInvocation);
            parentFqName = parentFqName.parent();
        }
    }

}
