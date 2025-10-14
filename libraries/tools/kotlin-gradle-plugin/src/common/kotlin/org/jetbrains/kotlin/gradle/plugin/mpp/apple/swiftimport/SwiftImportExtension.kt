package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import java.io.Serializable

abstract class SwiftImportExtension {

    abstract val spmDependencies: DomainObjectSet<SwiftPMDependency>
}

data class SwiftPMDependency(
    val repository: String,
    val version: String,
    val packageName: String,
    val products: List<String>,
) : Serializable