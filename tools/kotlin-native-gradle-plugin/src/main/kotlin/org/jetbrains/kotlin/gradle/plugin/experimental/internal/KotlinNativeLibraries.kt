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
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeLibrary
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

abstract class AbstractKotlinNativeLibrary(
        name: String,
        baseName: Provider<String>,
        componentImplementation: Configuration,
        component: KotlinNativeMainComponent,
        identity: KotlinNativeVariantIdentity,
        projectLayout: ProjectLayout,
        outputKind: CompilerOutputKind,
        objects: ObjectFactory,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : AbstractKotlinNativeBinary(name,
        baseName,
        component,
        identity,
        projectLayout,
        outputKind,
        objects,
        componentImplementation,
        configurations,
        fileOperations),
    KotlinNativeLibrary,
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

open class KotlinNativeLibraryImpl @Inject constructor(
    name: String,
    baseName: Provider<String>,
    componentImplementation: Configuration,
    component: KotlinNativeMainComponent,
    identity: KotlinNativeVariantIdentity,
    projectLayout: ProjectLayout,
    objects: ObjectFactory,
    configurations: ConfigurationContainer,
    fileOperations: FileOperations
) : AbstractKotlinNativeLibrary(
    name,
    baseName,
    componentImplementation,
    component,
    identity,
    projectLayout,
    CompilerOutputKind.LIBRARY,
    objects,
    configurations,
    fileOperations
)

open class KotlinNativeFrameworkImpl @Inject constructor(
    name: String,
    baseName: Provider<String>,
    componentImplementation: Configuration,
    component: KotlinNativeMainComponent,
    identity: KotlinNativeVariantIdentity,
    projectLayout: ProjectLayout,
    objects: ObjectFactory,
    configurations: ConfigurationContainer,
    fileOperations: FileOperations
) : AbstractKotlinNativeLibrary(
    name,
    baseName,
    componentImplementation,
    component,
    identity,
    projectLayout,
    CompilerOutputKind.FRAMEWORK,
    objects,
    configurations,
    fileOperations
)