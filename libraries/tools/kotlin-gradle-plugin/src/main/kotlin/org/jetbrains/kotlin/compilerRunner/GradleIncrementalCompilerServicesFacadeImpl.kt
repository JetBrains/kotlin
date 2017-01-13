package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.common.*
import org.jetbrains.kotlin.daemon.incremental.toDirtyData
import org.jetbrains.kotlin.daemon.incremental.toSimpleDirtyData
import org.jetbrains.kotlin.gradle.plugin.kotlinDebug
import org.jetbrains.kotlin.gradle.plugin.kotlinWarn
import org.jetbrains.kotlin.incremental.DirtyData
import org.jetbrains.kotlin.incremental.multiproject.ArtifactDifference
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

    override fun report(category: Int, severity: Int, message: String?, attachment: Serializable?) {
        val reportCategory = ReportCategory.fromCode(category)

        when (reportCategory) {
            ReportCategory.OUTPUT_MESSAGE -> {
                compilerMessageCollector.report(CompilerMessageSeverity.OUTPUT, message!!, CompilerMessageLocation.NO_LOCATION)
            }
            ReportCategory.EXCEPTION -> {
                compilerMessageCollector.report(CompilerMessageSeverity.EXCEPTION, message!!, CompilerMessageLocation.NO_LOCATION)
            }
            ReportCategory.COMPILER_MESSAGE -> {
                val compilerSeverity = when (ReportSeverity.fromCode(severity)) {
                    ReportSeverity.ERROR -> CompilerMessageSeverity.ERROR
                    ReportSeverity.WARNING -> CompilerMessageSeverity.WARNING
                    ReportSeverity.INFO -> CompilerMessageSeverity.INFO
                    ReportSeverity.DEBUG -> CompilerMessageSeverity.LOGGING
                    else -> throw IllegalStateException("Unexpected compiler message report severity $severity")
                }
                if (message != null && attachment is CompilerMessageLocation) {
                    compilerMessageCollector.report(compilerSeverity, message, attachment)
                }
                else {
                    reportUnexpectedMessage(category, severity, message, attachment)
                }
            }
            ReportCategory.IC_MESSAGE -> {
                log.kotlinDebug { "[IC] $message" }
            }
            ReportCategory.DAEMON_MESSAGE -> {
                log.kotlinDebug { "[DAEMON] $message" }
            }
            else -> {
                reportUnexpectedMessage(category, severity, message, attachment)
            }
        }
    }

    protected fun reportUnexpectedMessage(category: Int, severity: Int, message: String?, attachment: Serializable?) {
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