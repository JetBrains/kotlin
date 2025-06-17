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
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.SwiftExportedModuleMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.getCoordinatesFromGroupNameAndVersion
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl

/**
 * A sealed interface representing a dependency to be exported to Swift.
 * It combines the configuration from SwiftExportedModuleMetadata and is Named
 * to be used in Gradle containers.
 */
@ExperimentalSwiftExportDsl
internal sealed class SwiftExportedDependency(objectFactory: ObjectFactory) : SwiftExportedModuleMetadata, Named {

    override var moduleName: Property<String> = objectFactory.property(String::class.java)
    override var flattenPackage: Property<String> = objectFactory.property(String::class.java)

    internal class External(
        objectFactory: ObjectFactory,
        /**
         * The Maven coordinates of the dependency.
         */
        val coordinates: ModuleVersionIdentifier,
    ) : SwiftExportedDependency(objectFactory) {
        override fun getName(): String = coordinates.let { "${it.group}:${it.name}:${it.version}" }
    }

    internal class Project(
        objectFactory: ObjectFactory,
        /**
         * The path of the Gradle project (e.g., ":shared:core").
         */
        val projectPath: String,
    ) : SwiftExportedDependency(objectFactory) {
        override fun getName(): String = projectPath
    }
}

internal val Dependency.moduleVersionIdentifier
    get() = getCoordinatesFromGroupNameAndVersion(group, name, version)

internal val SwiftExportedDependency.inheritedName
    get() = when (this) {
        is SwiftExportedDependency.External -> coordinates.name
        is SwiftExportedDependency.Project -> projectPath
    }