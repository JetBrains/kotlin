/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.abi.tools.v2

import org.jetbrains.kotlin.abi.tools.api.AbiFilters
import org.jetbrains.kotlin.abi.tools.api.v2.AbiToolsV2
import org.jetbrains.kotlin.abi.tools.api.v2.KlibDump
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.abi.tools.filtering.compileMatcher
import org.jetbrains.kotlin.abi.tools.v2.klib.KlibDumpImpl
import java.io.File

internal object ToolsV2 : AbiToolsV2 {
    override fun <T : Appendable> printJvmDump(
        appendable: T,
        classfiles: Iterable<File>,
        filters: AbiFilters,
    ) {
        val filtersMatcher = compileMatcher(filters)

        val signatures = classfiles.asSequence()
            .map { classfile -> classfile.inputStream() }
            .loadApiFromJvmClasses()
            .filterByMatcher(filtersMatcher)

        signatures.dump(appendable)
    }

    override fun createKlibDump(): KlibDump {
        return KlibDumpImpl()
    }

    override fun loadKlibDump(dumpFile: File): KlibDump {
        return KlibDumpImpl.from(dumpFile)
    }

    override fun loadKlibDump(dump: CharSequence): KlibDump {
        return KlibDumpImpl.from(dump)
    }

    override fun extractKlibAbi(
        klib: File,
        target: KlibTarget,
        filters: AbiFilters,
    ): KlibDump {
        val dump = KlibDumpImpl.fromKlib(klib, filters)
        dump.renameSingleTarget(target)
        return dump
    }
}