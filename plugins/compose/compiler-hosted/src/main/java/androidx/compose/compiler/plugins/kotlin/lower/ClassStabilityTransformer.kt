/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.compose.compiler.plugins.kotlin.ComposeClassIds
import androidx.compose.compiler.plugins.kotlin.FeatureFlags
import androidx.compose.compiler.plugins.kotlin.ModuleMetrics
import androidx.compose.compiler.plugins.kotlin.analysis.*
import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrImplementationDetail
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

enum class StabilityBits(val bits: Int) {
    UNSTABLE(0b100),
    STABLE(0b000);

    fun bitsForSlot(slot: Int): Int = bits shl (1 + slot * 3)
}

/**
 * This transform determines the stability of every class, and synthesizes a StabilityInferred
 * annotation on it, as well as putting a static final int of the stability to be used at runtime.
 */
class ClassStabilityTransformer(
    private val useK2: Boolean,
    context: IrPluginContext,
    metrics: ModuleMetrics,
    stabilityInferencer: StabilityInferencer,
    private val classStabilityInferredCollection: ClassStabilityInferredCollection? = null,
    featureFlags: FeatureFlags,
    private val messageCollector: MessageCollector,
) : AbstractComposeLowering(context, metrics, stabilityInferencer, featureFlags),
    ClassLoweringPass,
    ModuleLoweringPass {

    private val StabilityInferredClass = getTopLevelClass(ComposeClassIds.StabilityInferred)
    private val UNSTABLE = StabilityBits.UNSTABLE.bitsForSlot(0)
    private val STABLE = StabilityBits.STABLE.bitsForSlot(0)
    private val unstableClassesWarning: MutableSet<ClassDescriptor>? = if (!context.platform.isJvm()) mutableSetOf() else null


    override fun lower(module: IrModuleFragment) {
        module.transformChildrenVoid(this)

        if (!context.platform.isJvm() && !unstableClassesWarning.isNullOrEmpty()) {
            val classIds = unstableClassesWarning.mapTo(mutableSetOf()) { it.fqNameSafe.toString() }
            val classesConcatenated = classIds.sorted().joinToString("\n")
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Some of the dependencies were build using an older version of the Compose compiler plugin, " +
                        "which may cause additional (or endless) recompositions on non-JVM targets. " +
                        "To prevent that consider updating dependency libraries to versions built with a newer Compose compiler. " +
                        "Right now, the following classes are considered `Unstable`:\n" +
                        classesConcatenated
            )
        }
    }

    override fun lower(irClass: IrClass) {
    }

    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid(this)
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitClass(declaration: IrClass): IrStatement {
        val result = super.visitClass(declaration)
        val cls = result as? IrClass ?: return result

        if (
            (
                    // Including public AND internal to support incremental compilation, which
                    // is separated by file.
                    cls.visibility != DescriptorVisibilities.PUBLIC &&
                            cls.visibility != DescriptorVisibilities.INTERNAL
                    ) ||
            cls.isEnumClass ||
            cls.isEnumEntry ||
            cls.isInterface ||
            cls.isAnnotationClass ||
            cls.isAnonymousObject ||
            cls.isExpect ||
            cls.isInner ||
            cls.isFileClass ||
            cls.isCompanion ||
            cls.defaultType.isInlineClassType()
        ) return cls

        if (declaration.hasStableMarker()) {
            metrics.recordClass(
                declaration,
                marked = true,
                stability = Stability.Stable,
            )
            cls.addStabilityMarkerField(irConst(STABLE))
            return cls
        }

        val stability = stabilityInferencer.stabilityOf(declaration.defaultType).normalize()

        // remove type parameters

        var parameterMask = 0
        val stableExpr: IrExpression

        if (cls.typeParameters.isNotEmpty()) {
            val symbols = cls.typeParameters.map { it.symbol }
            var externalParameters = false

            stability.forEach {
                when (it) {
                    is Stability.Parameter -> {
                        val index = symbols.indexOf(it.parameter.symbol)
                        if (index != -1) {
                            // the stability of this parameter matters for the stability of the
                            // class
                            parameterMask = parameterMask or (0b1 shl index)
                        } else {
                            externalParameters = true
                        }
                    }

                    else -> {
                        /* No action necessary */
                    }
                }
            }
            if (stability.knownStable() && symbols.size < 32) {
                parameterMask = parameterMask or (0b1 shl symbols.size)
            }
            stableExpr = if (externalParameters)
                irConst(UNSTABLE)
            else
                stability.irStableExpression(
                    resolve = { irConst(STABLE) },
                    reportUnknownStability = { unstableClassesWarning?.add(it.descriptor) }) ?: irConst(UNSTABLE)
        } else {
            stableExpr =
                stability.irStableExpression(reportUnknownStability = { unstableClassesWarning?.add(it.descriptor) }) ?: irConst(UNSTABLE)
            if (stability.knownStable()) {
                parameterMask = 0b1
            }
        }
        metrics.recordClass(
            declaration,
            marked = false,
            stability = stability
        )
        val annotation = IrConstructorCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            StabilityInferredClass.defaultType,
            StabilityInferredClass.constructors.first(),
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            origin = null
        ).also {
            it.putValueArgument(0, irConst(parameterMask))
        }

        if (useK2) {
            context.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(
                cls,
                annotation,
            )
        } else {
            cls.annotations += annotation
            classStabilityInferredCollection?.addClass(cls, parameterMask)
        }

        cls.addStabilityMarkerField(stableExpr)
        return result
    }

    @OptIn(IrImplementationDetail::class, UnsafeDuringIrConstructionAPI::class)
    private fun IrClass.addStabilityMarkerField(stabilityExpression: IrExpression) {
        val stabilityField = makeStabilityField()
        stabilityField.initializer = context.irFactory.createExpressionBody(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            stabilityExpression
        )
    }
}
