/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.extensions

import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.extensions.ProcessSourcesBeforeCompilingExtension
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrScript
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrFieldSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.powerassert.diagram.SourceFile
import org.jetbrains.kotlin.powerassert.diagram.irExplain
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.scripting.resolve.KtFileScriptSource
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.isStandalone

class SomeCallTransformer(
    private val sourceFile: SourceFile,
    private val context: IrPluginContext,
) : IrElementTransformerVoidWithContext() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitScriptNew(declaration: IrScript): IrStatement {
        val statements = declaration.statements.toList()
        declaration.statements.clear()
        val resProp = declaration.explicitCallParameters.firstOrNull { it.name.identifier == "res" }
            ?: return super.visitScriptNew(declaration)

        val mapType = context.irBuiltIns.mutableMapClass.typeWith(context.irBuiltIns.stringType, context.irBuiltIns.anyNType)
        val mapClass = mapType.getClass()!!
        val mapPut = mapClass.functions.single { it.name.asString() == "put" }

        for (statement in statements) {
            if (statement is IrProperty) {
                statement.backingField?.initializer?.let { body ->
                    val builder =
                        DeclarationIrBuilder(context, statement.backingField!!.symbol, declaration.startOffset, declaration.endOffset)
                    builder.irExplain(body.expression, sourceFile) { variables ->
                        val fields = variables.map {
                            context.irFactory.createField(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                IrDeclarationOrigin.DEFINED,
                                it.variable.name,
                                DescriptorVisibilities.PRIVATE,
                                IrFieldSymbolImpl(),
                                it.variable.type,
                                isFinal = true,
                                isStatic = false,
                                isExternal = false
                            )
                        }

                        declaration.statements.addAll(fields)
                        val last = fields.last()

                        val irCall = builder.irCall(mapPut).apply {
                            dispatchReceiver = builder.irGet(resProp)
                            putValueArgument(0, builder.irString(last.name.asString()))
//                            val receiver = builder.irGet(declaration.thisReceiver!!.type, declaration.thisReceiver!!.symbol)
                            putValueArgument(1, builder.irGetField(null, last, last.type))
                        }
//                        declaration.statements.add()
                        declaration.statements.add(irCall)
//                        val receiver = builder.irGet(declaration.thisReceiver!!.type, declaration.thisReceiver!!.symbol)
                        builder.irGetField(null, last, last.type)
                    }
                }

                declaration.statements.add(statement)
            }
        }
        return super.visitScriptNew(declaration)
    }

    override fun visitExpression(expression: IrExpression): IrExpression {
        val symbol = currentScope!!.scope.scopeOwnerSymbol
        val builder = DeclarationIrBuilder(context, symbol, expression.startOffset, expression.endOffset)
        return builder.irExplain(expression, sourceFile) { variables ->
            variables.last()
        }
    }
}

class SomeIrGenerationExtension() : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        for (file in moduleFragment.files) {
            SomeCallTransformer(SourceFile(file), pluginContext).visitFile(file)
        }
    }
}

class ScriptingProcessSourcesBeforeCompilingExtension(val project: Project) : ProcessSourcesBeforeCompilingExtension {

    override fun processSources(sources: Collection<KtFile>, configuration: CompilerConfiguration): Collection<KtFile> {
        val versionSettings = configuration.languageVersionSettings
        val shouldSkipStandaloneScripts = versionSettings.supportsFeature(LanguageFeature.SkipStandaloneScriptsInSourceRoots)
        val definitionProvider by lazy(LazyThreadSafetyMode.NONE) { ScriptDefinitionProvider.getInstance(project) }
        val messageCollector = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)

        fun KtFile.isStandaloneScript(): Boolean {
            val scriptDefinition = definitionProvider?.findDefinition(KtFileScriptSource(this))
            return scriptDefinition?.compilationConfiguration?.get(ScriptCompilationConfiguration.isStandalone) ?: true
        }

        if (configuration.getBoolean(CommonConfigurationKeys.ALLOW_ANY_SCRIPTS_IN_SOURCE_ROOTS)) return sources
        // TODO: see comment at LazyScriptDefinitionProvider.Companion.getNonScriptFilenameSuffixes
        val nonScriptFilenameSuffixes = arrayOf(".${KotlinFileType.EXTENSION}", ".${JavaFileType.DEFAULT_EXTENSION}")
        // filter out scripts that are not suitable for source roots, according to the compiler configuration and script definitions
        return sources.filter { ktFile ->
            when {
                nonScriptFilenameSuffixes.any { ktFile.virtualFilePath.endsWith(it) } -> true
                !ktFile.isStandaloneScript() -> true
                else -> {
                    if (!shouldSkipStandaloneScripts) {
                        messageCollector.report(
                            CompilerMessageSeverity.WARNING,
                            "Script '${ktFile.name}' is not supposed to be used along with regular Kotlin sources, and will be ignored in the future versions by default. (Use -Xallow-any-scripts-in-source-roots command line option to opt-in for the old behavior.)"
                        )
                    }
                    !shouldSkipStandaloneScripts
                }
            }
        }
    }
}