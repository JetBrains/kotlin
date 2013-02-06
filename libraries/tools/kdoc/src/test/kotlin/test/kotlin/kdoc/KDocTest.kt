package test.kotlin.kdoc

import java.io.File
import java.util.ArrayList
import kotlin.test.assertTrue
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler
import org.junit.Assert
import org.junit.Test

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
        args.kotlinHome = "../../../dist/kotlinc"
        val sourceDirs = ArrayList<String>()
        sourceDirs.add("../../stdlib/src")
        sourceDirs.add("../../kunit/src/main/kotlin")
        sourceDirs.add("../../kotlin-jdbc/src/main/kotlin")
        args.setSourceDirs(sourceDirs)
        args.setOutputDir("target/classes-stdlib")
        args.setNoStdlib(true)
        args.setClasspath("../runtime/target/kotlin-runtime-0.1-SNAPSHOT.jar${File.pathSeparator}../../lib/junit-4.9.jar")

        val config = args.docConfig
        config.docOutputDir = outDir.toString()
        config.title = "Kotlin API"

        val ignorePackages = config.ignorePackages
        ignorePackages.add("org.jetbrains.kotlin")
        ignorePackages.add("java")
        ignorePackages.add("jet")
        ignorePackages.add("junit")
        ignorePackages.add("sun")
        ignorePackages.add("org")

        val compiler = KDocCompiler()
        val r = compiler.exec(System.out, args)
        Assert.assertEquals(ExitCode.OK, r)
    }
}
