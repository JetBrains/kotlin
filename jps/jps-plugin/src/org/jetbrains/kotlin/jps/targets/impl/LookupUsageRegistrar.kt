/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.targets.impl

import org.jetbrains.jps.builders.java.dependencyView.Callbacks.Backend
import org.jetbrains.jps.dependency.java.LookupNameUsage
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.LookupTrackerImpl
import org.jetbrains.kotlin.incremental.components.LookupTracker
import java.nio.file.Paths

class LookupUsageRegistrar {
    fun processLookupTracker(lookupTracker: LookupTracker?, callback: Backend, messageCollector: MessageCollector) {
        if (!checkRequiredJpsBuildApi()) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Can't register lookup usages with this version of JPS. $HOW_TO_FIX"
            )
            return
        }

        when (lookupTracker) {
            is LookupTrackerImpl -> registerLookupTrackerImplEntries(lookupTracker, callback)
            else -> {
                // could be DO_NOTHING tracker, TestLookupTracker, RemoteLookupTrackerClient - the last two are not visible, and the first one is irrelevant
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "Can't register lookup usages with this compilation setup. lookupTracker is $lookupTracker. $HOW_TO_FIX"
                )
            }
        }
    }

    // Kotlin plugin can be used with older versions of jps-build, so we check for the availability of APIs.
    private fun checkRequiredJpsBuildApi(): Boolean {
        try {
            Class.forName("org.jetbrains.jps.dependency.java.LookupNameUsage")
        } catch (_: Throwable) {
            return false
        }
        return true
    }

    private fun registerLookupTrackerImplEntries(lookupTracker: LookupTrackerImpl, callback: Backend) {
        for ((lookupKey, fileList) in lookupTracker.lookups.entrySet()) {
            val symbolOwner = lookupKey.scope.replace('.', '/')
            val symbolName = lookupKey.name
            val usage = LookupNameUsage(symbolOwner, symbolName)
            for (file in fileList) {
                callback.registerUsage(Paths.get(file), usage)
            }
        }
    }

    private companion object {
        // these branches should only be reachable if the jps is in the dependency graph mode
        private const val HOW_TO_FIX =
            "Kotlin incremental compilation might be incorrect. Consider using build option -Djps.use.dependency.graph=false"
    }
}
