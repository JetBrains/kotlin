package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.tools.*
import org.junit.*
import org.junit.rules.TestName
import java.io.File

class CasesPublicAPITest {

    companion object {
        val visibilities by lazy { readKotlinVisibilities(File("build/cases-declarations.json")) }
        val baseClassPath = File("build/classes/test/cases").absoluteFile
        val baseOutputPath = File("src/test/kotlin/cases")
    }

    @[Rule JvmField]
    val testName = TestName()

    @Test fun companions() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun inline() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun interfaces() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun internal() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun java() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun localClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun nestedClasses() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun private() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun protected() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun public() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun special() { snapshotAPIAndCompare(testName.methodName) }

    @Test fun whenMappings() { snapshotAPIAndCompare(testName.methodName) }


    private fun snapshotAPIAndCompare(testClassRelativePath: String) {
        val testClassPath = baseClassPath.resolve(testClassRelativePath)
        val testClasses = testClassPath.listFiles() ?: throw IllegalStateException("Cannot list files in $testClassPath")
        val testClassStreams = testClasses.asSequence().filter { it.name.endsWith(".class") }.map { it.inputStream() }

        val api = getBinaryAPI(testClassStreams, visibilities).filterOutNonPublic()

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

        api.dumpAndCompareWith(target)
    }
}
