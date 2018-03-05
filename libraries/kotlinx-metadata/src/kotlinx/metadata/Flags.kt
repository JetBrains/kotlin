/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import org.jetbrains.kotlin.metadata.ProtoBuf.*
import org.jetbrains.kotlin.metadata.deserialization.Flags as F
import org.jetbrains.kotlin.metadata.ProtoBuf.Class.Kind as ClassKind

/**
 * A container of all flags applicable to all Kotlin declarations. A "flag" is a boolean trait that is either present or not
 * in a declaration. To check whether the flag is present in the bitmask, call [MetadataFlag.invoke] on the flag, passing the bitmask
 * as the argument:
 *
 *     override fun visitFunction(flags: Int, name: String): KmFunctionVisitor? {
 *         if (Flags.Function.IS_INLINE(flags)) {
 *             ...
 *         }
 *     }
 *
 * To construct a bitmask out of several flags, call [Flags.invoke] on the needed flags:
 *
 *     v.visitFunction(Flags(Flags.Function.IS_DECLARATION, Flags.Function.IS_INLINE), "foo")
 *
 * Flags common to multiple kinds of Kotlin declarations ("common flags") are declared directly in the [Flags] object.
 * Flags applicable to specific kinds of declarations ("declaration-specific flags") are declared in nested objects of the [Flags] object.
 *
 * Some flags are mutually exclusive, i.e. there are "flag groups" such that no more than one flag from each group can be present
 * in the same bitmask. Among common flags, there are the following flag groups:
 * * visibility flags: [IS_INTERNAL], [IS_PRIVATE], [IS_PROTECTED], [IS_PUBLIC], [IS_PRIVATE_TO_THIS], [IS_LOCAL]
 * * modality flags: [IS_FINAL], [IS_OPEN], [IS_ABSTRACT], [IS_SEALED]
 *
 * Some declaration-specific flags form other flag groups, see the documentation of the corresponding containers for more information.
 */
object Flags {
    /**
     * Combines several flags into an integer bitmask. Note that in case several mutually exclusive flags are passed (for example,
     * several visibility flags), the resulting bitmask will hold the value of the latest flag.
     *
     * For example, `Flags(Flags.IS_PRIVATE, Flags.IS_PUBLIC, Flags.IS_INTERNAL)` is the same as `Flags(Flags.IS_INTERNAL)`
     */
    operator fun invoke(vararg flags: MetadataFlag): Int =
        flags.fold(0) { acc, flag -> flag + acc }

    /**
     * Signifies that the corresponding declaration has at least one annotation.
     *
     * This flag is useful for reading Kotlin metadata on JVM efficiently. On JVM, most of the annotations are written not to the Kotlin
     * metadata, but directly on the corresponding declarations in the class file. This flag can be used as an optimization to avoid
     * reading annotations from the class file (which can be slow) in case when a declaration has no annotations.
     */
    @JvmField
    val HAS_ANNOTATIONS = MetadataFlag(F.HAS_ANNOTATIONS)


    /**
     * A visibility flag, signifying that the corresponding declaration is `internal`.
     */
    @JvmField
    val IS_INTERNAL = MetadataFlag(F.VISIBILITY, Visibility.INTERNAL_VALUE)

    /**
     * A visibility flag, signifying that the corresponding declaration is `private`.
     */
    @JvmField
    val IS_PRIVATE = MetadataFlag(F.VISIBILITY, Visibility.PRIVATE_VALUE)

    /**
     * A visibility flag, signifying that the corresponding declaration is `protected`.
     */
    @JvmField
    val IS_PROTECTED = MetadataFlag(F.VISIBILITY, Visibility.PROTECTED_VALUE)

    /**
     * A visibility flag, signifying that the corresponding declaration is `public`.
     */
    @JvmField
    val IS_PUBLIC = MetadataFlag(F.VISIBILITY, Visibility.PUBLIC_VALUE)

    /**
     * A visibility flag, signifying that the corresponding declaration is "private-to-this", which is a non-denotable visibility of
     * private members in Kotlin which are callable only on the same instance of the declaring class.
     */
    @JvmField
    val IS_PRIVATE_TO_THIS = MetadataFlag(F.VISIBILITY, Visibility.PRIVATE_TO_THIS_VALUE)

    /**
     * A visibility flag, signifying that the corresponding declaration is local, i.e. declared inside a code block
     * and not visible from the outside.
     */
    @JvmField
    val IS_LOCAL = MetadataFlag(F.VISIBILITY, Visibility.LOCAL_VALUE)


    /**
     * A modality flag, signifying that the corresponding declaration is `final`.
     */
    @JvmField
    val IS_FINAL = MetadataFlag(F.MODALITY, Modality.FINAL_VALUE)

    /**
     * A modality flag, signifying that the corresponding declaration is `open`.
     */
    @JvmField
    val IS_OPEN = MetadataFlag(F.MODALITY, Modality.OPEN_VALUE)

    /**
     * A modality flag, signifying that the corresponding declaration is `abstract`.
     */
    @JvmField
    val IS_ABSTRACT = MetadataFlag(F.MODALITY, Modality.ABSTRACT_VALUE)

    /**
     * A modality flag, signifying that the corresponding declaration is `sealed`.
     */
    @JvmField
    val IS_SEALED = MetadataFlag(F.MODALITY, Modality.SEALED_VALUE)


    /**
     * A container of flags applicable to Kotlin classes, including interfaces, objects, enum classes and annotation classes.
     *
     * In addition to the common flag groups, the following flag groups exist for class flags:
     * * class kind flags: [IS_CLASS], [IS_INTERFACE], [IS_ENUM_CLASS], [IS_ENUM_ENTRY], [IS_ANNOTATION_CLASS], [IS_OBJECT],
     * [IS_COMPANION_OBJECT]
     */
    object Class {
        /**
         * A class kind flag, signifying that the corresponding class is a usual `class`.
         */
        @JvmField
        val IS_CLASS = MetadataFlag(F.CLASS_KIND, ClassKind.CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `interface`.
         */
        @JvmField
        val IS_INTERFACE = MetadataFlag(F.CLASS_KIND, ClassKind.INTERFACE_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `enum class`.
         */
        @JvmField
        val IS_ENUM_CLASS = MetadataFlag(F.CLASS_KIND, ClassKind.ENUM_CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an enum entry.
         */
        @JvmField
        val IS_ENUM_ENTRY = MetadataFlag(F.CLASS_KIND, ClassKind.ENUM_ENTRY_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is an `annotation class`.
         */
        @JvmField
        val IS_ANNOTATION_CLASS = MetadataFlag(F.CLASS_KIND, ClassKind.ANNOTATION_CLASS_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is a non-companion `object`.
         */
        @JvmField
        val IS_OBJECT = MetadataFlag(F.CLASS_KIND, ClassKind.OBJECT_VALUE)

        /**
         * A class kind flag, signifying that the corresponding class is a `companion object`.
         */
        @JvmField
        val IS_COMPANION_OBJECT = MetadataFlag(F.CLASS_KIND, ClassKind.COMPANION_OBJECT_VALUE)


        /**
         * Signifies that the corresponding class is `inner`.
         */
        @JvmField
        val IS_INNER = MetadataFlag(F.IS_INNER)

        /**
         * Signifies that the corresponding class is `data`.
         */
        @JvmField
        val IS_DATA = MetadataFlag(F.IS_DATA)

        /**
         * Signifies that the corresponding class is `external`.
         */
        @JvmField
        val IS_EXTERNAL = MetadataFlag(F.IS_EXTERNAL_CLASS)

        /**
         * Signifies that the corresponding class is `expect`.
         */
        @JvmField
        val IS_EXPECT = MetadataFlag(F.IS_EXPECT_CLASS)

        /**
         * Signifies that the corresponding class is `inline`.
         */
        @JvmField
        val IS_INLINE = MetadataFlag(F.IS_INLINE_CLASS)
    }

    /**
     * A container of flags applicable to Kotlin constructors.
     */
    object Constructor {
        /**
         * Signifies that the corresponding constructor is primary, i.e. declared in the class header, not in the class body.
         */
        @JvmField
        val IS_PRIMARY = MetadataFlag(F.IS_SECONDARY, 0)
    }

    /**
     * A container of flags applicable to Kotlin functions.
     *
     * In addition to the common flag groups, the following flag groups exist for function flags:
     * * member kind flags: [IS_DECLARATION], [IS_FAKE_OVERRIDE], [IS_DELEGATION], [IS_SYNTHESIZED]
     */
    object Function {
        /**
         * A member kind flag, signifying that the corresponding function is explicitly declared in the containing class.
         */
        @JvmField
        val IS_DECLARATION = MetadataFlag(F.MEMBER_KIND, MemberKind.DECLARATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because a function with a suitable
         * signature exists in a supertype. This flag is not written by the Kotlin compiler and its effects are unspecified.
         */
        @JvmField
        val IS_FAKE_OVERRIDE = MetadataFlag(F.MEMBER_KIND, MemberKind.FAKE_OVERRIDE_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because it has been produced
         * by interface delegation (delegation "by").
         */
        @JvmField
        val IS_DELEGATION = MetadataFlag(F.MEMBER_KIND, MemberKind.DELEGATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding function exists in the containing class because it has been synthesized
         * by the compiler and has no declaration in the source code.
         */
        @JvmField
        val IS_SYNTHESIZED = MetadataFlag(F.MEMBER_KIND, MemberKind.SYNTHESIZED_VALUE)


        /**
         * Signifies that the corresponding function is `operator`.
         */
        @JvmField
        val IS_OPERATOR = MetadataFlag(F.IS_OPERATOR)

        /**
         * Signifies that the corresponding function is `infix`.
         */
        @JvmField
        val IS_INFIX = MetadataFlag(F.IS_INFIX)

        /**
         * Signifies that the corresponding function is `inline`.
         */
        @JvmField
        val IS_INLINE = MetadataFlag(F.IS_INLINE)

        /**
         * Signifies that the corresponding function is `tailrec`.
         */
        @JvmField
        val IS_TAILREC = MetadataFlag(F.IS_TAILREC)

        /**
         * Signifies that the corresponding function is `external`.
         */
        @JvmField
        val IS_EXTERNAL = MetadataFlag(F.IS_EXTERNAL_FUNCTION)

        /**
         * Signifies that the corresponding function is `suspend`.
         */
        @JvmField
        val IS_SUSPEND = MetadataFlag(F.IS_SUSPEND)

        /**
         * Signifies that the corresponding function is `expect`.
         */
        @JvmField
        val IS_EXPECT = MetadataFlag(F.IS_EXPECT_FUNCTION)
    }

    /**
     * A container of flags applicable to Kotlin properties.
     *
     * In addition to the common flag groups, the following flag groups exist for property flags:
     * * member kind flags: [IS_DECLARATION], [IS_FAKE_OVERRIDE], [IS_DELEGATION], [IS_SYNTHESIZED]
     */
    object Property {
        /**
         * A member kind flag, signifying that the corresponding property is explicitly declared in the containing class.
         */
        @JvmField
        val IS_DECLARATION = MetadataFlag(F.MEMBER_KIND, MemberKind.DECLARATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because a property with a suitable
         * signature exists in a supertype. This flag is not written by the Kotlin compiler and its effects are unspecified.
         */
        @JvmField
        val IS_FAKE_OVERRIDE = MetadataFlag(F.MEMBER_KIND, MemberKind.FAKE_OVERRIDE_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because it has been produced
         * by interface delegation (delegation "by").
         */
        @JvmField
        val IS_DELEGATION = MetadataFlag(F.MEMBER_KIND, MemberKind.DELEGATION_VALUE)

        /**
         * A member kind flag, signifying that the corresponding property exists in the containing class because it has been synthesized
         * by the compiler and has no declaration in the source code.
         */
        @JvmField
        val IS_SYNTHESIZED = MetadataFlag(F.MEMBER_KIND, MemberKind.SYNTHESIZED_VALUE)


        /**
         * Signifies that the corresponding property is `var`.
         */
        @JvmField
        val IS_VAR = MetadataFlag(F.IS_VAR)

        /**
         * Signifies that the corresponding property has a getter.
         */
        @JvmField
        val HAS_GETTER = MetadataFlag(F.HAS_GETTER)

        /**
         * Signifies that the corresponding property has a setter.
         */
        @JvmField
        val HAS_SETTER = MetadataFlag(F.HAS_SETTER)

        /**
         * Signifies that the corresponding property is `const`.
         */
        @JvmField
        val IS_CONST = MetadataFlag(F.IS_CONST)

        /**
         * Signifies that the corresponding property is `lateinit`.
         */
        @JvmField
        val IS_LATEINIT = MetadataFlag(F.IS_LATEINIT)

        /**
         * Signifies that the corresponding property has a constant value. On JVM, this flag allows an optimization similarly to
         * [F.HAS_ANNOTATIONS]: constant values of properties are written to the bytecode directly, and this flag can be used to avoid
         * reading the value from the bytecode in case there isn't one.
         */
        @JvmField
        val HAS_CONSTANT = MetadataFlag(F.HAS_CONSTANT)

        /**
         * Signifies that the corresponding property is `external`.
         */
        @JvmField
        val IS_EXTERNAL = MetadataFlag(F.IS_EXTERNAL_PROPERTY)

        /**
         * Signifies that the corresponding property is a delegated property.
         */
        @JvmField
        val IS_DELEGATED = MetadataFlag(F.IS_DELEGATED)

        /**
         * Signifies that the corresponding property is `expect`.
         */
        @JvmField
        val IS_EXPECT = MetadataFlag(F.IS_EXPECT_PROPERTY)
    }

    /**
     * A container of flags applicable to Kotlin property getters and setters.
     */
    object PropertyAccessor {
        /**
         * Signifies that the corresponding property accessor is not default, i.e. it has a body and/or annotations in the source code.
         */
        @JvmField
        val IS_NOT_DEFAULT = MetadataFlag(F.IS_NOT_DEFAULT)

        /**
         * Signifies that the corresponding property accessor is `external`.
         */
        @JvmField
        val IS_EXTERNAL = MetadataFlag(F.IS_EXTERNAL_ACCESSOR)

        /**
         * Signifies that the corresponding property accessor is `inline`.
         */
        @JvmField
        val IS_INLINE = MetadataFlag(F.IS_INLINE_ACCESSOR)
    }

    /**
     * A container of flags applicable to Kotlin types.
     */
    object Type {
        /**
         * Signifies that the corresponding type is marked as nullable, i.e. has a question mark at the end of its notation.
         */
        @JvmField
        val IS_NULLABLE = MetadataFlag(0, 1, 1)

        /**
         * Signifies that the corresponding type is `suspend`.
         */
        @JvmField
        val IS_SUSPEND = MetadataFlag(F.SUSPEND_TYPE.offset + 1, F.SUSPEND_TYPE.bitWidth, 1)
    }

    /**
     * A container of flags applicable to Kotlin type parameters.
     */
    object TypeParameter {
        /**
         * Signifies that the corresponding type parameter is `reified`.
         */
        @JvmField
        val IS_REIFIED = MetadataFlag(0, 1, 1)
    }

    /**
     * A container of flags applicable to Kotlin value parameters.
     */
    object ValueParameter {
        /**
         * Signifies that the corresponding value parameter declares a default value. Note that the default value itself can be a complex
         * expression and is not available via metadata. Also note that in case of an override of a parameter with default value, the
         * parameter in the derived method does _not_ declare the default value ([DECLARES_DEFAULT_VALUE] == false), but the parameter is
         * still optional at the call site because the default value from the base method is used.
         */
        @JvmField
        val DECLARES_DEFAULT_VALUE = MetadataFlag(F.DECLARES_DEFAULT_VALUE)

        /**
         * Signifies that the corresponding value parameter is `crossinline`.
         */
        @JvmField
        val IS_CROSSINLINE = MetadataFlag(F.IS_CROSSINLINE)

        /**
         * Signifies that the corresponding value parameter is `noinline`.
         */
        @JvmField
        val IS_NOINLINE = MetadataFlag(F.IS_NOINLINE)
    }

    /**
     * A container of flags applicable to Kotlin effect expressions.
     *
     * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
     * may change in a subsequent release.
     */
    object EffectExpression {
        /**
         * Signifies that the corresponding effect expression should be negated to compute the proposition or the conclusion of an effect.
         */
        @JvmField
        val IS_NEGATED = MetadataFlag(F.IS_NEGATED)

        /**
         * Signifies that the corresponding effect expression checks whether a value of some variable is `null`.
         */
        @JvmField
        val IS_NULL_CHECK_PREDICATE = MetadataFlag(F.IS_NULL_CHECK_PREDICATE)
    }
}
