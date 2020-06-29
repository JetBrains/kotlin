package org.jetbrains.kotlin.jps.artifacts

import com.intellij.util.PathUtil
import com.intellij.util.io.Decompressor
import com.intellij.util.io.DigestUtil
import org.jetbrains.kotlin.idea.artifacts.ProductionKotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.ProductionLikeKotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.RepoLocation
import org.jetbrains.kotlin.idea.artifacts.findLibrary
import java.io.File
import java.io.FileNotFoundException

/**
 * Used for testing purposes in jps-plugin. It extends [ProductionLikeKotlinArtifacts] because we want to compile code as
 * much close as compiler does it in JPS tests
 *
 * FAQ:
 *
 * Q: Wny don't we simply use [ProductionKotlinArtifacts] for this purpose?
 *
 * A: Because [ProductionKotlinArtifacts] is working thanks to Artifacts which are build by IDEA.
 * In order to have working JPS tests we should include Build Artifact pre-stage only for jps-plugin tests (for minimizing test startup
 * of non jps-plugin tests). But IDEA doesn't have such feature yet (It's only possible to Build Artifact only for **all** JUnit tests).
 * And it's not possible to keep run configuration template in VCS yet.
 */
class JpsPluginTestArtifacts private constructor(override val kotlinPluginDirectory: File) : ProductionLikeKotlinArtifacts() {
    companion object {
        fun getInstance(): JpsPluginTestArtifacts {
            val classFile = File(PathUtil.getJarPathForClass(JpsPluginTestArtifacts::class.java))
            if (!classFile.exists()) {
                throw FileNotFoundException("Class file not found for class ${JpsPluginTestArtifacts::class.java}")
            }
            var outDir = classFile
            while (outDir.name != "out") {
                outDir = outDir.parentFile ?: throw FileNotFoundException("`out` directory not found")
            }
            val jpsTestsArtifactsDir = outDir.resolve("artifacts/jps-tests-artifacts")
            val kotlinPluginDirectory = jpsTestsArtifactsDir.resolve("KotlinPlugin")
            val hashFile = jpsTestsArtifactsDir.resolve("KotlinPlugin.md5")
            val kotlincJar = findLibrary(
                RepoLocation.MAVEN_REPOSITORY,
                "kotlinc_dist.xml",
                "org.jetbrains.kotlin",
                "kotlin-dist-for-ide"
            )
            val hash = kotlincJar.md5()
            if (hashFile.exists() && hashFile.readText() == hash && kotlinPluginDirectory.exists()) {
                // On my machine (Intel Core i5-9300H + 32 gb of ram) all jps-plugin tests run for
                //   6m56s with this hashing
                //   7m41s without hashing
                // Measurements were made during not complete setup of kotlin-ide repo
                // so they might change when kotlin-ide will be fully setup
                return JpsPluginTestArtifacts(kotlinPluginDirectory)
            }
            val dirWhereToExtractKotlinc= kotlinPluginDirectory.resolve("kotlinc").also {
                it.deleteRecursively()
                it.mkdirs()
            }
            hashFile.writeText(hash)
            Decompressor.Zip(kotlincJar).extract(dirWhereToExtractKotlinc)
            return JpsPluginTestArtifacts(kotlinPluginDirectory)
        }
    }
}

private fun File.md5(): String {
    return DigestUtil.md5().digest(readBytes()).joinToString("") { "%02x".format(it) }
}
