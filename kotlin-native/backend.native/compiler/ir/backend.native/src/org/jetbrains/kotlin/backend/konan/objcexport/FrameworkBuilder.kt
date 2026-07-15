/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.NativeSecondStageCompilationConfig
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.io.ensureSymbolicLinkTo
import kotlin.io.path.Path
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

/**
 * Constructs an Apple framework without a binary.
 */
internal class FrameworkBuilder(
        private val config: NativeSecondStageCompilationConfig,
        private val infoPListBuilder: InfoPListBuilder,
        private val moduleMapBuilder: ModuleMapBuilder,
        private val objCHeaderWriter: ObjCHeaderWriter,
        private val mainPackageGuesser: MainPackageGuesser,
) {
    fun build(
            moduleDescriptor: ModuleDescriptor,
            frameworkDirectory: Path,
            frameworkName: String,
            headerLines: List<String>,
            moduleDependencies: Set<String>,
    ) {
        val target = config.target
        val frameworkContents = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkDirectory

            Family.OSX -> frameworkDirectory.resolve("Versions/A")
            else -> error(target)
        }

        val headers = frameworkContents.resolve("Headers")

        headers.createDirectories()
        objCHeaderWriter.write("$frameworkName.h", headerLines, headers)

        val modules = frameworkContents.resolve("Modules")
        modules.createDirectories()

        val moduleMap = moduleMapBuilder.build(frameworkName, moduleDependencies)

        modules.resolve("module.modulemap").writeBytes(moduleMap.toByteArray())

        val directory = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkContents

            Family.OSX -> frameworkContents.resolve("Resources").also { it.createDirectories() }
            else -> error(target)
        }

        val infoPlistFile = directory.resolve("Info.plist")
        val infoPlistContents = infoPListBuilder.build(frameworkName, mainPackageGuesser, moduleDescriptor)
        infoPlistFile.writeBytes(infoPlistContents.toByteArray())
        if (target.family == Family.OSX) {
            frameworkDirectory.resolve("Versions/Current").ensureSymbolicLinkTo(Path("A"))
            for (child in listOf(frameworkName, "Headers", "Modules", "Resources")) {
                frameworkDirectory.resolve(child).ensureSymbolicLinkTo(Path("Versions/Current/$child"))
            }
        }
    }
}