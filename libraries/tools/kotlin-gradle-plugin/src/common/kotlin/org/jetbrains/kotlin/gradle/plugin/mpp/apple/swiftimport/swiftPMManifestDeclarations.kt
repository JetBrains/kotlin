/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import java.io.File

/**
 * The arguments of the `.package(...)` entry declaring this package in a manifest. A local package is referenced
 * relative to [packageRoot].
 */
internal fun SwiftPMDependency.packageArguments(packageRoot: File): List<String> = buildList {
    when (this@packageArguments) {
        is SwiftPMDependency.Remote -> {
            add(
                when (val repository = repository) {
                    is SwiftPMDependency.Remote.Repository.Id -> "id: \"${repository.value}\""
                    is SwiftPMDependency.Remote.Repository.Url -> "url: \"${repository.value}\""
                }
            )
            add(
                when (val version = version) {
                    is SwiftPMDependency.Remote.Version.Exact -> "exact: \"${version.value}\""
                    is SwiftPMDependency.Remote.Version.From -> "from: \"${version.value}\""
                    is SwiftPMDependency.Remote.Version.Range -> "\"${version.from}\"...\"${version.through}\""
                    is SwiftPMDependency.Remote.Version.Branch -> "branch: \"${version.value}\""
                    is SwiftPMDependency.Remote.Version.Revision -> "revision: \"${version.value}\""
                }
            )
        }
        is SwiftPMDependency.Local -> {
            add("path: \"${absolutePath.normalizedAbsoluteFile().relativeTo(packageRoot).path}\"")
        }
    }
    if (traits.isNotEmpty()) {
        add("traits: [${traits.joinToString(", ") { "\"$it\"" }}]")
    }
}

/**
 * The arguments of the `.product(...)` entry a target uses to depend on this product of [packageName].
 * The platform condition follows [conditionPlatforms].
 */
internal fun SwiftPMDependency.Product.productArguments(
    packageName: String,
    implicitPlatformConstraints: Set<SwiftPMDependency.Platform>?,
    umbrellaPlatforms: Set<SwiftPMDependency.Platform>,
): List<String> = buildList {
    add("name: \"$name\"")
    add("package: \"$packageName\"")
    val conditionPlatforms = conditionPlatforms(
        explicitPlatformConstraints = platformConstraints,
        implicitPlatformConstraints = implicitPlatformConstraints,
        umbrellaPlatforms = umbrellaPlatforms,
    )
    if (conditionPlatforms != null) {
        add("condition: .when(platforms: [${conditionPlatforms.joinToString(", ") { ".${it.swiftEnumName}" }}])")
    }
}
