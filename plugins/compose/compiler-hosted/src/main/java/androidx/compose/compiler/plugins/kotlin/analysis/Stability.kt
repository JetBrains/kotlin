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

@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.lower.annotationClass
import androidx.compose.compiler.plugins.kotlin.lower.isSyntheticComposableFunction
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.isNullable

sealed class Stability {
    // class Foo(val bar: Int)
    class Certain(val stable: Boolean) : Stability() {
        override fun toString(): String {
            return if (stable) "Stable" else "Unstable"
        }
    }

    // class Foo(val bar: ExternalType) -> ExternalType.$stable
    class Runtime(val declaration: IrClass) : Stability() {
        override fun toString(): String {
            return "Runtime(${declaration.name.asString()})"
        }
    }

    // interface Foo { fun result(): Int }
    class Unknown(val declaration: IrClass) : Stability() {
        override fun toString(): String {
            return "Uncertain(${declaration.name.asString()})"
        }
    }

    // class <T> Foo(val value: T)
    class Parameter(val parameter: IrTypeParameter) : Stability() {
        override fun toString(): String {
            return "Parameter(${parameter.name.asString()})"
        }
    }

    // class Foo(val foo: A, val bar: B)
    class Combined(val elements: List<Stability>) : Stability() {
        override fun toString(): String {
            return elements.joinToString(",")
        }
    }

    operator fun plus(other: Stability): Stability = when {
        other is Certain -> if (other.stable) this else other
        this is Certain -> if (stable) other else this
        else -> Combined(listOf(this, other))
    }

    operator fun plus(other: List<Stability>): Stability {
        var stability = this
        for (el in other) {
            stability += el
        }
        return stability
    }

    companion object {
        val Stable: Stability = Certain(true)
        val Unstable: Stability = Certain(false)
    }
}

fun Stability.knownUnstable(): Boolean = when (this) {
    is Stability.Certain -> !stable
    is Stability.Runtime -> false
    is Stability.Unknown -> false
    is Stability.Parameter -> false
    is Stability.Combined -> elements.any { it.knownUnstable() }
}

fun Stability.knownStable(): Boolean = when (this) {
    is Stability.Certain -> stable
    is Stability.Runtime -> false
    is Stability.Unknown -> false
    is Stability.Parameter -> false
    is Stability.Combined -> elements.all { it.knownStable() }
}

fun Stability.isUncertain(): Boolean = when (this) {
    is Stability.Certain -> false
    is Stability.Runtime -> true
    is Stability.Unknown -> true
    is Stability.Parameter -> true
    is Stability.Combined -> elements.any { it.isUncertain() }
}

fun Stability.isExpressible(): Boolean = when (this) {
    is Stability.Certain -> true
    is Stability.Runtime -> true
    is Stability.Unknown -> false
    is Stability.Parameter -> true
    is Stability.Combined -> elements.all { it.isExpressible() }
}

fun Stability.normalize(): Stability {
    when (this) {
        // if not combined, there is no normalization needed
        is Stability.Certain,
        is Stability.Parameter,
        is Stability.Runtime,
        is Stability.Unknown,
            -> return this

        is Stability.Combined -> {
            // if combined, we perform the more expensive normalization process
        }
    }
    val parameters = mutableSetOf<IrTypeParameterSymbol>()
    val parts = mutableListOf<Stability>()
    val stack = mutableListOf<Stability>(this)
    while (stack.isNotEmpty()) {
        when (val stability: Stability = stack.removeAt(stack.size - 1)) {
            is Stability.Combined -> {
                stack.addAll(stability.elements)
            }

            is Stability.Certain -> {
                if (!stability.stable)
                    return Stability.Unstable
            }

            is Stability.Parameter -> {
                if (stability.parameter.symbol !in parameters) {
                    parameters.add(stability.parameter.symbol)
                    parts.add(stability)
                }
            }

            is Stability.Runtime -> parts.add(stability)
            is Stability.Unknown -> {
                /* do nothing */
            }
        }
    }
    return Stability.Combined(parts)
}

fun Stability.forEach(callback: (Stability) -> Unit) {
    if (this is Stability.Combined) {
        elements.forEach { it.forEach(callback) }
    } else {
        callback(this)
    }
}

fun IrAnnotationContainer.hasStableMarker(): Boolean =
    annotations.any { it.isStableMarker() }

private fun IrConstructorCall.isStableMarker(): Boolean {
    val owner = annotationClass?.owner ?: return false
    return owner.hasAnnotation(ComposeFqNames.StableMarker) || owner.classId in KnownStableConstructs.stableMarkers
}

private fun IrClass.hasStableMarkedDescendant(): Boolean {
    if (hasStableMarker()) return true
    return superTypes.any {
        !it.isAny() && it.classOrNull?.owner?.hasStableMarkedDescendant() == true
    }
}

private fun IrAnnotationContainer.stabilityParamBitmask(): Int? =
    (annotations.findAnnotation(ComposeFqNames.StabilityInferred)?.arguments[0] as? IrConst)
        ?.value as? Int

private data class SymbolForAnalysis(
    val symbol: IrClassifierSymbol,
    val typeParameters: List<IrTypeArgument?>,
    /**
     * The file containing the element that initiated the `stabilityOf` request tree that led to
     * the request associated with this object.
     *
     * This file is used to determine whether the response to the request associated with this
     * [SymbolForAnalysis] must be [Stability.Runtime]. See
     * `stabilityOf(IrClass, Map<IrTypeParameterSymbol, IrTypeArgument>, Set<SymbolForAnalysis>, IrFile?)`
     * for more details.
     */
    val analysisEntryFile: IrFile?,
)

class StabilityInferencer(
    private val isTargetJvm: Boolean,
    private val currentModule: ModuleDescriptor,
    externalStableTypeMatchers: Set<FqNameMatcher>,
) {
    private val externalTypeMatcherCollection = FqNameMatcherCollection(externalStableTypeMatchers)

    private val cache = mutableMapOf<SymbolForAnalysis, Stability>()

    /**
     * Returns the stability of [irType].
     *
     * @param fileContainingDependent The file containing the element that depends on the returned
     * result.
     */
    fun stabilityOf(irType: IrType, fileContainingDependent: IrFile?): Stability =
        stabilityOf(irType, emptyMap(), emptySet(), fileContainingDependent)

    /**
     * Returns the stability of [declaration].
     *
     * Note that to support incremental compilation, we are forced to use runtime stability when
     * [declaration] is `public` or `internal`, is contained in a different file than
     * [analysisEntryFile], and has a stability bitmask attached to it. When those conditions are
     * not all met, the stability of [declaration] will be inferred.
     *
     * @param analysisEntryFile The file containing the element that initiated the `stabilityOf`
     *   call tree that led to this call.
     */
    private fun stabilityOf(
        declaration: IrClass,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
        analysisEntryFile: IrFile?,
    ): Stability {
        val symbol = declaration.symbol
        val typeArguments = declaration.typeParameters.map { substitutions[it.symbol] }
        val fullSymbol = SymbolForAnalysis(symbol, typeArguments, analysisEntryFile)

        if (fullSymbol in cache) return cache[fullSymbol]!!

        val result = stabilityOf(declaration, fullSymbol, substitutions, currentlyAnalyzing)
        if (declaration.fileOrNull == analysisEntryFile) {
            cache[fullSymbol] = result
        }
        return result
    }

    /**
     * Returns the result of combining [this] with the stability of specified type parameters.
     *
     * @param mask If the i-th least significant bit of [mask] is set, then the stability of
     *   `typeParameters.get(i)` will be included in the result. If [mask] is null, then the
     *   stability of all elements of [typeParameters] will be included in the result.
     */
    private fun Stability.applyTypeParameterMask(
        mask: Int?,
        typeParameters: List<IrTypeParameter>,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
        analysisEntryFile: IrFile?,
    ): Stability {
        return when {
            mask == 0 || typeParameters.isEmpty() -> this
            else -> this + Stability.Combined(
                typeParameters.mapIndexedNotNull { index, irTypeParameter ->
                    if (index >= 32) return@mapIndexedNotNull null
                    if (mask == null || mask and (0b1 shl index) != 0) {
                        val sub = substitutions[irTypeParameter.symbol]
                        if (sub != null)
                            stabilityOf(sub, substitutions, currentlyAnalyzing, analysisEntryFile)
                        else
                            Stability.Parameter(irTypeParameter)
                    } else null
                }
            )
        }
    }

    private fun stabilityOf(
        declaration: IrClass,
        symbol: SymbolForAnalysis,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
    ): Stability {
        if (currentlyAnalyzing.contains(symbol)) return Stability.Unstable
        if (declaration.hasStableMarkedDescendant()) return Stability.Stable
        if (declaration.isEnumClass || declaration.isEnumEntry) return Stability.Stable
        if (declaration.defaultType.isPrimitiveType()) return Stability.Stable
        if (declaration.isProtobufType()) return Stability.Stable

        if (declaration.origin == IrDeclarationOrigin.IR_BUILTINS_STUB) {
            error("Builtins Stub: ${declaration.name}")
        }

        val analyzing = currentlyAnalyzing + symbol
        val fqName = declaration.fqNameWhenAvailable?.toString() ?: ""
        val typeParameters = declaration.typeParameters
        val fileContainingDeclaration = declaration.fileOrNull
        val analysisEntryFile = symbol.analysisEntryFile

        if (KnownStableConstructs.stableTypes.contains(fqName)) {
            val baseStability = Stability.Stable
            return baseStability.applyTypeParameterMask(
                mask = KnownStableConstructs.stableTypes[fqName] ?: 0,
                typeParameters = typeParameters,
                substitutions,
                analyzing,
                analysisEntryFile,
            )
        }

        if (declaration.isExternalStableType()) {
            val baseStability = Stability.Stable
            return baseStability.applyTypeParameterMask(
                mask = externalTypeMatcherCollection
                    .maskForName(declaration.fqNameWhenAvailable) ?: 0,
                typeParameters = typeParameters,
                substitutions,
                analyzing,
                analysisEntryFile,
            )
        }

        if (declaration.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
            return Stability.Unstable
        }

        if (declaration.isInterface) {
            // `Stability.Unknown` is always used for interfaces because stability bitmasks aren't populated for them.
            return Stability.Unknown(declaration)
        }

        if (declaration.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB && declaration.stabilityParamBitmask() == null) {
            return Stability.Unstable
        }

        val forcedToUseRuntimeStability = isTargetJvm &&
                (declaration.visibility.isPublicAPI || declaration.visibility == DescriptorVisibilities.INTERNAL) &&
                (fileContainingDeclaration == null || fileContainingDeclaration != analysisEntryFile)
        if (forcedToUseRuntimeStability) {
            if (typeParameters.isEmpty()) {
                return Stability.Runtime(declaration)
            } else {
                val baseStability = Stability.Runtime(declaration)
                return baseStability.applyTypeParameterMask(
                    mask = null,
                    typeParameters = typeParameters,
                    substitutions,
                    analyzing,
                    analysisEntryFile,
                )
            }
        }

        if (declaration.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB) {
            val mask = declaration.stabilityParamBitmask() ?: return Stability.Unstable

            val baseStability = Stability.Runtime(declaration)
            return baseStability.applyTypeParameterMask(
                mask,
                typeParameters = typeParameters,
                substitutions,
                analyzing,
                analysisEntryFile,
            )
        }

        var stability = Stability.Stable

        for (member in declaration.declarations) {
            when (member) {
                is IrProperty -> {
                    member.backingField?.let {
                        if (member.isVar && !member.isDelegated) return Stability.Unstable
                        stability += stabilityOf(it.type, substitutions, analyzing, analysisEntryFile)
                    }
                }

                is IrField -> {
                    stability += stabilityOf(member.type, substitutions, analyzing, analysisEntryFile)
                }
            }
        }

        declaration.superClass?.let {
            stability += stabilityOf(it, substitutions, analyzing, analysisEntryFile)
        }

        return stability
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrDeclaration.isInCurrentModule() =
        module == currentModule

    private fun IrClass.isProtobufType(): Boolean {
        // Quick exit as all protos are final
        if (!isFinalClass) return false
        val directParentClassName =
            superTypes.lastOrNull { !it.isInterface() }
                ?.classOrNull?.owner?.fqNameWhenAvailable?.toString()
        return directParentClassName == "com.google.protobuf.GeneratedMessageLite" ||
                directParentClassName == "com.google.protobuf.GeneratedMessage"
    }

    private fun IrClass.isExternalStableType(): Boolean {
        return externalTypeMatcherCollection.matches(fqNameWhenAvailable, superTypes)
    }

    /**
     * Returns the stability of [classifier].
     *
     * @param analysisEntryFile The file containing the element that initiated the `stabilityOf`
     *   call tree that led to this call.
     */
    private fun stabilityOf(
        classifier: IrClassifierSymbol,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
        analysisEntryFile: IrFile?,
    ): Stability {
        // if isEnum, return true
        // class hasStableAnnotation()
        return when (val owner = classifier.owner) {
            is IrClass -> stabilityOf(owner, substitutions, currentlyAnalyzing, analysisEntryFile)
            is IrTypeParameter -> Stability.Unstable
            is IrScript -> Stability.Stable
            else -> error("Unexpected IrClassifier: $owner")
        }
    }

    /**
     * Returns the stability of [argument].
     *
     * @param analysisEntryFile The file containing the element that initiated the `stabilityOf`
     *   call tree that led to this call.
     */
    private fun stabilityOf(
        argument: IrTypeArgument,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
        analysisEntryFile: IrFile?,
    ): Stability {
        return when (argument) {
            is IrStarProjection -> Stability.Unstable
            is IrTypeProjection -> stabilityOf(argument.type, substitutions, currentlyAnalyzing, analysisEntryFile)
        }
    }

    /**
     * Returns the stability of [type].
     *
     * @param analysisEntryFile The file containing the element that initiated the `stabilityOf`
     *   call tree that led to this call.
     */
    private fun stabilityOf(
        type: IrType,
        substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
        currentlyAnalyzing: Set<SymbolForAnalysis>,
        analysisEntryFile: IrFile?,
    ): Stability {
        return when {
            type is IrErrorType -> Stability.Unstable
            type is IrDynamicType -> Stability.Unstable

            type.isUnit() ||
                    type.isPrimitiveType() ||
                    type.isFunctionOrKFunction() ||
                    type.isSyntheticComposableFunction() ||
                    type.isString() -> Stability.Stable

            type.isTypeParameter() -> {
                val classifier = type.classifierOrFail
                val arg = substitutions[classifier]
                val symbol = SymbolForAnalysis(classifier, emptyList(), analysisEntryFile)
                if (arg != null && symbol !in currentlyAnalyzing) {
                    stabilityOf(arg, substitutions, currentlyAnalyzing + symbol, analysisEntryFile)
                } else {
                    Stability.Parameter(
                        classifier.owner as IrTypeParameter
                    )
                }
            }

            type.isNullable() -> stabilityOf(
                type.makeNotNull(),
                substitutions,
                currentlyAnalyzing,
                analysisEntryFile
            )

            type.isInlineClassType() -> {
                val inlineClassDeclaration = type.getClass()
                    ?: error("Failed to resolve the class definition of inline type $type")

                if (inlineClassDeclaration.hasStableMarker()) {
                    Stability.Stable
                } else {
                    stabilityOf(
                        type = getInlineClassUnderlyingType(inlineClassDeclaration),
                        substitutions = substitutions,
                        currentlyAnalyzing = currentlyAnalyzing,
                        analysisEntryFile
                    )
                }
            }

            type is IrSimpleType -> {
                stabilityOf(
                    type.classifier,
                    substitutions + type.substitutionMap(),
                    currentlyAnalyzing,
                    analysisEntryFile
                )
            }

            else -> error("Unexpected IrType: $type")
        }
    }

    private fun IrSimpleType.substitutionMap(): Map<IrTypeParameterSymbol, IrTypeArgument> {
        val cls = classOrNull ?: return emptyMap()
        val params = cls.owner.typeParameters.map { it.symbol }
        val args = arguments
        return params.zip(args).filter { (param, arg) ->
            param != (arg as? IrSimpleType)?.classifier
        }.toMap()
    }

    /**
     * Returns the stability of [expr].
     *
     * @param fileContainingDependent The file containing the element that depends on the returned
     * result.
     */
    private fun stabilityOf(
        expr: IrCall,
        baseStability: Stability,
        fileContainingDependent: IrFile?,
    ): Stability {
        val function = expr.symbol.owner
        val fqName = function.kotlinFqName

        return when (val mask = KnownStableConstructs.stableFunctions[fqName.asString()]) {
            null -> baseStability
            0 -> Stability.Stable
            else -> Stability.Combined(
                expr.typeArguments.indices.mapNotNull { index ->
                    if (mask and (0b1 shl index) != 0) {
                        val sub = expr.typeArguments[index]
                        if (sub != null)
                            stabilityOf(sub, fileContainingDependent)
                        else
                            Stability.Unstable
                    } else null
                }
            )
        }
    }

    /**
     * Returns the stability of [expr].
     *
     * @param fileContainingDependent The file containing the element that depends on the returned
     * result.
     */
    fun stabilityOf(expr: IrExpression, fileContainingDependent: IrFile?): Stability {
        // look at type first. if type is stable, whole expression is
        val stability = stabilityOf(expr.type, fileContainingDependent)
        if (stability.knownStable()) return stability
        return when (expr) {
            is IrConst -> Stability.Stable
            is IrCall -> stabilityOf(expr, stability, fileContainingDependent)
            is IrGetValue -> {
                val owner = expr.symbol.owner
                if (owner is IrVariable && !owner.isVar) {
                    owner.initializer?.let { stabilityOf(it, fileContainingDependent) } ?: stability
                } else {
                    stability
                }
            }

            is IrLocalDelegatedPropertyReference -> Stability.Stable
            // some default parameters and consts can be wrapped in composite
            is IrComposite -> {
                if (expr.statements.all { it is IrExpression && stabilityOf(it, fileContainingDependent).knownStable() }) {
                    Stability.Stable
                } else {
                    stability
                }
            }

            else -> stability
        }
    }
}
