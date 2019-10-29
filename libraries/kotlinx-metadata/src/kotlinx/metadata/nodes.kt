/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package kotlinx.metadata

import kotlinx.metadata.impl.extensions.*

/**
 * Represents a Kotlin declaration container, such as a class or a package fragment.
 */
interface KmDeclarationContainer {
    /**
     * Functions in the container.
     */
    val functions: MutableList<KmFunction>

    /**
     * Properties in the container.
     */
    val properties: MutableList<KmProperty>

    /**
     * Type aliases in the container.
     */
    val typeAliases: MutableList<KmTypeAlias>
}

/**
 * Represents a Kotlin class.
 */
class KmClass : KmClassVisitor(), KmDeclarationContainer {
    /**
     * Class flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Class] flags.
     */
    var flags: Flags = flagsOf()

    /**
     * Name of the class.
     */
    lateinit var name: ClassName

    /**
     * Type parameters of the class.
     */
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Supertypes of the class.
     */
    val supertypes: MutableList<KmType> = ArrayList(1)

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
    val constructors: MutableList<KmConstructor> = ArrayList(1)

    /**
     * Name of the companion object of this class, if it has one.
     */
    var companionObject: String? = null

    /**
     * Names of nested classes of this class.
     */
    val nestedClasses: MutableList<String> = ArrayList(0)

    /**
     * Names of enum entries, if this class is an enum class.
     */
    val enumEntries: MutableList<String> = ArrayList(0)

    /**
     * Names of direct subclasses of this class, if this class is `sealed`.
     */
    val sealedSubclasses: MutableList<ClassName> = ArrayList(0)

    /**
     * Version requirements on this class.
     */
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    private val extensions: List<KmClassExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createClassExtension)

    override fun visit(flags: Flags, name: ClassName) {
        this.flags = flags
        this.name = name
    }

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor =
        KmTypeParameter(flags, name, id, variance).addTo(typeParameters)

    override fun visitSupertype(flags: Flags): KmTypeVisitor =
        KmType(flags).addTo(supertypes)

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor =
        KmFunction(flags, name).addTo(functions)

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor =
        KmProperty(flags, name, getterFlags, setterFlags).addTo(properties)

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor =
        KmTypeAlias(flags, name).addTo(typeAliases)

    override fun visitConstructor(flags: Flags): KmConstructorVisitor =
        KmConstructor(flags).addTo(constructors)

    override fun visitCompanionObject(name: String) {
        this.companionObject = name
    }

    override fun visitNestedClass(name: String) {
        nestedClasses.add(name)
    }

    override fun visitEnumEntry(name: String) {
        enumEntries.add(name)
    }

    override fun visitSealedSubclass(name: ClassName) {
        sealedSubclasses.add(name)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    override fun visitExtensions(type: KmExtensionType): KmClassExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this class.
     *
     * @param visitor the visitor which will visit data in this class
     */
    fun accept(visitor: KmClassVisitor) {
        visitor.visit(flags, name)
        typeParameters.forEach { visitor.visitTypeParameter(it.flags, it.name, it.id, it.variance)?.let(it::accept) }
        supertypes.forEach { visitor.visitSupertype(it.flags)?.let(it::accept) }
        functions.forEach { visitor.visitFunction(it.flags, it.name)?.let(it::accept) }
        properties.forEach { visitor.visitProperty(it.flags, it.name, it.getterFlags, it.setterFlags)?.let(it::accept) }
        typeAliases.forEach { visitor.visitTypeAlias(it.flags, it.name)?.let(it::accept) }
        constructors.forEach { visitor.visitConstructor(it.flags)?.let(it::accept) }
        companionObject?.let(visitor::visitCompanionObject)
        nestedClasses.forEach(visitor::visitNestedClass)
        enumEntries.forEach(visitor::visitEnumEntry)
        sealedSubclasses.forEach(visitor::visitSealedSubclass)
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a Kotlin package fragment, including single file facades and multi-file class parts.
 */
class KmPackage : KmPackageVisitor(), KmDeclarationContainer {
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

    private val extensions: List<KmPackageExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPackageExtension)

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor =
        KmFunction(flags, name).addTo(functions)

    override fun visitProperty(flags: Flags, name: String, getterFlags: Flags, setterFlags: Flags): KmPropertyVisitor =
        KmProperty(flags, name, getterFlags, setterFlags).addTo(properties)

    override fun visitTypeAlias(flags: Flags, name: String): KmTypeAliasVisitor =
        KmTypeAlias(flags, name).addTo(typeAliases)

    override fun visitExtensions(type: KmExtensionType): KmPackageExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this package fragment.
     *
     * @param visitor the visitor which will visit data in this package fragment
     */
    fun accept(visitor: KmPackageVisitor) {
        functions.forEach { visitor.visitFunction(it.flags, it.name)?.let(it::accept) }
        properties.forEach { visitor.visitProperty(it.flags, it.name, it.getterFlags, it.setterFlags)?.let(it::accept) }
        typeAliases.forEach { visitor.visitTypeAlias(it.flags, it.name)?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a Kotlin module fragment. This is used to represent metadata of a part of a module on platforms other than JVM.
 */
class KmModuleFragment : KmModuleFragmentVisitor() {

    /**
     * Top-level functions, type aliases and properties in the module fragment.
     */
    var pkg: KmPackage? = null

    /**
     * Classes in the module fragment.
     */
    val classes: MutableList<KmClass> = ArrayList()

    private val extensions: List<KmModuleFragmentExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createModuleFragmentExtensions)

    override fun visitPackage(): KmPackageVisitor? =
        KmPackage().also { pkg = it }

    override fun visitExtensions(type: KmExtensionType): KmModuleFragmentExtensionVisitor? =
        extensions.singleOfType(type)

    override fun visitClass(): KmClassVisitor? =
        KmClass().addTo(classes)

    /**
     * Populates the given visitor with data in this module fragment.
     *
     * @param visitor the visitor which will visit data in the module fragment.
     */
    fun accept(visitor: KmModuleFragmentVisitor) {
        pkg?.let { visitor.visitPackage()?.let(it::accept) }
        classes.forEach { visitor.visitClass()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a synthetic class generated for a Kotlin lambda.
 */
class KmLambda : KmLambdaVisitor() {
    /**
     * Signature of the synthetic anonymous function, representing the lambda.
     */
    lateinit var function: KmFunction

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor =
        KmFunction(flags, name).also { function = it }

    /**
     * Populates the given visitor with data in this lambda.
     *
     * @param visitor the visitor which will visit data in this lambda
     */
    fun accept(visitor: KmLambdaVisitor) {
        visitor.visitFunction(function.flags, function.name)?.let(function::accept)
        visitor.visitEnd()
    }
}

/**
 * Represents a constructor of a Kotlin class.
 *
 * @property flags constructor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag and [Flag.Constructor] flags
 */
class KmConstructor(var flags: Flags) : KmConstructorVisitor() {
    /**
     * Value parameters of the constructor.
     */
    val valueParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Version requirements on the constructor.
     */
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    private val extensions: List<KmConstructorExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createConstructorExtension)

    override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor =
        KmValueParameter(flags, name).addTo(valueParameters)

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this constructor.
     *
     * @param visitor the visitor which will visit data in this class
     */
    fun accept(visitor: KmConstructorVisitor) {
        valueParameters.forEach { visitor.visitValueParameter(it.flags, it.name)?.let(it::accept) }
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a Kotlin function declaration.
 *
 * @property flags function flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Function] flags
 * @property name the name of the function
 */
class KmFunction(
    var flags: Flags,
    var name: String
) : KmFunctionVisitor() {
    /**
     * Type parameters of the function.
     */
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the function, if this is an extension function.
     */
    var receiverParameterType: KmType? = null

    /**
     * Value parameters of the function.
     */
    val valueParameters: MutableList<KmValueParameter> = ArrayList()

    /**
     * Return type of the function.
     */
    lateinit var returnType: KmType

    /**
     * Version requirements on the function.
     */
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    /**
     * Contract of the function.
     */
    var contract: KmContract? = null

    private val extensions: List<KmFunctionExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createFunctionExtension)

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor =
        KmTypeParameter(flags, name, id, variance).addTo(typeParameters)

    override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { receiverParameterType = it }

    override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor =
        KmValueParameter(flags, name).addTo(valueParameters)

    override fun visitReturnType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { returnType = it }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    override fun visitContract(): KmContractVisitor =
        KmContract().also { contract = it }

    override fun visitExtensions(type: KmExtensionType): KmFunctionExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this function.
     *
     * @param visitor the visitor which will visit data in this function
     */
    fun accept(visitor: KmFunctionVisitor) {
        typeParameters.forEach { visitor.visitTypeParameter(it.flags, it.name, it.id, it.variance)?.let(it::accept) }
        receiverParameterType?.let { visitor.visitReceiverParameterType(it.flags)?.let(it::accept) }
        valueParameters.forEach { visitor.visitValueParameter(it.flags, it.name)?.let(it::accept) }
        visitor.visitReturnType(returnType.flags)?.let(returnType::accept)
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        contract?.let { visitor.visitContract()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a Kotlin property declaration.
 *
 * @property flags property flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag and [Flag.Property] flags
 * @property name the name of the property
 * @property getterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
 *   and [Flag.PropertyAccessor] flags
 * @property setterFlags property accessor flags, consisting of [Flag.HAS_ANNOTATIONS], visibility flag, modality flag
 *   and [Flag.PropertyAccessor] flags
 */
class KmProperty(
    var flags: Flags,
    var name: String,
    var getterFlags: Flags,
    var setterFlags: Flags
) : KmPropertyVisitor() {
    /**
     * Type parameters of the property.
     */
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Type of the receiver of the property, if this is an extension property.
     */
    var receiverParameterType: KmType? = null

    /**
     * Value parameter of the setter of this property, if this is a `var` property.
     */
    var setterParameter: KmValueParameter? = null

    /**
     * Type of the property.
     */
    lateinit var returnType: KmType

    /**
     * Version requirements on the property.
     */
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    private val extensions: List<KmPropertyExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createPropertyExtension)

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor =
        KmTypeParameter(flags, name, id, variance).addTo(typeParameters)

    override fun visitReceiverParameterType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { receiverParameterType = it }

    override fun visitSetterParameter(flags: Flags, name: String): KmValueParameterVisitor =
        KmValueParameter(flags, name).also { setterParameter = it }

    override fun visitReturnType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { returnType = it }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    override fun visitExtensions(type: KmExtensionType): KmPropertyExtensionVisitor =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this property.
     *
     * @param visitor the visitor which will visit data in this property
     */
    fun accept(visitor: KmPropertyVisitor) {
        typeParameters.forEach { visitor.visitTypeParameter(it.flags, it.name, it.id, it.variance)?.let(it::accept) }
        receiverParameterType?.let { visitor.visitReceiverParameterType(it.flags)?.let(it::accept) }
        setterParameter?.let { visitor.visitSetterParameter(it.flags, it.name)?.let(it::accept) }
        visitor.visitReturnType(returnType.flags)?.let(returnType::accept)
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a Kotlin type alias declaration.
 *
 * @property flags type alias flags, consisting of [Flag.HAS_ANNOTATIONS] and visibility flag
 * @property name the name of the type alias
 */
class KmTypeAlias(
    var flags: Flags,
    var name: String
) : KmTypeAliasVisitor() {
    /**
     * Type parameters of the type alias.
     */
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)

    /**
     * Underlying type of the type alias, i.e. the type in the right-hand side of the type alias declaration.
     */
    lateinit var underlyingType: KmType

    /**
     * Expanded type of the type alias, i.e. the full expansion of the underlying type, where all type aliases are substituted
     * with their expanded types. If no type aliases are used in the underlying type, expanded type is equal to the underlying type.
     */
    lateinit var expandedType: KmType

    /**
     * Annotations on the type alias.
     */
    val annotations: MutableList<KmAnnotation> = ArrayList(0)

    /**
     * Version requirements on the type alias.
     */
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    override fun visitTypeParameter(flags: Flags, name: String, id: Int, variance: KmVariance): KmTypeParameterVisitor =
        KmTypeParameter(flags, name, id, variance).addTo(typeParameters)

    override fun visitUnderlyingType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { underlyingType = it }

    override fun visitExpandedType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { expandedType = it }

    override fun visitAnnotation(annotation: KmAnnotation) {
        annotations.add(annotation)
    }

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    /**
     * Populates the given visitor with data in this type alias.
     *
     * @param visitor the visitor which will visit data in this type alias
     */
    fun accept(visitor: KmTypeAliasVisitor) {
        typeParameters.forEach { visitor.visitTypeParameter(it.flags, it.name, it.id, it.variance)?.let(it::accept) }
        visitor.visitUnderlyingType(underlyingType.flags)?.let(underlyingType::accept)
        visitor.visitExpandedType(expandedType.flags)?.let(expandedType::accept)
        annotations.forEach(visitor::visitAnnotation)
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a value parameter of a Kotlin constructor, function or property setter.
 *
 * @property flags value parameter flags, consisting of [Flag.ValueParameter] flags
 * @property name the name of the value parameter
 */
class KmValueParameter(
    var flags: Flags,
    var name: String
) : KmValueParameterVisitor() {
    /**
     * Type of the value parameter, if this is **not** a `vararg` parameter.
     */
    var type: KmType? = null

    /**
     * Type of the value parameter, if this is a `vararg` parameter.
     */
    var varargElementType: KmType? = null

    override fun visitType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { type = it }

    override fun visitVarargElementType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { varargElementType = it }

    /**
     * Populates the given visitor with data in this value parameter.
     *
     * @param visitor the visitor which will visit data in this value parameter
     */
    fun accept(visitor: KmValueParameterVisitor) {
        type?.let { visitor.visitType(it.flags)?.let(it::accept) }
        varargElementType?.let { visitor.visitVarargElementType(it.flags)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a type parameter of a Kotlin class, function, property or type alias.
 *
 * @property flags type parameter flags, consisting of [Flag.TypeParameter] flags
 * @property name the name of the type parameter
 * @property id the id of the type parameter, useful to be able to uniquely identify the type parameter in different contexts where
 *           the name isn't enough (e.g. `class A<T> { fun <T> foo(t: T) }`)
 * @property variance the declaration-site variance of the type parameter
 */
class KmTypeParameter(
    var flags: Flags,
    var name: String,
    var id: Int,
    var variance: KmVariance
) : KmTypeParameterVisitor() {
    /**
     * Upper bounds of the type parameter.
     */
    val upperBounds: MutableList<KmType> = ArrayList(1)

    private val extensions: List<KmTypeParameterExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeParameterExtension)

    override fun visitUpperBound(flags: Flags): KmTypeVisitor =
        KmType(flags).addTo(upperBounds)

    override fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this type parameter.
     *
     * @param visitor the visitor which will visit data in this type parameter
     */
    fun accept(visitor: KmTypeParameterVisitor) {
        upperBounds.forEach { visitor.visitUpperBound(it.flags)?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a type.
 *
 * @property flags type flags, consisting of [Flag.Type] flags
 */
class KmType(var flags: Flags) : KmTypeVisitor() {
    /**
     * Classifier of the type.
     */
    lateinit var classifier: KmClassifier

    /**
     * Arguments of the type, if the type's classifier is a class or a type alias.
     */
    val arguments: MutableList<KmTypeProjection> = ArrayList(0)

    /**
     * Abbreviation of this type. Note that all types are expanded for metadata produced by the Kotlin compiler. For example:
     *
     *     typealias A<T> = MutableList<T>
     *
     *     fun foo(a: A<Any>) {}
     *
     * The type of the `foo`'s parameter in the metadata is actually `MutableList<Any>`, and its abbreviation is `A<Any>`.
     */
    var abbreviatedType: KmType? = null

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
    var outerType: KmType? = null

    /**
     * Upper bound of this type, if this type is flexible. In that case, all other data refers to the lower bound of the type.
     *
     * Flexible types in Kotlin include platform types in Kotlin/JVM and `dynamic` type in Kotlin/JS.
     */
    var flexibleTypeUpperBound: KmFlexibleTypeUpperBound? = null

    private val extensions: List<KmTypeExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeExtension)

    override fun visitClass(name: ClassName) {
        classifier = KmClassifier.Class(name)
    }

    override fun visitTypeAlias(name: ClassName) {
        classifier = KmClassifier.TypeAlias(name)
    }

    override fun visitTypeParameter(id: Int) {
        classifier = KmClassifier.TypeParameter(id)
    }

    override fun visitArgument(flags: Flags, variance: KmVariance): KmTypeVisitor =
        KmType(flags).also { arguments.add(KmTypeProjection(variance, it)) }

    override fun visitStarProjection() {
        arguments.add(KmTypeProjection.STAR)
    }

    override fun visitAbbreviatedType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { abbreviatedType = it }

    override fun visitOuterType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { outerType = it }

    override fun visitFlexibleTypeUpperBound(flags: Flags, typeFlexibilityId: String?): KmTypeVisitor =
        KmType(flags).also { flexibleTypeUpperBound = KmFlexibleTypeUpperBound(it, typeFlexibilityId) }

    override fun visitExtensions(type: KmExtensionType): KmTypeExtension =
        extensions.singleOfType(type)

    /**
     * Populates the given visitor with data in this type.
     *
     * @param visitor the visitor which will visit data in this type
     */
    fun accept(visitor: KmTypeVisitor) {
        when (val classifier = classifier) {
            is KmClassifier.Class -> visitor.visitClass(classifier.name)
            is KmClassifier.TypeParameter -> visitor.visitTypeParameter(classifier.id)
            is KmClassifier.TypeAlias -> visitor.visitTypeAlias(classifier.name)
        }
        arguments.forEach { argument ->
            if (argument == KmTypeProjection.STAR) visitor.visitStarProjection()
            else {
                val (variance, type) = argument
                if (variance == null || type == null)
                    throw InconsistentKotlinMetadataException("Variance and type must be set for non-star type projection")
                visitor.visitArgument(type.flags, variance)?.let(type::accept)
            }
        }
        abbreviatedType?.let { visitor.visitAbbreviatedType(it.flags)?.let(it::accept) }
        outerType?.let { visitor.visitOuterType(it.flags)?.let(it::accept) }
        flexibleTypeUpperBound?.let { visitor.visitFlexibleTypeUpperBound(it.type.flags, it.typeFlexibilityId)?.let(it.type::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a version requirement on a Kotlin declaration.
 *
 * Version requirement is an internal feature of the Kotlin compiler and the standard Kotlin library,
 * enabled for example with the internal [kotlin.internal.RequireKotlin] annotation.
 */
class KmVersionRequirement : KmVersionRequirementVisitor() {
    /**
     * Kind of the version that this declaration requires.
     */
    lateinit var kind: KmVersionRequirementVersionKind

    /**
     * Level of the diagnostic that must be reported on the usages of the declaration in case the version requirement is not satisfied.
     */
    lateinit var level: KmVersionRequirementLevel

    /**
     * Optional error code to be displayed in the diagnostic.
     */
    var errorCode: Int? = null

    /**
     * Optional message to be displayed in the diagnostic.
     */
    var message: String? = null

    /**
     * Version required by this requirement.
     */
    lateinit var version: KmVersion

    override fun visit(kind: KmVersionRequirementVersionKind, level: KmVersionRequirementLevel, errorCode: Int?, message: String?) {
        this.kind = kind
        this.level = level
        this.errorCode = errorCode
        this.message = message
    }

    override fun visitVersion(major: Int, minor: Int, patch: Int) {
        this.version = KmVersion(major, minor, patch)
    }

    /**
     * Populates the given visitor with data in this version requirement.
     *
     * @param visitor the visitor which will visit data in this version requirement
     */
    fun accept(visitor: KmVersionRequirementVisitor) {
        visitor.visit(kind, level, errorCode, message)
        visitor.visitVersion(version.major, version.minor, version.patch)
        visitor.visitEnd()
    }
}

/**
 * Represents a contract of a Kotlin function.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
class KmContract : KmContractVisitor() {
    /**
     * Effects of this contract.
     */
    val effects: MutableList<KmEffect> = ArrayList(1)

    override fun visitEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?): KmEffectVisitor =
        KmEffect(type, invocationKind).addTo(effects)

    /**
     * Populates the given visitor with data in this contract.
     *
     * @param visitor the visitor which will visit data in this contract
     */
    fun accept(visitor: KmContractVisitor) {
        effects.forEach { visitor.visitEffect(it.type, it.invocationKind)?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property type type of the effect
 * @property invocationKind optional number of invocations of the lambda parameter of this function,
 *   specified further in the effect expression
 */
class KmEffect(
    var type: KmEffectType,
    var invocationKind: KmEffectInvocationKind?
) : KmEffectVisitor() {
    /**
     * Arguments of the effect constructor, i.e. the constant value for the [KmEffectType.RETURNS_CONSTANT] effect,
     * or the parameter reference for the [KmEffectType.CALLS] effect.
     */
    val constructorArguments: MutableList<KmEffectExpression> = ArrayList(1)

    /**
     * Conclusion of the effect. If this value is set, the effect represents an implication with this value as the right-hand side.
     */
    var conclusion: KmEffectExpression? = null

    override fun visitConstructorArgument(): KmEffectExpressionVisitor =
        KmEffectExpression().addTo(constructorArguments)

    override fun visitConclusionOfConditionalEffect(): KmEffectExpressionVisitor =
        KmEffectExpression().also { conclusion = it }

    /**
     * Populates the given visitor with data in this effect.
     *
     * @param visitor the visitor which will visit data in this effect
     */
    fun accept(visitor: KmEffectVisitor) {
        constructorArguments.forEach { visitor.visitConstructorArgument()?.let(it::accept) }
        conclusion?.let { visitor.visitConclusionOfConditionalEffect()?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents an effect expression, the contents of an effect (a part of the contract of a Kotlin function).
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 */
class KmEffectExpression : KmEffectExpressionVisitor() {
    /**
     * Effect expression flags, consisting of [Flag.EffectExpression] flags.
     */
    var flags: Flags = flagsOf()

    /**
     * Optional 1-based index of the value parameter of the function, for effects which assert something about
     * the function parameters. The index 0 means the extension receiver parameter.
     */
    var parameterIndex: Int? = null

    /**
     * Constant value used in the effect expression.
     */
    var constantValue: KmConstantValue? = null

    /**
     * Type used as the target of an `is`-expression in the effect expression.
     */
    var isInstanceType: KmType? = null

    /**
     * Arguments of an `&&`-expression. If this list is non-empty, the resulting effect expression is a conjunction of this expression
     * and elements of the list.
     */
    val andArguments: MutableList<KmEffectExpression> = ArrayList(0)

    /**
     * Arguments of an `||`-expression. If this list is non-empty, the resulting effect expression is a disjunction of this expression
     * and elements of the list.
     */
    val orArguments: MutableList<KmEffectExpression> = ArrayList(0)

    override fun visit(flags: Flags, parameterIndex: Int?) {
        this.flags = flags
        this.parameterIndex = parameterIndex
    }

    override fun visitConstantValue(value: Any?) {
        constantValue = KmConstantValue(value)
    }

    override fun visitIsInstanceType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { isInstanceType = it }

    override fun visitAndArgument(): KmEffectExpressionVisitor =
        KmEffectExpression().addTo(andArguments)

    override fun visitOrArgument(): KmEffectExpressionVisitor =
        KmEffectExpression().addTo(orArguments)

    /**
     * Populates the given visitor with data in this effect expression.
     *
     * @param visitor the visitor which will visit data in this effect expression
     */
    fun accept(visitor: KmEffectExpressionVisitor) {
        visitor.visit(flags, parameterIndex)
        constantValue?.let { visitor.visitConstantValue(it.value) }
        isInstanceType?.let { visitor.visitIsInstanceType(it.flags)?.let(it::accept) }
        andArguments.forEach { visitor.visitAndArgument()?.let(it::accept) }
        orArguments.forEach { visitor.visitOrArgument()?.let(it::accept) }
        visitor.visitEnd()
    }
}

/**
 * Represents a classifier of a Kotlin type. A classifier is a class, type parameter or type alias.
 * For example, in `MutableMap<in String?, *>`, `MutableMap` is the classifier.
 */
sealed class KmClassifier {
    /**
     * Represents a class used as a classifier in a type.
     *
     * @property name the name of the class
     */
    data class Class(val name: ClassName) : KmClassifier()

    /**
     * Represents a type parameter used as a classifier in a type.
     *
     * @property id id of the type parameter
     */
    data class TypeParameter(val id: Int) : KmClassifier()

    /**
     * Represents a type alias used as a classifier in a type. Note that all types are expanded for metadata produced
     * by the Kotlin compiler, so the type with a type alias classifier may only appear in [KmType.abbreviatedType].
     *
     * @property name the name of the type alias
     */
    data class TypeAlias(val name: ClassName) : KmClassifier()
}

/**
 * Represents type projection used in a type argument of the type based on a class or on a type alias.
 * For example, in `MutableMap<in String?, *>`, `in String?` is the type projection which is the first type argument of the type.
 *
 * @property variance the variance of the type projection, or `null` if this is a star projection
 * @property type the projected type, or `null` if this is a star projection
 */
data class KmTypeProjection(var variance: KmVariance?, var type: KmType?) {
    companion object {
        /**
         * Star projection (`*`).
         * For example, in `MutableMap<in String?, *>`, `*` is the star projection which is the second type argument of the type.
         */
        @JvmField
        val STAR = KmTypeProjection(null, null)
    }
}

/**
 * Represents an upper bound of a flexible Kotlin type.
 *
 * @property type upper bound of the flexible type
 * @property typeFlexibilityId id of the kind of flexibility this type has. For example, "kotlin.jvm.PlatformType" for JVM platform types,
 *                          or "kotlin.DynamicType" for JS dynamic type
 */
data class KmFlexibleTypeUpperBound(var type: KmType, var typeFlexibilityId: String?)

/**
 * Represents a version used in a version requirement.
 *
 * @property major the major component of the version (e.g. "1" in "1.2.3")
 * @property minor the minor component of the version (e.g. "2" in "1.2.3")
 * @property patch the patch component of the version (e.g. "3" in "1.2.3")
 */
data class KmVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"
}

/**
 * Represents a constant value used in an effect expression.
 *
 * Contracts are an internal feature of the standard Kotlin library, and their behavior and/or binary format
 * may change in a subsequent release.
 *
 * @property value the constant value. May be `true`, `false` or `null`
 */
data class KmConstantValue(val value: Any?)

internal fun <T> T.addTo(collection: MutableCollection<T>): T {
    collection.add(this)
    return this
}
