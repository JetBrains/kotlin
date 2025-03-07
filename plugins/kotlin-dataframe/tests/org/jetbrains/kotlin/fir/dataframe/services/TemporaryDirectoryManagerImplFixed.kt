package org.jetbrains.kotlin.fir.dataframe.services

import org.jetbrains.kotlin.test.services.TemporaryDirectoryManager
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.testInfo
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import java.nio.file.Paths
import java.util.Locale
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

// Copied from org.jetbrains.kotlin.test.services.impl.TemporaryDirectoryManagerImpl
// because it uses NioFiles#deleteRecursively and throws method not found as a result.
class TemporaryDirectoryManagerImplFixed(testServices: TestServices) : TemporaryDirectoryManager(testServices) {
    private val cache = mutableMapOf<String, File>()
    private val rootTempDir = lazy {
        val testInfo = testServices.testInfo
        val className = testInfo.className
        val methodName = testInfo.methodName
        if (!onWindows && className.length + methodName.length < 255) {
            return@lazy KtTestUtil.tmpDirForTest(className, methodName)
        }

        // This code will simplify directory name for windows. This is needed because there can occur errors due to long name
        val lastDot = className.lastIndexOf('.')
        val simplifiedClassName = className.substring(lastDot + 1).getOnlyUpperCaseSymbols()
        val simplifiedMethodName = methodName.getOnlyUpperCaseSymbols()
        KtTestUtil.tmpDirForTest(simplifiedClassName, simplifiedMethodName)
    }

    override val rootDir: File
        get() = rootTempDir.value

    override fun getOrCreateTempDirectory(name: String): File {
        return cache.getOrPut(name) { KtTestUtil.tmpDir(rootDir, name) }
    }

    @OptIn(ExperimentalPathApi::class)
    override fun cleanupTemporaryDirectories() {
        cache.clear()

        if (rootTempDir.isInitialized()) {
            Paths.get(rootDir.path).deleteRecursively()
        }
    }

    companion object {
        private val onWindows: Boolean = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

        private fun String.getOnlyUpperCaseSymbols(): String {
            return this.filter { it.isUpperCase() || it == '$' }.toList().joinToString(separator = "")
        }
    }
}
