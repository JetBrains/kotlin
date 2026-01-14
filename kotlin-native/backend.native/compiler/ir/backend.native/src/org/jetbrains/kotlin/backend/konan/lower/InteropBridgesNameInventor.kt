/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.getCompilerMessageLocation
import org.jetbrains.kotlin.backend.common.peek
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.ir.buildSimpleAnnotation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrPackageFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.declarations.name
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.objcinterop.isObjCClass
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getAnnotationStringValue
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.Name

/*
 * Giving unique names to the interop bridges is not an easy task.
 * Firstly, some interop calls may produce more than one bridge (like a call to an Obj-C function which in turn
 * calls some Kotlin functions back). Secondly, along with Kotlin bridges, some C bridges also might be generated,
 * with some identifiers inside which also must be unique. Thirdly, to give more flexibility to the lowering, it may be called
 * before the inliner. But a bridge may be generated inside an inline lambda (if there was a call from a cinterop klib there),
 * and will be copied during inlining. This copy will have the same name thus breaking the names uniqueness required.
 *
 * The solution is to divide the process of bridges naming into two phases. Note, there is always a root interop call (the first
 * call to a cinterop klib). This root call works as a scope for all the bridges that will be generated for it.
 * The first stage is to give some placeholder names for every bridge inside a scope (this is usually just some reasonable prefix
 * plus a counter). The root call gets wrapped into the call to a special intrinsic - [interopCallMarker] which will be removed
 * during the second stage. Otherwise, there won't be any easy way to restore the scope. The generated C bridges are placed into
 * a special annotation ([KotlinToCBridge] or [CToKotlinBridge]) of the corresponding Kotlin bridge.
 * The second stage takes all the bridges within a scope and replaces <placeHolder> (which may be coming from the bridge's name,
 * or from its annotation) with <uniquePrefix>_<scopeCounter>_<placeHolder> where <uniquePrefix> is some name unique per each file,
 * <scopeCounter> is a counter increasing with each scope. Since all the placeholders within a scope are unique by construction,
 * the resulting names are indeed unique.
 */

private fun getUniqueName(packageFragment: IrPackageFragment, fileName: String) =
        packageFragment.moduleDescriptor.name.asString().let { it.substring(1, it.lastIndex) } + fileName

private val IrFile.uniqueName: String
    get() = getUniqueName(this, fileEntry.name)

private fun IrDeclaration.getUniqueName(context: Context) =
        getUniqueName(this.getPackageFragment(), context.externalDeclarationFileNameProvider.getExternalDeclarationFileName(this))

internal class InteropBridgesNameInventor(val generationState: NativeGenerationState) : FileLoweringPass, BodyLoweringPass {
    private val context = generationState.context

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(InteropBridgeCollector(irFile, irFile.uniqueName))
    }

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        val transformer = InteropBridgeCollector(container.fileOrNull, container.getUniqueName(context))
        irBody.transform(transformer, data = null)
    }

    sealed class Bridge(val function: IrSimpleFunction, val annotation: IrConstructorCall) {
        class CToKotlin(function: IrSimpleFunction, annotation: IrConstructorCall) : Bridge(function, annotation)
        class KotlinToC(function: IrSimpleFunction, annotation: IrConstructorCall) : Bridge(function, annotation)
    }

    private inner class InteropBridgeCollector(val irFile: IrFile?, val uniqueName: String) : IrElementTransformerVoid() {
        val uniquePrefix = buildString {
            append('_')
            uniqueName.toByteArray().joinTo(this, "") {
                (0xFF and it.toInt()).toString(16).padStart(2, '0')
            }
            append('_')
        }

        val scopes = mutableListOf<MutableList<Bridge>>()

        override fun visitClass(declaration: IrClass): IrStatement {
            val bridges = mutableListOf<Bridge>()
            scopes.push(bridges)
            declaration.transformChildrenVoid(this)
            scopes.pop()
            if (bridges.isNotEmpty()) {
                require(declaration.isObjCClass()) { "Only an ObjC class can have standalone bridges: ${declaration.render()}" }

                comeUpWithUniqueNamesAndRegisterBridges(bridges)
            }

            return declaration
        }

        override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
            declaration.transformChildrenVoid(this)

            declaration.getAnnotation(InteropFqNames.cToKotlinBridge)?.let {
                scopes.peek()?.add(Bridge.CToKotlin(declaration, it))
                        ?: error("No scope for ${declaration.render()}, parent ${declaration.parent.render()} from ${irFile?.name}")
            }
            declaration.getAnnotation(InteropFqNames.kotlinToCBridge)?.let {
                scopes.peek()?.add(Bridge.KotlinToC(declaration, it))
                        ?: error("No scope for ${declaration.render()}, parent ${declaration.parent.render()} from ${irFile?.name}")
            }

            return declaration
        }

        override fun visitCall(expression: IrCall): IrExpression {
            if (expression.symbol != context.symbols.interopCallMarker)
                return super.visitCall(expression)

            val bridges = mutableListOf<Bridge>()
            scopes.push(bridges)
            expression.transformChildrenVoid(this)
            scopes.pop()

            comeUpWithUniqueNamesAndRegisterBridges(bridges)

            return expression.arguments[0]!!
        }

        fun String.extractIdentifier() = this.substring(1, this.length - 1)

        fun comeUpWithUniqueNamesAndRegisterBridges(bridges: List<Bridge>) {
            val bridgesContainerIndex = generationState.fileLowerState.getCStubIndex()
            val mapping = mutableMapOf<String, String>()

            fun getUniqueIdentifier(placeHolder: String) = mapping.getOrPut(placeHolder) {
                "${uniquePrefix}${bridgesContainerIndex + 1}_${placeHolder.extractIdentifier()}"
            }

            bridges.forEach { getUniqueIdentifier(it.function.name.asString()) }

            fun fixUpAllPlaceHolders(snippet: String): String = buildString {
                var index = 0
                var state = 0
                var placeHolderStart = 0
                while (index < snippet.length) {
                    if (snippet[index] == InteropLowering.NAME_PLACEHOLDER_QUOTE) {
                        if (state == 0) {
                            state = 1
                            placeHolderStart = index++
                        } else {
                            val placeHolder = snippet.substring(placeHolderStart, index + 1)
                            val identifier = getUniqueIdentifier(placeHolder)
                            append(identifier)
                            ++index
                            state = 0
                        }
                    } else {
                        if (state == 0)
                            append(snippet[index])
                        ++index
                    }
                }
                require(state == 0) {
                    val expectedChar = "\\u${InteropLowering.NAME_PLACEHOLDER_QUOTE.code.toString(16).padStart(4, '0')}"
                    """Bad code snippet, no closing '$expectedChar' was found after position $placeHolderStart:
                        |$snippet""".trimMargin()
                }
            }

            for (bridge in bridges) {
                val function = bridge.function
                val annotation = bridge.annotation
                val oldName = function.name.asString()
                val newName = mapping[oldName]!!
                val location = irFile?.let { bridge.function.getCompilerMessageLocation(it) }
                val newAnnotations = function.annotations.toMutableList()
                when (bridge) {
                    is Bridge.CToKotlin -> {
                        val language = annotation.getAnnotationStringValue("language")
                        val declaration = fixUpAllPlaceHolders(annotation.getAnnotationStringValue("declaration"))
                        newAnnotations[newAnnotations.indexOf(annotation)] =
                                buildSimpleAnnotation(
                                        context.irBuiltIns, function.startOffset, function.endOffset,
                                        context.symbols.cToKotlinBridge.owner, language, declaration
                                )
                        newAnnotations.add(
                                buildSimpleAnnotation(
                                        context.irBuiltIns, function.startOffset, function.endOffset,
                                        context.symbols.exportForCppRuntime.owner, newName
                                )
                        )

                        generationState.cStubsManager.addStub(location, listOf(declaration), language)
                    }
                    is Bridge.KotlinToC -> {
                        val language = annotation.getAnnotationStringValue("language")
                        val impl = fixUpAllPlaceHolders(annotation.getAnnotationStringValue("impl"))
                        val libraryName = annotation.getAnnotationStringValue("library")
                        newAnnotations[newAnnotations.indexOf(annotation)] =
                                buildSimpleAnnotation(
                                        context.irBuiltIns, function.startOffset, function.endOffset,
                                        context.symbols.kotlinToCBridge.owner, language, impl, libraryName
                                )
                        newAnnotations.add(
                                buildSimpleAnnotation(
                                        context.irBuiltIns, function.startOffset, function.endOffset,
                                        context.symbols.symbolName.owner, newName
                                )
                        )

                        generationState.cStubsManager.addStub(location, impl.split('\n'), language)
                        if (libraryName.isNotEmpty()) {
                            val library = generationState.config.librariesWithDependencies().firstOrNull { it.uniqueName == libraryName }
                                    ?: error("Library with name $libraryName not found in the dependencies")
                            generationState.dependenciesTracker.add(library)
                        }
                    }
                }

                function.annotations = newAnnotations
                function.name = Name.identifier(newName)
            }
        }
    }
}
