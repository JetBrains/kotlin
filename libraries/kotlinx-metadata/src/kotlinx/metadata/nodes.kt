/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("MemberVisibilityCanBePrivate")

package kotlinx.metadata

import kotlinx.metadata.impl.extensions.*

interface KmDeclarationContainer {
    val functions: MutableList<KmFunction>
    val properties: MutableList<KmProperty>
    val typeAliases: MutableList<KmTypeAlias>
}

class KmClass : KmClassVisitor(), KmDeclarationContainer {
    var flags: Flags = flagsOf()
    lateinit var name: ClassName
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)
    val supertypes: MutableList<KmType> = ArrayList(1)
    override val functions: MutableList<KmFunction> = ArrayList()
    override val properties: MutableList<KmProperty> = ArrayList()
    override val typeAliases: MutableList<KmTypeAlias> = ArrayList(0)
    val constructors: MutableList<KmConstructor> = ArrayList(1)
    var companionObject: String? = null
    val nestedClasses: MutableList<String> = ArrayList(0)
    val enumEntries: MutableList<String> = ArrayList(0)
    val sealedSubclasses: MutableList<ClassName> = ArrayList(0)
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

class KmPackage : KmPackageVisitor(), KmDeclarationContainer {
    override val functions: MutableList<KmFunction> = ArrayList()
    override val properties: MutableList<KmProperty> = ArrayList()
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

    fun accept(visitor: KmPackageVisitor) {
        functions.forEach { visitor.visitFunction(it.flags, it.name)?.let(it::accept) }
        properties.forEach { visitor.visitProperty(it.flags, it.name, it.getterFlags, it.setterFlags)?.let(it::accept) }
        typeAliases.forEach { visitor.visitTypeAlias(it.flags, it.name)?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmLambda : KmLambdaVisitor() {
    lateinit var function: KmFunction

    override fun visitFunction(flags: Flags, name: String): KmFunctionVisitor =
        KmFunction(flags, name).also { function = it }

    fun accept(visitor: KmLambdaVisitor) {
        visitor.visitFunction(function.flags, function.name)?.let(function::accept)
        visitor.visitEnd()
    }
}

class KmConstructor(var flags: Flags) : KmConstructorVisitor() {
    val valueParameters: MutableList<KmValueParameter> = ArrayList()
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)

    private val extensions: List<KmConstructorExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createConstructorExtension)

    override fun visitValueParameter(flags: Flags, name: String): KmValueParameterVisitor =
        KmValueParameter(flags, name).addTo(valueParameters)

    override fun visitVersionRequirement(): KmVersionRequirementVisitor =
        KmVersionRequirement().addTo(versionRequirements)

    override fun visitExtensions(type: KmExtensionType): KmConstructorExtensionVisitor =
        extensions.singleOfType(type)

    fun accept(visitor: KmConstructorVisitor) {
        valueParameters.forEach { visitor.visitValueParameter(it.flags, it.name)?.let(it::accept) }
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmFunction(
    var flags: Flags,
    var name: String
) : KmFunctionVisitor() {
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)
    var receiverParameterType: KmType? = null
    val valueParameters: MutableList<KmValueParameter> = ArrayList()
    lateinit var returnType: KmType
    val versionRequirements: MutableList<KmVersionRequirement> = ArrayList(0)
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

class KmProperty(
    var flags: Flags,
    var name: String,
    var getterFlags: Flags,
    var setterFlags: Flags
) : KmPropertyVisitor() {
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)
    var receiverParameterType: KmType? = null
    var setterParameter: KmValueParameter? = null
    lateinit var returnType: KmType
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

class KmTypeAlias(
    var flags: Flags,
    var name: String
) : KmTypeAliasVisitor() {
    val typeParameters: MutableList<KmTypeParameter> = ArrayList(0)
    lateinit var underlyingType: KmType
    lateinit var expandedType: KmType
    val annotations: MutableList<KmAnnotation> = ArrayList(0)
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

    fun accept(visitor: KmTypeAliasVisitor) {
        typeParameters.forEach { visitor.visitTypeParameter(it.flags, it.name, it.id, it.variance)?.let(it::accept) }
        visitor.visitUnderlyingType(underlyingType.flags)?.let(underlyingType::accept)
        visitor.visitExpandedType(expandedType.flags)?.let(expandedType::accept)
        annotations.forEach(visitor::visitAnnotation)
        versionRequirements.forEach { visitor.visitVersionRequirement()?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmValueParameter(
    var flags: Flags,
    var name: String
) : KmValueParameterVisitor() {
    var type: KmType? = null
    var varargElementType: KmType? = null

    override fun visitType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { type = it }

    override fun visitVarargElementType(flags: Flags): KmTypeVisitor =
        KmType(flags).also { varargElementType = it }

    fun accept(visitor: KmValueParameterVisitor) {
        type?.let { visitor.visitType(it.flags)?.let(it::accept) }
        varargElementType?.let { visitor.visitVarargElementType(it.flags)?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmTypeParameter(
    var flags: Flags,
    var name: String,
    var id: Int,
    var variance: KmVariance
) : KmTypeParameterVisitor() {
    val upperBounds: MutableList<KmType> = ArrayList(1)

    private val extensions: List<KmTypeParameterExtension> =
        MetadataExtensions.INSTANCES.map(MetadataExtensions::createTypeParameterExtension)

    override fun visitUpperBound(flags: Flags): KmTypeVisitor =
        KmType(flags).addTo(upperBounds)

    override fun visitExtensions(type: KmExtensionType): KmTypeParameterExtensionVisitor? =
        extensions.singleOfType(type)

    fun accept(visitor: KmTypeParameterVisitor) {
        upperBounds.forEach { visitor.visitUpperBound(it.flags)?.let(it::accept) }
        extensions.forEach { visitor.visitExtensions(it.type)?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmType(var flags: Flags) : KmTypeVisitor() {
    lateinit var classifier: KmClassifier
    val arguments: MutableList<KmTypeProjection> = ArrayList(0)
    var abbreviatedType: KmType? = null
    var outerType: KmType? = null
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

class KmVersionRequirement : KmVersionRequirementVisitor() {
    lateinit var kind: KmVersionRequirementVersionKind
    lateinit var level: KmVersionRequirementLevel
    var errorCode: Int? = null
    var message: String? = null
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

    fun accept(visitor: KmVersionRequirementVisitor) {
        visitor.visit(kind, level, errorCode, message)
        visitor.visitVersion(version.major, version.minor, version.patch)
        visitor.visitEnd()
    }
}

class KmContract : KmContractVisitor() {
    val effects: MutableList<KmEffect> = ArrayList(1)

    override fun visitEffect(type: KmEffectType, invocationKind: KmEffectInvocationKind?): KmEffectVisitor =
        KmEffect(type, invocationKind).addTo(effects)

    fun accept(visitor: KmContractVisitor) {
        effects.forEach { visitor.visitEffect(it.type, it.invocationKind)?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmEffect(
    var type: KmEffectType,
    var invocationKind: KmEffectInvocationKind?
) : KmEffectVisitor() {
    val constructorArguments: MutableList<KmEffectExpression> = ArrayList(1)
    var conclusion: KmEffectExpression? = null

    override fun visitConstructorArgument(): KmEffectExpressionVisitor =
        KmEffectExpression().addTo(constructorArguments)

    override fun visitConclusionOfConditionalEffect(): KmEffectExpressionVisitor =
        KmEffectExpression().also { conclusion = it }

    fun accept(visitor: KmEffectVisitor) {
        constructorArguments.forEach { visitor.visitConstructorArgument()?.let(it::accept) }
        conclusion?.let { visitor.visitConclusionOfConditionalEffect()?.let(it::accept) }
        visitor.visitEnd()
    }
}

class KmEffectExpression : KmEffectExpressionVisitor() {
    var flags: Flags = flagsOf()
    var parameterIndex: Int? = null
    var constantValue: KmConstantValue? = null
    var isInstanceType: KmType? = null
    val andArguments: MutableList<KmEffectExpression> = ArrayList(0)
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

    fun accept(visitor: KmEffectExpressionVisitor) {
        visitor.visit(flags, parameterIndex)
        constantValue?.let { visitor.visitConstantValue(it.value) }
        isInstanceType?.let { visitor.visitIsInstanceType(it.flags)?.let(it::accept) }
        andArguments.forEach { visitor.visitAndArgument()?.let(it::accept) }
        orArguments.forEach { visitor.visitOrArgument()?.let(it::accept) }
        visitor.visitEnd()
    }
}

sealed class KmClassifier {
    data class Class(val name: ClassName) : KmClassifier()

    data class TypeParameter(val id: Int) : KmClassifier()

    data class TypeAlias(val name: ClassName) : KmClassifier()
}

data class KmTypeProjection(var variance: KmVariance?, var type: KmType?) {
    companion object {
        @JvmField
        val STAR = KmTypeProjection(null, null)
    }
}

data class KmFlexibleTypeUpperBound(var type: KmType, var typeFlexibilityId: String?)

data class KmVersion(val major: Int, val minor: Int, val patch: Int) {
    override fun toString(): String = "$major.$minor.$patch"
}

data class KmConstantValue(val value: Any?)

internal fun <T> T.addTo(collection: MutableCollection<T>): T {
    collection.add(this)
    return this
}
