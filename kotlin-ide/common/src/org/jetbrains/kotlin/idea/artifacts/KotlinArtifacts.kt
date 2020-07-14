package org.jetbrains.kotlin.idea.artifacts

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.util.io.Decompressor
import org.eclipse.aether.repository.RemoteRepository
import org.jdom.input.SAXBuilder
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.kotlin.idea.artifacts.RepoLocation.MAVEN_REPOSITORY
import org.jetbrains.kotlin.idea.artifacts.RepoLocation.PROJECT_DIR
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest

object KotlinArtifacts {
    val kotlinPluginDirectory: File by lazy {
        val classFile = PathUtil.getResourcePathForClass(KotlinArtifacts::class.java)
        if (!classFile.exists()) {
            throw FileNotFoundException("Class file not found for class ${KotlinArtifacts::class.java}")
        }
        var outDir = classFile
        while (outDir.name != "out") {
            outDir = outDir.parentFile ?: throw FileNotFoundException("`out` directory not found")
        }
        val jpsTestsArtifactsDir = outDir.resolve("artifacts/jps-tests-artifacts")
        val kotlinPluginDirectory = jpsTestsArtifactsDir.resolve("KotlinPlugin")
        val hashFile = jpsTestsArtifactsDir.resolve("KotlinPlugin.md5")
        val kotlincJar = findLibrary(
            MAVEN_REPOSITORY,
            "kotlinc_dist.xml",
            "org.jetbrains.kotlin",
            "kotlin-dist-for-ide"
        )
        val hash = kotlincJar.md5()
        if (hashFile.exists() && hashFile.readText() == hash && kotlinPluginDirectory.exists()) {
            return@lazy kotlinPluginDirectory
        }
        val dirWhereToExtractKotlinc= kotlinPluginDirectory.resolve("kotlinc").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        hashFile.writeText(hash)
        Decompressor.Zip(kotlincJar).extract(dirWhereToExtractKotlinc)
        return@lazy kotlinPluginDirectory
    }

    val kotlincDirectory by lazy { findFile(kotlinPluginDirectory, "kotlinc") }
    val kotlincLibDirectory by lazy { findFile(kotlincDirectory, "lib") }

    val jetbrainsAnnotations by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.JETBRAINS_ANNOTATIONS) }
    val kotlinStdlib by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB) }
    val kotlinStdlibSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_SOURCES) }
    val kotlinStdlibJdk7 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7) }
    val kotlinStdlibJdk7Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK7_SOURCES) }
    val kotlinStdlibJdk8 by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8) }
    val kotlinStdlibJdk8Sources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JDK8_SOURCES) }
    val kotlinReflect by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_REFLECT) }
    val kotlinStdlibJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS) }
    val kotlinStdlibJsSources by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_STDLIB_JS_SOURCES) }
    val kotlinTest by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST) }
    val kotlinTestJunit by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JUNIT) }
    val kotlinTestJs by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_TEST_JS) }
    val kotlinMainKts by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_MAIN_KTS) }
    val kotlinScriptRuntime by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPT_RUNTIME) }
    val kotlinScriptingCommon by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMMON) }
    val kotlinScriptingJvm by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_JVM) }
    val kotlinCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_COMPILER) }
    val trove4j: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.TROVE4J) }
    val kotlinDaemon: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_DAEMON) }
    val kotlinScriptingCompiler: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER) }
    val kotlinScriptingCompilerImpl: File by lazy { findFile(kotlincLibDirectory, KotlinArtifactNames.KOTLIN_SCRIPTING_COMPILER_IMPL) }

    val kotlinStdlibCommon: File get() = throw error("'stdlib-common' artifact is not available")
    val kotlinStdlibCommonSources: File get() = throw error("'stdlib-common' artifact is not available")

    private fun findFile(parent: File, path: String): File {
        val result = File(parent, path)
        if (!result.exists()) {
            throw FileNotFoundException("File $result doesn't exist")
        }
        return result
    }
}

private fun File.md5(): String {
    return MessageDigest.getInstance("MD5").digest(readBytes()).joinToString("") { "%02x".format(it) }
}

private fun substitutePathVariables(path: String): String {
    if (path.startsWith("$PROJECT_DIR/")) {
        val projectDir = File(kotlinIdeHome()).parentFile
        return projectDir.absolutePath + path.drop(PROJECT_DIR.toString().length)
    }
    else if (path.startsWith("$MAVEN_REPOSITORY/")) {
        val userHomeDir = System.getProperty("user.home", null) ?: error("Unable to get the user home directory")
        val repoDir = File(userHomeDir, ".m2/repository")
        return repoDir.absolutePath + path.drop(MAVEN_REPOSITORY.toString().length)
    }

    return path
}

private enum class RepoLocation {
    PROJECT_DIR {
        override fun toString(): String {
            return "\$PROJECT_DIR\$"
        }
    },
    MAVEN_REPOSITORY {
        override fun toString(): String {
            return "\$MAVEN_REPOSITORY\$"
        }
    }
}

private enum class LibraryFileKind(val classifierSuffix: String, val artifactKind: ArtifactKind) {
    CLASSES("", ArtifactKind.ARTIFACT), SOURCES("-sources", ArtifactKind.SOURCES);
}

private fun kotlinIdeHome(): String {
    var current = Paths.get(".").toAbsolutePath().normalize()
    while (current != null && !Files.isRegularFile(current.resolve("kotlin.kotlin-ide.iml"))) {
        current = current.parent
    }
    checkNotNull(current) { "Cannot find kotlin-ide root" }
    current = current.resolve("kotlin")
    return current.toString()
}

private fun findLibrary(
    repoLocation: RepoLocation,
    library: String,
    groupId: String,
    artifactId: String,
    kind: LibraryFileKind = LibraryFileKind.CLASSES
): File {
    val librariesDir = File(kotlinIdeHome(), "../.idea/libraries")
    if (!librariesDir.exists()) {
        throw IllegalStateException("Can't find $librariesDir")
    }

    val libraryFile = File(librariesDir, library)
    if (!libraryFile.exists()) {
        throw IllegalStateException("Can't find library $library")
    }

    val document = libraryFile.inputStream().use { stream -> SAXBuilder().build(stream) }
    val urlScheme = "jar://"
    val pathInRepository = groupId.replace('.', '/') + '/' + artifactId
    val pathPrefix = "$urlScheme$repoLocation/$pathInRepository/"

    val root = document.rootElement
        .getChild("library")
        ?.getChild(kind.name)
        ?.getChildren("root")
        ?.singleOrNull { (it.getAttributeValue("url") ?: "").startsWith(pathPrefix) }
        ?: throw IllegalStateException("Root '$pathInRepository' not found in library $library")

    val url = root.getAttributeValue("url") ?: ""
    val path = url.drop(urlScheme.length).dropLast(2) // last '!/'

    val result = File(substitutePathVariables(path))
    if (!result.exists()) {
        if (kind == LibraryFileKind.SOURCES) {
            val version = result.nameWithoutExtension.drop(artifactId.length + 1).dropLast(kind.classifierSuffix.length)
            return resolveArtifact(groupId, artifactId, version, kind)
        }

        throw IllegalStateException("File $result doesn't exist")
    }
    return result
}

private val remoteMavenRepositories: List<RemoteRepository> by lazy {
    val jarRepositoriesFile = File(kotlinIdeHome(), "../.idea/jarRepositories.xml")
    val document = jarRepositoriesFile.inputStream().use { stream -> SAXBuilder().build(stream) }

    val repositories = mutableListOf<RemoteRepository>()

    for (remoteRepo in document.rootElement.getChild("component")?.getChildren("remote-repository").orEmpty()) {
        val options = remoteRepo.getChildren("option") ?: continue

        fun getOptionValue(key: String): String? {
            val option = options.find { it.getAttributeValue("name") == key } ?: return null
            return option.getAttributeValue("value")?.takeIf { it.isNotEmpty() }
        }

        val id = getOptionValue("id") ?: continue
        val url = getOptionValue("url") ?: continue
        repositories += ArtifactRepositoryManager.createRemoteRepository(id, url)
    }

    return@lazy repositories
}

private fun resolveArtifact(groupId: String, artifactId: String, version: String, kind: LibraryFileKind): File {
    val repositoryManager = ArtifactRepositoryManager(
        JarRepositoryManager.getLocalRepositoryPath(),
        remoteMavenRepositories,
        ProgressConsumer.DEAF,
        false
    )

    val artifacts = repositoryManager.resolveDependencyAsArtifact(
        groupId, artifactId, version,
        setOf(kind.artifactKind), false, emptyList()
    )

    assert(artifacts.size == 1) { "Single artifact expected for library \"$groupId:$artifactId:$version\", got $artifacts" }
    return artifacts.single().file
}
