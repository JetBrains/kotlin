/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.resolve.constants.fromUIntToLong
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

// https://github.com/aidansteele/osx-abi-macho-file-format-reference
@Suppress("SpellCheckingInspection")
internal object MachO {

    private const val FAT_MAGIC = 0xcafebabeL
    private const val FAT_MAGIC_64 = 0xcafebabfL

    private const val MH_CIGAM = 0xcefaedfeL
    private const val MH_CIGAM_64 = 0xcffaedfeL

    private const val MH_BILYD = 0x06000000L // MH_DYLIB = 0x6 converted for JVM

    private const val FILE_TYPE_OFFSET = 12L
    private const val FAT_FIRST_MACHO_OFFSET_OFFSET = 16L

    /**
     * Checks if the [file] is a Mach-O dynamic shared library
     * or a Mach-O fat binary containing a number of dynamic libraries
     */
    fun isDylib(file: File, logger: Logger): Boolean {
        try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = raf.readInt().fromUIntToLong()

                val fileTypeOffset = when (magic) {
                    // all supported platforms are little-endian and JVM is big-endian, so we don't check MH_MAGIC[_64]
                    MH_CIGAM, MH_CIGAM_64 -> FILE_TYPE_OFFSET
                    FAT_MAGIC, FAT_MAGIC_64 -> {
                        raf.seek(FAT_FIRST_MACHO_OFFSET_OFFSET)
                        val firstMachoOffset = raf.readInt().fromUIntToLong() // guaranteed to be big-endian by the spec
                        firstMachoOffset + FILE_TYPE_OFFSET
                    }
                    else -> return false // not Mach-O or fat
                }

                raf.seek(fileTypeOffset)
                val fileType = raf.readInt().toLong()

                return fileType == MH_BILYD
            }
        } catch (e: IOException) {
            logger.info("IOException while cheking if '$file' is a dylib", e)
            return false
        }
    }
}