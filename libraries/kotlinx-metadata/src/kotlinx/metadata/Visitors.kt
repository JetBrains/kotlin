/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata

import kotlin.contracts.ExperimentalContracts

internal const val VISITOR_API_MESSAGE =
    "Visitor API is deprecated as excessive and cumbersome. Please use nodes (such as KmClass) and their properties."

/**
 * A visitor containing the common code to visit Kotlin declaration containers, such as classes and package fragments.
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmDeclarationContainerVisitor @JvmOverloads constructor(protected open val delegate: KmDeclarationContainerVisitor? = null) {
    /**
     * Visits a function in the container.
     *
     * @param flags function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Function] flags
     * @param name the name of the function
     */
    open fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        delegate?.visitFunction(flags, name)

    /**
     * Visits a property in the container.
     *
     * @param flags property flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Property] flags
     * @param name the name of the property
     * @param getterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
     *   and [Flag.PropertyAccessor] flags
     * @param setterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
     *   and [Flag.PropertyAccessor] flags
     */
    open fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor? =
        delegate?.visitProperty(flags, name, getterFlags, setterFlags)

    /**
     * Visits a type alias in the container.
     *
     * @param flags type alias flags, consisting of [Flag.HAS_ANNOTATIONS] and visibility flag
     * @param name the name of the type alias
     */
    open fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor? =
        delegate?.visitTypeAlias(flags, name)

    /**
     * Visits the extensions of the given type on the container.
     *
     * @param type the type of extension visitor to be returned
     */
    abstract fun visitExtensions(type: KmExtensionType): KmDeclarationContainerExtensionVisitor?
}

/**
 * A visitor to visit Kotlin classes, including interfaces, objects, enum classes and annotation classes.
 *
 * When using this class, [visit] must be called first, followed by zero or more [visitTypeParameter] calls, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmClassVisitor @JvmOverloads constructor(delegate: KmClassVisitor? = null) : KmDeclarationContainerVisitor(delegate) {
    override val delegate: KmClassVisitor?
        get() = super.delegate as KmClassVisitor?

    /**
     * Visits the basic information about the class.
     *
     * @param flags class flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Class] flags
     * @param name the name of the class
     */
    open fun visit(flags: Flags, name: ClassName) {
        delegate?.visit(flags, name)
    }

    /**
     * Visits a type parameter of the class.
     *
     * @param flags type parameter flags, consisting of [Flag.TypeParameter] flags
     * @param name the name of the type parameter
     * @param id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
     *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
     * @param variance the declaration-site variance of the type parameter
     */
    open fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance)

    /**
     * Visits a supertype of the class.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitSupertype(flags: Flags): KmTypeVisitor? =
        delegate?.visitSupertype(flags)

    /**
     * Visits a constructor of the class.
     *
     * @param flags constructor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag and [Flag.Constructor] flags
     */
    open fun visitConstructor(flags: Flags): KmConstructorVisitor? =
        delegate?.visitConstructor(flags)

    /**
     * Visits the name of the companion object of this class, if it has one.
     *
     * @param name the name of the companion object
     */
    open fun visitCompanionObject(name: String) {
        delegate?.visitCompanionObject(name)
    }

    /**
     * Visits the name of a nested class of this class.
     *
     * @param name the name of a nested class
     */
    open fun visitNestedClass(name: String) {
        delegate?.visitNestedClass(name)
    }

    /**
     * Visits the name of an enum entry, if this class is an enum class.
     *
     * @param name the name of an enum entry
     */
    open fun visitEnumEntry(name: String) {
        delegate?.visitEnumEntry(name)
    }

    /**
     * Visits the name of a direct subclass of this class, if this class is `sealed`.
     *
     * @param name the name of a direct subclass
     */
    open fun visitSealedSubclass(name: ClassName) {
        delegate?.visitSealedSubclass(name)
    }

    /**
     * Visits the name of the underlying property, if this class is `inline`.
     *
     * @param name the name of the underlying property.
     */
    open fun visitInlineClassUnderlyingPropertyName(name: String) {
        delegate?.visitInlineClassUnderlyingPropertyName(name)
    }

    /**
     * Visits the type of the underlying property, if this class is `inline`.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitInlineClassUnderlyingType(flags: Flags): KmTypeVisitor? =
        delegate?.visitInlineClassUnderlyingType(flags)

    /**
     * Visits the type of a context receiver of the class.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    @ExperimentalContextReceivers
    open fun visitContextReceiverType(flags: Flags): KmTypeVisitor? =
        delegate?.visitContextReceiverType(flags)

    /**
     * Visits the version requirement on this class.
     */
    open fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    /**
     * Visits the extensions of the given type on the class.
     *
     * @param type the type of extension visitor to be returned
     */
    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the class.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit Kotlin package fragments, including single file facades and multi-file class parts.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmPackageVisitor @JvmOverloads constructor(delegate: KmPackageVisitor? = null) : KmDeclarationContainerVisitor(delegate) {
    override val delegate: KmPackageVisitor?
        get() = super.delegate as KmPackageVisitor?

    /**
     * Visits the extensions of the given type on the package fragment.
     *
     * @param type the type of extension visitor to be returned
     */
    override fun visitExtensions(type: KmExtensionType): KmPackageExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the package fragment.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit the metadata of a synthetic class generated for a Kotlin lambda.
 *
 * When using this class, [visitFunction] must be called first, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmLambdaVisitor @JvmOverloads constructor(private val delegate: KmLambdaVisitor? = null) {
    /**
     * Visits the signature of a synthetic anonymous function, representing the lambda.
     *
     * @param flags function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Function] flags
     * @param name the name of the function (usually `"<anonymous>"` or `"<no name provided>"` for lambdas emitted by the Kotlin compiler)
     */
    open fun visitFunction(flags: Flags, name: String): KmFunctionVisitor? =
        delegate?.visitFunction(flags, name)

    /**
     * Visits the end of the lambda.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a constructor of a Kotlin class.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmConstructorVisitor @JvmOverloads constructor(private val delegate: KmConstructorVisitor? = null) {
    /**
     * Visits a value parameter of the constructor.
     *
     * @param flags value parameter flags, consisting of [Flag.ValueParameter] flags
     * @param name the name of the value parameter
     */
    open fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
        delegate?.visitValueParameter(flags, name)

    /**
     * Visits the version requirement on this constructor.
     */
    open fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    /**
     * Visits the extensions of the given type on the constructor.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the constructor.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a Kotlin function declaration.
 *
 * When using this class, zero or more calls to [visitTypeParameter] must be done first, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmFunctionVisitor @JvmOverloads constructor(private val delegate: KmFunctionVisitor? = null) {
    /**
     * Visits a type parameter of the function.
     *
     * @param flags type parameter flags, consisting of [Flag.TypeParameter] flags
     * @param name the name of the type parameter
     * @param id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
     *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
     * @param variance the declaration-site variance of the type parameter
     */
    open fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance)

    /**
     * Visits the type of the receiver of the function, if this is an extension function.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
        delegate?.visitReceiverParameterType(flags)

    /**
     * Visits the type of a context receiver of the function.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    @ExperimentalContextReceivers
    open fun visitContextReceiverType(flags: Flags): KmTypeVisitor? =
        delegate?.visitContextReceiverType(flags)

    /**
     * Visits a value parameter of the function.
     *
     * @param flags value parameter flags, consisting of [Flag.ValueParameter] flags
     * @param name the name of the value parameter
     */
    open fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor? =
        delegate?.visitValueParameter(flags, name)

    /**
     * Visits the return type of the function.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitReturnType(flags: Flags): KmTypeVisitor? =
        delegate?.visitReturnType(flags)

    /**
     * Visits the version requirement on this function.
     */
    open fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    /**
     * Visits the contract of the function.
     */
    @ExperimentalContracts
    open fun visitContract(): KmContractVisitor? =
        delegate?.visitContract()

    /**
     * Visits the extensions of the given type on the function.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the function.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a Kotlin property declaration.
 *
 * When using this class, zero or more calls to [visitTypeParameter] must be done first, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmPropertyVisitor @JvmOverloads constructor(private val delegate: KmPropertyVisitor? = null) {
    /**
     * Visits a type parameter of the property.
     *
     * @param flags type parameter flags, consisting of [Flag.TypeParameter] flags
     * @param name the name of the type parameter
     * @param id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
     *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
     * @param variance the declaration-site variance of the type parameter
     */
    open fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance)

    /**
     * Visits the type of the receiver of the property, if this is an extension property.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitReceiverParameterType(flags: Flags): KmTypeVisitor? =
        delegate?.visitReceiverParameterType(flags)

    /**
     * Visits the type of a context receiver of the property.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    @ExperimentalContextReceivers
    open fun visitContextReceiverType(flags: Flags): KmTypeVisitor? =
        delegate?.visitContextReceiverType(flags)

    /**
     * Visits a value parameter of the setter of this property, if this is a `var` property.
     *
     * @param flags value parameter flags, consisting of [Flag.ValueParameter] flags
     * @param name the name of the value parameter (`"<set-?>"` for properties emitted by the Kotlin compiler)
     */
    open fun visitSetterParameter(flags: Flags, name: String): KmValueParameterVisitor? =
        delegate?.visitSetterParameter(flags, name)

    /**
     * Visits the type of the property.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitReturnType(flags: Flags): KmTypeVisitor? =
        delegate?.visitReturnType(flags)

    /**
     * Visits the version requirement on this property.
     */
    open fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    /**
     * Visits the extensions of the given type on the property.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the property.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a Kotlin type alias declaration.
 *
 * When using this class, zero or more calls to [visitTypeParameter] must be done first, followed by zero or more calls
 * to other visit* methods, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmTypeAliasVisitor @JvmOverloads constructor(private val delegate: KmTypeAliasVisitor? = null) {
    /**
     * Visits a type parameter of the type alias.
     *
     * @param flags type parameter flags, consisting of [Flag.TypeParameter] flags
     * @param name the name of the type parameter
     * @param id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
     *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
     * @param variance the declaration-site variance of the type parameter
     */
    open fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor? =
        delegate?.visitTypeParameter(flags, name, id, variance)

    /**
     * Visits the underlying type of the type alias, i.e. the type in the right-hand side of the type alias declaration.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitUnderlyingType(flags: Flags): KmTypeVisitor? =
        delegate?.visitUnderlyingType(flags)

    /**
     * Visits the expanded type of the type alias, i.e. the full expansion of the underlying type, where all type aliases are substituted
     * with their expanded types. If no type aliases are used in the underlying type, expanded type is equal to the underlying type.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitExpandedType(flags: Flags): KmTypeVisitor? =
        delegate?.visitExpandedType(flags)

    /**
     * Visits the annotation on the type alias.
     *
     * @param annotation annotation on the type alias
     */
    open fun visitAnnotation(annotation: KmAnnotation) {
        delegate?.visitAnnotation(annotation)
    }

    /**
     * Visits the version requirement on this type alias.
     */
    open fun visitVersionRequirement(): KmVersionRequirementVisitor? =
        delegate?.visitVersionRequirement()

    /**
     * Visits the extensions of the given type on the type alias.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmTypeAliasExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the type alias.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a value parameter of a Kotlin constructor, function or property setter.
 *
 * When using this class, either [visitType] or [visitVarargElementType] must be called first (depending on whether the value parameter
 * is `vararg` or not), followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmValueParameterVisitor @JvmOverloads constructor(private val delegate: KmValueParameterVisitor? = null) {
    /**
     * Visits the type of the value parameter, if this is **not** a `vararg` parameter.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitType(flags: Flags): KmTypeVisitor? =
        delegate?.visitType(flags)

    /**
     * Visits the type of the value parameter, if this is a `vararg` parameter.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitVarargElementType(flags: Flags): KmTypeVisitor? =
        delegate?.visitVarargElementType(flags)

    /**
     * Visits the extensions of the given type on the value parameter.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmValueParameterExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the value parameter.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a type parameter of a Kotlin class, function, property or type alias.
 *
 * When using this class, zero or more [visitUpperBound] calls must be done first, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmTypeParameterVisitor @JvmOverloads constructor(private val delegate: KmTypeParameterVisitor? = null) {
    /**
     * Visits the upper bound of the type parameter.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitUpperBound(flags: Flags): KmTypeVisitor? =
        delegate?.visitUpperBound(flags)

    /**
     * Visits the extensions of the given type on the type parameter.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the type parameter.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit a type. The type must have a classifier which is one of: a class [visitClass], type parameter [visitTypeParameter]
 * or type alias [visitTypeAlias]. If the type's classifier is a class or a type alias, it can have type arguments ([visitArgument] and
 * [visitStarProjection]). If the type's classifier is an inner class, it can have the outer type ([visitOuterType]), which captures
 * the generic type arguments of the outer class. Also, each type can have an abbreviation ([visitAbbreviatedType]) in case a type alias
 * was used originally at this site in the declaration (all types are expanded by default for metadata produced by the Kotlin compiler).
 * If [visitFlexibleTypeUpperBound] is called, this type is regarded as a flexible type, and its contents represent the lower bound,
 * and the result of the call represents the upper bound.
 *
 * When using this class, [visitEnd] must be called exactly once and after calls to all other visit* methods.
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
abstract class KmTypeVisitor @JvmOverloads constructor(private val delegate: KmTypeVisitor? = null) {
    /**
     * Visits the name of the class, if this type's classifier is a class.
     *
     * @param name the name of the class
     */
    open fun visitClass(name: ClassName) {
        delegate?.visitClass(name)
    }

    /**
     * Visits the name of the type alias, if this type's classifier is a type alias. Note that all types are expanded for metadata produced
     * by the Kotlin compiler, so the type with a type alias classifier may only appear in a call to [visitAbbreviatedType].
     *
     * @param name the name of the type alias
     */
    open fun visitTypeAlias(name: ClassName) {
        delegate?.visitTypeAlias(name)
    }

    /**
     * Visits the id of the type parameter, if this type's classifier is a type parameter.
     *
     * @param id id of the type parameter
     */
    open fun visitTypeParameter(id: Int) {
        delegate?.visitTypeParameter(id)
    }

    /**
     * Visits the type projection used in a type argument of the type based on a class or on a type alias.
     * For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     * @param variance the variance of the type projection
     */
    open fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor? =
        delegate?.visitArgument(flags, variance)

    /**
     * Visits the star (`*`) projection used in a type argument of the type based on a class or on a type alias.
     * For example, in `MutableMap<in String?, *>`, `*` is the star projection which is the second type argument of the type.
     */
    open fun visitStarProjection() {
        delegate?.visitStarProjection()
    }

    /**
     * Visits the abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin compiler. For example:
     *
     *     typealias A<T> = MutableList<T>
     *
     *     fun foo(a: A<Any>) {}
     *
     * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its abbreviation is `A<Any>`.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitAbbreviatedType(flags: Flags): KmTypeVisitor? =
        delegate?.visitAbbreviatedType(flags)

    /**
     * Visits the outer type, if this type's classifier is an inner class. For example:
     *
     *     class A<T> { inner class B<U> }
     *
     *     fun foo(a: A<*>.B<Byte?>) {}
     *
     * The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is class `B`, and it has one type argument,
     * type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it has one type argument, star projection).
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitOuterType(flags: Flags): KmTypeVisitor? =
        delegate?.visitOuterType(flags)

    /**
     * Visits the upper bound of the type, marking it as flexible and its contents as the lower bound. Flexible types in Kotlin include
     * platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     * @param typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
     *                          or "kotlin.DynamicType" for JS dynamic type
     */
    open fun visitFlexibleTypeUpperBound(flags: Flags, typeFlexibilityId: String?): KmTypeVisitor? =
        delegate?.visitFlexibleTypeUpperBound(flags, typeFlexibilityId)

    /**
     * Visits the extensions of the given type on the type.
     *
     * @param type the type of extension visitor to be returned
     */
    open fun visitExtensions(type: KmExtensionType): KmTypeExtensionVisitor? =
        delegate?.visitExtensions(type)

    /**
     * Visits the end of the type.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit the contents of a version requirement on a Kotlin declaration.
 *
 * Version requirement is an internal feature of the Kotlin compiler and the standard Kotlin library,
 * enabled for example with the internal [kotlin.internal.RequireKotlin] annotation.
 *
 * When using this class, [visit] must be called first, followed by [visitVersion], followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
abstract class KmVersionRequirementVisitor @JvmOverloads constructor(@Suppress("DEPRECATION") private val delegate: KmVersionRequirementVisitor? = null) {
    /**
     * Visits the description of this version requirement.
     *
     * @param kind the kind of the version that this declaration requires: compiler, language or API version
     * @param level the level of the diagnostic that must be reported on the usages of the declaration in case
     *              the version requirement is not satisfied
     * @param errorCode optional error code to be displayed in the diagnostic
     * @param message optional message to be displayed in the diagnostic
     */
    open fun visit(kind: KmVersionRequirementVersionKind, level: KmVersionRequirementLevel, errorCode: Int?, message: String?) {
        delegate?.visit(kind, level, errorCode, message)
    }

    /**
     * Visits the version required by this requirement.
     *
     * @param major the major component of the version (e.g. "1" in "1.2.3")
     * @param minor the minor component of the version (e.g. "2" in "1.2.3")
     * @param patch the patch component of the version (e.g. "3" in "1.2.3")
     */
    open fun visitVersion(major: Int, minor: Int, patch: Int) {
        delegate?.visitVersion(major, minor, patch)
    }

    /**
     * Visits the end of the version requirement.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit the contents of the contract of a Kotlin function.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * When using this class, zero or more calls to [visitEffect] must be done first, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
@ExperimentalContracts
abstract class KmContractVisitor @JvmOverloads constructor(private val delegate: KmContractVisitor? = null) {
    /**
     * Visits an effect of this contract.
     *
     * @param type type of the effect
     * @param invocationKind optional number of invocations of the lambda parameter of this function,
     *   specified further in the effect expression
     */
    open fun visitEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?): KmEffectVisitor? =
        delegate?.visitEffect(type, invocationKind)

    /**
     * Visits the end of the contract.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * When using this class, zero or more calls to [visitConstructorArgument] or [visitConclusionOfConditionalEffect] must be done first,
 * followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
@ExperimentalContracts
abstract class KmEffectVisitor @JvmOverloads constructor(private val delegate: KmEffectVisitor? = null) {
    /**
     * Visits the optional argument of the effect constructor, i.e. the constant value for the [KmEffectType.RETURNS_CONSTANT] effect,
     * or the parameter reference for the [KmEffectType.CALLS] effect.
     */
    open fun visitConstructorArgument(): KmEffectExpressionVisitor? =
        delegate?.visitConstructorArgument()

    /**
     * Visits the optional conclusion of the effect. If this method is called, the effect represents an implication with the
     * right-hand side handled by the returned visitor.
     */
    open fun visitConclusionOfConditionalEffect(): KmEffectExpressionVisitor? =
        delegate?.visitConclusionOfConditionalEffect()

    /**
     * Visits the end of the effect.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * A visitor to visit the effect expression, the contents of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * When using this class, [visit] must be called first, followed by zero or more calls to other visit* methods, followed by [visitEnd].
 */
@Deprecated(VISITOR_API_MESSAGE)
@Suppress("DEPRECATION")
@ExperimentalContracts
abstract class KmEffectExpressionVisitor @JvmOverloads constructor(private val delegate: KmEffectExpressionVisitor? = null) {
    /**
     * Visits the basic information of the effect expression.
     *
     * @param flags effect expression flags, consisting of [Flag.EffectExpression] flags
     * @param parameterIndex optional 1-based index of the value parameter of the function, for effects which assert something about
     *                       the function parameters. The index 0 means the extension receiver parameter
     */
    open fun visit(flags: Flags, parameterIndex: Int?) {
        delegate?.visit(flags, parameterIndex)
    }

    /**
     * Visits the constant value used in the effect expression. May be `true`, `false` or `null`.
     *
     * @param value the constant value
     */
    open fun visitConstantValue(value: Any?) {
        delegate?.visitConstantValue(value)
    }

    /**
     * Visits the type used as the target of an `is`-expression in the effect expression.
     *
     * @param flags type flags, consisting of [Flag.Type] flags
     */
    open fun visitIsInstanceType(flags: Flags): KmTypeVisitor? =
        delegate?.visitIsInstanceType(flags)

    /**
     * Visits the argument of an `&&`-expression. If this method is called, the expression represents the left-hand side and
     * the returned visitor handles the right-hand side.
     */
    open fun visitAndArgument(): KmEffectExpressionVisitor? =
        delegate?.visitAndArgument()

    /**
     * Visits the argument of an `||`-expression. If this method is called, the expression represents the left-hand side and
     * the returned visitor handles the right-hand side.
     */
    open fun visitOrArgument(): KmEffectExpressionVisitor? =
        delegate?.visitOrArgument()

    /**
     * Visits the end of the effect expression.
     */
    open fun visitEnd() {
        delegate?.visitEnd()
    }
}

/**
 * Variance applied to a type parameter on the declaration site (*declaration-site variance*),
 * or to a type in a projection (*use-site variance*).
 */
enum class KmVariance {
    /**
     * The affected type parameter or type is *invariant*, which means it has no variance applied to it.
     */
    INVARIANT,

    /**
     * The affected type parameter or type is *contravariant*. Denoted by the `in` modifier in the source code.
     */
    IN,

    /**
     * The affected type parameter or type is *covariant*. Denoted by the `out` modifier in the source code.
     */
    OUT,
}

/**
 * Type of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
enum class KmEffectType {
    /**
     * Represents `returns(value)` contract effect:
     * a situation when a function returns normally with the specified return value.
     * Return value is stored in the [KmEffect.constructorArguments].
     */
    RETURNS_CONSTANT,

    /**
     * Represents `callsInPlace` contract effect:
     * A situation when the referenced lambda is invoked in place (optionally) specified number of times.
     *
     * Referenced lambda is stored in the [KmEffect.constructorArguments].
     * Number of invocations, if specified, is stored in [KmEffect.invocationKind].
     */
    CALLS,

    /**
     * Represents `returnsNotNull` contract effect:
     * a situation when a function returns normally with any value that is not null.
     */
    RETURNS_NOT_NULL,
}

/**
 * Number of invocations of a lambda parameter specified by an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
@ExperimentalContracts
enum class KmEffectInvocationKind {
    /**
     * A function parameter will be invoked one time or not invoked at all.
     */
    AT_MOST_ONCE,

    /**
     * A function parameter will be invoked exactly one time.
     */
    EXACTLY_ONCE,

    /**
     * A function parameter will be invoked one or more times.
     */
    AT_LEAST_ONCE,
}

/**
 * Severity of the diagnostic reported by the compiler when a version requirement is not satisfied.
 */
enum class KmVersionRequirementLevel {
    /**
     * Represents a diagnostic with 'WARNING' severity.
     */
    WARNING,

    /**
     * Represents a diagnostic with 'ERROR' severity.
     */
    ERROR,

    /**
     * Excludes the declaration from the resolution process completely when the version requirement is not satisfied.
     */
    HIDDEN,
}

/**
 * The kind of the version that is required by a version requirement.
 */
enum class KmVersionRequirementVersionKind {
    /**
     * Indicates that certain language version is required.
     */
    LANGUAGE_VERSION,

    /**
     * Indicates that certain compiler version is required.
     */
    COMPILER_VERSION,

    /**
     * Indicates that certain API version is required.
     */
    API_VERSION,
}
