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

package androidx.compose.compiler.plugins.kotlin.analysis

import androidx.compose.compiler.plugins.kotlin.ComposeFqNames
import androidx.compose.compiler.plugins.kotlin.lower.annotationClass
import org.jetbrains.kotlin.backend.jvm.ir.isInlineClassType
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrComposite
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrDynamicType
import org.jetbrains.kotlin.ir.types.IrErrorType
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrStarProjection
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeAbbreviation
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.isPrimitiveType
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.findAnnotation
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.getInlineClassUnderlyingType
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.isFinalClass
import org.jetbrains.kotlin.ir.util.isFunctionOrKFunction
import org.jetbrains.kotlin.ir.util.isInterface
import org.jetbrains.kotlin.ir.util.isTypeParameter

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
        is Stability.Unknown -> return this
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
                if (parameters.contains(stability.parameter.symbol)) {
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

private fun IrConstructorCall.isStableMarker(): Boolean =
    annotationClass?.owner?.hasAnnotation(ComposeFqNames.StableMarker) == true

private fun IrClass.hasStableMarkedDescendant(): Boolean {
    if (hasStableMarker()) return true
    return superTypes.any {
        !it.isAny() && it.classOrNull?.owner?.hasStableMarkedDescendant() == true
    }
}

private fun IrClass.isProtobufType(): Boolean {
    // Quick exit as all protos are final
    if (!isFinalClass) return false
    val directParentClassName =
        superTypes.lastOrNull { !it.isInterface() }
            ?.classOrNull?.owner?.fqNameWhenAvailable?.toString()
    return directParentClassName == "com.google.protobuf.GeneratedMessageLite" ||
        directParentClassName == "com.google.protobuf.GeneratedMessage"
}

private fun IrAnnotationContainer.stabilityParamBitmask(): Int? =
    (annotations.findAnnotation(ComposeFqNames.StabilityInferred)
        ?.getValueArgument(0) as? IrConst<*>
        )?.value as? Int

// TODO: FunctionReference
private val stableBuiltinTypes = mapOf(
    "kotlin.Pair" to 0b11,
    "kotlin.Triple" to 0b111,
    "kotlin.Comparator" to 0,
    "kotlin.Result" to 0b1,
    "kotlin.ranges.ClosedRange" to 0b1,
    "kotlin.ranges.ClosedFloatingPointRange" to 0b1,
    // Guava
    "com.google.common.collect.ImmutableList" to 0b1,
    "com.google.common.collect.ImmutableEnumMap" to 0b11,
    "com.google.common.collect.ImmutableMap" to 0b11,
    "com.google.common.collect.ImmutableEnumSet" to 0b1,
    "com.google.common.collect.ImmutableSet" to 0b1,
    // Kotlinx immutable
    "kotlinx.collections.immutable.ImmutableList" to 0b1,
    "kotlinx.collections.immutable.ImmutableSet" to 0b1,
    "kotlinx.collections.immutable.ImmutableMap" to 0b11,
    // Dagger
    "dagger.Lazy" to 0b1,
)

// TODO: buildList, buildMap, buildSet, etc.
private val stableProducingFunctions = mapOf(
    "kotlin.collections.CollectionsKt.emptyList" to 0,
    "kotlin.collections.CollectionsKt.listOf" to 0b1,
    "kotlin.collections.CollectionsKt.listOfNotNull" to 0b1,
    "kotlin.collections.MapsKt.mapOf" to 0b11,
    "kotlin.collections.MapsKt.emptyMap" to 0,
    "kotlin.collections.SetsKt.setOf" to 0b1,
    "kotlin.collections.SetsKt.emptySet" to 0,
)

fun stabilityOf(irType: IrType): Stability =
    stabilityOf(irType, emptyMap(), emptySet())

private fun stabilityOf(
    declaration: IrClass,
    substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
    currentlyAnalyzing: Set<IrClassifierSymbol>
): Stability {
    val symbol = declaration.symbol
    if (currentlyAnalyzing.contains(symbol)) return Stability.Unstable
    if (declaration.hasStableMarkedDescendant()) return Stability.Stable
    if (declaration.isEnumClass || declaration.isEnumEntry) return Stability.Stable
    if (declaration.defaultType.isPrimitiveType()) return Stability.Stable
    if (declaration.isProtobufType()) return Stability.Stable

    if (declaration.origin == IrDeclarationOrigin.IR_BUILTINS_STUB) {
        error("Builtins Stub: ${declaration.name}")
    }

    val analyzing = currentlyAnalyzing + symbol

    if (canInferStability(declaration)) {
        val fqName = declaration.fqNameWhenAvailable?.toString() ?: ""
        val stability: Stability
        val mask: Int
        if (stableBuiltinTypes.contains(fqName)) {
            mask = stableBuiltinTypes[fqName] ?: 0
            stability = Stability.Stable
        } else {
            mask = declaration.stabilityParamBitmask() ?: return Stability.Unstable
            stability = Stability.Runtime(declaration)
        }
        return when (mask) {
            0 -> stability
            else -> stability + Stability.Combined(
                declaration.typeParameters.mapIndexedNotNull { index, irTypeParameter ->
                    if (mask and (0b1 shl index) != 0) {
                        val sub = substitutions[irTypeParameter.symbol]
                        if (sub != null)
                            stabilityOf(sub, substitutions, analyzing)
                        else
                            Stability.Parameter(irTypeParameter)
                    } else null
                }
            )
        }
    } else if (declaration.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB) {
        return Stability.Unstable
    }

    if (declaration.isInterface) {
        return Stability.Unknown(declaration)
    }

    var stability = Stability.Stable

    for (member in declaration.declarations) {
        when (member) {
            is IrProperty -> {
                member.backingField?.let {
                    if (member.isVar && !member.isDelegated) return Stability.Unstable
                    stability += stabilityOf(it.type, substitutions, analyzing)
                }
            }
            is IrField -> {
                stability += stabilityOf(member.type, substitutions, analyzing)
            }
        }
    }

    return stability
}

private fun canInferStability(declaration: IrClass): Boolean {
    val fqName = declaration.fqNameWhenAvailable?.toString() ?: ""
    return stableBuiltinTypes.contains(fqName) ||
        declaration.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
}

private fun stabilityOf(
    classifier: IrClassifierSymbol,
    substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
    currentlyAnalyzing: Set<IrClassifierSymbol>
): Stability {
    // if isEnum, return true
    // class hasStableAnnotation()
    return when (val owner = classifier.owner) {
        is IrClass -> stabilityOf(owner, substitutions, currentlyAnalyzing)
        is IrTypeParameter -> Stability.Unstable
        else -> error("Unexpected IrClassifier: $owner")
    }
}

private fun stabilityOf(
    argument: IrTypeArgument,
    substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
    currentlyAnalyzing: Set<IrClassifierSymbol>
): Stability {
    return when (argument) {
        is IrStarProjection -> Stability.Unstable
        is IrTypeProjection -> stabilityOf(argument.type, substitutions, currentlyAnalyzing)
        else -> error("Unexpected IrTypeArgument: $argument")
    }
}

private fun stabilityOf(
    type: IrType,
    substitutions: Map<IrTypeParameterSymbol, IrTypeArgument>,
    currentlyAnalyzing: Set<IrClassifierSymbol>
): Stability {
    return when {
        type is IrErrorType -> Stability.Unstable
        type is IrDynamicType -> Stability.Unstable

        type.isUnit() ||
            type.isPrimitiveType() ||
            type.isFunctionOrKFunction() ||
            type.isString() -> Stability.Stable

        type.isTypeParameter() -> {
            val arg = substitutions[type.classifierOrNull as IrTypeParameterSymbol]
            if (arg != null) {
                stabilityOf(arg, substitutions, currentlyAnalyzing)
            } else {
                Stability.Parameter(
                    type.classifierOrFail.owner as IrTypeParameter
                )
            }
        }

        type.isNullable() -> stabilityOf(
            type.makeNotNull(),
            substitutions,
            currentlyAnalyzing
        )
        type.isInlineClassType() -> stabilityOf(
            type.getInlinedClass()!!,
            substitutions,
            currentlyAnalyzing
        )
        type is IrSimpleType -> {
            stabilityOf(
                type.classifier,
                substitutions + type.substitutionMap(),
                currentlyAnalyzing
            )
        }
        type is IrTypeAbbreviation -> {
            val aliased = type.typeAlias.owner.expandedType
            // TODO(lmr): figure out how type.arguments plays in here
            stabilityOf(aliased, substitutions, currentlyAnalyzing)
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

private fun stabilityOf(expr: IrCall, baseStability: Stability): Stability {
    val function = expr.symbol.owner
    val fqName = function.fqNameForIrSerialization

    return when (val mask = stableProducingFunctions[fqName.asString()]) {
        null -> baseStability
        0 -> Stability.Stable
        else -> Stability.Combined(
            (0 until expr.typeArgumentsCount).mapNotNull { index ->
                if (mask and (0b1 shl index) != 0) {
                    val sub = expr.getTypeArgument(index)
                    if (sub != null)
                        stabilityOf(sub)
                    else
                        Stability.Unstable
                } else null
            }
        )
    }
}

fun stabilityOf(expr: IrExpression): Stability {
    // look at type first. if type is stable, whole expression is
    val stability = stabilityOf(expr.type)
    if (stability.knownStable()) return stability
    return when (expr) {
        is IrConst<*> -> Stability.Stable
        is IrCall -> stabilityOf(expr, stability)
        is IrGetValue -> {
            val owner = expr.symbol.owner
            if (owner is IrVariable && !owner.isVar) {
                owner.initializer?.let { stabilityOf(it) } ?: stability
            } else {
                stability
            }
        }
        // some default parameters and consts can be wrapped in composite
        is IrComposite -> {
            if (expr.statements.all { it is IrExpression && stabilityOf(it).knownStable() }) {
                Stability.Stable
            } else {
                stability
            }
        }
        else -> stability
    }
}

private fun IrType.getInlinedClass(): IrClass? {
    val erased = erase(this) ?: return null
    if (this is IrSimpleType && isInlineClassType()) {
        val fieldType = getInlineClassUnderlyingType(erased)
        return fieldType.getInlinedClass()
    }
    return erased
}

// From Kotin's InlineClasses.kt
private tailrec fun erase(type: IrType): IrClass? =
    when (val classifier = type.classifierOrFail) {
        is IrClassSymbol -> classifier.owner
        is IrScriptSymbol -> null // TODO: check if correct
        is IrTypeParameterSymbol -> erase(classifier.owner.superTypes.first())
        else -> error(classifier)
    }
