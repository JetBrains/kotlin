/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import androidx.compose.compiler.plugins.kotlin.lower.ClassStabilityTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunInterfaceLowering
import androidx.compose.compiler.plugins.kotlin.lower.ComposableFunctionBodyTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposerIntrinsicTransformer
import androidx.compose.compiler.plugins.kotlin.lower.ComposerLambdaMemoization
import androidx.compose.compiler.plugins.kotlin.lower.ComposerParamTransformer
import androidx.compose.compiler.plugins.kotlin.lower.DurableKeyVisitor
import androidx.compose.compiler.plugins.kotlin.lower.LiveLiteralTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContextImpl
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class ComposeIrGenerationExtension(
    @Suppress("unused") private val liveLiteralsEnabled: Boolean = false,
    private val sourceInformationEnabled: Boolean = true,
    private val intrinsicRememberEnabled: Boolean = true,
) : IrGenerationExtension {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        generateSymbols(pluginContext)

        VersionChecker(pluginContext).check()

        // TODO: refactor transformers to work with just BackendContext
        val bindingTrace = DelegatingBindingTrace(
            pluginContext.bindingContext,
            "trace in " +
                "ComposeIrGenerationExtension"
        )

        // create a symbol remapper to be used across all transforms
        val symbolRemapper = DeepCopySymbolRemapper()

        ClassStabilityTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        LiveLiteralTransformer(
            liveLiteralsEnabled,
            DurableKeyVisitor(),
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        ComposableFunInterfaceLowering(pluginContext).lower(moduleFragment)

        // Memoize normal lambdas and wrap composable lambdas
        ComposerLambdaMemoization(pluginContext, symbolRemapper, bindingTrace).lower(moduleFragment)

        // transform all composable functions to have an extra synthetic composer
        // parameter. this will also transform all types and calls to include the extra
        // parameter.
        ComposerParamTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace
        ).lower(moduleFragment)

        // transform calls to the currentComposer to just use the local parameter from the
        // previous transform
        ComposerIntrinsicTransformer(pluginContext).lower(moduleFragment)

        ComposableFunctionBodyTransformer(
            pluginContext,
            symbolRemapper,
            bindingTrace,
            sourceInformationEnabled,
            intrinsicRememberEnabled
        ).lower(moduleFragment)

        generateSymbols(pluginContext)
    }
}

val SymbolTable.allUnbound: List<IrSymbol>
    get() {
        val r = mutableListOf<IrSymbol>()
        r.addAll(unboundClasses)
        r.addAll(unboundConstructors)
        r.addAll(unboundEnumEntries)
        r.addAll(unboundFields)
        r.addAll(unboundSimpleFunctions)
        r.addAll(unboundProperties)
        r.addAll(unboundTypeParameters)
        r.addAll(unboundTypeAliases)
        return r
    }

@OptIn(ObsoleteDescriptorBasedAPI::class)
@Suppress("UNUSED_PARAMETER", "DEPRECATION")
fun generateSymbols(pluginContext: IrPluginContext) {
    lateinit var unbound: List<IrSymbol>
    val visited = mutableSetOf<IrSymbol>()
    do {
        unbound = (pluginContext.symbolTable as SymbolTable).allUnbound

        for (symbol in unbound) {
            if (visited.contains(symbol)) {
                continue
            }
            // Symbol could get bound as a side effect of deserializing other symbols.
            if (!symbol.isBound) {
                (pluginContext as IrPluginContextImpl).linker.getDeclaration(symbol)
            }
            if (!symbol.isBound) { visited.add(symbol) }
        }
    } while ((unbound - visited).isNotEmpty())
}
