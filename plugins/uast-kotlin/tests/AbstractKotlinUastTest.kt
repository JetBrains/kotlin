package org.jetbrains.uast.test.kotlin

import com.intellij.mock.MockProject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.io.URLUtil
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoot
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.script.KotlinScriptDefinitionProvider
import org.jetbrains.kotlin.script.StandardScriptDefinition
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.uast.UastLanguagePlugin
import org.jetbrains.uast.evaluation.UEvaluatorExtension
import org.jetbrains.uast.kotlin.KotlinUastBindingContextProviderService
import org.jetbrains.uast.kotlin.KotlinUastLanguagePlugin
import org.jetbrains.uast.kotlin.evaluation.KotlinEvaluatorExtension
import org.jetbrains.uast.kotlin.internal.CliKotlinUastBindingContextProviderService
import org.jetbrains.uast.kotlin.internal.UastAnalysisHandlerExtension
import org.jetbrains.uast.test.env.AbstractCoreEnvironment
import org.jetbrains.uast.test.env.AbstractUastTest
import java.io.File

abstract class AbstractKotlinUastTest : AbstractUastTest() {
    protected companion object {
        val TEST_KOTLIN_MODEL_DIR = File("plugins/uast-kotlin/testData")
    }

    private lateinit var compilerConfiguration: CompilerConfiguration
    private var kotlinCoreEnvironment: KotlinCoreEnvironment? = null

    override fun getVirtualFile(testName: String): VirtualFile {
        val testFile = TEST_KOTLIN_MODEL_DIR.listFiles { pathname -> pathname.nameWithoutExtension == testName }.first()

        super.initializeEnvironment(testFile)

        initializeKotlinEnvironment()

        KotlinScriptDefinitionProvider.getInstance(project)?.addScriptDefinition(StandardScriptDefinition)

        val trace = CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace()

        val environment = kotlinCoreEnvironment!!
        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                project, environment.getSourceFiles(), trace, compilerConfiguration, environment::createPackagePartProvider
        )

        val vfs = VirtualFileManager.getInstance().getFileSystem(URLUtil.FILE_PROTOCOL)

        val ideaProject = project
        ideaProject.baseDir = vfs.findFileByPath(TEST_KOTLIN_MODEL_DIR.canonicalPath)

        return vfs.findFileByPath(testFile.canonicalPath)!!
    }

    private fun initializeKotlinEnvironment() {
        val area = Extensions.getRootArea()
        area.getExtensionPoint(UastLanguagePlugin.extensionPointName)
                .registerExtension(KotlinUastLanguagePlugin())
        area.getExtensionPoint(UEvaluatorExtension.EXTENSION_POINT_NAME)
                .registerExtension(KotlinEvaluatorExtension())

        project.registerService(
                KotlinUastBindingContextProviderService::class.java,
                CliKotlinUastBindingContextProviderService::class.java)
    }

    override fun createEnvironment(source: File): AbstractCoreEnvironment {
        val appWasNull = ApplicationManager.getApplication() == null
        compilerConfiguration = createKotlinCompilerConfiguration(source)
        val parentDisposable = Disposer.newDisposable()
        val kotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
                parentDisposable,
                compilerConfiguration,
                EnvironmentConfigFiles.JVM_CONFIG_FILES)

        this.kotlinCoreEnvironment = kotlinCoreEnvironment

        AnalysisHandlerExtension.registerExtension(
                kotlinCoreEnvironment.project, UastAnalysisHandlerExtension())

        return KotlinCoreEnvironmentWrapper(kotlinCoreEnvironment, parentDisposable, appWasNull)
    }

    override fun tearDown() {
        kotlinCoreEnvironment = null
        super.tearDown()
    }

    private fun createKotlinCompilerConfiguration(sourceFile: File): CompilerConfiguration {
        return KotlinTestUtils.newConfiguration(ConfigurationKind.ALL, TestJdkKind.FULL_JDK).apply {
            addKotlinSourceRoot(sourceFile.canonicalPath)

            val messageCollector = PrintingMessageCollector(System.err, MessageRenderer.PLAIN_RELATIVE_PATHS, true)
            put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)

            put(CommonConfigurationKeys.MODULE_NAME, "test-module")
        }
    }

    private class KotlinCoreEnvironmentWrapper(val environment: KotlinCoreEnvironment,
                                               val parentDisposable: Disposable,
                                               val appWasNull: Boolean) : AbstractCoreEnvironment() {
        override fun addJavaSourceRoot(root: File) {
            TODO("not implemented")
        }

        override fun addJar(root: File) {
            TODO("not implemented")
        }

        override val project: MockProject
            get() = environment.project as MockProject

        override fun dispose() {
            Disposer.dispose(parentDisposable)
            if (appWasNull) {
                KtUsefulTestCase.resetApplicationToNull()
            }
        }
    }
}