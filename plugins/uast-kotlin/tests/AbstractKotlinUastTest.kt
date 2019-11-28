package org.jetbrains.uast.test.kotlin

import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.uast.UFile
import org.jetbrains.uast.UastFacade
import java.io.File

abstract class AbstractKotlinUastTest : KotlinLightCodeInsightFixtureTestCase() {

    override fun getProjectDescriptor(): LightProjectDescriptor =
        KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        Registry.get("kotlin.uast.multiresolve.enabled").setValue(true, testRootDisposable)
    }

    protected companion object {
        val TEST_KOTLIN_MODEL_DIR = File("plugins/uast-kotlin/testData")
    }

    fun getVirtualFile(testName: String): VirtualFile {
        val testFile = TEST_KOTLIN_MODEL_DIR.listFiles { pathname -> pathname.nameWithoutExtension == testName }.first()


        val vfs = VirtualFileManager.getInstance().getFileSystem(URLUtil.FILE_PROTOCOL)

//        val ideaProject = project
//        ideaProject.baseDir = vfs.findFileByPath(TEST_KOTLIN_MODEL_DIR.canonicalPath)

        return vfs.findFileByPath(testFile.canonicalPath)!!
    }

    abstract fun check(testName: String, file: UFile)

    fun doTest(testName: String, checkCallback: (String, UFile) -> Unit = { testName, file -> check(testName, file) }) {
        val virtualFile = getVirtualFile(testName)

        val psiFile = psiManager.findFile(virtualFile) ?: error("Can't get psi file for $testName")
        val uFile = UastFacade.convertElementWithParent(psiFile, null) ?: error("Can't get UFile for $testName")
        checkCallback(testName, uFile as UFile)
    }

}