/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package kotlin.metadata

import org.jetbrains.kotlin.metadata.deserialization.Flags
import kotlin.contracts.ExperimentalContracts
import kotlin.metadata.internal.FlagImpl
import kotlin.metadata.internal.extensions.*
import kotlin.metadata.internal.propertyBooleanFlag

/**
 * Represents a Kotlin declaration container, such as a class or a package fragment.
 */
public interface KmDeclarationContainer {
    /**
     * Functions in the container.
     */
    public val functions: MutableList<KmFunction>

    /**
     * Properties in the container.
     */
    public val properties: MutableList<KmProperty>

    /**
     * Type aliases in the container.
     */
    public val typeAliases: MutableList<KmTypeAlias>
}

/**
 * Represents a Kotlin class.
 *
 * 'Class' here is used in a broad sense and includes interfaces, enum classes, companion objects, et cetera.
 * Precise kind of the class can be obtained via [KmClass.kind].
 * Various class attributes can be read and manipulated via extension properties, such as [KmClass.visibility] or [KmClass.isData].
 */
public class KmClass : KmDeclarationContainer {
    internal var flags: Int = 0

    /**
     * Name of the class.
     */
    public lateinit var name: ClassName

    /**
     * Type parameters of the class.
     */
    public val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Supertypes of the class.
     */
    public val supertypes: MutableList<KmType> = ArrayList(1)

    /**
     * Functions in the class.
     */
    override val functions: MutableList<KmFunction> = ArrayList()

    /**
     * Properties in the class.
     */
    override val properties: MutableList<KmProperty> = ArrayList()

    /**
     * Type aliases in the class.
     */
    override val typeAliases: MutableList<KmTypeAlias> = ArrayList(0)

    /**
     * Constructors of the class.
     */
    public val constructors: MutableList<KmConstructor> = ArrayList(1)

    /**
     * Name of the companion object of this class, if it has one.
     */
    public var companionObject: String? = null

    /**
     * Names of nested classes of this class.
     */
    public val nestedClasses: MutableList<String> = ArrayList(0)

    /**
     * Names of enum entries, if this class is an enum class.
     */
    @Deprecated("Use `kmEnumEntries` instead.")
    public val enumEntries: MutableList<String> = ArrayList(0)

    /**
     * Enum entries, if this class is an enum class.
     */
    public val kmEnumEntries: MutableList<KmEnumEntry> = ArrayList(0)

    /**
     * Names of direct subclasses of this class, if this class is `sealed`.
     */
    public val sealedSubclasses: MutableList<ClassName> = ArrayList(0)

    /**
     * Name of the underlying property, if this class is `inline`.
     */
    public var inlineClassUnderlyingPropertyName: String? = null

    /**
     * Type of the underlying property, if this class is `inline`.
     */
    public var inlineClassUnderlyingType: KmType? = null

    /**
     * Annotations on the class.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Types of context receivers of the class.
     *
     * Context receivers feature is replaced with context parameters.
     * Context parameters on classes are not supported.
     * This field is still read and written to binary metadata for purposes of working with older Kotlin's files.
     *
     * See https://kotl.in/context-parameters for more information about the new proposal.
     */
    @ExperimentalContextReceivers
    @Deprecated(CtxReceiversDeprecated, level = DeprecationLevel.WARNING) // WARNING instead of ERROR because no replacement
    public val contextReceiverTypes: MutableList<KmType> = ArrayList(0)

    /**
     * Version requirements on this class.
     */
    public val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    internal val extensions: List<KmClassExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createClassExtension)
}

/**
 * Represents a Kotlin package fragment that contains top-level functions, properties, and type aliases.
 * Package fragments are produced from single file facades and multi-file class parts.
 * Note that a package fragment does not contain any classes, as classes are not a part of file facades and have their own metadata.
 */
public class KmPackage : KmDeclarationContainer {
    /**
     * Functions in the package fragment.
     */
    override val functions: MutableList<KmFunction> = ArrayList()

    /**
     * Properties in the package fragment.
     */
    override val properties: MutableList<KmProperty> = ArrayList()

    /**
     * Type aliases in the package fragment.
     */
    override val typeAliases: MutableList<KmTypeAlias> = ArrayList(0)

    internal val extensions: List<KmPackageExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPackageExtension)
}

/**
 * Represents a synthetic class generated for a Kotlin lambda.
 */
public class KmLambda {
    /**
     * Signature of the synthetic anonymous function, representing the lambda.
     */
    public lateinit var function: KmFunction
}

/**
 * Represents a constructor of a Kotlin class.
 *
 * Various constructor attributes can be read and manipulated via extension properties,
 * such as [KmConstructor.visibility] or [KmConstructor.isSecondary].
 */
public class KmConstructor internal constructor(internal var flags: Int) {
    public constructor() : this(0)

    /**
     * Value parameters of the constructor.
     */
    public val valueParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Version requirements on the constructor.
     */
    public val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    /**
     * Annotations on the constructor.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    internal val extensions: List<KmConstructorExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createConstructorExtension)
}

/**
 * Represents a Kotlin function declaration.
 *
 * Various function attributes can be read and manipulated via extension properties,
 * such as [KmFunction.visibility] or [KmFunction.isSuspend].
 *
 * @property name the name of the function
 */
public class KmFunction internal constructor(internal var flags: Int, public var name: String) {

    public constructor(name: String) : this(0, name)

    /**
     * Type parameters of the function.
     */
    public val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the function, if this is an extension function.
     */
    public var receiverParameterType: KmType? = null

    /**
     * Annotations on the extension receiver of the function, if this is an extension function.
     */
    public val extensionReceiverParameterAnnotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Types of context receivers of the function.
     *
     * Context receivers feature is replaced with context parameters.
     * This list is no longer being read or written. Please use [contextParameters] instead.
     * Older Kotlin compilations with context receivers are represented as parameters with the "_" name.
     *
     * See https://kotl.in/context-parameters for more information about the new proposal.
     */
    @ExperimentalContextReceivers
    @Deprecated(CtxReceiversDeprecated, level = DeprecationLevel.ERROR)
    public val contextReceiverTypes: MutableList<KmType> = ArrayList(0)

    /**
     * Value parameters of the function.
     */
    public val valueParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Context parameters of the function.
     *
     * To support the legacy context receivers feature, this list may
     * also contain parameters with the "_" name representing them.
     */
    @ExperimentalContextParameters
    public val contextParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Return type of the function.
     */
    public lateinit var returnType: KmType

    /**
     * Version requirements on the function.
     */
    public val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    /**
     * Contract of the function.
     */
    @ExperimentalContracts
    public var contract: KmContract? = null

    /**
     * Annotations on the function.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    internal val extensions: List<KmFunctionExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createFunctionExtension)
}

/**
 * Represents a Kotlin property accessor.
 *
 * Contains only accessor annotations and attributes, such as visibility and modality.
 * Attributes can be read and written using extension properties, e.g. [KmPropertyAccessorAttributes.visibility] or [KmPropertyAccessorAttributes.isNotDefault].
 */
public class KmPropertyAccessorAttributes internal constructor(internal var flags: Int) {
    public constructor() : this(0)

    /**
     * Annotations on the property accessor.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)
}

/**
 * Represents a Kotlin property declaration.
 *
 * Various property attributes can be read and manipulated via extension properties,
 * such as [KmProperty.visibility] or [KmProperty.isVar].
 *
 * Getter and setter attributes are available separately via extensions on [KmProperty.getter] and [KmProperty.setter] correspondingly.
 *
 * @property name the name of the property
 */
public class KmProperty internal constructor(
    internal var flags: Int,
    public var name: String,
    getterFlags: Int,
    setterFlags: Int,
) {
    public constructor(name: String) : this(0, name, 0, 0)

    // needed for reading/writing flags back to protobuf as a whole pack
    private var _hasSetter: Boolean by propertyBooleanFlag(FlagImpl(Flags.HAS_SETTER))
    private var _hasGetter: Boolean by propertyBooleanFlag(FlagImpl(Flags.HAS_GETTER))

    /**
     * Attributes of the getter of this property.
     * Attributes can be retrieved with extension properties, such as [KmPropertyAccessorAttributes.visibility] or [KmPropertyAccessorAttributes.isNotDefault].
     *
     * Getter for property is always present, therefore, the type of this property is non-nullable.
     */
    public val getter: KmPropertyAccessorAttributes = KmPropertyAccessorAttributes(getterFlags).also { _hasGetter = true }

    /**
     * Attributes of the setter of this property.
     * Attributes can be retrieved with extension properties, such as [KmPropertyAccessorAttributes.visibility] or [KmPropertyAccessorAttributes.isNotDefault].
     *
     * Returns null if setter is absent, i.e., [KmProperty.isVar] is false.
     *
     * Note that setting [KmProperty.isVar] to true does not automatically create [KmProperty.setter] and vice versa. This has to be done explicitly.
     */
    public var setter: KmPropertyAccessorAttributes? = if (this._hasSetter) KmPropertyAccessorAttributes(setterFlags) else null
        set(new) {
            this._hasSetter = new != null
            field = new
        }

    /**
     * Type parameters of the property.
     */
    public val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the property, if this is an extension property.
     */
    public var receiverParameterType: KmType? = null

    /**
     * Annotations on the extension receiver of the property, if this is an extension property.
     */
    @ExperimentalAnnotationsInMetadata
    public val extensionReceiverParameterAnnotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Types of context receivers of the property.
     *
     * Context receivers feature is replaced with context parameters.
     * This list is no longer being read or written. Please use [contextParameters] instead.
     * Older Kotlin compilations with context receivers are represented as parameters with the "_" name.
     *
     * See https://kotl.in/context-parameters for more information about the new proposal.
     */
    @ExperimentalContextReceivers
    @Deprecated(CtxReceiversDeprecated, level = DeprecationLevel.ERROR)
    public val contextReceiverTypes: MutableList<KmType> = ArrayList(0)

    /**
     * Context parameters of the property.
     *
     * To support the legacy context receivers feature, this list may
     * also contain parameters with the "_" name representing them.
     */
    @ExperimentalContextParameters
    public val contextParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Value parameter of the setter of this property, if this is a `var` property and parameter is present.
     * Parameter is present if and only if the setter is not default:
     *
     * ```kotlin
     * var foo: String = ""
     *   set(param) {
     *     field = param.removePrefix("bar")
     *   }
     * ```
     */
    public var setterParameter: KmValueParameter? = null

    /**
     * Type of the property.
     */
    public lateinit var returnType: KmType

    /**
     * Version requirements on the property.
     */
    public val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    /**
     * Annotations on the property.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Annotations on the property's backing field, or empty list if the property doesn't have one.
     */
    @ExperimentalAnnotationsInMetadata
    public val backingFieldAnnotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Annotations on the property's delegate field, or empty list if the property is not delegated.
     */
    @ExperimentalAnnotationsInMetadata
    public val delegateFieldAnnotations: MutableList<KmAnnotation> = ArrayList(0)

    internal val extensions: List<KmPropertyExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPropertyExtension)
}

/**
 * Represents a Kotlin type alias declaration.
 *
 * Various type alias attributes can be read and manipulated via extension properties,
 * such as [KmTypeAlias.visibility] or [KmTypeAlias.hasAnnotations].
 *
 * @property name the name of the type alias
 */
public class KmTypeAlias internal constructor(
    internal var flags: Int,
    public var name: String,
) {

    public constructor(name: String) : this(0, name)

    /**
     * Type parameters of the type alias.
     */
    public val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Underlying type of the type alias, i.e., the type in the right-hand side of the type alias declaration.
     */
    public lateinit var underlyingType: KmType

    /**
     * Expanded type of the type alias, i.e., the full expansion of the underlying type, where all type aliases are substituted
     * with their expanded types. If no type aliases are used in the underlying type, the expanded type is equal to the underlying type.
     */
    public lateinit var expandedType: KmType

    /**
     * Annotations on the type alias.
     */
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Version requirements on the type alias.
     */
    public val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    internal val extensions: List<KmTypeAliasExtension> =
        MetadataExtensions.INSTANCES.mapNotNull(MetadataExtensions::createTypeAliasExtension)
}

/**
 * Represents a value parameter of a Kotlin constructor, function, or property setter.
 *
 * Various value parameter attributes can be read and manipulated via extension properties,
 * such as [KmValueParameter.declaresDefaultValue].
 *
 * @property name the name of the value parameter
 */
public class KmValueParameter internal constructor(
    internal var flags: Int,
    public var name: String,
) {

    public constructor(name: String) : this(0, name)

    /**
     * Type of the value parameter.
     * If this is a `vararg` parameter of type `X`, returns the type `Array<out X>`.
     */
    public lateinit var type: KmType

    /**
     * Type of the `vararg` value parameter, or `null` if this is not a `vararg` parameter.
     */
    public var varargElementType: KmType? = null

    /**
     * Default value of the parameter, if this is a parameter of an annotation class constructor.
     */
    public var annotationParameterDefaultValue: KmAnnotationArgument? = null

    /**
     * Annotations on the value parameter.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    internal val extensions: List<KmValueParameterExtension> =
        MetadataExtensions.INSTANCES.mapNotNull(MetadataExtensions::createValueParameterExtension)
}

/**
 * Represents a type parameter of a Kotlin class, function, property, or type alias.
 *
 * Various type parameter attributes can be read and manipulated via extension properties,
 * such as [KmTypeParameter.isReified].
 *
 * @property name the name of the type parameter
 * @property id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
 *           the name is not enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
 * @property variance the declaration-site variance of the type parameter
 */
public class KmTypeParameter internal constructor(
    internal var flags: Int,
    public var name: String,
    public var id: Int,
    public var variance: KmVariance,
) {

    public constructor(name: String, id: Int, variance: KmVariance) : this(0, name, id, variance)

    /**
     * Upper bounds of the type parameter.
     */
    public val upperBounds: MutableList<KmType> = ArrayList(1)

    internal val extensions: List<KmTypeParameterExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeParameterExtension)
}

/**
 * Represents an enum entry.
 *
 * @property name the name of the enum entry
 */
public class KmEnumEntry(public var name: String) {
    /**
     * Annotations on the enum entry.
     */
    @ExperimentalAnnotationsInMetadata
    public val annotations: MutableList<KmAnnotation> = ArrayList(0)

    internal val extensions: List<KmEnumEntryExtension> =
        MetadataExtensions.INSTANCES.mapNotNull(MetadataExtensions::createEnumEntryExtension)

    override fun toString(): String = name
}

/**
 * Represents a type.
 *
 * Various type attributes can be read and manipulated via extension properties,
 * such as [KmType.isNullable].
 */
public class KmType internal constructor(
    internal var flags: Int,
) {

    public constructor() : this(0)

    /**
     * Classifier of the type.
     */
    public lateinit var classifier: KmClassifier

    /**
     * Arguments of the type, if the type's classifier is a class or a type alias.
     */
    public val arguments: MutableList<KmTypeProjection> = ArrayList(0)

    /**
     * Abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin compiler. For example:
     *
     *     typealias A<T> = MutableList<T>
     *
     *     fun foo(a: A<Any>) {}
     *
     * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its abbreviation is `A<Any>`.
     */
    public var abbreviatedType: KmType? = null

    /**
     * Outer type of this type, if this type's classifier is an inner class. For example:
     *
     *     class A<T> { inner class B<U> }
     *
     *     fun foo(a: A<*>.B<Byte?>) {}
     *
     * The type of the `foo`'s parameter in the metadata is `B<Byte>` (a type whose classifier is class `B`, and it has one type argument,
     * type `Byte?`), and its outer type is `A<*>` (a type whose classifier is class `A`, and it has one type argument, star projection).
     */
    public var outerType: KmType? = null

    /**
     * Upper bound of this type, if this type is flexible. In that case, all other data refers to the lower bound of the type.
     *
     * Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
     */
    public var flexibleTypeUpperBound: KmFlexibleTypeUpperBound? = null

    internal val extensions: List<KmTypeExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeExtension)

    /**
     * Determines whether this KmType is equal to the given [other].
     *
     * KmTypes are compared using structural equality, i.e., two objects are considered equal
     * if they have all the following parts equal:
     * attributes (such as [isNullable] and [isSuspend]), [classifier],
     * [arguments], [outerType], [abbreviatedType], [flexibleTypeUpperBound],
     * and all platform extensions (such as annotations on JVM).
     *
     * Note that equality of [KmType] instances differs from the concept of type equality in the Kotlin language.
     * In the language, types A and B are equal if `A.isSubtypeOf(B) && B.isSubtypeOf(A)`.
     * Since kotlin-metadata-jvm does not provide subtyping algorithms (or any kind of type inference algorithms whatsoever),
     * [KmType] equality adheres to a comparison of what is written to the metadata.
     * For example, flexible types are not considered equal to their lower and upper bounds —
     * which means that `String?` and `String!` are not equal KmTypes, despite being freely assignable from each other
     * in the Kotlin language.
     *
     * @param other The object to compare for equality.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KmType

        if (flags != other.flags) return false
        if (classifier != other.classifier) return false
        if (arguments != other.arguments) return false
        if (outerType != other.outerType) return false
        if (abbreviatedType != other.abbreviatedType) return false
        if (flexibleTypeUpperBound != other.flexibleTypeUpperBound) return false
        if (extensions != other.extensions) return false

        return true
    }

    /**
     * Computes the hash code of the KmType object using its properties.
     *
     * @return The computed hash code of the KmType object.
     */
    override fun hashCode(): Int {
        var result = flags
        result = 31 * result + classifier.hashCode()
        result = 31 * result + arguments.hashCode()
        /**
         * outerType, abbreviatedType, and flexibleTypeUpperBound are omitted, so we can compute hash code faster
         * with a trade-off for rare collisions.
         */
        return result
    }
}

/**
 * Represents a version requirement on a Kotlin declaration.
 *
 * Version requirement is an internal feature of the Kotlin compiler and the standard Kotlin library,
 * enabled, for example, with the internal [kotlin.internal.RequireKotlin] annotation.
 */
public class KmVersionRequirement {
    /**
     * Kind of the version that this declaration requires.
     */
    public lateinit var kind: KmVersionRequirementVersionKind

    /**
     * Level of the diagnostic that must be reported on the usages of the declaration in case the version requirement is not satisfied.
     */
    public lateinit var level: KmVersionRequirementLevel

    /**
     * Optional error code to be displayed in the diagnostic.
     */
    public var errorCode: Int? = null

    /**
     * Optional message to be displayed in the diagnostic.
     */
    public var message: String? = null

    /**
     * Version required by this requirement.
     */
    public lateinit var version: KmVersion

    /**
     * Returns the String representation of this KmVersionRequirement object, consisting of
     * [kind], [level], [version], [errorCode], and [message].
     */
    override fun toString(): String {
        return "KmVersionRequirement(kind=$kind, level=$level, version=$version, errorCode=$errorCode, message=$message)"
    }
}

/**
 * Represents a classifier of a Kotlin type. A classifier is a class, type parameter, or type alias.
 * For example, in `MutableMap<in String?, *>`, `MutableMap` is the classifier.
 */
public sealed class KmClassifier {
    /**
     * Represents a class used as a classifier in a type.
     *
     * @property name the name of the class
     */
    public data class Class(val name: ClassName) : KmClassifier()

    /**
     * Represents a type parameter used as a classifier in a type.
     *
     * @property id id of the type parameter
     */
    public data class TypeParameter(val id: Int) : KmClassifier()

    /**
     * Represents a type alias used as a classifier in a type. Note that all types are expanded for metadata produced
     * by the Kotlin compiler, so the type with a type alias classifier may only appear in [KmType.abbreviatedType].
     *
     * @property name the name of the type alias
     */
    public data class TypeAlias(val name: ClassName) : KmClassifier()
}

/**
 * Represents type projection used in a type argument of the type based on a class or on a type alias.
 * For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
 *
 * @property variance the variance of the type projection, or `null` if this is a star projection
 * @property type the projected type, or `null` if this is a star projection
 */
public data class KmTypeProjection(var variance: KmVariance?, var type: KmType?) {
    /**
     * Contains default instance for star projection: [KmTypeProjection.STAR].
     */
    public companion object {
        /**
         * Star projection (`*`).
         * For example, in `MutableMap<in String?, *>`, `*` is the star projection which is the second type argument of the type.
         */
        @JvmField
        public val STAR: KmTypeProjection = KmTypeProjection(null, null)
    }
}

/**
 * Represents an upper bound of a flexible Kotlin type.
 *
 * @property type upper bound of the flexible type
 * @property typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
 *                          or "kotlin.DynamicType" for JS dynamic type
 */
public data class KmFlexibleTypeUpperBound(var type: KmType, var typeFlexibilityId: String?) {
    /**
     * A companion object providing possibility to declare various platform-dependent constant ids as extension properties of it.
     */
    public companion object
}

/**
 * Variance applied to a type parameter on the declaration site (*declaration-site variance*),
 * or to a type in a projection (*use-site variance*).
 */
public enum class KmVariance {
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
 * Represents a version used in a version requirement.
 *
 * @property major the major component of the version (e.g. "1" in "1.2.3")
 * @property minor the minor component of the version (e.g. "2" in "1.2.3")
 * @property patch the patch component of the version (e.g. "3" in "1.2.3")
 */
public data class KmVersion(val major: Int, val minor: Int, val patch: Int) {

    /**
     * Returns a string representation of this version in "$major.$minor.$patch" form.
     */
    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Severity of the diagnostic reported by the compiler when a version requirement is not satisfied.
 */
public enum class KmVersionRequirementLevel {
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
public enum class KmVersionRequirementVersionKind {
    /**
     * Indicates that a certain language version is required.
     */
    LANGUAGE_VERSION,

    /**
     * Indicates that a certain compiler version is required.
     */
    COMPILER_VERSION,

    /**
     * Indicates that a certain API version is required.
     */
    API_VERSION,

    /**
     * Represents a version requirement not successfully parsed from the metadata.
     *
     * The old metadata format (from Kotlin 1.3 and earlier) did not have enough information for correct parsing of version requirements in some cases,
     * so a stub of this kind is inserted instead.
     *
     * [KmVersionRequirement] with this kind always has [KmVersionRequirementLevel.HIDDEN] level, `256.256.256` [KmVersionRequirement.version],
     * and `null` [KmVersionRequirement.errorCode] and [KmVersionRequirement.message].
     *
     * Version requirements of this kind are being ignored by writers (i.e., are not written back).
     *
     * See the following issues for details: [KT-60870](https://youtrack.jetbrains.com/issue/KT-60870), [KT-25120](https://youtrack.jetbrains.com/issue/KT-25120)
     */
    UNKNOWN
    ;
}
