package org.jetbrains.kotlin.idea.artifacts

import com.intellij.jarRepository.JarRepositoryManager
import org.eclipse.aether.repository.RemoteRepository
import org.jdom.input.SAXBuilder
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.kotlin.idea.artifacts.RepoLocation.*
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

private fun substitutePathVariables(path: String): String {
    if (path.startsWith("$PROJECT_DIR/")) {
        val projectDir = File(KotlinTestUtils.getHomeDirectory()).parentFile
        return projectDir.absolutePath + path.drop(PROJECT_DIR.toString().length)
    }
    else if (path.startsWith("$MAVEN_REPOSITORY/")) {
        val userHomeDir = System.getProperty("user.home", null) ?: error("Unable to get the user home directory")
        val repoDir = File(userHomeDir, ".m2/repository")
        return repoDir.absolutePath + path.drop(MAVEN_REPOSITORY.toString().length)
    }

    return path
}

enum class RepoLocation {
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

enum class LibraryFileKind(val classifierSuffix: String, val artifactKind: ArtifactKind) {
    CLASSES("", ArtifactKind.ARTIFACT), SOURCES("-sources", ArtifactKind.SOURCES);
}

fun findLibrary(
    repoLocation: RepoLocation,
    library: String,
    groupId: String,
    artifactId: String,
    kind: LibraryFileKind = LibraryFileKind.CLASSES
): File {
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
    val jarRepositoriesFile = File(KotlinTestUtils.getHomeDirectory(), "../.idea/jarRepositories.xml")
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

object TestKotlinArtifacts : KotlinArtifacts() {
    private const val artifactName = "KotlinPlugin"

    private val artifactDirectory: File by lazy {
        val result = File(KotlinTestUtils.getHomeDirectory(), "../out/artifacts/$artifactName")
        if (!result.exists()) {
            throw IllegalStateException("Artifact '$artifactName' doesn't exist")
        }
        return@lazy result
    }

    override val kotlincDirectory: File by lazy { findFile(artifactDirectory, "kotlinc") }
    override val kotlincLibDirectory: File by lazy { findFile(artifactDirectory, "lib") }

    override val jetbrainsAnnotations by lazy {
        findLibrary(MAVEN_REPOSITORY, "jetbrains_annotations.xml", "org.jetbrains", "annotations")
    }

    override val kotlinStdlib by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib")
    }

    override val kotlinStdlibSources by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib", LibraryFileKind.SOURCES)
    }

    override val kotlinStdlibJdk7 by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    }

    override val kotlinStdlibJdk7Sources by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib-jdk7", LibraryFileKind.SOURCES)
    }

    override val kotlinStdlibJdk8 by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    }

    override val kotlinStdlibJdk8Sources by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_jdk8.xml", "org.jetbrains.kotlin", "kotlin-stdlib-jdk8", LibraryFileKind.SOURCES)
    }

    override val kotlinStdlibCommon by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common")
    }

    override val kotlinStdlibCommonSources by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common", LibraryFileKind.SOURCES)
    }

    override val kotlinReflect by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_reflect.xml", "org.jetbrains.kotlin", "kotlin-reflect")
    }

    override val kotlinStdlibJs by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_js.xml", "org.jetbrains.kotlin", "kotlin-stdlib-js")
    }

    override val kotlinStdlibJsSources by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_js.xml", "org.jetbrains.kotlin", "kotlin-stdlib-js", LibraryFileKind.SOURCES)
    }

    override val kotlinTest by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlin_test.xml", "org.jetbrains.kotlin", "kotlin-test")
    }

    override val kotlinTestJunit by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlin_test_junit.xml", "org.jetbrains.kotlin", "kotlin-test-junit")
    }

    override val kotlinTestJs by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_test_js.xml", "org.jetbrains.kotlin", "kotlin-test-js")
    }

    override val kotlinMainKts by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_main_kts.xml", "org.jetbrains.kotlin", "kotlin-main-kts")
    }

    override val kotlinScriptRuntime by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_script_runtime.xml", "org.jetbrains.kotlin", "kotlin-script-runtime")
    }

    override val kotlinScriptingCommon by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_scripting_common.xml", "org.jetbrains.kotlin", "kotlin-scripting-common")
    }

    override val kotlinScriptingJvm by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_scripting_jvm.xml", "org.jetbrains.kotlin", "kotlin-scripting-jvm")
    }

    override val kotlinCompiler: File by lazy {
        findLibrary(MAVEN_REPOSITORY, "org_jetbrains_kotlin_kotlin_compiler.xml", "org.jetbrains.kotlin", "kotlin-compiler")
    }

    override val trove4j: File by lazy {
        findLibrary(MAVEN_REPOSITORY, "Trove4j.xml", "org.jetbrains.intellij.deps", "trove4j")
    }

    override val kotlinDaemon: File by lazy {
        findLibrary(MAVEN_REPOSITORY, "org_jetbrains_kotlin_kotlin_daemon_1_3_61.xml", "org.jetbrains.kotlin", "kotlin-daemon")
    }

    override val kotlinScriptingCompiler: File by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_scripting_compiler.xml", "org.jetbrains.kotlin", "kotlin-scripting-compiler")
    }

    override val kotlinScriptingCompilerImpl: File by lazy {
        findLibrary(MAVEN_REPOSITORY, "kotlinc_kotlin_scripting_compiler_impl.xml", "org.jetbrains.kotlin", "kotlin-scripting-compiler-impl")
    }
}
