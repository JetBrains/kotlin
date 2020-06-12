package org.jetbrains.kotlin.idea.artifacts

import com.intellij.openapi.application.ApplicationManager
import java.io.File
import java.io.FileNotFoundException

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
    abstract val kotlincLibDirectory: File

    abstract val jetbrainsAnnotations: File
    abstract val kotlinStdlib: File
    abstract val kotlinStdlibSources: File
    abstract val kotlinStdlibJdk7: File
    abstract val kotlinStdlibJdk7Sources: File
    abstract val kotlinStdlibJdk8: File
    abstract val kotlinStdlibJdk8Sources: File
    abstract val kotlinStdlibCommon: File
    abstract val kotlinStdlibCommonSources: File
    abstract val kotlinReflect: File
    abstract val kotlinStdlibJs: File
    abstract val kotlinStdlibJsSources: File
    abstract val kotlinTest: File
    abstract val kotlinTestJunit: File
    abstract val kotlinTestJs: File
    abstract val kotlinMainKts: File
    abstract val kotlinScriptRuntime: File
    abstract val kotlinScriptingCommon: File
    abstract val kotlinScriptingJvm: File

    protected fun findFile(parent: File, path: String): File {
        val result = File(parent, path)
        if (!result.exists()) {
            throw FileNotFoundException("File $result doesn't exist")
        }
        return result
    }
}