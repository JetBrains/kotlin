package org.jetbrains.kotlin.site

import junit.framework.TestCase
import java.io.File
import kotlin.util.arrayList
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler

class GenerateSiteTest : TestCase() {
    val srcDir = findTemplateDir()
    val siteOutputDir = File(srcDir, "../../../target/site")

    val version = System.getProperty("project.version") ?: "SNAPSHOT"
    val versionDir = if (version.contains("SNAPSHOT")) "snapshot" else version

    fun testGenerateSite(): Unit {
        val generator = SiteGenerator(srcDir, siteOutputDir)
        generator.run()
    }

    fun testGenerateStdlibKDoc(): Unit {
        val outDir = File(siteOutputDir, "versions/$versionDir/apidocs")
        println("Generating library KDocs to $outDir")

        copyDocResources(outDir)
        val args = KDocArguments()
        args.setSrc("../stdlib/src")
        args.setDocOutputDir(outDir.toString())
        args.setOutputDir("target/classes-stdlib")
        val config = args.docConfig
        config.title = "Kotlin API ($version)"
        config.version = version

        val compiler = KDocCompiler()
        compiler.exec(System.out, args)
    }

    // TODO re-enable when KT-1522 fixed
    fun DISABLED_testGenerateJsKDoc(): Unit {
        val outDir = File(siteOutputDir, "versions/$versionDir/jsdocs")
        println("Generating JS KDocs to $outDir")

        copyDocResources(outDir)
        val args = KDocArguments()
        args.setModule("../../js/js.libraries/module.kt")
        args.setDocOutputDir(outDir.toString())
        args.setOutputDir("target/classes-js")
        val config = args.docConfig
        config.title = "Kotlin JS API ($version)"
        config.version = version

        val compiler = KDocCompiler()
        compiler.exec(System.out, args)
    }

    fun copyDocResources(outDir: File): Unit {
        val sourceDir = File(srcDir, "../apidocs")
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
        for (p in arrayList(".", "apidocs", "library/apidocs")) {
            val sourceDir = File(".", path)
            if (sourceDir.exists()) {
                return sourceDir
            }
        }
        throw IllegalArgumentException("Could not find template directory: $path")
    }
}