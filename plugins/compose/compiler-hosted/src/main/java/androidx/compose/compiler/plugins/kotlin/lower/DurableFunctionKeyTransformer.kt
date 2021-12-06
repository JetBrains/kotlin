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

package androidx.compose.compiler.plugins.kotlin.lower

import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.ComposeWritableSlices.DURABLE_FUNCTION_KEY
import androidx.compose.compiler.plugins.kotlin.irTrace
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace

class KeyInfo(
    val name: String,
    val startOffset: Int,
    val endOffset: Int,
    val hasDuplicates: Boolean,
) {
    var used: Boolean = false
    val key: Int get() = name.hashCode()
}

/**
 * This transform will generate a "durable" and mostly unique key for every function in the module.
 * In this case "durable" means that when the code is edited over time, a function with the same
 * semantic identity will usually have the same key each time it is compiled. This is important so
 * that new code can be recompiled and the key that the function gets after that recompile ought to
 * be the same as before, so one could inject this new code and signal to the runtime that
 * composable functions with that key should be considered invalid.
 *
 * This transform runs early on in the lowering pipeline, and stores the keys for every function in
 * the file in the BindingTrace for each function. These keys are then retrieved later on by other
 * lowerings and marked as used. After all lowerings have completed, one can use the
 * [includeFunctionKeyMetaClasses] method to generate additional empty classes that include annotations
 * with the keys of each function and their source locations for tooling to utilize.
 *
 * For example, this transform will run on code like the following:
 *
 *     @Composable fun Example() {
 *       Box {
 *          Text("Hello WOrld")
 *       }
 *     }
 *
 * And produce code like the following:
 *
 *     @Composable fun Example() {
 *       startGroup(123)
 *       Box {
 *         startGroup(345)
 *         Text("Hello World")
 *         endGroup()
 *       }
 *       endGroup()
 *     }
 *
 *     @FunctionKeyMetaClass
 *     @FunctionKeyMeta(key=123, startOffset=24, endOffset=56)
 *     @FunctionKeyMeta(key=345, startOffset=32, endOffset=43)
 *     class Example-KeyMeta
 *
 * @see DurableKeyVisitor
 */
class DurableFunctionKeyTransformer(
    context: IrPluginContext,
    symbolRemapper: DeepCopySymbolRemapper,
    bindingTrace: BindingTrace,
    metrics: ModuleMetrics,
) : DurableKeyTransformer(
    DurableKeyVisitor(),
    context,
    symbolRemapper,
    bindingTrace,
    metrics
) {
    inner class Meta(
        val file: IrFile,
        val metaClass: IrClass,
    ) {
        val keys = mutableListOf<KeyInfo>()
        fun includeFunctionKeyMetaClass() {
            val usedKeys = keys.filter { it.used }
            if (usedKeys.isEmpty()) {
                // If none of the keys were used, don't generate a class
                return
            }
            metaClass.annotations += usedKeys.map { irKeyMetaAnnotation(it) }
            file.addChild(metaClass)
        }
    }

    val metas = mutableListOf<Meta>()

    var current: Meta? = null

    fun includeFunctionKeyMetaClasses() {
        if (keyMetaAnnotation == null || metaClassAnnotation == null) {
            // if the generate key meta flag was passed in to the compiler but the annotations
            // aren't in the runtime, we are just going to silently ignore it.
            return
        }
        metas.forEach { it.includeFunctionKeyMetaClass() }
    }

    private val keyMetaAnnotation =
        getInternalClassOrNull("FunctionKeyMeta")
    private val metaClassAnnotation =
        getInternalClassOrNull("FunctionKeyMetaClass")

    private fun irKeyMetaAnnotation(
        key: KeyInfo
    ): IrConstructorCall = IrConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        keyMetaAnnotation!!.defaultType,
        keyMetaAnnotation.constructors.single(),
        0,
        0,
        2
    ).apply {
        putValueArgument(0, irConst(key.key.hashCode()))
        putValueArgument(1, irConst(key.startOffset))
        putValueArgument(1, irConst(key.endOffset))
    }

    private fun irMetaClassAnnotation(
        file: String
    ): IrConstructorCall = IrConstructorCallImpl(
        UNDEFINED_OFFSET,
        UNDEFINED_OFFSET,
        metaClassAnnotation!!.defaultType,
        metaClassAnnotation.constructors.single(),
        0,
        0,
        1
    ).apply {
        putValueArgument(0, irConst(file))
    }

    private fun buildClass(filePath: String): IrClass {
        val fileName = filePath.split('/').last()
        return context.irFactory.buildClass {
            kind = ClassKind.CLASS
            visibility = DescriptorVisibilities.INTERNAL
            val shortName = PackagePartClassUtils.getFilePartShortName(fileName)
            // the name of the LiveLiterals class is per-file, so we use the same name that
            // the kotlin file class lowering produces, prefixed with `LiveLiterals$`.
            name = Name.identifier("$shortName\$KeyMeta")
        }.also {
            it.createParameterDeclarations()

            // store the full file path to the file that this class is associated with in an
            // annotation on the class. This will be used by tooling to associate the keys
            // inside of this class with actual PSI in the editor.
            if (metaClassAnnotation != null) {
                it.annotations += irMetaClassAnnotation(filePath)
            }
            it.addConstructor {
                isPrimary = true
            }.also { ctor ->
                ctor.body = DeclarationIrBuilder(context, it.symbol).irBlockBody {
                    +irDelegatingConstructorCall(
                        context
                            .irBuiltIns
                            .anyClass
                            .owner
                            .primaryConstructor!!
                    )
                }
            }
        }
    }

    override fun visitFile(declaration: IrFile): IrFile {
        val stringKeys = mutableSetOf<String>()
        return root(stringKeys) {
            val prev = current
            val next = Meta(
                declaration,
                buildClass(declaration.fileEntry.name)
            )
            metas.push(next)
            try {
                current = next
                super.visitFile(declaration)
            } finally {
                current = prev
            }
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val signature = declaration.signatureString()
        val (fullName, success) = buildKey("fun-$signature")
        val info = KeyInfo(
            fullName,
            declaration.startOffset,
            declaration.endOffset,
            success,
        )
        current?.keys?.add(info)
        context.irTrace.record(DURABLE_FUNCTION_KEY, declaration, info)
        return super.visitSimpleFunction(declaration)
    }
}