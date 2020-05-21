package org.jetbrains.kotlin.idea.test

import org.jdom.input.SAXBuilder
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifactNames
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

private const val PROJECT_DIR = "\$PROJECT_DIR\$"
private const val MAVEN_REPOSITORY = "\$MAVEN_REPOSITORY\$"

private fun substitutePathVariables(path: String): String {
    if (path.startsWith("$PROJECT_DIR/")) {
        val projectDir = File(KotlinTestUtils.getHomeDirectory()).parentFile
        return projectDir.absolutePath + path.drop(PROJECT_DIR.length)
    } else if (path.startsWith("$MAVEN_REPOSITORY/")) {
        val userHomeDir = System.getProperty("user.home", null) ?: error("Unable to get the user home directory")
        val repoDir = File(userHomeDir, ".m2/repository")
        return repoDir.absolutePath + path.drop(MAVEN_REPOSITORY.length)
    }

    return path
}

private fun findLibrary(repoLocation: String, library: String, group: String, artifact: String): File {
    val librariesDir = File(KotlinTestUtils.getHomeDirectory(), "../.idea/libraries")
    if (!librariesDir.exists()) {
        throw IllegalStateException("Can't find $librariesDir")
    }

    val libraryFile = File(librariesDir, library)
    if (!libraryFile.exists()) {
        throw IllegalStateException("Can't find library $library")
    }

    val document = libraryFile.inputStream().use { stream -> SAXBuilder().build(stream) }
    val urlScheme = "jar://"
    val pathInRepository = group.replace('.', '/') + '/' + artifact
    val pathPrefix = "$urlScheme$repoLocation/$pathInRepository/"

    val root = document.rootElement
        .getChild("library")
        ?.getChild("CLASSES")
        ?.getChildren("root")
        ?.singleOrNull { (it.getAttributeValue("url") ?: "").startsWith(pathPrefix) }
        ?: error("Root '$pathInRepository' not found in library $library")

    val url = root.getAttributeValue("url") ?: ""
    val path = url.drop(urlScheme.length).dropLast(2) // last '!/'

    val result = File(substitutePathVariables(path))
    if (!result.exists()) {
        throw IllegalStateException("File $result doesn't exist")
    }
    return result
}

object TestKotlinArtifacts : KotlinArtifacts() {
    private const val artifactName = "KotlinPlugin"
    private const val repoPath = "$PROJECT_DIR/dependencies/repo"

    private val artifactDirectory: File by lazy {
        val result = File(KotlinTestUtils.getHomeDirectory(), "../out/artifacts/$artifactName")
        if (!result.exists()) {
            throw IllegalStateException("Artifact '$artifactName' doesn't exist")
        }
        return@lazy result
    }

    override val kotlincDirectory = findFile(artifactDirectory, "kotlinc")
    private val kotlincLibDirectory = findFile(artifactDirectory, "lib")

    override val kotlinStdlib by lazy { findLibrary(repoPath, "kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib") }
    override val kotlinStdlibJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS) }
    override val kotlinTest by lazy { findLibrary(MAVEN_REPOSITORY, "kotlin_test.xml", "org.jetbrains.kotlin", "kotlin-test") }
}