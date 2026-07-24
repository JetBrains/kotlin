package org.jetbrains.kotlin.backend.konan.llvm

import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.*
import kotlin.time.Duration.Companion.minutes
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.native.executors.*
import org.jetbrains.kotlin.konan.target.HostManager

private fun fileProperty(name: String): File {
    val path = System.getProperty(name) ?: error("$name property is not set")
    return File(path).also {
        check(it.exists()) { "$name does not exist at $it" }
    }
}

private fun fileEnv(name: String): File {
    val path = System.getenv(name) ?: error("$name environment var is not set")
    return File(path).also {
        check(it.exists()) { "$name does not exist at $it" }
    }
}

abstract class AbstractKotlinPassesFileCheckTest {
    private val testOutputDir = fileEnv("PROJECT_BUILD_DIR").resolve("fileCheck")
    private val testInputDir = File("testData").resolve("fileCheck")

    private val llvmPlugin = fileProperty("kotlin.llvmPlugin")
    private val llvmDistribution = fileProperty("kotlin.llvmDistribution")
    private val executor = HostExecutor()
    private val optTimeout = 10.minutes
    private val fileCheckTimeout = 10.minutes

    private fun llvmExecutable(name: String): File {
        val extension = if (HostManager.hostIsMingw) ".exe" else ""
        return llvmDistribution.resolve("bin/$name$extension").also {
            check(it.exists()) { "Cannot find $name in llvm distribution" }
        }
    }

    private fun opt(inputFile: File, outputFile: File, args: List<String>) {
        runProcess(
            llvmExecutable("opt").absolutePath,
            "-S",
            "--load-pass-plugin=$llvmPlugin",
            "-o", outputFile.absolutePath,
            *args.toTypedArray(),
            inputFile.absolutePath,
        ) {
            this.timeout = optTimeout
        }
    }

    private fun fileCheck(inputFile: File, testedFile: File, args: List<String>) {
        runProcess(
            llvmExecutable("FileCheck").absolutePath,
            "--input-file=$testedFile",
            *args.toTypedArray(),
            inputFile.absolutePath,
        ) {
            this.timeout = fileCheckTimeout
        }
    }

    private fun File.parseArgs(prefix: String): List<String> = useLines { lines ->
        val argMatcher = ";\\h*\\Q$prefix\\E:\\h*(.+)".toRegex()
        val whitespaceMatcher = "\\h+".toRegex()
        lines.mapNotNull {
            argMatcher.find(it)?.groupValues[1]
        }.flatMap {
            it.splitToSequence(whitespaceMatcher)
        }.filterNot {
            it.isEmpty()
        }.toList()
    }

    protected fun runTest(name: String) {
        val inputFile = ForTestCompileRuntime.transformTestDataPath(name)
        val outputFile = testOutputDir.resolve(name).also {
            it.parentFile.mkdirs()
            it.delete()
        }
        val optArgs = inputFile.parseArgs("OPT")
        val fileCheckArgs = inputFile.parseArgs("FILECHECK")
        opt(inputFile, outputFile, optArgs)
        fileCheck(inputFile, outputFile, fileCheckArgs)
    }
}
