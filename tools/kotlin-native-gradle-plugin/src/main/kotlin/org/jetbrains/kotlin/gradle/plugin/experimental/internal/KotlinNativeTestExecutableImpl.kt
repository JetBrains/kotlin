package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.FileCollection
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeTestComponent
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeTestExecutable
import org.jetbrains.kotlin.gradle.plugin.experimental.sourcesets.KotlinNativeSourceSet
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject


open class KotlinNativeTestExecutableImpl @Inject constructor(
        name: String,
        baseName: Provider<String>,
        componentImplementation: Configuration,
        testComponent: KotlinNativeTestComponent,
        val mainSources: KotlinNativeSourceSet,
        identity: KotlinNativeVariantIdentity,
        objects: ObjectFactory,
        projectLayout: ProjectLayout,
        configurations: ConfigurationContainer,
        fileOperations: FileOperations
) : AbstractKotlinNativeBinary(name,
        baseName,
        testComponent,
        identity,
        projectLayout,
        CompilerOutputKind.PROGRAM,
        objects,
        componentImplementation,
        configurations,
        fileOperations),
    KotlinNativeTestExecutable {

    private val runTaskProperty : Property<Task> = objects.property(Task::class.java)
    override fun getRunTask() = runTaskProperty

    override val sources: FileCollection
        get() = super.sources + mainSources.getAllSources(konanTarget)

    override val outputRootName: String = "test-exe"

    override val additionalCompilerOptions: Collection<String>
            get() = listOf("-tr") + super.additionalCompilerOptions
}