/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModule
import org.jetbrains.kotlin.backend.konan.objcexport.sx.SXClangModuleDumper
import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

internal class FrameworkBuilder(
        private val clangModule: SXClangModule,
        private val target: KonanTarget,
        private val frameworkDirectory: File,
        private val frameworkName: String,
        private val exportKDoc: Boolean,
) {
    fun build(infoPListBuilder: InfoPListBuilder, moduleMapBuilder: ModuleMapBuilder) {
        val frameworkContents = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkDirectory

            Family.OSX -> frameworkDirectory.child("Versions/A")
            else -> error(target)
        }

        val headers = frameworkContents.child("Headers")

        headers.mkdirs()
        SXClangModuleDumper(exportKDoc).dumpHeaders(clangModule, headers)

        val modules = frameworkContents.child("Modules")
        modules.mkdirs()

        val moduleMap = moduleMapBuilder.build()

        modules.child("module.modulemap").writeBytes(moduleMap.toByteArray())

        val directory = when (target.family) {
            Family.IOS,
            Family.WATCHOS,
            Family.TVOS -> frameworkContents

            Family.OSX -> frameworkContents.child("Resources").also { it.mkdirs() }
            else -> error(target)
        }

        val infoPlistFile = directory.child("Info.plist")
        val infoPlistContents = infoPListBuilder.build(frameworkName)
        infoPlistFile.writeBytes(infoPlistContents.toByteArray())
        if (target.family == Family.OSX) {
            frameworkDirectory.child("Versions/Current").createAsSymlink("A")
            for (child in listOf(frameworkName, "Headers", "Modules", "Resources")) {
                frameworkDirectory.child(child).createAsSymlink("Versions/Current/$child")
            }
        }
    }
}