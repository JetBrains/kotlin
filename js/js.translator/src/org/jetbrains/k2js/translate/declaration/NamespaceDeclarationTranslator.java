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
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.k2js.translate.context.Namer;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.general.AbstractTranslator;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;
import static org.jetbrains.k2js.translate.declaration.DefineInvocation.createDefineInvocation;

public final class NamespaceDeclarationTranslator extends AbstractTranslator {
    private final Iterable<JetFile> files;
    private final Map<PackageFragmentDescriptor, NamespaceTranslator> packageFragmentToTranslator =
            new LinkedHashMap<PackageFragmentDescriptor, NamespaceTranslator>();

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
        Map<FqName, DefineInvocation> packageFqNameToDefineInvocation = new THashMap<FqName, DefineInvocation>();

        for (JetFile file : files) {
            PackageFragmentDescriptor packageFragment = context().bindingContext().get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);

            NamespaceTranslator translator = packageFragmentToTranslator.get(packageFragment);
            if (translator == null) {
                createRootPackageDefineInvocationIfNeeded(packageFqNameToDefineInvocation);
                translator = new NamespaceTranslator(packageFragment, packageFqNameToDefineInvocation, context());
                packageFragmentToTranslator.put(packageFragment, translator);
            }

            translator.translate(file);
        }

        for (NamespaceTranslator translator : packageFragmentToTranslator.values()) {
            translator.add(packageFqNameToDefineInvocation);
        }

        JsVars vars = new JsVars(true);
        vars.addIfHasInitializer(getRootPackageDeclaration(packageFqNameToDefineInvocation.get(FqName.ROOT)));

        return Collections.<JsStatement>singletonList(vars);
    }

    private void createRootPackageDefineInvocationIfNeeded(@NotNull Map<FqName, DefineInvocation> packageFqNameToDefineInvocation) {
        if (!packageFqNameToDefineInvocation.containsKey(FqName.ROOT)) {
            packageFqNameToDefineInvocation.put(
                    FqName.ROOT, createDefineInvocation(FqName.ROOT, null, new JsObjectLiteral(true), context()));
        }
    }

    private JsVar getRootPackageDeclaration(@NotNull DefineInvocation defineInvocation) {
        JsExpression rootPackageVar = new JsInvocation(context().namer().rootPackageDefinitionMethodReference(), defineInvocation.asList());
        return new JsVar(context().scope().declareName(Namer.getRootNamespaceName()), rootPackageVar);
    }
}
