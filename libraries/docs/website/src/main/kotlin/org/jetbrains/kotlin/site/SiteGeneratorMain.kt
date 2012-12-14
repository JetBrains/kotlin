package org.jetbrains.kotlin.site

import java.io.File

class SiteGeneratorMain {
    val srcDir = findTemplateDir()
    val siteOutputDir = File(srcDir, "../../../target/site")

    val version = System.getProperty("project.version") ?: "SNAPSHOT"
    val versionDir = if (version.contains("SNAPSHOT")) "snapshot" else version

    fun generateSite(): Unit {
        val generator = SiteGenerator(srcDir, siteOutputDir)
        generator.run()
    }

    fun copyApiDocs(): Unit {
        val apidocDir = File(siteOutputDir, "../../../apidoc/target/site/apidocs")
        assertTrue(apidocDir.exists(), "Directory does not exist ${apidocDir.getCanonicalPath()}")

        val outDir = File(siteOutputDir, "versions/$versionDir/apidocs")
        println("Copying API docs to $outDir")

        copyDocResources(outDir)
        copyRecursive(apidocDir, outDir)

        // now lets assert that the API docs generated everything
        assertFilesExist(outDir, "Expected generate API docs are missing - API doc generation failure",
                "kotlin/package-frame.html", "kotlin/dom/package-frame.html", "kotlin/test/package-frame.html")
    }

    fun copyJSApiDocs(): Unit {
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

    // TODO this could make a handy test function
    fun assertFilesExist(dir: File, message: String, vararg names: String): Unit {
        for (name in names) {
            val file = File(dir, name)
            assertTrue(file.exists(), "${file.getPath()} does not exist. $message")
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

    fun assertTrue(condition: Boolean, message: String): Unit {
        if (!condition) {
            throw RuntimeException(message);
        }
    }
}

fun main(args : Array<String>) {
    val main = SiteGeneratorMain()
    main.generateSite()
    main.copyApiDocs()
    main.copyJSApiDocs()
}
