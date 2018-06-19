package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.PublishableComponent
import org.gradle.api.file.ProjectLayout
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.language.cpp.internal.DefaultUsageContext
import org.gradle.nativeplatform.Linkage
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeKLibrary
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

open class KotlinNativeKLibraryImpl @Inject constructor(
        name: String,
        baseName: Provider<String>,
        componentImplementation: Configuration,
        sources: KotlinNativeSourceSet,
        identity: KotlinNativeVariantIdentity,
        objects: ObjectFactory,
        projectLayout: ProjectLayout,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : AbstractKotlinNativeBinary(name,
        baseName,
        sources,
        identity,
        projectLayout,
        CompilerOutputKind.LIBRARY,
        objects,
        componentImplementation,
        configurations,
        fileOperations),
    KotlinNativeKLibrary,
    SoftwareComponentInternal, // TODO: SoftwareComponentInternal will be replaced with ComponentWithVariants by Gradle
    PublishableComponent
{
    override fun getCoordinates(): ModuleVersionIdentifier = identity.coordinates

    // Properties

    // The link elements configuration is created by the NativeBase plugin.
    private val linkElementsProperty: Property<Configuration> = objects.property(Configuration::class.java)
    private val linkFileProperty: RegularFileProperty = projectLayout.fileProperty()

    // Interface

    override fun getLinkElements()  = linkElementsProperty
    override fun getLinkFile() = linkFileProperty

    override fun getUsages(): Set<UsageContext> = linkElementsProperty.get().let {
        setOf(DefaultUsageContext(identity.linkUsageContext, it.allArtifacts, it))
    }

    override fun getLinkAttributes(): AttributeContainer = identity.linkUsageContext.attributes

    // TODO: Does Klib really match a static linkage in Gradle's terms?
    override fun getLinkage(): Linkage? = Linkage.STATIC

    override val outputRootName = "lib"
}
