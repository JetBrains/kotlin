package org.jetbrains.kotlin.testGenerator.generator.methods

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.testGenerator.generator.Code
import org.jetbrains.kotlin.testGenerator.generator.TestMethod
import org.jetbrains.kotlin.testGenerator.generator.appendBlock
import org.jetbrains.kotlin.testGenerator.generator.appendList
import org.jetbrains.kotlin.testGenerator.model.TModel

class TestAllFilesPresentMethod(private val model: TModel, private val isNested: Boolean, private val testDataPath: String) : TestMethod {
    override val methodName = "testAllFilesPresent"

    override fun Code.render() {
        val args = mutableListOf(
            "this.getClass()",
            "new File(\"${testDataPath}\")",
            "Pattern.compile(\"" + StringUtil.escapeStringCharacters(model.pattern.pattern) + "\")",
            "null"
        )

        if (model.targetBackend != TargetBackend.ANY) {
            args += TargetBackend::class.java.simpleName + "." + model.targetBackend.toString()
        }

        if (!model.flatten) {
            args += (model.depth > 1).toString()
        }

        if (!isNested) {
            model.excludedDirectories.mapTo(args) { '"' + StringUtil.escapeStringCharacters(it) + '"' }
        }

        val callMethodName = when {
          model.flatten -> "assertAllTestsPresentInSingleGeneratedClassWithExcluded"
          else -> "assertAllTestsPresentByMetadataWithExcluded"
        }

        appendBlock("public void $methodName() throws Exception") {
            append("KotlinTestUtils.$callMethodName(")
            appendList(args) { append(it) }
            append(");")
        }
    }
}