package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DomainObjectSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.io.Serializable
import javax.inject.Inject

abstract class SwiftImportExtension @Inject constructor(
    private val objects: ObjectFactory,
) {
    // FIXME: Maybe tests against CI RECOMMENDED_ version to keep up to date?
    val iosDeploymentVersion: Property<String> = objects.property(String::class.java)
        .convention("15.0")
    val macosDeploymentVersion: Property<String> = objects.property(String::class.java)
        .convention("10.15")
    val watchosDeploymentVersion: Property<String> = objects.property(String::class.java)
        .convention("15.0")
    val tvosDeploymentVersion: Property<String> = objects.property(String::class.java)
        .convention("7.0")

    internal abstract val spmDependencies: DomainObjectSet<SwiftPMDependency>

    fun `package`(
        repository: String,
        fromVersion: String,
        products: List<String>,
        cinteropClangModules: List<String> = products
    ) {
        // FIXME: check if this is actually correct
        val packageName = repository.split("/").last().split(".git").first()
        spmDependencies.add(
            SwiftPMDependency(
                repository = repository,
                fromVersion = fromVersion,
                packageName = packageName,
                products = products,
                cinteropClangModules = cinteropClangModules
            )
        )
    }

    companion object {
        const val EXTENSION_NAME = "swiftPMDependencies"
    }
}

internal data class SwiftPMDependency(
    val repository: String,
    val fromVersion: String,
    // FIXME: This can actually be inferred from the repository URL
    val packageName: String,
    val products: List<String>,
    val cinteropClangModules: List<String>,
) : Serializable