package org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem

import org.jetbrains.kotlin.tools.projectWizard.core.safeAs
import org.jetbrains.kotlin.tools.projectWizard.ir.buildsystem.gradle.GradleIR
import org.jetbrains.kotlin.tools.projectWizard.library.LibraryArtifact
import org.jetbrains.kotlin.tools.projectWizard.library.MavenArtifact
import org.jetbrains.kotlin.tools.projectWizard.library.NpmArtifact
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.BuildFilePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.GradlePrinter
import org.jetbrains.kotlin.tools.projectWizard.plugins.printer.MavenPrinter
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.DefaultRepository.Companion.MAVEN_CENTRAL
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.ModulePath
import org.jetbrains.kotlin.tools.projectWizard.settings.buildsystem.SourcesetType
import org.jetbrains.kotlin.tools.projectWizard.settings.version.Version

interface DependencyIR : BuildSystemIR {
    val dependencyType: DependencyType

    fun withDependencyType(type: DependencyType): DependencyIR
}

data class ModuleDependencyIR(
    val path: ModulePath,
    val pomIR: PomIR,
    override val dependencyType: DependencyType
) : DependencyIR {
    override fun withDependencyType(type: DependencyType): DependencyIR = copy(dependencyType = type)

    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> call(dependencyType.gradleName) {
            call("project", forceBrackets = true) {
                +path.parts.joinToString(separator = "") { ":$it" }.quotified
            }
        }
        is MavenPrinter -> node("dependency") {
            pomIR.render(this)
        }
        else -> Unit
    }
}

data class GradleRootProjectDependencyIR(
    override val dependencyType: DependencyType
) : DependencyIR, GradleIR {
    override fun withDependencyType(type: DependencyType): GradleRootProjectDependencyIR =
        copy(dependencyType = type)

    override fun GradlePrinter.renderGradle() {
        +"rootProject"
    }
}

interface LibraryDependencyIR : DependencyIR {
    val artifact: LibraryArtifact
    val version: Version
}

enum class DependencyType {
    MAIN, TEST
}

fun SourcesetType.toDependencyType() = when (this) {
    SourcesetType.main -> DependencyType.MAIN
    SourcesetType.test -> DependencyType.TEST
}

val DependencyType.gradleName
    get() = when (this) {
        DependencyType.MAIN -> "implementation"
        DependencyType.TEST -> "testImplementation"
    }

data class ArtifactBasedLibraryDependencyIR(
    override val artifact: LibraryArtifact,
    override val version: Version,
    override val dependencyType: DependencyType
) : LibraryDependencyIR {
    override fun withDependencyType(type: DependencyType): ArtifactBasedLibraryDependencyIR =
        copy(dependencyType = type)

    override fun BuildFilePrinter.render() = when (this) {
        is GradlePrinter -> call(dependencyType.gradleName) {
            with(artifact) {
                when (this) {
                    is MavenArtifact -> +"$groupId:$artifactId:${version}".quotified
                    is NpmArtifact -> {
                        +"npm(${name.quotified}"
                        +","
                        +version.toString().quotified
                        +")"
                    }
                }
            }
        }
        is MavenPrinter -> {
            node("dependency") {
                with(artifact as MavenArtifact) {
                    singleLineNode("groupId") { +groupId }
                    singleLineNode("artifactId") { +artifactId }
                }
                singleLineNode("version") { +version.toString() }
                if (dependencyType == DependencyType.TEST) {
                    singleLineNode("scope") { +"test" }
                }
            }
        }
        else -> Unit
    }
}


data class KotlinLibraryDependencyIR(
    val name: String,
    override val version: Version,
    override val dependencyType: DependencyType
) : LibraryDependencyIR {
    override val artifact: LibraryArtifact
        get() = MavenArtifact(
            MAVEN_CENTRAL,
            "org.jetbrains.kotlin",
            "kotlin-$name"
        )

    override fun withDependencyType(type: DependencyType): KotlinLibraryDependencyIR =
        copy(dependencyType = type)

    override fun BuildFilePrinter.render() {
        when (this) {
            is GradlePrinter -> call(dependencyType.gradleName) {
                +"kotlin("
                +name.quotified
                +")"
            }
            is MavenPrinter -> node("dependency") {
                singleLineNode("groupId") { +"org.jetbrains.kotlin" }
                singleLineNode("artifactId") { +"kotlin-stdlib" }
                singleLineNode("version") { +version.toString() }
                if (dependencyType == DependencyType.TEST) {
                    singleLineNode("scope") { +"test" }
                }
            }
        }
    }
}

val LibraryDependencyIR.isKotlinStdlib
    get() = safeAs<KotlinLibraryDependencyIR>()?.name?.contains("stdlib") == true