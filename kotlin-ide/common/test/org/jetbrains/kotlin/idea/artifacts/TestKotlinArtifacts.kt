package org.jetbrains.kotlin.idea.artifacts

import com.intellij.jarRepository.JarRepositoryManager
import com.intellij.openapi.application.PathManager
import com.intellij.util.io.Decompressor
import org.eclipse.aether.repository.RemoteRepository
import org.jdom.input.SAXBuilder
import org.jetbrains.idea.maven.aether.ArtifactKind
import org.jetbrains.idea.maven.aether.ArtifactRepositoryManager
import org.jetbrains.idea.maven.aether.ProgressConsumer
import org.jetbrains.kotlin.test.KotlinRoot
import java.io.File
import java.security.MessageDigest

/**
 * This is used via reflection in [KotlinArtifacts]
 */
@Suppress("unused")
private class TestKotlinArtifacts : KotlinArtifacts() {
    override val kotlincDistDir: File by lazy {
        val outDir = File(PathManager.getHomePath(), "out")
        val kotlincDistDir = outDir.resolve("kotlinc-dist")
        val hashFile = outDir.resolve("kotlinc-dist/kotlinc-dist.md5")
        val kotlincJar = findLibrary(
                RepoLocation.MAVEN_REPOSITORY,
                "kotlinc_dist.xml",
                "org.jetbrains.kotlin",
                "kotlin-dist-for-ide"
        )
        val hash = kotlincJar.md5()
        if (hashFile.exists() && hashFile.readText() == hash && kotlincDistDir.exists()) {
            return@lazy kotlincDistDir
        }
        val dirWhereToExtractKotlinc = kotlincDistDir.resolve("kotlinc").also {
            it.deleteRecursively()
            it.mkdirs()
        }
        hashFile.writeText(hash)
        Decompressor.Zip(kotlincJar).extract(dirWhereToExtractKotlinc)
        return@lazy kotlincDistDir
    }

    private fun File.md5(): String {
        return MessageDigest.getInstance("MD5").digest(readBytes()).joinToString("") { "%02x".format(it) }
    }
}

private fun findLibrary(
        repoLocation: RepoLocation,
        library: String,
        groupId: String,
        artifactId: String,
        kind: LibraryFileKind = LibraryFileKind.CLASSES
): File {
    val librariesDir = File(KotlinRoot.REPO, ".idea/libraries")
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

object AdditionalKotlinArtifacts {
    val kotlinStdlibCommon: File by lazy {
        findLibrary(RepoLocation.MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common")
    }

    val kotlinStdlibCommonSources: File by lazy {
        findLibrary(RepoLocation.MAVEN_REPOSITORY, "kotlinc_kotlin_stdlib_common.xml", "org.jetbrains.kotlin", "kotlin-stdlib-common", LibraryFileKind.SOURCES)
    }

    val parcelizeRuntime: File by lazy {
        KotlinArtifacts.instance.kotlincDistDir.resolve("kotlinc/lib/parcelize-runtime.jar").also { check(it.exists()) }
    }
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

private val remoteMavenRepositories: List<RemoteRepository> by lazy {
    val jarRepositoriesFile = File(KotlinRoot.REPO, ".idea/jarRepositories.xml")
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

private fun substitutePathVariables(path: String): String {
    if (path.startsWith("${RepoLocation.PROJECT_DIR}/")) {
        val projectDir = KotlinRoot.REPO
        return projectDir.resolve(path.drop(RepoLocation.PROJECT_DIR.toString().length)).absolutePath
    }
    else if (path.startsWith("${RepoLocation.MAVEN_REPOSITORY}/")) {
        val m2 = System.getenv("M2_HOME")?.let { File(it) }
                 ?: File(System.getProperty("user.home", null) ?: error("Unable to get the user home directory"), ".m2")
        val repoDir = m2.resolve("repository")
        return repoDir.absolutePath + path.drop(RepoLocation.MAVEN_REPOSITORY.toString().length)
    }

    return path
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
