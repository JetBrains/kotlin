package org.jetbrains.kotlin.idea.artifacts

import java.io.File

object AdditionalKotlinArtifacts : BaseKotlinArtifactsProvider() {
    val kotlinStdlibCommon: File by lazy {
        findLibrary(RepoLocation.MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common")
    }

    val kotlinStdlibCommonSources: File by lazy {
        findLibrary(RepoLocation.MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common", LibraryFileKind.SOURCES)
    }
}
