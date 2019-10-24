package org.jetbrains.kotlin.idea.externalAnnotations

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.JavaModuleExternalPaths
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.codeStyle.JavaCodeStyleSettings
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TargetBackend
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class ExternalAnnotationTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getTestDataPath() = KotlinTestUtils.getHomeDirectory()

    fun testNotNullMethod() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/notNullMethod.kt")
    }

    fun testNullableMethod() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/nullableMethod.kt")
    }


    fun testNullableField() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/nullableField.kt")
    }

    fun testNullableMethodParameter() {
        KotlinTestUtils.runTest(::doTest, TargetBackend.ANY, "idea/testData/externalAnnotations/nullableMethodParameter.kt")
    }

    override fun setUp() {
        super.setUp()
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = true
        addFile(classWithExternalAnnotatedMembers)
    }

    override fun tearDown() {
        JavaCodeStyleSettings.getInstance(project).USE_EXTERNAL_ANNOTATIONS = false
        super.tearDown()
    }

    private fun addFile(path: String) {
        val file = File(path)
        val root = LightPlatformTestCase.getSourceRoot()
        runWriteAction {
            val virtualFile = root.createChildData(null, file.name)
            virtualFile.getOutputStream(null).writer().use { it.write(FileUtil.loadFile(file)) }
        }
    }

    private fun doTest(kotlinFilePath: String) {
        myFixture.configureByFiles(kotlinFilePath, externalAnnotationsFile, classWithExternalAnnotatedMembers)
        myFixture.checkHighlighting()
    }

    override fun getProjectDescriptor() = object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)
            model.getModuleExtension(JavaModuleExternalPaths::class.java)
                    .setExternalAnnotationUrls(arrayOf(VfsUtilCore.pathToUrl(externalAnnotationsPath)))
        }
    }

    companion object {
        private const val externalAnnotationsPath = "idea/testData/externalAnnotations/annotations/"
        private const val classWithExternalAnnotatedMembers = "idea/testData/externalAnnotations/ClassWithExternalAnnotatedMembers.java"
        private const val externalAnnotationsFile = "$externalAnnotationsPath/annotations.xml"
    }
}
