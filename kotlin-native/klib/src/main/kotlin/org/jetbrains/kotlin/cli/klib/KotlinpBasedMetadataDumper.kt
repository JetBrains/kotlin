/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import kotlin.metadata.KmConstructor
import kotlin.metadata.KmFunction
import kotlin.metadata.KmPackage
import kotlin.metadata.KmProperty
import kotlin.metadata.internal.common.KmModuleFragment
import kotlinx.metadata.klib.KlibModuleMetadata
import kotlinx.metadata.klib.className
import kotlinx.metadata.klib.fqName
import org.jetbrains.kotlin.backend.konan.serialization.KonanIdSignaturer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerDesc
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.kotlinp.Printer
import org.jetbrains.kotlin.kotlinp.Settings
import org.jetbrains.kotlin.kotlinp.klib.*
import org.jetbrains.kotlin.kotlinp.klib.TypeArgumentId.VarianceId
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.*

internal class KotlinpBasedMetadataDumper(
        private val output: KlibToolOutput,
        private val signatureRenderer: IdSignatureRenderer?, // `null` means no signatures should be rendered.
) {

    private val printer = Printer(output)

    /**
     * @param testMode if `true` then a special pre-processing is performed towards the metadata before rendering:
     *        - empty package fragments are removed
     *        - package fragments with the same package FQN are merged
     *        - classes are sorted in alphabetical order
     */
    fun dumpLibrary(library: KotlinLibrary, testMode: Boolean) {
        val moduleMetadata = loadModuleMetadata(library)
                .let { originalModuleMetadata -> if (testMode) preprocessMetadataForTests(originalModuleMetadata) else originalModuleMetadata }

        val signatureComputer = prepareSignatureComputer(library, moduleMetadata)

        KlibKotlinp(DEFAULT_SETTINGS, signatureComputer).renderModule(moduleMetadata, printer)
    }

    private fun preprocessMetadataForTests(originalModuleMetadata: KlibModuleMetadata) = KlibModuleMetadata(
            name = originalModuleMetadata.name,
            fragments = originalModuleMetadata.fragments.groupBy { it.fqName }.mapNotNull { (packageFqName, fragments) ->
                val classNames = fragments.flatMap { it.className }.sorted()
                val classes = fragments.flatMap { it.classes }.sortedBy { it.name }
                val functions = fragments.flatMap { it.pkg?.functions.orEmpty() }.sortedBy { it.sortingKey() }
                val properties = fragments.flatMap { it.pkg?.properties.orEmpty() }.sortedBy { it.sortingKey() }
                val typeAliases = fragments.flatMap { it.pkg?.typeAliases.orEmpty() }.sortedBy { it.name }

                if (classNames.isEmpty() && classes.isEmpty() && functions.isEmpty() && properties.isEmpty() && typeAliases.isEmpty())
                    return@mapNotNull null

                classes.forEach { clazz ->
                    clazz.constructors.sortBy { it.sortingKey() }
                    clazz.functions.sortBy { it.sortingKey() }
                    clazz.properties.sortBy { it.sortingKey() }
                    clazz.typeAliases.sortBy { it.name }
                }

                KmModuleFragment().apply {
                    this.fqName = packageFqName
                    this.className += classNames
                    this.classes += classes

                    if (functions.isNotEmpty() || properties.isNotEmpty() || typeAliases.isNotEmpty()) {
                        this.pkg = KmPackage().apply {
                            this.functions += functions
                            this.properties += properties
                            this.typeAliases += typeAliases
                        }
                    }
                }
            }.sortedBy { it.fqName.orEmpty() },
            annotations = originalModuleMetadata.annotations
    )

    private fun loadModuleMetadata(library: KotlinLibrary) = KlibModuleMetadata.read(
            object : KlibModuleMetadata.MetadataLibraryProvider {
                override val moduleHeaderData get() = library.moduleHeaderData
                override fun packageMetadata(fqName: String, partName: String) = library.packageMetadata(fqName, partName)
                override fun packageMetadataParts(fqName: String) = library.packageMetadataParts(fqName)
            }
    )

    private fun prepareSignatureComputer(library: KotlinLibrary, moduleMetadata: KlibModuleMetadata): ExternalSignatureComputer? {
        val signatureCollector = SignaturesCollector(signatureRenderer ?: return null)

        val moduleDescriptor = ModuleDescriptorLoader(output).load(library)
        moduleDescriptor.accept(signatureCollector, Unit)

        return ExternalSignatureComputer(moduleMetadata, signatureCollector.signatures::get)
    }

    private fun KmConstructor.sortingKey() = constructorId("")
    private fun KmFunction.sortingKey() = functionId("")
    private fun KmProperty.sortingKey() = propertyId("")

    companion object {
        private val DEFAULT_SETTINGS = Settings(isVerbose = true, sortDeclarations = true)
    }
}

private class SignaturesCollector(private val renderer: IdSignatureRenderer) : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
    private val signaturer = KonanIdSignaturer(KonanManglerDesc)

    val signatures = hashMapOf<DeclarationId, String>()

    override fun visitModuleDeclaration(module: ModuleDescriptor, data: Unit) {
        module.getPackageFragments().forEach { it.accept(this, data) }
    }

    override fun visitPackageFragmentDescriptor(fragment: PackageFragmentDescriptor, data: Unit) {
        fragment.getMemberScope().getContributedDescriptors().forEach { it.accept(this, data) }
    }

    override fun visitClassDescriptor(clazz: ClassDescriptor, data: Unit) {
        if (clazz.kind == ClassKind.ENUM_ENTRY) {
            collectEnumEntrySignature(clazz)
        } else {
            collectSignature(clazz) { clazz.id() }
        }

        clazz.constructors.forEach { it.accept(this, data) }
        clazz.unsubstitutedMemberScope.getContributedDescriptors().forEach { it.accept(this, data) }
    }

    override fun visitConstructorDescriptor(constructor: ConstructorDescriptor, data: Unit) {
        collectSignature(constructor) { constructor.id() }
    }

    override fun visitFunctionDescriptor(function: FunctionDescriptor, data: Unit) {
        collectSignature(function) { function.id() }
    }

    override fun visitPropertyDescriptor(property: PropertyDescriptor, data: Unit) {
        collectSignature(property) { property.id() }
        property.getter?.accept(this, data)
        property.setter?.accept(this, data)
    }

    override fun visitPropertyGetterDescriptor(getter: PropertyGetterDescriptor, data: Unit) {
        collectSignature(getter) { getter.id() }
    }

    override fun visitPropertySetterDescriptor(setter: PropertySetterDescriptor, data: Unit) {
        collectSignature(setter) { setter.id(ignoreParameterNames = true) }
    }

    override fun visitTypeAliasDescriptor(typeAlias: TypeAliasDescriptor, data: Unit) {
        collectSignature(typeAlias) { typeAlias.id() }
    }

    private inline fun <T : DeclarationDescriptor> collectSignature(descriptor: T, id: T.() -> DeclarationId?) {
        val rawSignature = signaturer.composeSignature(descriptor) ?: return
        val renderedSignature = renderer.render(rawSignature)
        val declarationId = descriptor.id() ?: return
        signatures[declarationId] = renderedSignature
    }

    private fun collectEnumEntrySignature(clazz: ClassDescriptor) {
        val rawSignature = signaturer.composeEnumEntrySignature(clazz) ?: return
        val renderedSignature = renderer.render(rawSignature)
        val declarationId = clazz.id() ?: return
        signatures[declarationId] = renderedSignature
    }

    private fun ClassifierDescriptorWithTypeParameters.id() = classId?.asString()?.let(::ClassOrTypeAliasId)

    private fun DeclarationDescriptor.qualifiedName(): String {
        fun ClassifierDescriptorWithTypeParameters.classIdOrFail() = classId
                ?: error("Failed to compute class ID for ${this::class.java}, $this")

        return when (this) {
            is ClassifierDescriptorWithTypeParameters -> classIdOrFail()
            is CallableDescriptor -> when (val containingDeclaration = containingDeclaration) {
                is ClassifierDescriptorWithTypeParameters -> containingDeclaration.classIdOrFail().createNestedClassId(name)
                is PackageFragmentDescriptor -> ClassId(containingDeclaration.fqName, name)
                else -> containingDeclaration.unexpectedDeclarationType()
            }
            else -> unexpectedDeclarationType()
        }.asString()
    }

    private fun ConstructorDescriptor.id() = ConstructorId(
            qualifiedName = qualifiedName(),
            parameters = valueParameters.map { it.id(ignoreName = false) }
    )

    private fun PropertyDescriptor.id() = PropertyId(
            qualifiedName = qualifiedName(),
            contextReceivers = contextReceiverParameters.map { it.type.id() },
            extensionReceiver = extensionReceiverParameter?.type?.id(),
            returnType = type.id()
    )

    private fun FunctionDescriptor.id(ignoreParameterNames: Boolean = false) = FunctionId(
            qualifiedName = qualifiedName(),
            contextReceivers = contextReceiverParameters.map { it.type.id() },
            extensionReceiver = extensionReceiverParameter?.type?.id(),
            parameters = valueParameters.map { it.id(ignoreParameterNames) },
            returnType = returnType?.id() ?: error("Function without return type: ${this::class.java}, $this")
    )

    private fun Variance.id() = when (this) {
        Variance.INVARIANT -> VarianceId.INVARIANT
        Variance.IN_VARIANCE -> VarianceId.IN
        Variance.OUT_VARIANCE -> VarianceId.OUT
    }

    private fun SimpleType.classifierId(): ClassifierId = when (val typeConstructorDescriptor = constructor.declarationDescriptor) {
        is TypeParameterDescriptor -> TypeParameterId(typeConstructorDescriptor.index)
        is ClassifierDescriptorWithTypeParameters -> ClassOrTypeAliasId(typeConstructorDescriptor.qualifiedName())
        else -> typeConstructorDescriptor.unexpectedDeclarationType()
    }

    private fun TypeProjection.id() = if (isStarProjection) TypeArgumentId.Star else TypeArgumentId.Regular(type.id(), projectionKind.id())

    private fun KotlinType.id(): TypeId {
        val simpleType = asSimpleType()
        return TypeId(simpleType.classifierId(), simpleType.arguments.map { it.id() })
    }

    private fun ValueParameterDescriptor.id(ignoreName: Boolean) =
            if (ignoreName) ParameterId(type.id(), isVararg) else ParameterId(name.asString(), type.id(), isVararg)

    private fun DeclarationDescriptor?.unexpectedDeclarationType(): Nothing =
            error(if (this == null) "Declaration descriptor is null" else "Unexpected declaration type: ${this::class.java}, $this")
}
