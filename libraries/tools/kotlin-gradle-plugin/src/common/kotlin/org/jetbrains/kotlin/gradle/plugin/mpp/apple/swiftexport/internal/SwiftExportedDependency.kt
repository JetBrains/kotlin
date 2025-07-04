/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal

import org.gradle.api.Named
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportedModuleMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.getCoordinatesFromGroupNameAndVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

/**
 * A sealed interface representing a dependency to be exported to Swift.
 * It combines the configuration from SwiftExportedModuleMetadata and is Named
 * to be used in Gradle containers.
 */
@ExperimentalSwiftExportDsl
internal sealed interface SwiftExportedDependency : SwiftExportedModuleMetadata, Named

/**
 * Represents an external dependency identified by its Maven coordinates.
 */
@ExperimentalSwiftExportDsl
internal interface SwiftExportedExternalDependency : SwiftExportedDependency {
    /**
     * The Maven coordinates of the dependency.
     */
    @get:Internal
    val coordinates: ModuleVersionIdentifier

    @Internal
    override fun getName(): String = coordinates.let { "${it.group}:${it.name}:${it.version}" }
}

/**
 * Represents an internal dependency on another project within the same build.
 */
@ExperimentalSwiftExportDsl
internal interface SwiftExportedProjectDependency : SwiftExportedDependency {
    /**
     * The path of the Gradle project (e.g., ":shared:core").
     */
    @get:Internal
    val projectPath: String

    @Internal
    override fun getName(): String = projectPath
}

internal val Dependency.moduleVersionIdentifier
    get() = getCoordinatesFromGroupNameAndVersion(group, name, version)

internal val SwiftExportedDependency.inheritedName
    get() = when (this) {
        is SwiftExportedExternalDependency -> coordinates.name
        is SwiftExportedProjectDependency -> projectPath
    }

internal sealed class DefaultSwiftExportedDependency(objectFactory: ObjectFactory) : SwiftExportedModuleMetadata {

    override val moduleName: Property<String> = objectFactory.property(String::class.java)

    override val flattenPackage: Property<String> = objectFactory.property(String::class.java)

    internal class External(
        objectFactory: ObjectFactory,
        override val coordinates: ModuleVersionIdentifier,
    ) : DefaultSwiftExportedDependency(objectFactory), SwiftExportedExternalDependency

    internal class Project(
        objectFactory: ObjectFactory,
        override val projectPath: String,
    ) : DefaultSwiftExportedDependency(objectFactory), SwiftExportedProjectDependency
}