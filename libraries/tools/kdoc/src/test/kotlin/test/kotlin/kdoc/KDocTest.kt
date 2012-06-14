package test.kotlin.kdoc

import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler

import kotlin.test.assertTrue

import org.junit.Test
import java.io.File
import org.junit.Assert
import org.jetbrains.jet.cli.common.ExitCode
import java.util.ArrayList

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
        //args.setModule(moduleName)
        val sourceDirs = ArrayList<String?>()
        sourceDirs.add("../../stdlib/src")
        sourceDirs.add("../../kunit/src/main/kotlin")
        sourceDirs.add("../../kotlin-jdbc/src/main/kotlin")
        args.setSourceDirs(sourceDirs)
        args.setOutputDir("target/classes-stdlib")
        args.setMode("stdlib")
        args.setClasspath("../runtime/target/kotlin-runtime-0.1-SNAPSHOT.jar:../../lib/junit-4.9.jar")

        val config = args.docConfig
        config.docOutputDir = outDir.toString()!!
        config.title = "Kotlin API"
        config.ignorePackages.add("org.jetbrains.kotlin")
        config.ignorePackages.add("java")
        config.ignorePackages.add("jet")
        config.ignorePackages.add("junit")
        config.ignorePackages.add("sun")
        config.ignorePackages.add("org")

        val compiler = KDocCompiler()
        val r = compiler.exec(System.out, args)
        Assert.assertEquals(ExitCode.OK, r)
    }
}
