/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.*
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl
import com.intellij.util.ArrayUtilRt
import com.intellij.util.ThrowableRunnable
import com.intellij.util.containers.toArray
import com.intellij.util.indexing.UnindexedFilesUpdater
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.perf.ProjectBuilder
import org.jetbrains.kotlin.idea.perf.Stats
import org.jetbrains.kotlin.idea.perf.Stats.Companion.runAndMeasure
import org.jetbrains.kotlin.idea.perf.performanceTest
import org.jetbrains.kotlin.idea.perf.util.ProfileTools.Companion.disableAllInspections
import org.jetbrains.kotlin.idea.perf.util.ProfileTools.Companion.enableAllInspections
import org.jetbrains.kotlin.idea.perf.util.ProfileTools.Companion.enableInspections
import org.jetbrains.kotlin.idea.perf.util.ProfileTools.Companion.enableSingleInspection
import org.jetbrains.kotlin.idea.perf.util.ProfileTools.Companion.initDefaultProfile
import org.jetbrains.kotlin.idea.test.invalidateLibraryCache
import org.jetbrains.kotlin.idea.testFramework.*
import org.jetbrains.kotlin.idea.testFramework.TestApplicationManager
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import java.io.File

/**
 * @author Vladimir Ilmov
 */
class PerformanceSuite {
    companion object {

        fun suite(
            name: String,
            stats: StatsScope,
            block: (StatsScope) -> Unit
        ) {
            TeamCity.suite(name) {
                stats.stats.use {
                    block(stats)
                }
            }
        }

        private fun PsiFile.highlightFile(): List<HighlightInfo> {
            val document = FileDocumentManager.getInstance().getDocument(virtualFile)!!
            val editor = EditorFactory.getInstance().getEditors(document).first()
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            return CodeInsightTestFixtureImpl.instantiateAndRun(this, editor, ArrayUtilRt.EMPTY_INT_ARRAY, true)
        }

        fun rollbackChanges(vararg file: VirtualFile) {
            val fileDocumentManager = FileDocumentManager.getInstance()
            runInEdtAndWait {
                fileDocumentManager.reloadFiles(*file)
            }

            ProjectManagerEx.getInstanceEx().openProjects.forEach { project ->
                val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

                runInEdtAndWait {
                    psiDocumentManagerBase.clearUncommittedDocuments()
                    psiDocumentManagerBase.commitAllDocuments()
                }
            }
        }
    }

    class StatsScope(val config: StatsScopeConfig, val stats: Stats, val rootDisposable: Disposable) {
        fun app(f: ApplicationScope.() -> Unit) = ApplicationScope(rootDisposable, this).use(f)

        fun <T> measure(name: String, f: MeasurementScope<T>.() -> Unit): List<T?> =
            MeasurementScope<T>(name, stats, config).apply(f).run()

        fun <T> measure(name: String, f: MeasurementScope<T>.() -> Unit, after: (() -> Unit)?): List<T?> =
            MeasurementScope<T>(name, stats, config, after = after).apply(f).run()

        fun logStatValue(name: String, value: Any) {
            logMessage { "buildStatisticValue key='${stats.name}: $name' value='$value'" }
            TeamCity.statValue("${stats.name}: $name", value)
        }
    }

    data class TypingConfig(
        val fixture: Fixture,
        val marker: String,
        val insertString: String,
        val surroundItems: String = "\n",
        val typeAfterMarker: Boolean = true,
        val note: String = "",
        val delayMs: Long? = null
    ) : AutoCloseable {
        override fun close() {
            fixture.close()
        }
    }

    class MeasurementScope<T>(
        val name: String,
        val stats: Stats,
        val config: StatsScopeConfig,
        var before: () -> Unit = {},
        var test: (() -> T?)? = null,
        var after: (() -> Unit)? = null
    ) {
        fun run(): List<T?> {
            val t = test ?: error("test procedure isn't set")
            val value = mutableListOf<T?>()
            performanceTest<Unit, T> {
                name(name)
                stats(stats)
                warmUpIterations(config.warmup)
                iterations(config.iterations)
                setUp {
                    before()
                }
                test {
                    value.add(t.invoke())
                }
                tearDown {
                    after?.invoke()
                }
                profilerEnabled(config.profile)
            }
            return value
        }
    }

    class ApplicationScope(val rootDisposable: Disposable, val stats: StatsScope) : AutoCloseable {
        val application = initApp(rootDisposable)
        val jdk: Sdk = initSdk(rootDisposable)

        fun project(externalProject: ExternalProject, refresh: Boolean = false, block: ProjectScope.() -> Unit) =
            ProjectScope(ProjectScopeConfig(externalProject, refresh), this).use(block)

        fun project(block: ProjectWithDescriptorScope.() -> Unit) =
            ProjectWithDescriptorScope(this).use(block)

        fun project(path: String, openWith: ProjectOpenAction = ProjectOpenAction.EXISTING_IDEA_PROJECT, block: ProjectScope.() -> Unit) =
            ProjectScope(ProjectScopeConfig(path, openWith), this).use(block)

        fun gradleProject(path: String, refresh: Boolean = false, block: ProjectScope.() -> Unit) =
            ProjectScope(ProjectScopeConfig(path, ProjectOpenAction.GRADLE_PROJECT, refresh), this).use(block)

        fun warmUpProject() = project {
            descriptor {
                name("helloWorld")

                kotlinFile("HelloMain") {
                    topFunction("main") {
                        param("args", "Array<String>")
                        body("""println("Hello World!")""")
                    }
                }
            }

            fixture("src/HelloMain.kt").use {
                highlight(it)
            }
        }

        override fun close() {
            application?.setDataProvider(null)
        }

        companion object {
            fun initApp(rootDisposable: Disposable): TestApplicationManager {
                val application = TestApplicationManager.getInstance()
                GradleProcessOutputInterceptor.install(rootDisposable)
                return application
            }

            fun initSdk(rootDisposable: Disposable): Sdk {
                return runWriteAction {
                    val jdkTableImpl = JavaAwareProjectJdkTableImpl.getInstanceEx()
                    val homePath = if (jdkTableImpl.internalJdk.homeDirectory!!.name == "jre") {
                        jdkTableImpl.internalJdk.homeDirectory!!.parent.path
                    } else {
                        jdkTableImpl.internalJdk.homePath!!
                    }

                    val javaSdk = JavaSdk.getInstance()
                    val jdk = javaSdk.createJdk("1.8", homePath)
                    val internal = javaSdk.createJdk("IDEA jdk", homePath)
                    val gradle = javaSdk.createJdk(GRADLE_JDK_NAME, homePath)

                    val jdkTable = getProjectJdkTableSafe()
                    jdkTable.addJdk(jdk, rootDisposable)
                    jdkTable.addJdk(internal, rootDisposable)
                    jdkTable.addJdk(gradle, rootDisposable)
                    KotlinSdkType.setUpIfNeeded()
                    jdk
                }
            }
        }
    }


    class StatsScopeConfig(var name: String? = null, var warmup: Int = 2, var iterations: Int = 5, var profile: Boolean = false)

    class ProjectScopeConfig(val path: String, val openWith: ProjectOpenAction, val refresh: Boolean = false) {
        val name: String = path.lastPathSegment()

        constructor(externalProject: ExternalProject, refresh: Boolean) : this(externalProject.path, externalProject.openWith, refresh)
    }

    abstract class AbstractProjectScope(val app: ApplicationScope) : AutoCloseable {
        abstract val project: Project
        val openFiles = mutableListOf<VirtualFile>()

        fun profile(profile: ProjectProfile) {
            when (profile) {
                EmptyProfile -> project.disableAllInspections()
                DefaultProfile -> project.initDefaultProfile()
                FullProfile -> project.enableAllInspections()
                is CustomProfile -> project.enableInspections(*profile.inspectionNames.toArray(emptyArray()))
            }
        }

        fun highlight(fixture: Fixture) = highlight(fixture.psiFile)

        fun highlight(editorFile: PsiFile?) =
            editorFile?.let {
                it.highlightFile()
            } ?: error("editor isn't ready for highlight")

        fun moveCursor(config: TypingConfig) {
            val fixture = config.fixture
            val editor = fixture.editor
            updateScriptDependenciesIfNeeded(fixture)

            val marker = config.marker
            val tasksIdx = fixture.text.indexOf(marker)
            check(tasksIdx > 0) {
                "marker '$marker' not found in ${fixture.fileName}"
            }
            if (config.typeAfterMarker) {
                editor.caretModel.moveToOffset(tasksIdx + marker.length + 1)
            } else {
                editor.caretModel.moveToOffset(tasksIdx - 1)
            }

            for (surroundItem in config.surroundItems) {
                EditorTestUtil.performTypingAction(editor, surroundItem)
            }

            editor.caretModel.moveToOffset(editor.caretModel.offset - if (config.typeAfterMarker) 1 else 2)

            if (!config.typeAfterMarker) {
                for (surroundItem in config.surroundItems) {
                    EditorTestUtil.performTypingAction(editor, surroundItem)
                }
                editor.caretModel.moveToOffset(editor.caretModel.offset - 2)
            }
        }

        fun typeAndHighlight(config: TypingConfig): List<HighlightInfo> {
            val string = config.insertString
            for (i in string.indices) {
                config.fixture.type(string[i])
                config.delayMs?.let { d -> Thread.sleep(d) }
            }
            return config.fixture.doHighlighting()
        }

        fun updateScriptDependenciesIfNeeded(fixture: Fixture) {
            val path = fixture.vFile.path
            if (Fixture.isAKotlinScriptFile(path)) {
                runAndMeasure("update script dependencies for $path") {
                    ScriptConfigurationManager.updateScriptDependenciesSynchronously(fixture.psiFile)
                }
            }
        }

        fun enableSingleInspection(inspectionName: String) =
            this.project.enableSingleInspection(inspectionName)

        fun enableAllInspections() =
            this.project.enableAllInspections()

        fun editor(path: String) =
            Fixture.openFileInEditor(project, path).psiFile.also { openFiles.add(it.virtualFile) }

        fun fixture(path: String): Fixture {
            val fixture = Fixture.openFixture(project, path)
            openFiles.add(fixture.vFile)
            if (Fixture.isAKotlinScriptFile(path)) {
                ScriptConfigurationManager.updateScriptDependenciesSynchronously(fixture.psiFile)
            }
            return fixture
        }

        fun rollbackChanges() =
            rollbackChanges(*openFiles.toTypedArray())

        fun close(editorFile: PsiFile?) {
            commitAllDocuments()
            editorFile?.virtualFile?.let {
                FileEditorManager.getInstance(project).closeFile(it)
            }
        }

        fun <T> measure(vararg name: String, f: MeasurementScope<T>.() -> Unit): List<T?> {
            val after = { PsiManager.getInstance(project).dropPsiCaches() }
            return app.stats.measure("${name.joinToString("-")}", f, after)
        }

        fun <T> measure(vararg name: String, fixture: Fixture, f: MeasurementScope<T>.() -> Unit): List<T?> =
            measure("${name.joinToString("-")} ${fixture.fileName}", f = f)

        override fun close() {
            RunAll(
                ThrowableRunnable {
                    project?.let { prj ->
                        app.application?.closeProject(prj)
                    }
                }).run()
        }
    }

    class ProjectWithDescriptorScope(app: ApplicationScope) : AbstractProjectScope(app) {
        private var descriptor: ProjectBuilder? = null
        override val project: Project by lazy {
            val builder = descriptor ?: error("project is not configured")
            val openProject = builder.openProjectOperation()

            openProject.openProject().also {
                openProject.postOpenProject(it)
            }
        }

        fun descriptor(descriptor: ProjectBuilder.() -> Unit) {
            this.descriptor = ProjectBuilder().apply(descriptor)
        }
    }

    class ProjectScope(config: ProjectScopeConfig, app: ApplicationScope) : AbstractProjectScope(app) {
        override val project: Project = initProject(config, app)

        companion object {
            fun initProject(config: ProjectScopeConfig, app: ApplicationScope): Project {
                val projectPath = File(config.path).canonicalPath

                UsefulTestCase.assertTrue("path ${config.path} does not exist, check README.md", File(projectPath).exists())

                val openProject = OpenProject(
                    projectPath = projectPath,
                    projectName = config.name,
                    jdk = app.jdk,
                    projectOpenAction = config.openWith
                )
                val project = ProjectOpenAction.openProject(openProject)
                openProject.projectOpenAction.postOpenProject(project, openProject)

                // indexing
                if (config.refresh) {
                    invalidateLibraryCache(project)
                }

                CodeInsightTestFixtureImpl.ensureIndexesUpToDate(project)

                dispatchAllInvocationEvents()
                with(DumbService.getInstance(project)) {
                    queueTask(UnindexedFilesUpdater(project))
                    completeJustSubmittedTasks()
                }
                dispatchAllInvocationEvents()

                Fixture.enableAnnotatorsAndLoadDefinitions(project)

                app.application?.setDataProvider(TestDataProvider(project))

                return project
            }
        }
    }
}

sealed class ProjectProfile
object EmptyProfile : ProjectProfile()
object DefaultProfile : ProjectProfile()
object FullProfile : ProjectProfile()
data class CustomProfile(val inspectionNames: List<String>) : ProjectProfile()

fun UsefulTestCase.suite(
    suiteName: String? = null,
    config: PerformanceSuite.StatsScopeConfig = PerformanceSuite.StatsScopeConfig(),
    block: PerformanceSuite.StatsScope.() -> Unit
) {
    PerformanceSuite.suite(
        suiteName ?: this.javaClass.name,
        PerformanceSuite.StatsScope(config, Stats(config.name ?: suiteName ?: name), testRootDisposable),
        block
    )
}