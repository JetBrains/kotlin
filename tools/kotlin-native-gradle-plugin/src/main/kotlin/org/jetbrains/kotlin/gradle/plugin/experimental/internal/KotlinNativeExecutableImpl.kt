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
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeExecutable
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject

open class KotlinNativeExecutableImpl @Inject constructor(
        name: String,
        baseName: Provider<String>,
        componentImplementation: Configuration,
        component: KotlinNativeMainComponent,
        identity: KotlinNativeVariantIdentity,
        objects: ObjectFactory,
        projectLayout: ProjectLayout,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : AbstractKotlinNativeBinary(name,
        baseName,
        component,
        identity,
        projectLayout,
        CompilerOutputKind.PROGRAM,
        objects,
        componentImplementation,
        configurations,
        fileOperations),
    KotlinNativeExecutable,
    SoftwareComponentInternal, // TODO: SoftwareComponentInternal will be replaced with ComponentWithVariants by Gradle
    PublishableComponent
{
    override fun getCoordinates(): ModuleVersionIdentifier = identity.coordinates

    // Properties

    // Runtime elements configuration is created by the NativeBase plugin
    private val runtimeElementsProperty: Property<Configuration> = objects.property(Configuration::class.java)
    private val runtimeFileProperty: RegularFileProperty = projectLayout.fileProperty()

    // Interface Implementation
    override fun getRuntimeElements() = runtimeElementsProperty
    override fun getRuntimeFile() = runtimeFileProperty

    override fun hasRuntimeFile() = true
    override fun getRuntimeAttributes(): AttributeContainer = identity.runtimeUsageContext.attributes
    override fun getLinkage(): Linkage? = null

    override fun getUsages(): Set<UsageContext> = runtimeElementsProperty.get().let {
        setOf(DefaultUsageContext(identity.runtimeUsageContext, it.allArtifacts, it))
    }

    override val outputRootName = "exe"
}
