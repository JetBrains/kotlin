package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.incremental.CompileIterationResult
import org.jetbrains.kotlin.daemon.incremental.IncrementalCompilationSeverity
import org.jetbrains.kotlin.daemon.incremental.toDirtyData
import org.jetbrains.kotlin.daemon.incremental.toSimpleDirtyData
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.kotlinWarn
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifference
import org.jetbrains.kotlin.incremental.pathsAsStringRelativeTo
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.io.File
import java.io.Serializable
import java.rmi.Remote
import java.rmi.server.UnicastRemoteObject

internal open class GradleCompilerServicesFacadeImpl(
        project: Project,
        val compilerMessageCollector: MessageCollector,
        port: Int = SOCKET_ANY_FREE_PORT
) : UnicastRemoteObject(port), CompilerServicesFacadeBase, Remote {
    protected val log: Logger = project.logger

    override fun report(category: ReportCategory, severity: Int, message: String?, attachment: Serializable?) {
        when (category) {
            ReportCategory.COMPILER_MESSAGE -> {
                val compilerSeverity = CompilerMessageSeverity.values().firstOrNull { it.value == severity }

                if (compilerSeverity != null && message != null && attachment is CompilerMessageLocation) {
                    compilerMessageCollector.report(compilerSeverity, message, attachment)
                }
                else {
                    reportUnexpectedMessage(category, severity, message, attachment)
                }
            }
            ReportCategory.DAEMON_MESSAGE -> {
                log.kotlinDebug { "[DAEMON] $message" }
            }
            else -> {
                reportUnexpectedMessage(category, severity, message, attachment)
            }
        }
    }

    protected fun reportUnexpectedMessage(category: ReportCategory, severity: Int, message: String?, attachment: Serializable?) {
        // todo add assert to tests
        log.kotlinWarn("Received unexpected message from compiler daemon: category=$category, severity=$severity, message='$message', attachment=$attachment")
    }
}

internal class GradleIncrementalCompilerServicesFacadeImpl(
        project: Project,
        private val environment: GradleIncrementalCompilerEnvironment,
        port: Int = SOCKET_ANY_FREE_PORT
) : GradleCompilerServicesFacadeImpl(project, environment.messageCollector, port),
    IncrementalCompilerServicesFacade {

    private val projectRootFile = project.rootProject.projectDir

    override fun report(category: ReportCategory, severity: Int, message: String?, attachment: Serializable?) {
        when (category) {
            ReportCategory.INCREMENTAL_COMPILATION -> {
                val icSeverity = IncrementalCompilationSeverity.values().firstOrNull { it.value == severity }
                when (icSeverity) {
                    IncrementalCompilationSeverity.COMPILED_FILES -> {
                        @Suppress("UNCHECKED_CAST")
                        val compileIterationResult = attachment as? CompileIterationResult
                        if (compileIterationResult == null) {
                            reportUnexpectedMessage(category, severity, message, attachment)
                        }
                        else {
                            val sourceFiles = compileIterationResult.sourceFiles
                            if (sourceFiles.any()) {
                                log.kotlinDebug { "compile iteration: ${sourceFiles.pathsAsStringRelativeTo(projectRootFile)}" }
                            }
                            val exitCode = compileIterationResult.exitCode
                            log.kotlinDebug { "compiler exit code: $exitCode" }
                        }
                    }
                    IncrementalCompilationSeverity.LOGGING -> {
                        log.kotlinDebug { "[IC] $message" }
                    }
                    else -> {
                        reportUnexpectedMessage(category, severity, message, attachment)
                    }
                }
            }
            else -> {
                super.report(category, severity, message, attachment)
            }
        }
    }

    override fun hasAnnotationsFileUpdater(): Boolean =
            environment.kaptAnnotationsFileUpdater != null

    override fun updateAnnotations(outdatedClassesJvmNames: Iterable<String>) {
        val jvmNames = outdatedClassesJvmNames.map { JvmClassName.byInternalName(it) }
        environment.kaptAnnotationsFileUpdater!!.updateAnnotations(jvmNames)
    }

    override fun revert() {
        environment.kaptAnnotationsFileUpdater!!.revert()
    }

    override fun getChanges(artifact: File, sinceTS: Long): Iterable<SimpleDirtyData>? {
        val artifactChanges = environment.artifactDifferenceRegistryProvider?.withRegistry(environment.reporter) { registry ->
            registry[artifact]
        } ?: return null

        val (beforeLastBuild, afterLastBuild) = artifactChanges.partition { it.buildTS < sinceTS }
        if (beforeLastBuild.isEmpty()) return null

        return afterLastBuild.map { it.dirtyData.toSimpleDirtyData() }
    }

    override fun registerChanges(timestamp: Long, dirtyData: SimpleDirtyData) {
        val artifactFile = environment.artifactFile ?: return

        environment.artifactDifferenceRegistryProvider?.withRegistry(environment.reporter) { registry ->
            registry.add(artifactFile, ArtifactDifference(timestamp, dirtyData.toDirtyData()))
        }
    }

    override fun unknownChanges(timestamp: Long) {
        val artifactFile = environment.artifactFile ?: return

        environment.artifactDifferenceRegistryProvider?.withRegistry(environment.reporter) { registry ->
            registry.remove(artifactFile)
            registry.add(artifactFile, ArtifactDifference(timestamp, DirtyData()))
        }
    }
}