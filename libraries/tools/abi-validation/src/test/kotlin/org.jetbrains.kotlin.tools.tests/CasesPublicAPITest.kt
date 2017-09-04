package org.jetbrains.kotlin.tools.tests

import org.jetbrains.kotlin.tools.*
import org.junit.*
import org.junit.rules.TestName
import java.io.File

class CasesPublicAPITest {

    companion object {
        val visibilities by lazy { readKotlinVisibilities(File(System.getenv("PROJECT_BUILD_DIR") ?: "build", "cases-declarations.json")) }
        val baseClassPaths: List<File> =
                (System.getenv("PROJECT_CLASSES_DIRS")?.let { it.split(File.pathSeparator) }
                ?: listOf("build/classes/kotlin/test/cases", "build/classes/java/test/cases"))
                        .mapNotNull { File(it, "cases").canonicalFile.takeIf { it.isDirectory } }
                        .takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("Unable to get classes output dirs, set PROJECT_CLASSES_DIRS environment variable")
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
        val testClassPaths = baseClassPaths.map { it.resolve(testClassRelativePath) }
        val testClasses = testClassPaths.flatMap { it.listFiles()?.asIterable() ?: emptyList() }
        val testClassStreams = testClasses.asSequence().filter { it.name.endsWith(".class") }.map { it.inputStream() }

        val api = getBinaryAPI(testClassStreams, visibilities).filterOutNonPublic()

        val target = baseOutputPath.resolve(testClassRelativePath).resolve(testName.methodName + ".txt")

        api.dumpAndCompareWith(target)
    }
}
