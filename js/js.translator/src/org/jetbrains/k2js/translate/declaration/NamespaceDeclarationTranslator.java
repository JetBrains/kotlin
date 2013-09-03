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

import com.google.dart.compiler.backend.js.ast.*;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;
import org.jetbrains.k2js.translate.utils.JsAstUtils;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

public final class NamespaceDeclarationTranslator extends AbstractTranslator {
    private final Iterable<JetFile> files;
    private final Map<NamespaceDescriptor,NamespaceTranslator> descriptorToTranslator =
            new LinkedHashMap<NamespaceDescriptor, NamespaceTranslator>();

    public static List<JsStatement> translateFiles(@NotNull Collection<JetFile> files, @NotNull TranslationContext context) {
        return new NamespaceDeclarationTranslator(files, context).translate();
    }

    private NamespaceDeclarationTranslator(@NotNull Iterable<JetFile> files, @NotNull TranslationContext context) {
        super(context);

        this.files = files;
    }

    @NotNull
    private List<JsStatement> translate() {
        // predictable order
        Map<NamespaceDescriptor, List<JsExpression>> descriptorToDefineInvocation = new THashMap<NamespaceDescriptor, List<JsExpression>>();
        NamespaceDescriptor rootNamespaceDescriptor = null;

        for (JetFile file : files) {
            NamespaceDescriptor descriptor = context().bindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
            assert descriptor != null;
            NamespaceTranslator translator = descriptorToTranslator.get(descriptor);
            if (translator == null) {
                if (rootNamespaceDescriptor == null) {
                    rootNamespaceDescriptor = getRootPackageDescriptor(descriptorToDefineInvocation, descriptor);
                }
                translator = new NamespaceTranslator(descriptor, descriptorToDefineInvocation, context());
                descriptorToTranslator.put(descriptor, translator);
            }

            translator.translate(file);
        }

        if (rootNamespaceDescriptor == null) {
            return Collections.emptyList();
        }

        JsVars vars = new JsVars(true);
        List<JsStatement> result;
        if (context().isEcma5()) {
            result = Collections.<JsStatement>singletonList(vars);
        }
        else {
            result = new ArrayList<JsStatement>();
            result.add(vars);
        }

        context().classDeclarationTranslator().generateDeclarations();
        for (NamespaceTranslator translator : descriptorToTranslator.values()) {
            translator.add(descriptorToDefineInvocation, result);
        }

        vars.addIfHasInitializer(context().classDeclarationTranslator().getDeclaration());
        vars.addIfHasInitializer(getRootPackageDeclaration(descriptorToDefineInvocation.get(rootNamespaceDescriptor)));
        return result;
    }

    @NotNull
    private NamespaceDescriptor getRootPackageDescriptor(
            @NotNull Map<NamespaceDescriptor, List<JsExpression>> descriptorToDefineInvocation,
            @NotNull NamespaceDescriptor descriptor
    ) {
        NamespaceDescriptor rootNamespace = descriptor;
        while (DescriptorUtils.isTopLevelDeclaration(rootNamespace)) {
            rootNamespace = (NamespaceDescriptor) rootNamespace.getContainingDeclaration();
        }

        descriptorToDefineInvocation.put(rootNamespace, createDefineInvocation(rootNamespace, null, new JsObjectLiteral(true), context()));
        return rootNamespace;
    }

    static List<JsExpression> createDefineInvocation(
            @NotNull NamespaceDescriptor descriptor,
            @Nullable JsExpression initializer,
            @NotNull JsObjectLiteral members,
            @NotNull TranslationContext context
    ) {
        if (context.isEcma5()) {
            return Arrays.asList(initializer == null ? JsLiteral.NULL : initializer,
                                 new JsDocComment(JsAstUtils.LENDS_JS_DOC_TAG, context.getQualifiedReference(descriptor)),
                                 members);
        }
        else {
            return Collections.<JsExpression>singletonList(members);
        }
    }

    private JsVar getRootPackageDeclaration(@NotNull List<JsExpression> defineInvocation) {
        JsExpression rootPackageVar = new JsInvocation(context().namer().rootPackageDefinitionMethodReference(), defineInvocation);
        return new JsVar(context().scope().declareName(Namer.getRootNamespaceName()), rootPackageVar);
    }
}
