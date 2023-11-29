/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION_ERROR") // flags will become internal eventually
@file:JvmName("Attributes")

package kotlin.metadata

import kotlin.metadata.internal.*
import kotlin.metadata.internal.BooleanFlagDelegate
import kotlin.metadata.internal.EnumFlagDelegate
import kotlin.metadata.internal.classBooleanFlag
import kotlin.metadata.internal.constructorBooleanFlag
import kotlin.contracts.ExperimentalContracts
import org.jetbrains.kotlin.metadata.deserialization.Flags as ProtoFlags

// --- ANNOTATIONS ---

/**
 * Indicates that the corresponding class has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a class has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmClass.hasAnnotations: Boolean by annotationsOn(KmClass::flags)

/**
 * Indicates that the corresponding constructor has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a constructor has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmConstructor.hasAnnotations: Boolean by annotationsOn(KmConstructor::flags)

/**
 * Indicates that the corresponding function has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a function has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmFunction.hasAnnotations: Boolean by annotationsOn(KmFunction::flags)

/**
 * Indicates that the corresponding property has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a property has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmProperty.hasAnnotations: Boolean by annotationsOn(KmProperty::flags)

/**
 * Indicates that the corresponding property accessor has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a property accessor has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmPropertyAccessorAttributes.hasAnnotations: Boolean by annotationsOn(KmPropertyAccessorAttributes::flags)

/**
 * Indicates that the corresponding value parameter has at least one annotation.
 *
 * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
 * metadata, but directly to the corresponding declarations in the class file. This flag can be used as an optimization to avoid
 * reading annotations from the class file (which can be slow) in case when a value parameter has no annotations.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files.
 */
public var KmValueParameter.hasAnnotations: Boolean by annotationsOn(KmValueParameter::flags)

/**
 * Indicates that the corresponding type alias has at least one annotation.
 *
 * Type aliases store their annotation in metadata directly (accessible via [KmTypeAlias.annotations]) and
 * in the class file at the same time.
 * As a result, Kotlin compiler still writes this flag for them, and this extension is left for completeness.
 *
 * Only annotations with [AnnotationRetention.BINARY] and [AnnotationRetention.RUNTIME] are written to the class files and metadata.
 */
public var KmTypeAlias.hasAnnotations: Boolean by annotationsOn(KmTypeAlias::flags)

// KmType and KmTypeParameter have annotations in it, and this flag for them is not written

// --- CLASS ---

/**
 * Represents modality of the corresponding class.
 *
 * Modality determines when and where it is possible to extend/implement a class/interface.
 */
public var KmClass.modality: Modality by modalityDelegate(KmClass::flags)

/**
 * Represents visibility of the corresponding class.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmClass.visibility: Visibility by visibilityDelegate(KmClass::flags)

/**
 * Represents kind of the corresponding class — whether it is a regular class or an interface, companion object, et cetera.
 */
public var KmClass.kind: ClassKind by EnumFlagDelegate(
    KmClass::flags,
    ProtoFlags.CLASS_KIND,
    ClassKind.entries,
    ClassKind.entries.map { it.flag },
)

/**
 * Indicates that the corresponding class is `inner`.
 */
public var KmClass.isInner: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_INNER))

/**
 * Indicates that the corresponding `class` or `object` is `data`.
 * Always false for other kinds.
 */
public var KmClass.isData: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_DATA))

/**
 * Indicates that the corresponding class is `external`.
 */
public var KmClass.isExternal: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_CLASS))

/**
 * Indicates that the corresponding class is `expect`.
 */
public var KmClass.isExpect: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_EXPECT_CLASS))

/**
 * Indicates that the corresponding class is either a pre-Kotlin-1.5 `inline` class, or a 1.5+ `value` class.
 *
 * Note that it does not imply that the class has [JvmInline] annotation and will be inlined.
 * Currently, it is impossible to declare a value class without this annotation, but this can be changed in the future.
 */
public var KmClass.isValue: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_VALUE_CLASS))

/**
 * Indicates that the corresponding class is a functional interface, i.e., marked with the keyword `fun`.
 *
 * Always `false` if [KmClass.kind] is not an interface.
 */
public var KmClass.isFunInterface: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.IS_FUN_INTERFACE))

/**
 * Indicates that the corresponding enum class has synthetic ".entries" property in bytecode.
 *
 * Always `false` if [KmClass.kind] is not an enum.
 * Enum classes always have enum entries property starting from Kotlin 1.9.0.
 */
public var KmClass.hasEnumEntries: Boolean by classBooleanFlag(FlagImpl(ProtoFlags.HAS_ENUM_ENTRIES))

// --- CONSTRUCTOR ---

/**
 * Represents visibility of the corresponding constructor.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmConstructor.visibility: Visibility by visibilityDelegate(KmConstructor::flags)

/**
 * Indicates that the corresponding constructor is secondary, i.e., declared not in the class header, but in the class body.
 */
public var KmConstructor.isSecondary: Boolean by constructorBooleanFlag(FlagImpl(ProtoFlags.IS_SECONDARY))

/**
 * Indicates that the corresponding constructor has non-stable parameter names, i.e., cannot be called with named arguments.
 *
 * Currently, this attribute is Kotlin/Native-specific and is never set by Kotlin/JVM compiler.
 * This may be changed in the future.
 */
public var KmConstructor.hasNonStableParameterNames: Boolean by constructorBooleanFlag(FlagImpl(ProtoFlags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES))

// --- FUNCTION ---

/**
 * Represents kind of the corresponding function.
 *
 * Kind indicates the origin of a declaration within a containing class. For details, see [MemberKind].
 */
public var KmFunction.kind: MemberKind by memberKindDelegate(KmFunction::flags)

/**
 * Represents visibility of the corresponding function.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmFunction.visibility: Visibility by visibilityDelegate(KmFunction::flags)

/**
 * Represents modality of the corresponding function.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [KmFunction] and setting it as a value results in undefined behavior.
 */
public var KmFunction.modality: Modality by modalityDelegate(KmFunction::flags)

/**
 * Indicates that the corresponding function is `operator`.
 */
public var KmFunction.isOperator: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_OPERATOR))

/**
 * Indicates that the corresponding function is `infix`.
 */
public var KmFunction.isInfix: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_INFIX))

/**
 * Indicates that the corresponding function is `inline`.
 */
public var KmFunction.isInline: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_INLINE))

/**
 * Indicates that the corresponding function is `tailrec`.
 */
public var KmFunction.isTailrec: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_TAILREC))

/**
 * Indicates that the corresponding function is `external`.
 */
public var KmFunction.isExternal: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_FUNCTION))

/**
 * Indicates that the corresponding function is `suspend`.
 */
public var KmFunction.isSuspend: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_SUSPEND))

/**
 * Indicates that the corresponding function is `expect`.
 */
public var KmFunction.isExpect: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_EXPECT_FUNCTION))

/**
 * Indicates that the corresponding function has non-stable parameter names, i.e., cannot be called with named arguments.
 *
 * Currently, this attribute is Kotlin/Native-specific and is never set by Kotlin/JVM compiler.
 * This may be changed in the future.
 */
public var KmFunction.hasNonStableParameterNames: Boolean by functionBooleanFlag(FlagImpl(ProtoFlags.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES))

// --- PROPERTY ---

/**
 * Represents visibility of the corresponding property.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmProperty.visibility: Visibility by visibilityDelegate(KmProperty::flags)

/**
 * Represents modality of the corresponding property.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [KmProperty] and setting it as a value results in undefined behavior.
 */
public var KmProperty.modality: Modality by modalityDelegate(KmProperty::flags)

/**
 * Represents kind of the corresponding property.
 *
 * Kind indicates the origin of a declaration within a containing class. For details, see [MemberKind].
 */
public var KmProperty.kind: MemberKind by memberKindDelegate(KmProperty::flags)

/**
 * Indicates that the corresponding property is `var`.
 *
 * Note that setting [KmProperty.isVar] to true does not automatically create [KmProperty.setter] and vice versa. This has to be done explicitly.
 */
public var KmProperty.isVar: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_VAR))

/**
 * Indicates that the corresponding property has a getter.
 */
@Deprecated("Kotlin properties always have getters", ReplaceWith("true"), DeprecationLevel.ERROR)
public var KmProperty.hasGetter: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.HAS_GETTER))

/**
 * Indicates that the corresponding property has a setter.
 */
@Deprecated("Check .setter for nullability instead", ReplaceWith("this.setter != null"), DeprecationLevel.ERROR)
public var KmProperty.hasSetter: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.HAS_SETTER))

/**
 * Indicates that the corresponding property is `const`.
 */
public var KmProperty.isConst: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_CONST))

/**
 * Indicates that the corresponding property is `lateinit`.
 */
public var KmProperty.isLateinit: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_LATEINIT))

/**
 * Indicates that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
 * [KmProperty.hasAnnotations]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
 * reading the value from the bytecode in case there isn't one.
 *
 * Not to be confused with [KmProperty.isConst], because `const` modifier is applicable only to properties on top-level and inside objects,
 * while property in a regular class can also have constant value.
 * Whether the property has a constant value is ultimately decided by the compiler and its optimizations.
 * Generally, a property initializer that can be computed in compile time is likely to have a constant value written to the bytecode.
 * In the following example, properties `a` and `b` have `hasConstant = true`, while `c` has `hasConstant = false`:
 *
 * ```
 * class X {
 *   val a = 1
 *   val b = a
 *
 *   fun x() = 2
 *   val c = x()
 * }
 * ```
 */
public var KmProperty.hasConstant: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.HAS_CONSTANT))

/**
 * Indicates that the corresponding property is `external`.
 */
public var KmProperty.isExternal: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_PROPERTY))

/**
 * Indicates that the corresponding property is a delegated property.
 *
 * Not to be confused with interface delegation.
 * If a property was produced by interface delegation, it would have the corresponding [KmProperty.kind].
 */
public var KmProperty.isDelegated: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_DELEGATED))

/**
 * Indicates that the corresponding property is `expect`.
 */
public var KmProperty.isExpect: Boolean by propertyBooleanFlag(FlagImpl(ProtoFlags.IS_EXPECT_PROPERTY))

// --- PROPERTY ACCESSOR ---

/**
 * Represents visibility of the corresponding property accessor.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmPropertyAccessorAttributes.visibility: Visibility by visibilityDelegate(KmPropertyAccessorAttributes::flags)

/**
 * Represents modality of the corresponding property accessor.
 *
 * Modality determines when and where it is possible or mandatory to override a declaration.
 * For additional details, see [Modality].
 *
 * [Modality.SEALED] is not applicable for [KmPropertyAccessorAttributes] and setting it as a value results in undefined behavior.
 */
public var KmPropertyAccessorAttributes.modality: Modality by modalityDelegate(KmPropertyAccessorAttributes::flags)

/**
 * Indicates that the corresponding property accessor is not default, i.e. it has a body and/or annotations in the source code,
 * or the property is delegated.
 */
public var KmPropertyAccessorAttributes.isNotDefault: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_NOT_DEFAULT))

/**
 * Indicates that the corresponding property accessor is `external`.
 */
public var KmPropertyAccessorAttributes.isExternal: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_EXTERNAL_ACCESSOR))

/**
 * Indicates that the corresponding property accessor is `inline`.
 */
public var KmPropertyAccessorAttributes.isInline: Boolean by propertyAccessorBooleanFlag(FlagImpl(ProtoFlags.IS_INLINE_ACCESSOR))

// --- TYPE & TYPE_PARAM

/**
 * Indicates that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
 */
public var KmType.isNullable: Boolean by typeBooleanFlag(FlagImpl(0, 1, 1))

/**
 * Indicates that the corresponding type is `suspend`.
 */
public var KmType.isSuspend: Boolean by typeBooleanFlag(FlagImpl(ProtoFlags.SUSPEND_TYPE.offset + 1, ProtoFlags.SUSPEND_TYPE.bitWidth, 1))

/**
 * Indicates that the corresponding type is [definitely non-null](https://kotlinlang.org/docs/whatsnew17.html#stable-definitely-non-nullable-types).
 */
public var KmType.isDefinitelyNonNull: Boolean by typeBooleanFlag(
    FlagImpl(
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.offset + 1,
        ProtoFlags.DEFINITELY_NOT_NULL_TYPE.bitWidth,
        1
    )
)


/**
 * Indicates that the corresponding type parameter is `reified`.
 */
public var KmTypeParameter.isReified: Boolean by BooleanFlagDelegate(KmTypeParameter::flags, FlagImpl(0, 1, 1))

// --- TYPE ALIAS ---

/**
 * Represents visibility of the corresponding type alias.
 *
 * Note that Kotlin metadata has an extended list of visibilities; some of them are non-denotable.
 * For additional details, see [Visibility].
 */
public var KmTypeAlias.visibility: Visibility by visibilityDelegate(KmTypeAlias::flags)


// --- VALUE PARAMETER ---


/**
 * Indicates that the corresponding value parameter declares a default value. Note that the default value itself can be a complex
 * expression and is not available via metadata. Also note that in case of an override of a parameter with default value, the
 * parameter in the derived method does _not_ declare the default value, but the parameter is
 * still optional at the call site because the default value from the base method is used.
 */
public var KmValueParameter.declaresDefaultValue: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.DECLARES_DEFAULT_VALUE))

/**
 * Indicates that the corresponding value parameter is `crossinline`.
 */
public var KmValueParameter.isCrossinline: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.IS_CROSSINLINE))

/**
 * Indicates that the corresponding value parameter is `noinline`.
 */
public var KmValueParameter.isNoinline: Boolean by valueParameterBooleanFlag(FlagImpl(ProtoFlags.IS_NOINLINE))

// --- EFFECT EXPRESSION ---

/**
 * Indicates that the corresponding effect expression should be negated to compute the proposition or the conclusion of an effect.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
public var KmEffectExpression.isNegated: Boolean by BooleanFlagDelegate(KmEffectExpression::flags, FlagImpl(ProtoFlags.IS_NEGATED))

/**
 * Indicates that the corresponding effect expression checks whether a value of some variable is `null`.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
public var KmEffectExpression.isNullCheckPredicate: Boolean by BooleanFlagDelegate(KmEffectExpression::flags, FlagImpl(ProtoFlags.IS_NULL_CHECK_PREDICATE))
