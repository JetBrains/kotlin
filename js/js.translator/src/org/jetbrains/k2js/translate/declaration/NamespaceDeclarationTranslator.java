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
        JsObjectLiteral rootNamespaceDefinition = null;

        ClassDeclarationTranslator classDeclarationTranslator = new ClassDeclarationTranslator(context());
        for (JetFile file : files) {
            NamespaceDescriptor descriptor = context().bindingContext().get(BindingContext.FILE_TO_NAMESPACE, file);
            assert descriptor != null;
            NamespaceTranslator translator = descriptorToTranslator.get(descriptor);
            if (translator == null) {
                if (rootNamespaceDefinition == null) {
                    rootNamespaceDefinition = getRootPackage(descriptorToDefineInvocation, descriptor);
                }
                translator = new NamespaceTranslator(descriptor, classDeclarationTranslator, descriptorToDefineInvocation, context());
                descriptorToTranslator.put(descriptor, translator);
            }

            translator.translate(file);
        }

        if (rootNamespaceDefinition == null) {
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

        classDeclarationTranslator.generateDeclarations();
        for (NamespaceTranslator translator : descriptorToTranslator.values()) {
            translator.add(descriptorToDefineInvocation, result);
        }

        vars.addIfHasInitializer(classDeclarationTranslator.getDeclaration());
        vars.addIfHasInitializer(getDeclaration(rootNamespaceDefinition));
        return result;
    }

    private JsObjectLiteral getRootPackage(Map<NamespaceDescriptor, List<JsExpression>> descriptorToDefineInvocation,
            NamespaceDescriptor descriptor) {
        NamespaceDescriptor rootNamespace = descriptor;
        while (DescriptorUtils.isTopLevelDeclaration(rootNamespace)) {
            rootNamespace = (NamespaceDescriptor) rootNamespace.getContainingDeclaration();
        }

        List<JsExpression> args;
        JsObjectLiteral rootNamespaceDefinition = new JsObjectLiteral(true);
        if (context().isEcma5()) {
            args = Arrays.<JsExpression>asList(JsLiteral.NULL, rootNamespaceDefinition);
        }
        else {
            args = Collections.<JsExpression>singletonList(rootNamespaceDefinition);
        }

        descriptorToDefineInvocation.put(rootNamespace, args);
        return rootNamespaceDefinition;
    }

    private JsVar getDeclaration(@NotNull JsObjectLiteral rootNamespaceDefinition) {
        JsExpression packageMapValue;
        if (context().isEcma5()) {
            packageMapValue = new JsInvocation(JsAstUtils.CREATE_OBJECT, JsLiteral.NULL, rootNamespaceDefinition);
        }
        else {
            packageMapValue = rootNamespaceDefinition;
        }
        return new JsVar(context().scope().declareName(Namer.getRootNamespaceName()), packageMapValue);
    }
}
