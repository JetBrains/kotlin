/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Inferring the 'changed' or 'affected' files by a set of changes is done by building the diff since branching from master.
 * This effectively shows the same files changed as in typical review tools (such as space or GitHub)
 */
internal val Project.featureBranchDiffService: Provider<FeatureBranchDiffBuildService>
    get() = gradle.sharedServices.registerIfAbsent("featureBranchDiffService", FeatureBranchDiffBuildService::class.java)

internal abstract class FeatureBranchDiffBuildService : BuildService<FeatureBranchDiffBuildService.Params>, AutoCloseable {
    interface Params : BuildServiceParameters

    @get:Inject
    internal abstract val exec: ExecOperations

    private var _diff: List<String>? = null

    @get:Synchronized
    val diff: List<String>
        get() {
            _diff?.let { return it }
            _diff = calculateFeatureBranchChangedFiles()
            return _diff.orEmpty()
        }

    private var _messages: List<String>? = null

    @get:Synchronized
    val messages: List<String>
        get() {
            _messages?.let { return it }
            _messages = collectFeatureBranchCommitMessages()
            return _messages.orEmpty()
        }

    private fun calculateFeatureBranchChangedFiles(): List<String> {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine("git", "diff", "--name-only", "origin/master...HEAD")
            isIgnoreExitValue = true
            standardOutput = out
            errorOutput = err
        }

        if (result.exitValue != 0) throw Exception(
            "Inferring changed fails (git diff) failed with exit code ${result.exitValue}\n" + err.toByteArray().decodeToString()
        )

        return out.toByteArray().decodeToString().lines().filter { it.isNotBlank() }
    }

    private fun collectFeatureBranchCommitMessages(): List<String> {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val result = exec.exec {
            commandLine("git", "log", "origin/master..HEAD", "--format=%B%x00")
            isIgnoreExitValue = true
            standardOutput = out
            errorOutput = err
        }

        if (result.exitValue != 0) throw Exception(
            "Inferring commit messages fails (git log) failed with exit code ${result.exitValue}\n" + err.toByteArray().decodeToString()
        )

        return out.toByteArray().decodeToString().split('\u0000').map { it.trim() }.filter { it.isNotBlank() }
    }

    @Synchronized
    override fun close() {
        _diff = null
        _messages = null
    }
}
