/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import kotlinx.metadata.internal.IgnoreInApiDump
import org.jetbrains.kotlin.metadata.ProtoBuf.*
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Kind as ClassKind
import org.jetbrains.kotlin.metadata.deserialization.Flags as F
import org.jetbrains.kotlin.metadata.ProtoBuf.Modality as ProtoModality
import org.jetbrains.kotlin.metadata.ProtoBuf.Visibility as ProtoVisibility
import org.jetbrains.kotlin.metadata.ProtoBuf.MemberKind as ProtoMemberKind

private const val prefix = "Flag API is deprecated. Please use"

/**
 * Represents a boolean flag that is either present or not in a Kotlin declaration. A "flag" is a boolean trait that is either present
 * or not in a declaration. To check whether the flag is present in the bitmask, call [Flag.invoke] on the flag, passing the bitmask
 * as the argument:
 *
 *     override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? {
 *         if (Flag.Function.IS_INLINE(flags)) {
 *             ...
 *         }
 *     }
 *
 * To construct a bitmask out of several flags, call [flagsOf] on the needed flags:
 *
 *     v.visitFunction(flagsOf(Flag.Function.IS_DECLARATION, Flag.Function.IS_INLINE), "foo")
 *
 * Flags common to multiple kinds of Kotlin declarations ("common flags") are declared in [Flag.Common].
 * Flags applicable to specific kinds of declarations ("declaration-specific flags") are declared in nested objects of the [Flag] object.
 *
 * Some flags are mutually exclusive, i.e. there are "flag groups" such that no more than one flag from each group can be present
 * in the same bitmask. Among common flags, there are the following flag groups:
 * * visibility flags: [IS_INTERNAL], [IS_PRIVATE], [IS_PROTECTED], [IS_PUBLIC], [IS_PRIVATE_TO_THIS], [IS_LOCAL]
 * * modality flags: [IS_FINAL], [IS_OPEN], [IS_ABSTRACT], [IS_SEALED]
 *
 * Some declaration-specific flags form other flag groups, see the documentation of the corresponding containers for more information.
 *
 * @see Flags
 * @see flagsOf
 */
@Deprecated("$prefix corresponding extensions on Km nodes, such as KmClass.visibility", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
public class Flag public constructor(internal val offset: Int, internal val bitWidth: Int, internal val value: Int) {

    // Implementation copies FlagImpl to avoid reusing of deprecated declarations.
    @IgnoreInApiDump
    internal constructor(field: F.FlagField<*>, value: Int) : this(field.offset, field.bitWidth, value)

    @IgnoreInApiDump
    internal constructor(field: F.BooleanFlagField) : this(field, 1)

    internal operator fun plus(flags: Int): Int =
        (flags and (((1 shl bitWidth) - 1) shl offset).inv()) + (value shl offset)

    /**
     * Checks whether the flag is present in the given bitmask.
     */
    public operator fun invoke(flags: Int): Boolean = (flags ushr offset) and ((1 shl bitWidth) - 1) == value

    /** @suppress deprecated */
    @Deprecated("$prefix corresponding extensions on Km nodes, such as KmClass.visibility", level = DeprecationLevel.ERROR)
    public companion object Common {
        /**
         * Signifies that the corresponding declaration has at least one annotation.
         *
         * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
         * metadata, but directly on the corresponding declarations in the class file. This flag can be used as an optimization to avoid
         * reading annotations from the class file (which can be slow) in case when a declaration has no annotations.
         */
        @JvmField
        @Deprecated("$prefix corresponding extension on a node, e.g. KmClass.hasAnnotations", level = DeprecationLevel.ERROR)
        public val HAS_ANNOTATIONS: Flag = Flag(F.HAS_ANNOTATIONS)


        /**
         * A visibility flag, signifying that the corresponding declaration is `internal`.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_INTERNAL: Flag = Flag(F.VISIBILITY, ProtoVisibility.INTERNAL_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is `private`.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_PRIVATE: Flag = Flag(F.VISIBILITY, ProtoVisibility.PRIVATE_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is `protected`.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_PROTECTED: Flag = Flag(F.VISIBILITY, ProtoVisibility.PROTECTED_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is `public`.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_PUBLIC: Flag = Flag(F.VISIBILITY, ProtoVisibility.PUBLIC_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is "private-to-this", which is a non-denotable visibility of
         * private members in Kotlin which are callable only on the same instance of the declaring class.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_PRIVATE_TO_THIS: Flag = Flag(F.VISIBILITY, ProtoVisibility.PRIVATE_TO_THIS_VALUE)

        /**
         * A visibility flag, signifying that the corresponding declaration is local, i.e. declared inside a code block
         * and not visible from the outside.
         */
        @JvmField
        @Deprecated("$prefix visibility extension on a node, e.g. KmClass.visibility", level = DeprecationLevel.ERROR)
        public val IS_LOCAL: Flag = Flag(F.VISIBILITY, ProtoVisibility.LOCAL_VALUE)


        /**
         * A modality flag, signifying that the corresponding declaration is `final`.
         */
        @JvmField
        @Deprecated("$prefix modality extension on a node, e.g. KmClass.modality or KmFunction.modality", level = DeprecationLevel.ERROR)
        public val IS_FINAL: Flag = Flag(F.MODALITY, ProtoModality.FINAL_VALUE)

        /**
         * A modality flag, signifying that the corresponding declaration is `open`.
         */
        @JvmField
        @Deprecated("$prefix modality extension on a node, e.g. KmClass.modality or KmFunction.modality", level = DeprecationLevel.ERROR)
        public val IS_OPEN: Flag = Flag(F.MODALITY, ProtoModality.OPEN_VALUE)

        /**
         * A modality flag, signifying that the corresponding declaration is `abstract`.
         */
        @JvmField
        @Deprecated("$prefix modality extension on a node, e.g. KmClass.modality or KmFunction.modality", level = DeprecationLevel.ERROR)
        public val IS_ABSTRACT: Flag = Flag(F.MODALITY, ProtoModality.ABSTRACT_VALUE)

        /**
         * A modality flag, signifying that the corresponding declaration is `sealed`.
         */
        @JvmField
        @Deprecated("$prefix modality extension on a node, e.g. KmClass.modality", level = DeprecationLevel.ERROR)
        public val IS_SEALED: Flag = Flag(F.MODALITY, ProtoModality.SEALED_VALUE)
    }

    /**
     * A container of flags applicable to Kotlin classes, including interfaces, objects, enum classes and annotation classes.
     *
     * In addition to the common flag groups, the following flag groups exist for class flags:
     * * class kind flags: [IS_CLASS], [IS_INTERFACE], [IS_ENUM_CLASS], [IS_ENUM_ENTRY], [IS_ANNOTATION_CLASS], [IS_OBJECT],
     * [IS_COMPANION_OBJECT]
     */
    @Deprecated("$prefix corresponding extension on a KmClass", level = DeprecationLevel.ERROR)
    public object Class {
        /**
         * A class kind flag, signifying that the corresponding class is a usual `class`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_CLASS: Flag = Flag(F.CLASS_KIND, ClassKind.CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `interface`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_INTERFACE: Flag = Flag(F.CLASS_KIND, ClassKind.INTERFACE_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `enum class`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_ENUM_CLASS: Flag = Flag(F.CLASS_KIND, ClassKind.ENUM_CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an enum entry.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_ENUM_ENTRY: Flag = Flag(F.CLASS_KIND, ClassKind.ENUM_ENTRY_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `annotation class`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_ANNOTATION_CLASS: Flag = Flag(F.CLASS_KIND, ClassKind.ANNOTATION_CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is a non-companion `object`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_OBJECT: Flag = Flag(F.CLASS_KIND, ClassKind.OBJECT_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is a `companion object`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.kind", level = DeprecationLevel.ERROR)
        public val IS_COMPANION_OBJECT: Flag = Flag(F.CLASS_KIND, ClassKind.COMPANION_OBJECT_VALUE)


        /**
         * Signifies that the corresponding class is `inner`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isInner", level = DeprecationLevel.ERROR)
        public val IS_INNER: Flag = Flag(F.IS_INNER)

        /**
         * Signifies that the corresponding class is `data`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isData", level = DeprecationLevel.ERROR)
        public val IS_DATA: Flag = Flag(F.IS_DATA)

        /**
         * Signifies that the corresponding class is `external`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isExternal", level = DeprecationLevel.ERROR)
        public val IS_EXTERNAL: Flag = Flag(F.IS_EXTERNAL_CLASS)

        /**
         * Signifies that the corresponding class is `expect`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isExpect", level = DeprecationLevel.ERROR)
        public val IS_EXPECT: Flag = Flag(F.IS_EXPECT_CLASS)

        @JvmField
        @Deprecated(
            "Use IS_VALUE instead, which returns true if the class is either a pre-1.5 inline class, or a 1.5+ value class.",
            level = DeprecationLevel.ERROR
        )
        @Suppress("unused")
        public val IS_INLINE: Flag = Flag(F.IS_VALUE_CLASS)

        /**
         * Signifies that the corresponding class is either a pre-Kotlin-1.5 `inline` class, or a 1.5+ `value` class.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isValue", level = DeprecationLevel.ERROR)
        public val IS_VALUE: Flag = Flag(F.IS_VALUE_CLASS)

        /**
         * Signifies that the corresponding class is a functional interface, i.e. marked with the keyword `fun`.
         */
        @JvmField
        @Deprecated("$prefix KmClass.isFun", level = DeprecationLevel.ERROR)
        public val IS_FUN: Flag = Flag(F.IS_FUN_INTERFACE)

        /**
         * Signifies that the corresponding enum class has ".entries" property in bytecode.
         * Always `false` for not enum classes.
         */
        @JvmField
        @Deprecated("$prefix KmClass.hasEnumEntries", level = DeprecationLevel.ERROR)
        public val HAS_ENUM_ENTRIES: Flag = Flag(F.HAS_ENUM_ENTRIES)
    }

    /**
     * A container of flags applicable to Kotlin constructors.
     */
    public object Constructor {
        @JvmField
        @Deprecated("Use IS_SECONDARY which holds inverted value instead.", level = DeprecationLevel.ERROR)
        @Suppress("unused")
        public val IS_PRIMARY: Flag = Flag(F.IS_SECONDARY, 0)

        /**
         * Signifies that the corresponding constructor is secondary, i.e. declared not in the class header, but in the class body.
         */
        @JvmField
        @Deprecated("$prefix KmConstructor.isSecondary", level = DeprecationLevel.ERROR)
        public val IS_SECONDARY: Flag = Flag(F.IS_SECONDARY)

        /**
         * Signifies that the corresponding constructor has non-stable parameter names, i.e. cannot be called with named arguments.
         */
        @JvmField
        @Deprecated("$prefix KmConstructor.hasNonStableParameterNames", level = DeprecationLevel.ERROR)
        public val HAS_NON_STABLE_PARAMETER_NAMES: Flag = Flag(F.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES)
    }

    /**
     * A container of flags applicable to Kotlin functions.
     *
     * In addition to the common flag groups, the following flag groups exist for function flags:
     * * member kind flags: [IS_DECLARATION], [IS_FAKE_OVERRIDE], [IS_DELEGATION], [IS_SYNTHESIZED]
     */
    public object Function {
        /**
         * A member kind flag, signifying that the corresponding function is explicitly declared in the containing class.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.kind", level = DeprecationLevel.ERROR)
        public val IS_DECLARATION: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.DECLARATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because a function with a suitable
         * signature exists in a supertype. This flag is not written by the Kotlin compiler and its effects are unspecified.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.kind", level = DeprecationLevel.ERROR)
        public val IS_FAKE_OVERRIDE: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.FAKE_OVERRIDE_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because it has been produced
         * by interface delegation (delegation "by").
         */
        @JvmField
        @Deprecated("$prefix KmFunction.kind", level = DeprecationLevel.ERROR)
        public val IS_DELEGATION: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.DELEGATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because it has been synthesized
         * by the compiler and has no declaration in the source code.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.kind", level = DeprecationLevel.ERROR)
        public val IS_SYNTHESIZED: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.SYNTHESIZED_VALUE)

        /**
         * Signifies that the corresponding function is `operator`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isOperator", level = DeprecationLevel.ERROR)
        public val IS_OPERATOR: Flag = Flag(F.IS_OPERATOR)

        /**
         * Signifies that the corresponding function is `infix`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isInfix", level = DeprecationLevel.ERROR)
        public val IS_INFIX: Flag = Flag(F.IS_INFIX)

        /**
         * Signifies that the corresponding function is `inline`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isInline", level = DeprecationLevel.ERROR)
        public val IS_INLINE: Flag = Flag(F.IS_INLINE)

        /**
         * Signifies that the corresponding function is `tailrec`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isTailrec", level = DeprecationLevel.ERROR)
        public val IS_TAILREC: Flag = Flag(F.IS_TAILREC)

        /**
         * Signifies that the corresponding function is `external`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isExternalFunction", level = DeprecationLevel.ERROR)
        public val IS_EXTERNAL: Flag = Flag(F.IS_EXTERNAL_FUNCTION)

        /**
         * Signifies that the corresponding function is `suspend`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isSuspend", level = DeprecationLevel.ERROR)
        public val IS_SUSPEND: Flag = Flag(F.IS_SUSPEND)

        /**
         * Signifies that the corresponding function is `expect`.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isExpectFunction", level = DeprecationLevel.ERROR)
        public val IS_EXPECT: Flag = Flag(F.IS_EXPECT_FUNCTION)

        /**
         * Signifies that the corresponding function has non-stable parameter names, i.e. cannot be called with named arguments.
         */
        @JvmField
        @Deprecated("$prefix KmFunction.isFunctionWithNonStableParameterNames", level = DeprecationLevel.ERROR)
        public val HAS_NON_STABLE_PARAMETER_NAMES: Flag = Flag(F.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES)
    }

    /**
     * A container of flags applicable to Kotlin properties.
     *
     * In addition to the common flag groups, the following flag groups exist for property flags:
     * * member kind flags: [IS_DECLARATION], [IS_FAKE_OVERRIDE], [IS_DELEGATION], [IS_SYNTHESIZED]
     */
    public object Property {
        /**
         * A member kind flag, signifying that the corresponding property is explicitly declared in the containing class.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.kind", level = DeprecationLevel.ERROR)
        public val IS_DECLARATION: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.DECLARATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because a property with a suitable
         * signature exists in a supertype. This flag is not written by the Kotlin compiler and its effects are unspecified.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.kind", level = DeprecationLevel.ERROR)
        public val IS_FAKE_OVERRIDE: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.FAKE_OVERRIDE_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because it has been produced
         * by interface delegation (delegation "by").
         */
        @JvmField
        @Deprecated("$prefix KmProperty.kind", level = DeprecationLevel.ERROR)
        public val IS_DELEGATION: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.DELEGATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because it has been synthesized
         * by the compiler and has no declaration in the source code.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.kind", level = DeprecationLevel.ERROR)
        public val IS_SYNTHESIZED: Flag = Flag(F.MEMBER_KIND, ProtoMemberKind.SYNTHESIZED_VALUE)

        /**
         * Signifies that the corresponding property is `var`.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isVar", level = DeprecationLevel.ERROR)
        public val IS_VAR: Flag = Flag(F.IS_VAR)

        /**
         * Signifies that the corresponding property has a getter.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.hasGetter", level = DeprecationLevel.ERROR)
        public val HAS_GETTER: Flag = Flag(F.HAS_GETTER)

        /**
         * Signifies that the corresponding property has a setter.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.hasSetter", level = DeprecationLevel.ERROR)
        public val HAS_SETTER: Flag = Flag(F.HAS_SETTER)

        /**
         * Signifies that the corresponding property is `const`.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isConst", level = DeprecationLevel.ERROR)
        public val IS_CONST: Flag = Flag(F.IS_CONST)

        /**
         * Signifies that the corresponding property is `lateinit`.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isLateinit", level = DeprecationLevel.ERROR)
        public val IS_LATEINIT: Flag = Flag(F.IS_LATEINIT)

        /**
         * Signifies that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
         * [F.HAS_ANNOTATIONS]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
         * reading the value from the bytecode in case there isn't one.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.hasConstant", level = DeprecationLevel.ERROR)
        public val HAS_CONSTANT: Flag = Flag(F.HAS_CONSTANT)

        /**
         * Signifies that the corresponding property is `external`.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isExternal", level = DeprecationLevel.ERROR)
        public val IS_EXTERNAL: Flag = Flag(F.IS_EXTERNAL_PROPERTY)

        /**
         * Signifies that the corresponding property is a delegated property.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isDelegated", level = DeprecationLevel.ERROR)
        public val IS_DELEGATED: Flag = Flag(F.IS_DELEGATED)

        /**
         * Signifies that the corresponding property is `expect`.
         */
        @JvmField
        @Deprecated("$prefix KmProperty.isExpect", level = DeprecationLevel.ERROR)
        public val IS_EXPECT: Flag = Flag(F.IS_EXPECT_PROPERTY)
    }


    /**
     * A container of flags applicable to Kotlin property getters and setters.
     */
    public object PropertyAccessor {
        /**
         * Signifies that the corresponding property accessor is not default, i.e. it has a body and/or annotations in the source code.
         */
        @JvmField
        @Deprecated("$prefix KmPropertyAccessorAttributes.isNotDefault", level = DeprecationLevel.ERROR)
        public val IS_NOT_DEFAULT: Flag = Flag(F.IS_NOT_DEFAULT)

        /**
         * Signifies that the corresponding property accessor is `external`.
         */
        @JvmField
        @Deprecated("$prefix KmPropertyAccessorAttributes.isExternal", level = DeprecationLevel.ERROR)
        public val IS_EXTERNAL: Flag = Flag(F.IS_EXTERNAL_ACCESSOR)

        /**
         * Signifies that the corresponding property accessor is `inline`.
         */
        @JvmField
        @Deprecated("$prefix KmPropertyAccessorAttributes.isInline", level = DeprecationLevel.ERROR)
        public val IS_INLINE: Flag = Flag(F.IS_INLINE_ACCESSOR)
    }

    /**
     * A container of flags applicable to Kotlin types.
     */
    public object Type {
        /**
         * Signifies that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
         */
        @JvmField
        @Deprecated("$prefix KmType.isNullable", level = DeprecationLevel.ERROR)
        public val IS_NULLABLE: Flag = Flag(0, 1, 1)

        /**
         * Signifies that the corresponding type is `suspend`.
         */
        @JvmField
        @Deprecated("$prefix KmType.isSuspend", level = DeprecationLevel.ERROR)
        public val IS_SUSPEND: Flag = Flag(F.SUSPEND_TYPE.offset + 1, F.SUSPEND_TYPE.bitWidth, 1)

        /**
         * Signifies that the corresponding type is
         * [definitely non-null](https://kotlinlang.org/docs/whatsnew17.html#stable-definitely-non-nullable-types).
         */
        @JvmField
        @Deprecated("$prefix KmType.isDefinitelyNonNull", level = DeprecationLevel.ERROR)
        public val IS_DEFINITELY_NON_NULL: Flag = Flag(F.DEFINITELY_NOT_NULL_TYPE.offset + 1, F.DEFINITELY_NOT_NULL_TYPE.bitWidth, 1)
    }

    /**
     * A container of flags applicable to Kotlin type parameters.
     */
    public object TypeParameter {
        /**
         * Signifies that the corresponding type parameter is `reified`.
         */
        @JvmField
        @Deprecated("$prefix KmTypeParameter.isReified", level = DeprecationLevel.ERROR)
        public val IS_REIFIED: Flag = Flag(0, 1, 1)
    }

    /**
     * A container of flags applicable to Kotlin value parameters.
     */
    public object ValueParameter {
        /**
         * Signifies that the corresponding value parameter declares a default value. Note that the default value itself can be a complex
         * expression and is not available via metadata. Also note that in case of an override of a parameter with default value, the
         * parameter in the derived method does _not_ declare the default value ([DECLARES_DEFAULT_VALUE] == false), but the parameter is
         * still optional at the call site because the default value from the base method is used.
         */
        @JvmField
        @Deprecated("$prefix KmValueParameter.declaresDefaultValue", level = DeprecationLevel.ERROR)
        public val DECLARES_DEFAULT_VALUE: Flag = Flag(F.DECLARES_DEFAULT_VALUE)

        /**
         * Signifies that the corresponding value parameter is `crossinline`.
         */
        @JvmField
        @Deprecated("$prefix KmValueParameter.isCrossinline", level = DeprecationLevel.ERROR)
        public val IS_CROSSINLINE: Flag = Flag(F.IS_CROSSINLINE)

        /**
         * Signifies that the corresponding value parameter is `noinline`.
         */
        @JvmField
        @Deprecated("$prefix KmValueParameter.isNoinline", level = DeprecationLevel.ERROR)
        public val IS_NOINLINE: Flag = Flag(F.IS_NOINLINE)
    }

    /**
     * A container of flags applicable to Kotlin effect expressions.
     *
     * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
     * may change in a subsequent release.
     */
    public object EffectExpression {
        /**
         * Signifies that the corresponding effect expression should be negated to compute the proposition or the conclusion of an effect.
         */
        @JvmField
        @Deprecated("$prefix KmEffectExpression.isNegated", level = DeprecationLevel.ERROR)
        public val IS_NEGATED: Flag = Flag(F.IS_NEGATED)

        /**
         * Signifies that the corresponding effect expression checks whether a value of some variable is `null`.
         */
        @JvmField
        @Deprecated("$prefix KmEffectExpression.isNullCheckPredicate", level = DeprecationLevel.ERROR)
        public val IS_NULL_CHECK_PREDICATE: Flag = Flag(F.IS_NULL_CHECK_PREDICATE)
    }
}
