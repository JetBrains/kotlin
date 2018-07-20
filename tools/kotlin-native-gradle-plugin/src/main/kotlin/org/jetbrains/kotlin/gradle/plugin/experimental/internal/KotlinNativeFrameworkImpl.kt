package org.jetbrains.kotlin.gradle.plugin.experimental.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.file.ProjectLayout
import org.gradle.api.internal.file.FileOperations
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.experimental.KotlinNativeFramework
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import javax.inject.Inject


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
) : AbstractKotlinNativeBinary(
    name,
    baseName,
    component,
    identity,
    projectLayout,
    CompilerOutputKind.FRAMEWORK,
    objects,
    componentImplementation,
    configurations,
    fileOperations
), KotlinNativeFramework {
    override val outputRootName: String = "lib"
}