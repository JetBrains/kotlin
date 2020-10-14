/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.DebuggerInvocationUtil
import com.intellij.debugger.engine.JavaDebugProcess
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.ui.RunnerLayoutUi
import com.intellij.execution.ui.layout.PlaceInGrid
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JdkUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem
import com.intellij.ui.content.Content
import com.intellij.util.messages.MessageBusConnection
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerManager
import com.intellij.xdebugger.XDebuggerManagerListener
import com.intellij.xdebugger.impl.XDebugSessionImpl
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.jetbrains.kotlin.idea.debugger.coroutine.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.coroutine.view.XCoroutineView

class DebuggerConnection(
    val project: Project,
    val configuration: RunConfigurationBase<*>,
    val params: JavaParameters?,
    modifyArgs: Boolean = true
) : XDebuggerManagerListener, Disposable {
    var connection: MessageBusConnection? = null
    private var coroutineAgentAttached: Boolean = false
    private val log by logger

    init {
        if (params is JavaParameters && modifyArgs) {
            // gradle related logic in KotlinGradleCoroutineDebugProjectResolver
            val kotlinxCoroutinesCore = params.classPath?.pathList?.firstOrNull { it.contains("kotlinx-coroutines-core") }
            if (kotlinxCoroutinesCore != null) {
                val mode = determineCoreVersionMode(kotlinxCoroutinesCore)
                when (mode) {
                    CoroutineDebuggerMode.VERSION_1_3_8_AND_UP -> {
                        initializeCoroutineAgent(params, kotlinxCoroutinesCore)
                        coroutineAgentAttached = true
                    }
                    else -> log.debug("CoroutineDebugger disabled.")
                }
            }
        }
        connect()
    }

    private fun determineCoreVersionMode(kotlinxCoroutinesCore: String): CoroutineDebuggerMode {
        val regex = Regex(""".+\Wkotlinx-coroutines-core(\-jvm)?-(\d[\w\.\-]+)?\.jar""")
        val matchResult = regex.matchEntire(kotlinxCoroutinesCore) ?: return CoroutineDebuggerMode.DISABLED
        val versionToCompareTo = DefaultArtifactVersion("1.3.7-255")

        val artifactVersion = DefaultArtifactVersion(matchResult.groupValues[2])
        return if (artifactVersion > versionToCompareTo)
            CoroutineDebuggerMode.VERSION_1_3_8_AND_UP
        else
            CoroutineDebuggerMode.DISABLED
    }

    private fun initializeCoroutineAgent(params: JavaParameters, it: String?) {
        params.vmParametersList?.add("-javaagent:$it")
        // Fix for NoClassDefFoundError: kotlin/collections/AbstractMutableMap via CommandLineWrapper.
        // If classpathFile used in run configuration - kotlin-stdlib should be included in the -classpath
        if (params.isClasspathFile) {
            params.classPath.rootDirs.filter { it.isKotlinStdlib() }.forEach {
                val fs = it.fileSystem
                val path = when (fs) {
                    is ArchiveFileSystem -> fs.getLocalByEntry(it)?.path
                    else -> it.path
                }
                it.putUserData(JdkUtil.AGENT_RUNTIME_CLASSPATH, path)
            }
        }
    }

    private fun connect() {
        connection = project.messageBus.connect()
        connection?.subscribe(XDebuggerManager.TOPIC, this)
    }

    override fun processStarted(debugProcess: XDebugProcess) {
        DebuggerInvocationUtil.swingInvokeLater(project) {
            if (debugProcess is JavaDebugProcess) {
                if (!Disposer.isDisposed(this) && coroutinesPanelShouldBeShown()) {
                    registerXCoroutinesPanel(debugProcess.session)?.let {
                        Disposer.register(this, it)
                    }
                }
            }
        }
    }

    override fun processStopped(debugProcess: XDebugProcess) {
        ApplicationManager.getApplication().invokeLater {
            Disposer.dispose(this)
        }
    }

    private fun registerXCoroutinesPanel(session: XDebugSession): Disposable? {
        val ui = session.ui ?: return null
        val xCoroutineThreadView = XCoroutineView(project, session as XDebugSessionImpl)
        val framesContent: Content = createContent(ui, xCoroutineThreadView)
        framesContent.isCloseable = false
        ui.addContent(framesContent, 0, PlaceInGrid.right, false)
        session.addSessionListener(xCoroutineThreadView.debugSessionListener(session))
        session.rebuildViews()
        return xCoroutineThreadView
    }

    private fun coroutinesPanelShouldBeShown() = configuration is ExternalSystemRunConfiguration || coroutineAgentAttached

    private fun createContent(ui: RunnerLayoutUi, createContentParamProvider: CreateContentParamsProvider): Content {
        val param = createContentParamProvider.createContentParams()
        return ui.createContent(param.id, param.component, param.displayName, param.icon, param.parentComponent)
    }

    override fun dispose() {
        connection?.disconnect()
        connection = null
    }
}

fun VirtualFile.isKotlinStdlib() =
        this.path.contains("kotlin-stdlib")

enum class CoroutineDebuggerMode {
    DISABLED,
    VERSION_UP_TO_1_3_5,
    VERSION_1_3_6_AND_UP,
    VERSION_1_3_8_AND_UP,
}