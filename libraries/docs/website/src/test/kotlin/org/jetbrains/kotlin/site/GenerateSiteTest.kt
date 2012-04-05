package org.jetbrains.kotlin.site

import kotlin.test.*
import junit.framework.TestCase
import java.io.File

class GenerateSiteTest : TestCase() {
    val srcDir = findTemplateDir()
    val siteOutputDir = File(srcDir, "../../../target/site")

    val version = System.getProperty("project.version") ?: "SNAPSHOT"
    val versionDir = if (version.contains("SNAPSHOT")) "snapshot" else version

    fun testGenerateSite(): Unit {
        val generator = SiteGenerator(srcDir, siteOutputDir)
        generator.run()
    }

    fun testCopyApiDocs(): Unit {
        val apidocDir = File(siteOutputDir, "../../../apidoc/target/site/apidocs")
        assertTrue(apidocDir.exists(), "Directory does not exist ${apidocDir.getCanonicalPath()}")

        val outDir = File(siteOutputDir, "versions/$versionDir/apidocs")
        println("Copying API docs to $outDir")

        copyDocResources(outDir)
        copyRecursive(apidocDir, outDir)
    }

    fun testCopyJSApiDocs(): Unit {
        val apidocDir = File(siteOutputDir, "../../../jsdoc/target/site/apidocs")
        //assertTrue(apidocDir.exists(), "Directory does not exist ${apidocDir.getCanonicalPath()}")
        if (!apidocDir.exists()) {
            println("WARNING - no JS API docs available. Though they don't work right now so are optional :)")
            return
        }

        val outDir = File(siteOutputDir, "versions/$versionDir/jsdocs")
        println("Copying JavaScript API docs to $outDir")

        copyDocResources(outDir)

        copyRecursive(apidocDir, outDir)
    }

    fun copyDocResources(outDir: File): Unit {
        val sourceDir = File(srcDir, "../apidocs")
        copyRecursive(sourceDir, outDir)
    }


    // TODO this would make a handy extension function on File :)
    fun copyRecursive(sourceDir: File, outDir: File): Unit {
        sourceDir.recurse {
            if (it.isFile()) {
                var relativePath = sourceDir.relativePath(it)
                val outFile = File(outDir, relativePath)
                outFile.directory.mkdirs()
                it.copyTo(outFile)
            }
        }
    }

    fun findTemplateDir(): File {
        val path = "src/main/templates"
        for (p in arrayList(".", "website", "library/website")) {
            val sourceDir = File(".", path)
            if (sourceDir.exists()) {
                return sourceDir
            }
        }
        throw IllegalArgumentException("Could not find template directory: $path")
    }
}