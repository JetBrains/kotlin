// CORRECT_ERROR_TYPES
// EMIT_JVM_TYPE_ANNOTATIONS
// FULL_JDK

@file:Suppress("UNRESOLVED_REFERENCE")

package org.jetbrains

import missing.types.Placeholder as ImportedMissing

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
annotation class Anno

@Target(AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
annotation class SourceAnno

class Outer {
    class Nested<T>
}

class FinalBox<out T>
class InvariantBox<T>
class ContravariantBox<in T, U>
open class ResolvedContainer<A, B>

typealias WithMissing<T> = Map<Missing, List<T>>
typealias Reversed<A, B> = Map<B, A>
typealias SameOrder<A, B> = Map<A, B>
typealias MissingAlias<T> = Missing<T>
typealias ArrayAlias<T> = Array<List<T>>
typealias PrimitiveArrayAlias = IntArray
typealias Chained<A, B> = Reversed<B, List<A>>
typealias Duplicated<A> = Map<A, A>
typealias MissingArray<A> = Array<Map<Missing, A>>
typealias ArrayOf<A> = Array<A>
typealias AliasFunction<A, B> = (B) -> A
typealias AnnotatedAlias<A> = @Anno Map<Missing, @SourceAnno List<A>>
typealias ChainedMissing<A> = MissingAlias<List<A>>
typealias WithGenericMissing<A> = Map<Missing<A>, A>
typealias AliasSuspend<A, B> = suspend (B) -> List<A>
typealias WithImportedMissing<A> = Map<ImportedMissing, A>

class FieldTypeAnnotationsErrorTypes<T> {
    @JvmField
    val resolvedRoot: @Anno @SourceAnno List<Missing>? = null

    @JvmField
    val resolvedContainer: List<@Anno @SourceAnno List<Missing>>? = null

    @JvmField
    val missingFirst: Map<Missing, @Anno @SourceAnno String>? = null

    @JvmField
    val missingLast: Map<@Anno @SourceAnno String, Missing>? = null

    @JvmField
    val typeVariable: Map<Missing, @Anno @SourceAnno T>? = null

    @JvmField
    val wildcardUpperBound: MutableList<out @Anno @SourceAnno List<Missing>>? = null

    @JvmField
    var wildcardLowerBound: MutableList<in @Anno @SourceAnno List<Missing>>? = null

    @JvmField
    val forcedWildcard: Map<Missing, @JvmWildcard @Anno @SourceAnno Number>? = null

    // Annotation paths follow the expansion, including wildcard modes on alias arguments.
    @JvmField
    val aliasForcedWildcard: SameOrder<Missing, @Anno @SourceAnno @JvmWildcard Number>? = null

    @JvmField
    var aliasSuppressedWildcard: SameOrder<Missing, @Anno @SourceAnno @JvmSuppressWildcards Number>? = null

    @JvmField
    var aliasNestedWildcard: SameOrder<Missing, List<Number>>? = null

    // @JvmWildcard affects this argument, but must restore the val field's mode inside List.
    @JvmField
    val aliasForcedNestedWildcard: SameOrder<Missing, @JvmWildcard List<Number>>? = null

    @JvmField
    var aliasSuppressedNestedWildcard: SameOrder<Missing, @JvmSuppressWildcards List<Number>>? = null

    @JvmField
    var suppressedWildcard: Map<Missing, @JvmSuppressWildcards @Anno @SourceAnno Number>? = null

    @JvmField
    @JvmSuppressWildcards
    var declarationSuppressedWildcard: Map<Missing, @Anno @SourceAnno Number>? = null

    @JvmField
    @JvmSuppressWildcards(false)
    val declarationForcedWildcard: Map<Missing, @Anno @SourceAnno Number>? = null

    @JvmField
    val nestedWildcardBound: MutableList<in Map<Missing, @Anno @SourceAnno Number>>? = null

    @JvmField
    val boxedPrimitive: Map<Missing, @Anno @SourceAnno Int>? = null

    @JvmField
    val starProjection: Map<Missing, @Anno @SourceAnno List<*>>? = null

    @JvmField
    val contravariantNothing: @Anno @SourceAnno ContravariantBox<Nothing, Missing>? = null

    // The backend must produce the same unbounded wildcard without error-type correction.
    @JvmField
    val resolvedContravariantNothing: @Anno @SourceAnno ContravariantBox<Nothing, String>? = null

    @JvmField
    var finalContainer: List<@Anno @SourceAnno FinalBox<Missing>>? = null

    @JvmField
    var invariantContainer: List<@Anno @SourceAnno InvariantBox<Missing>>? = null

    @JvmField
    var resolvedFinalContainer: List<@Anno @SourceAnno FinalBox<String>>? = null

    @JvmField
    var resolvedOpenContainer: List<@Anno @SourceAnno FinalBox<Number>>? = null

    // Cover outer/inner array dimensions and the element type with distinguishable annotation positions.
    @JvmField
    val arrayDimensions: @Anno Array<@SourceAnno Array<@Anno @SourceAnno List<Missing>>>? = null

    @JvmField
    val mixedTypePath: @Anno Array<@SourceAnno MutableMap<Missing, out @Anno @SourceAnno List<Missing>>>? = null

    @JvmField
    var outArray: @Anno Array<out @SourceAnno List<Missing>>? = null

    @JvmField
    val inArray: @Anno @SourceAnno Array<in @Anno @SourceAnno List<Missing>>? = null

    @JvmField
    val starArray: Map<Missing, @Anno @SourceAnno Array<*>>? = null

    @JvmField
    val boxedArray: Map<Missing, @Anno Array<@SourceAnno Int>>? = null

    @JvmField
    val primitiveArray: Map<Missing, @Anno @SourceAnno IntArray>? = null

    @JvmField
    val nestedPrimitiveArray: Map<Missing, @Anno Array<@SourceAnno IntArray>>? = null

    @JvmField
    val typeVariableArray: Map<Missing, @Anno Array<@SourceAnno T>>? = null

    @JvmField
    val wildcardArray: MutableList<out @Anno Array<@SourceAnno List<Missing>>>? = null

    // Fully resolved aliases must retain their expanded structure inside an otherwise erroneous type.
    @JvmField
    val arrayAlias: Map<Missing, @Anno @SourceAnno ArrayAlias<@Anno @SourceAnno String>>? = null

    @JvmField
    val nestedArrayAlias: Map<Missing, @Anno Array<@SourceAnno ArrayAlias<@Anno @SourceAnno String>>>? = null

    @JvmField
    val primitiveArrayAlias: Map<Missing, @Anno @SourceAnno PrimitiveArrayAlias>? = null

    // Compare reconstruction with ordinary backend mapping of fully resolved array types.
    @JvmField
    var resolvedOutArray: @Anno Array<out @SourceAnno List<Number>>? = null

    @JvmField
    val resolvedInArray: @Anno @SourceAnno Array<in @Anno @SourceAnno List<Number>>? = null

    @JvmField
    val resolvedArrayDimensions: @Anno Array<@SourceAnno Array<@Anno @SourceAnno List<Number>>>? = null

    @JvmField
    val nestedClass: @Anno @SourceAnno Outer.Nested<Missing>? = null

    @JvmField
    val function: @Anno @SourceAnno (Missing) -> String = TODO()

    @JvmField
    val functionParameter: (@Anno @SourceAnno List<Missing>) -> String = TODO()

    @JvmField
    val functionReturn: (Missing) -> @Anno @SourceAnno List<String> = TODO()

    // Context parameters precede both the extension receiver and ordinary parameters in Function4.
    // Distinct annotations make component cursor mix-ups observable.
    @JvmField
    val functionContextParameters:
            context(@Anno List<Missing>, @SourceAnno String) (@Anno Number).(@SourceAnno Int) -> @Anno @SourceAnno T = TODO()

    @JvmField
    var functionWildcards: (@Anno List<Missing>) -> @SourceAnno List<Number> = TODO()

    // Explicit ordinary function interfaces preserve stars, unlike suspend function lowering.
    @JvmField
    val explicitFunctionStarInput: @Anno Function2<*, Missing, @SourceAnno List<@Anno Number>> = TODO()

    @JvmField
    var explicitFunctionStarReturn: @Anno Function1<@SourceAnno List<Missing>, *> = TODO()

    @JvmField
    val nestedFunction: (Missing) -> @Anno ((@SourceAnno Number) -> @Anno T) = TODO()

    @JvmField
    val functionArray: (@Anno Array<@SourceAnno List<Missing>>) -> @SourceAnno Int = TODO()

    @JvmField
    val functionNoParameters: @Anno @SourceAnno () -> List<Missing> = TODO()

    @JvmField
    val functionNothingReturn: @Anno @SourceAnno (Missing) -> Nothing = TODO()

    @JvmField
    val function22: @Anno (
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, @SourceAnno Number,
    ) -> @Anno T = TODO()

    @JvmField
    val suspendFunction: @Anno @SourceAnno suspend (@Anno Number, Missing) -> @SourceAnno List<String> = TODO()

    // Moving R under Continuation changes wildcard mapping, including inside its nested arguments.
    @JvmField
    val suspendReturn: suspend (Missing) -> @Anno @SourceAnno List<@Anno @SourceAnno Number> = TODO()

    @JvmField
    var suspendWildcards: suspend (@Anno List<Missing>) -> @SourceAnno List<@Anno Number> = TODO()

    @JvmField
    val suspendContextParameters:
            suspend context(@Anno List<Missing>, @SourceAnno String) (@Anno Number).(@SourceAnno Int) -> @Anno @SourceAnno T = TODO()

    @JvmField
    val nestedSuspendFunction: (Missing) -> @Anno suspend () -> @SourceAnno Array<@Anno T> = TODO()

    // The continuation is the 22nd input: this still uses a numbered function interface.
    @JvmField
    val suspendFunction22: @Anno suspend (
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, @SourceAnno Number,
    ) -> @Anno @SourceAnno T = TODO()

    // With 22 original inputs, neither the inputs nor R survive in FunctionN<Object>.
    @JvmField
    val largeSuspendFunction: @Anno @SourceAnno suspend (
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @Anno @SourceAnno List<@Anno @SourceAnno Number> = TODO()

    @JvmField
    val suspendNothingReturn: @Anno @SourceAnno suspend (Missing) -> @Anno @SourceAnno Nothing = TODO()

    @JvmField
    val suspendErrorReturn: @Anno @SourceAnno suspend () -> @Anno List<@SourceAnno List<Missing>> = TODO()

    @JvmField
    val suspendArrayReturn: suspend (Missing) -> @Anno Array<@SourceAnno List<@Anno Number>> = TODO()

    @JvmField
    val suspendSuppressedReturn: suspend (Missing) -> @Anno @JvmSuppressWildcards List<@SourceAnno Number> = TODO()

    // Above the boundary, even a Nothing source return must not erase the synthetic Object argument.
    @JvmField
    val largeSuspendNothingReturn: @Anno @SourceAnno suspend (
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @Anno @SourceAnno Nothing = TODO()

    // Explicit suspend interfaces use the same continuation lowering as suspend function syntax.
    @JvmField
    val explicitSuspendReturn: kotlin.coroutines.SuspendFunction1<Missing, @Anno @SourceAnno List<@Anno @SourceAnno Number>> = TODO()

    @JvmField
    var explicitSuspendWildcards: kotlin.coroutines.SuspendFunction1<in @Anno List<Missing>, out @SourceAnno List<@Anno Number>> = TODO()

    @JvmField
    val explicitNestedSuspend: (Missing) -> @Anno kotlin.coroutines.SuspendFunction0<@SourceAnno Array<@Anno T>> = TODO()

    @JvmField
    val explicitSuspendNothingReturn: @Anno @SourceAnno kotlin.coroutines.SuspendFunction1<Missing, @Anno @SourceAnno Nothing> = TODO()

    @JvmField
    val explicitSuspendErrorReturn: @Anno @SourceAnno kotlin.coroutines.SuspendFunction0<@Anno List<@SourceAnno List<Missing>>> = TODO()

    @JvmField
    val explicitSuspendFunction22: @Anno kotlin.coroutines.SuspendFunction21<
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, @SourceAnno Number,
        @Anno @SourceAnno T,
    > = TODO()

    @JvmField
    val explicitLargeSuspendFunction: @Anno @SourceAnno kotlin.coroutines.SuspendFunction22<
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        @Anno @SourceAnno List<@Anno @SourceAnno Number>,
    > = TODO()

    @JvmField
    val explicitLargeSuspendNothingReturn: @Anno @SourceAnno kotlin.coroutines.SuspendFunction23<
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        @Anno @SourceAnno Nothing,
    > = TODO()

    // Star projections become Any? components, not wildcards on the lowered function interface.
    @JvmField
    val explicitSuspendStarInput: @Anno kotlin.coroutines.SuspendFunction2<*, Missing, @SourceAnno List<@Anno Number>> = TODO()

    @JvmField
    val suspendStarInput: @Anno suspend (Any?, Missing) -> @SourceAnno List<@Anno Number> = TODO()

    @JvmField
    var explicitSuspendStarReturn: @Anno kotlin.coroutines.SuspendFunction1<@SourceAnno List<Missing>, *> = TODO()

    @JvmField
    var suspendStarReturn: @Anno suspend (@SourceAnno List<Missing>) -> Any? = TODO()

    // Unannotated, resolved counterparts verify the actual JVM mapping independently of error correction.
    @JvmField
    val resolvedSuspendReturn: suspend (Number) -> List<Number> = TODO()

    @JvmField
    var resolvedSuspendWildcards: suspend (List<Number>) -> List<Number> = TODO()

    @JvmField
    val resolvedSuspendContextParameters: suspend context(List<Number>, String) Number.(Int) -> T = TODO()

    @JvmField
    val resolvedNestedSuspendFunction: (Number) -> suspend () -> Array<T> = TODO()

    @JvmField
    val resolvedSuspendFunction22: suspend (
        Number, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Number,
    ) -> T = TODO()

    // The existing backend maps a resolved SuspendFunction22 to FunctionN with the original arguments.
    // Verify the lowered representation instead until that separate signature-mapping bug is fixed.
    @JvmField
    val resolvedLargeSuspendFunction: Function23<
        Number, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        kotlin.coroutines.Continuation<List<Number>>, Any?,
    > = TODO()

    @JvmField
    val resolvedSuspendNothingReturn: suspend (Number) -> Nothing = TODO()

    @JvmField
    val resolvedSuspendErrorReturn: suspend () -> List<List<Number>> = TODO()

    @JvmField
    val resolvedSuspendArrayReturn: suspend (Number) -> Array<List<Number>> = TODO()

    @JvmField
    val resolvedSuspendSuppressedReturn: suspend (Number) -> @JvmSuppressWildcards List<Number> = TODO()

    @JvmField
    val largeFunction: @Anno @SourceAnno (
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @Anno @SourceAnno List<@Anno @SourceAnno String> = TODO()

    @JvmField
    var largeFunctionWildcards: @Anno @SourceAnno (
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @Anno @SourceAnno List<@Anno @SourceAnno Number> = TODO()

    // Explicit numbered interfaces must use the same arity erasure and annotation paths as function syntax.
    @JvmField
    val explicitLargeFunction: @Anno @SourceAnno Function23<
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        @Anno @SourceAnno List<@Anno @SourceAnno String>,
    > = TODO()

    @JvmField
    var explicitLargeFunctionWildcards: @Anno @SourceAnno Function23<
        Missing, @Anno @SourceAnno Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        @Anno @SourceAnno List<@Anno @SourceAnno Number>,
    > = TODO()

    @JvmField
    val explicitLargeFunctionStars: @Anno Function23<
        Missing, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *, *,
        @SourceAnno List<@Anno String>,
    > = TODO()

    // Rebase the cursor at a non-root position, retaining array steps below the return argument.
    @JvmField
    val nestedLargeFunction: Map<Missing, @Anno (
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @SourceAnno Array<@Anno T>> = TODO()

    // Nothing erases all generic arguments, so no return-argument path can be retained.
    @JvmField
    val largeFunctionNothingReturn: @Anno @SourceAnno (
        Missing, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
        Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int,
    ) -> @Anno @SourceAnno Nothing = TODO()

    @JvmField
    val aliasExpansion: @Anno WithMissing<@Anno @SourceAnno String>? = null

    @JvmField
    val reorderedAliasArguments: Reversed<@Anno @SourceAnno String, Missing>? = null

    @JvmField
    val chainedAlias: @Anno Chained<Missing, @SourceAnno Number>? = null

    @JvmField
    val duplicatedAlias: Duplicated<@Anno List<@SourceAnno List<Missing>>>? = null

    // The substitution must preserve this cursor below a wildcard, an array, and a type argument.
    @JvmField
    val nestedReorderedAlias: MutableList<out @Anno Array<@SourceAnno Reversed<@Anno T, Missing>>>? = null

    @JvmField
    val arrayArgumentAlias: @Anno MissingArray<@SourceAnno Array<@Anno T>>? = null

    @JvmField
    val substitutedArrayDimensions: @Anno ArrayOf<@SourceAnno ArrayOf<@Anno List<Missing>>>? = null

    @JvmField
    val functionAlias: @Anno AliasFunction<@SourceAnno T, Missing>? = null

    @JvmField
    val resolvedReorderedAlias: Map<Missing, @Anno Reversed<@SourceAnno String, @Anno Number>>? = null

    @JvmField
    val annotatedExpansion: AnnotatedAlias<@Anno T>? = null

    @JvmField
    val starAlias: @Anno @SourceAnno WithMissing<*>? = null

    @JvmField
    val aliasGenericMissing: @Anno WithGenericMissing<@SourceAnno String>? = null

    @JvmField
    val suspendAlias: @Anno AliasSuspend<@SourceAnno Number, Missing> = TODO()

    @JvmField
    val nestedSuspendAlias: Map<Missing, @Anno AliasSuspend<@SourceAnno Number, String>> = TODO()

    @JvmField
    val aliasImportedMissing: @Anno WithImportedMissing<@SourceAnno String>? = null

    @JvmField
    val nestedMissingAlias: Map<@Anno String, MissingAlias<@SourceAnno Number>>? = null

    // Annotations on or below missing types do not reach the backend's type annotation collector.
    @JvmField
    val missingRoot: @Anno @SourceAnno Missing? = null

    @JvmField
    val missingArgument: List<@Anno @SourceAnno Missing>? = null

    @JvmField
    val argumentOfMissingClass: Missing<@Anno @SourceAnno String>? = null

    @JvmField
    val aliasOfMissingClass: @Anno @SourceAnno MissingAlias<@Anno @SourceAnno String>? = null

    @JvmField
    val chainedMissingAlias: @Anno ChainedMissing<@SourceAnno String>? = null

    // A missing root loses its generic arguments in IR; compare reconstruction with the normal JVM mapping.
    @JvmField
    val missingRootFunction: MissingContainer<String, (List<Missing>) -> Unit>? = null

    @JvmField
    val resolvedMissingRootFunction: ResolvedContainer<String, (List<Number>) -> Unit>? = null

    @JvmField
    var mutableMissingRootFunction: MissingContainer<String, (List<Missing>) -> Unit>? = null

    @JvmField
    var resolvedMutableMissingRootFunction: ResolvedContainer<String, (List<Number>) -> Unit>? = null

    @JvmField
    @JvmSuppressWildcards(false)
    val forcedMissingRootFunction: MissingContainer<String, (List<Missing>) -> Unit>? = null

    @JvmField
    @JvmSuppressWildcards(false)
    val resolvedForcedMissingRootFunction: ResolvedContainer<String, (List<Number>) -> Unit>? = null

    @JvmField
    @JvmSuppressWildcards
    val suppressedMissingRootFunction: MissingContainer<String, (List<Missing>) -> Unit>? = null

    @JvmField
    @JvmSuppressWildcards
    val resolvedSuppressedMissingRootFunction: ResolvedContainer<String, (List<Number>) -> Unit>? = null
}

class InferredFieldTypeAnnotationsErrorTypes<T> {
    @JvmField
    val inferred = inferredType()

    @JvmField
    var mutableInferred = inferredType()

    @JvmField
    val inferredSuspend = FieldTypeAnnotationsErrorTypes<T>().suspendReturn

    @JvmField
    var inferredSuspendWildcards = FieldTypeAnnotationsErrorTypes<T>().suspendWildcards

    @JvmField
    val inferredLargeFunction = FieldTypeAnnotationsErrorTypes<T>().largeFunction

    @JvmField
    var inferredLargeFunctionWildcards = FieldTypeAnnotationsErrorTypes<T>().largeFunctionWildcards

    @JvmField
    val inferredNestedSuspend = FieldTypeAnnotationsErrorTypes<T>().nestedSuspendFunction

    @JvmField
    val inferredNestedLargeFunction = FieldTypeAnnotationsErrorTypes<T>().nestedLargeFunction

    @JvmField
    val inferredSuspendNothingReturn = FieldTypeAnnotationsErrorTypes<T>().suspendNothingReturn

    @JvmField
    val inferredLargeFunctionNothingReturn = FieldTypeAnnotationsErrorTypes<T>().largeFunctionNothingReturn

    @JvmField
    val inferredExplicitSuspendStarInput = FieldTypeAnnotationsErrorTypes<T>().explicitSuspendStarInput

    @JvmField
    var inferredExplicitSuspendStarReturn = FieldTypeAnnotationsErrorTypes<T>().explicitSuspendStarReturn
}

fun inferredType(): @Anno @SourceAnno Map<Missing, @Anno @SourceAnno List<@Anno @SourceAnno Number>> = TODO()
