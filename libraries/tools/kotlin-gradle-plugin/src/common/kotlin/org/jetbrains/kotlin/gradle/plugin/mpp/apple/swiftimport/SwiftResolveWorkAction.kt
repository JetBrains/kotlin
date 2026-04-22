/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import javax.inject.Inject

internal interface SwiftResolveWorkParameters : WorkParameters {
    val syntheticImportProjectRoot: RegularFileProperty
    val swiftPMDependenciesCheckout: RegularFileProperty
    val additionalSwiftPackageResolveArgs: ListProperty<String>
    val gitIgnoreCheckoutDir: Property<Boolean>
}


internal abstract class SwiftResolveWorkAction @Inject constructor(
    private val execOps: ExecOperations,
) : WorkAction<SwiftResolveWorkParameters> {
    override fun execute() {
        execOps.exec { exec ->

            exec.workingDir(parameters.syntheticImportProjectRoot.get().asFile)

            val args = mutableListOf(
                "/usr/bin/swift",
                "package",
                "--scratch-path", parameters.swiftPMDependenciesCheckout.get().asFile,
                "resolve",
            )

            if (parameters.additionalSwiftPackageResolveArgs.isPresent) {
                args.addAll(parameters.additionalSwiftPackageResolveArgs.get())
            }

            val environmentToFilter = listOf("SDKROOT")
            environmentToFilter.forEach { key ->
                if (exec.environment.containsKey(key)) {
                    exec.environment.remove(key)
                }
            }

            exec.commandLine(args)
        }

        if (parameters.gitIgnoreCheckoutDir.get()) {
            writeCheckoutDirToGitIgnore()
        }
    }

    private fun writeCheckoutDirToGitIgnore() {
        val checkoutDir = parameters.swiftPMDependenciesCheckout.get().asFile
        val root = checkoutDir.parentFile
        val exclude = root.resolve(".gitignore")

        if (!exclude.exists()) {
            exclude.parentFile.mkdirs()
            exclude.createNewFile()
        }

        val entry = "${checkoutDir.name}/"

        exclude.writeText(entry)
    }


}
