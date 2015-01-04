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
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.js.translate.context.TranslationContext;
import org.jetbrains.kotlin.js.translate.general.AbstractTranslator;

import java.util.*;

import static com.google.dart.compiler.backend.js.ast.JsVars.JsVar;

public final class PackageDeclarationTranslator extends AbstractTranslator {
    private final Iterable<JetFile> files;
    private final Map<PackageFragmentDescriptor, PackageTranslator> packageFragmentToTranslator =
            new LinkedHashMap<PackageFragmentDescriptor, PackageTranslator>();

    public static List<JsStatement> translateFiles(@NotNull Collection<JetFile> files, @NotNull TranslationContext context) {
        return new PackageDeclarationTranslator(files, context).translate();
    }

    private PackageDeclarationTranslator(@NotNull Iterable<JetFile> files, @NotNull TranslationContext context) {
        super(context);

        this.files = files;
    }

    @NotNull
    private List<JsStatement> translate() {
        // predictable order
        Map<FqName, DefineInvocation> packageFqNameToDefineInvocation = new THashMap<FqName, DefineInvocation>();

        for (JetFile file : files) {
            PackageFragmentDescriptor packageFragment =
                    BindingContextUtils.getNotNull(context().bindingContext(), BindingContext.FILE_TO_PACKAGE_FRAGMENT, file);

            PackageTranslator translator = packageFragmentToTranslator.get(packageFragment);
            if (translator == null) {
                createRootPackageDefineInvocationIfNeeded(packageFqNameToDefineInvocation);
                translator = PackageTranslator.create(packageFragment, context());
                packageFragmentToTranslator.put(packageFragment, translator);
            }

            translator.translate(file);
        }

        for (PackageTranslator translator : packageFragmentToTranslator.values()) {
            translator.add(packageFqNameToDefineInvocation);
        }

        JsVars vars = new JsVars(true);
        vars.addIfHasInitializer(getRootPackageDeclaration(packageFqNameToDefineInvocation.get(FqName.ROOT)));

        return Collections.<JsStatement>singletonList(vars);
    }

    private void createRootPackageDefineInvocationIfNeeded(@NotNull Map<FqName, DefineInvocation> packageFqNameToDefineInvocation) {
        if (!packageFqNameToDefineInvocation.containsKey(FqName.ROOT)) {
            packageFqNameToDefineInvocation.put(
                    FqName.ROOT, DefineInvocation.create(FqName.ROOT, null, new JsObjectLiteral(true), context()));
        }
    }

    private JsVar getRootPackageDeclaration(@NotNull DefineInvocation defineInvocation) {
        JsExpression rootPackageVar = new JsInvocation(context().namer().rootPackageDefinitionMethodReference(), defineInvocation.asList());
        return new JsVar(context().scope().declareName(Namer.getRootPackageName()), rootPackageVar);
    }
}
