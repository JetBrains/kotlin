package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.DomainObjectSet
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Repository
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.SwiftPMDependency.Remote.Version
import java.io.File
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
        url: String,
        version: String,
        products: List<String>,
        packageName: String = inferPackageName(url),
        importedModules: List<String> = products
    ) {
        spmDependencies.add(
            SwiftPMDependency.Remote(
                repository = Repository.Url(url),
                version = Version.From(version),
                packageName = packageName,
                products = products,
                cinteropClangModules = importedModules
            )
        )
    }

    fun `package`(
        url: Repository.Url,
        version: Version,
        products: List<String>,
        packageName: String = inferPackageName(url.value),
        importedModules: List<String> = products
    ) {
        spmDependencies.add(
            SwiftPMDependency.Remote(
                repository = url,
                version = version,
                packageName = packageName,
                products = products,
                cinteropClangModules = importedModules
            )
        )
    }

    fun `package`(
        repository: Repository,
        version: Version,
        products: List<String>,
        packageName: String,
        importedModules: List<String> = products
    ) {
        spmDependencies.add(
            SwiftPMDependency.Remote(
                repository = repository,
                version = version,
                packageName = packageName,
                products = products,
                cinteropClangModules = importedModules
            )
        )
    }

    fun url(value: String): Repository.Url = Repository.Url(value)
    fun id(value: String): Repository.Id = Repository.Id(value)

    fun from(version: String): Version = Version.From(version)
    fun exact(version: String): Version = Version.Exact(version)
    fun revision(version: String): Version = Version.Revision(version)
    fun branch(version: String): Version = Version.Branch(version)
    fun range(from: String, through: String): Version = Version.Range(from, through)

    fun localPackage(
        path: File,
        products: List<String>,
        importedModules: List<String> = products
    ) {
        spmDependencies.add(
            SwiftPMDependency.Local(
                path = path.path,
                packageName = path.name,
                products = products,
                cinteropClangModules = importedModules
            )
        )
    }

    // FIXME: check if this is actually correct
    private fun inferPackageName(url: String) = url.split("/").last().split(".git").first()

    companion object {
        const val EXTENSION_NAME = "swiftPMDependencies"
    }
}

sealed class SwiftPMDependency(
    val packageName: String,
    val products: List<String>,
    val cinteropClangModules: List<String>,
) : Serializable {
    class Remote internal constructor(
        val repository: Repository,
        val version: Version,
        // FIXME: This can actually be inferred from the repository URL
        products: List<String>,
        cinteropClangModules: List<String>,
        packageName: String,
    ) : SwiftPMDependency(
        products = products,
        cinteropClangModules = cinteropClangModules,
        packageName = packageName,
    ) {
        sealed class Version : Serializable {
            data class Exact internal constructor(val value: String) : Version()
            data class From internal constructor(val value: String) : Version()
            data class Range internal constructor(val from: String, val through: String) : Version()
            data class Branch internal constructor(val value: String) : Version()
            data class Revision internal constructor(val value: String) : Version()
        }

        sealed class Repository : Serializable {
            data class Id internal constructor(val value: String) : Repository()
            data class Url internal constructor(val value: String) : Repository()
        }
    }

    class Local internal constructor(
        val path: String,
        products: List<String>,
        cinteropClangModules: List<String>,
        packageName: String,
    ) : SwiftPMDependency(
        products = products,
        cinteropClangModules = cinteropClangModules,
        packageName = packageName,
    )
}