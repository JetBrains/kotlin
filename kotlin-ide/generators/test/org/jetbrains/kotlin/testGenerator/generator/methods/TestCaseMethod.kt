package org.jetbrains.kotlin.testGenerator.generator.methods

import com.intellij.openapi.util.io.systemIndependentPath
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testGenerator.generator.Code
import org.jetbrains.kotlin.testGenerator.generator.TestMethod
import org.jetbrains.kotlin.testGenerator.generator.appendAnnotation
import org.jetbrains.kotlin.testGenerator.generator.appendBlock
import org.jetbrains.kotlin.testGenerator.model.TAnnotation
import org.jetbrains.kotlin.testGenerator.model.makeJavaIdentifier
import java.io.File

class TestCaseMethod(private val methodNameBase: String, private val contentRootPath: String, private val localPath: String) : TestMethod {
    override val methodName = run {
        "test" + when (val qualifier = File(localPath).parentFile?.systemIndependentPath ?: "") {
            "" -> methodNameBase
            else -> makeJavaIdentifier(qualifier).capitalize() + "_" + methodNameBase
        }
    }

    fun embed(path: String): TestCaseMethod {
        return TestCaseMethod(methodNameBase, contentRootPath, File(path, localPath).systemIndependentPath)
    }

    override fun Code.render() {
        appendAnnotation(TAnnotation<TestMetadata>(localPath))
        appendBlock("public void $methodName() throws Exception") {
            append("runTest(\"$contentRootPath\");")
        }
    }
}