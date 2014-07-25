package test.kotlin.kdoc

import java.io.File
import java.io.IOException
import org.jetbrains.jet.cli.common.ExitCode
import org.jetbrains.kotlin.doc.KDocArguments
import org.jetbrains.kotlin.doc.KDocCompiler
import org.junit.Assert
import org.junit.Test

fun File.rmrf() {
    val children = listFiles()
    if (children != null) {
        for (child in children) {
            child.rmrf()
        }
    }
    delete()
    if (exists()) {
        throw IOException("failed to delete $this")
    }
}

fun File.mkdirsProperly() {
    mkdirs()
    if (!exists())
        throw IOException("failed to crete directory $this")
    if (!isDirectory()) {
        throw IOException("cannot create directory $this because it exists and it is not a directory")
    }
}


class KDocSampleTest {

    [Test]
    fun generateKDocForSample() {
        val compiler = KDocCompiler()

        val args = KDocArguments()
        args.kotlinHome = "../../../dist/kotlinc"

        args.freeArgs = listOf("src/test/sample")

        val outputDir = File("target/apidocs-sample")
        outputDir.rmrf()
        outputDir.mkdirsProperly()

        val classesOutputDir = File("target/classes-sample")
        classesOutputDir.rmrf()
        classesOutputDir.mkdirsProperly()

        args.destination = classesOutputDir.getPath()

        args.docConfig.docOutputDir = outputDir.getPath()
        args.docConfig.title = "Sample"

        val exitCode = compiler.exec(System.err, args)
        Assert.assertEquals(ExitCode.OK, exitCode)
    }
}
