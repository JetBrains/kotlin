/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.handlers

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.needSmallBinary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestDirectives
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.testRunSettings
import org.jetbrains.kotlin.konan.test.klib.fileCheckDump
import org.jetbrains.kotlin.konan.test.klib.fileCheckStage
import org.jetbrains.kotlin.native.executors.runProcess
import org.jetbrains.kotlin.test.groupingPhaseInputs
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.GroupingPhaseHandler
import org.jetbrains.kotlin.test.model.TestArtifactKind
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.io.File

class FileCheckHandler(testServices: TestServices) : GroupingPhaseHandler<BinaryArtifacts.Native>(
    testServices,
    failureDisablesNextSteps = false,
    doNotRunIfThereWerePreviousFailures = false
) {
    override val artifactKind: TestArtifactKind<BinaryArtifacts.Native>
        get() = ArtifactKinds.Native

    /**
     * Mimics [org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunCheck.FileCheckMatcher]
     */
    override fun processArtifact(artifact: BinaryArtifacts.Native) {
        val originalModuleStructures = testServices.groupingPhaseInputs.map { it.testServices.moduleStructure }
        if (originalModuleStructures.first().allDirectives[TestDirectives.FILECHECK_STAGE].isEmpty())
            return
        require(originalModuleStructures.size == 1) {
            "Test having FILECHECK_STAGE must be standalone: ${originalModuleStructures.first().originalTestDataFiles}"
        }
        val module = originalModuleStructures.single().modules.first()
        val fileCheckStage = module.fileCheckStage() ?: error("FILECHECK_STAGE directive is missing in module ${module.name}")
        val testDataFile = module.files.first().originalFile
        val fileCheckDump = artifact.executable.fileCheckDump(fileCheckStage)

        val settings = testServices.testRunSettings
        val prefixes = getPrefixes(settings)
        val fileCheckExecutable = settings.configurables.absoluteLlvmHome + File.separator + "bin" + File.separator +
                if (SystemInfo.isWindows) "FileCheck.exe" else "FileCheck"
        require(File(fileCheckExecutable).exists()) {
            "$fileCheckExecutable does not exist. Make sure Distribution for `settings.configurables` " +
                    "was created using `propertyOverrides` to specify development variant of LLVM instead of user variant."
        }

        val result = try {
            runProcess(
                fileCheckExecutable,
                testDataFile.absolutePath,
                "--input-file",
                fileCheckDump.absolutePath,
                "--check-prefixes", prefixes,
                "--allow-unused-prefixes",
                "--allow-deprecated-dag-overlap"
            )
        } catch (t: Throwable) {
            testServices.assertions.fail { "FileCheck utility failed: $t" }
        }

        if (!(result.stdout.isEmpty() && result.stderr.isEmpty())) {
            val shortOutText = result.stdout.lines().take(100)
            val shortErrText = result.stderr.lines().take(100)

            testServices.assertions.fail {
                """
                FileCheck matching of ${fileCheckDump.absolutePath}
                with '--check-prefixes $prefixes'
                failed with result=$result:
                ${shortOutText.joinToString("\n")}
                ${shortErrText.joinToString("\n")}
                """.trimIndent()
            }
        }
    }

    private fun getPrefixes(settings: Settings): String {
        val testTarget = settings.get<KotlinNativeTargets>().testTarget
        val optimizationMode = settings.get<OptimizationMode>()
        val checkPrefixes = buildList {
            add("CHECK")
            add("CHECK-${testTarget.abiInfoString}")
            add("CHECK-${testTarget.name.toUpperCaseAsciiOnly()}")
            if (testTarget.family.isAppleFamily) {
                add("CHECK-APPLE")
            }
            if (testTarget.needSmallBinary() || optimizationMode == OptimizationMode.DEBUG
                || settings.get<ExplicitBinaryOptions>().getOrNull<Boolean>(BinaryOptions.smallBinary) == true
            ) {
                add("CHECK-SMALLBINARY")
            } else {
                add("CHECK-BIGBINARY")
            }
        }
        val optMode = when (optimizationMode) {
            OptimizationMode.NO, OptimizationMode.DEBUG -> "DEBUG"
            OptimizationMode.OPT -> "OPT"
        }
        val checkPrefixesWithOptMode = checkPrefixes.map { "$it-$optMode" }
        val cacheMode = settings.get<CacheMode>().alias
        val checkPrefixesWithCacheMode = checkPrefixes.map { "$it-CACHE_$cacheMode" }
        return (checkPrefixes + checkPrefixesWithOptMode + checkPrefixesWithCacheMode).joinToString(",")
    }

    private val KonanTarget.abiInfoString: String
        get() = when {
            this == KonanTarget.MINGW_X64 -> "WINDOWSX64"
            !family.isAppleFamily && architecture == Architecture.ARM64 -> "AAPCS"
            else -> "DEFAULTABI"
        }
}
