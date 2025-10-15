package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DomainObjectCollection
import org.gradle.api.DomainObjectSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.Serializable
import javax.inject.Inject

abstract class SwiftImportExtension @Inject constructor(
    private val objects: ObjectFactory,
) {
    // FIXME: Maybe tests with against RECOMMENDED_ version?
    val iosDeploymentVersion: Property<String> = objects.property(String::class.java).convention(
        "15.0"
    )
    abstract val spmDependencies: DomainObjectSet<SwiftPMDependency>
}

data class SwiftPMDependency(
    val repository: String,
    val version: String,
    val packageName: String,
    val products: List<String>,
    val cinteropTargets: List<String>,
) : Serializable