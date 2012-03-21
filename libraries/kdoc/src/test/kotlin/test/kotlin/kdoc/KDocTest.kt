package test.kotlin.kdoc

import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler

import kotlin.test.assertTrue

import org.junit.Test
import java.io.File

/**
 */
class KDocTest {
    Test fun generateKDocForStandardLibrary() {
        var moduleName = "ApiDocsModule.kt"
        var dir = "."
        if (!File(moduleName).exists()) {
            dir = "kdoc"
            moduleName = dir + "/" + moduleName
            assertTrue(File(moduleName).exists(), "Cannot find file $moduleName")
        }
        val outDir = File(dir, "target/apidocs")
        println("Generating library KDocs to $outDir")

        val args = KDocArguments()
        args.setModule(moduleName)
        args.setDocOutputDir(outDir.toString())
        args.setOutputDir("target/classes-stdlib")

        val config = args.docConfig
        config.title = "Kotlin API"
        config.ignorePackages.add("org.jetbrains.kotlin")
        config.ignorePackages.add("java")
        config.ignorePackages.add("jet")
        config.ignorePackages.add("junit")
        config.ignorePackages.add("sun")
        config.ignorePackages.add("org")

        val compiler = KDocCompiler()
        compiler.exec(System.out, args)
    }
}