package org.jetbrains.kotlin.idea.artifacts

import com.intellij.openapi.application.ApplicationManager
import java.io.File

abstract class KotlinArtifacts {
    companion object {
        fun getInstance(): KotlinArtifacts {
            return when {
                ApplicationManager.getApplication().isUnitTestMode -> TestKotlinArtifacts
                else -> ProductionKotlinArtifacts
            }
        }
    }

    abstract val kotlincDirectory: File

    abstract val jetbrainsAnnotations: File
    abstract val kotlinStdlib: File
    abstract val kotlinStdlibSources: File
    abstract val kotlinReflect: File
    abstract val kotlinStdlibJs: File
    abstract val kotlinTest: File
    abstract val kotlinMainKts: File
    abstract val kotlinScriptRuntime: File

    protected fun findFile(parent: File, path: String): File {
        val result = File(parent, path)
        if (!result.exists()) {
            throw IllegalStateException("File $result doesn't exist")
        }
        return result
    }
}