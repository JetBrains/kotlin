package org.jetbrains.kotlin.tools.projectWizard.library

import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.Repository
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

sealed class LibraryArtifact

data class MavenArtifact(
    val repository: Repository,
    val groupId: String,
    val artifactId: String
) : LibraryArtifact()

data class NpmArtifact(
    val name: String
) : LibraryArtifact()


sealed class LibraryDescriptor {
    abstract val artifact: LibraryArtifact
    abstract val type: LibraryType
    abstract val version: Version
}

data class MavenLibraryDescriptor(
    override val artifact: MavenArtifact,
    override val type: LibraryType,
    override val version: Version
) : LibraryDescriptor()


data class NpmLibraryDescriptor(
    override val artifact: NpmArtifact,
    override val version: Version
) : LibraryDescriptor() {
    override val type: LibraryType get() = LibraryType.NPM
}


sealed class LibraryType(
    val supportJvm: Boolean,
    val supportJs: Boolean,
    val supportNative: Boolean
) {
    object JVM_ONLY : LibraryType(supportJvm = true, supportJs = false, supportNative = false)
    object JS_ONLY : LibraryType(supportJvm = false, supportJs = true, supportNative = false)
    object NPM : LibraryType(supportJvm = false, supportJs = true, supportNative = false)
    object MULTIPLATFORM : LibraryType(supportJvm = true, supportJs = true, supportNative = true)
}

