/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlin.metadata.internal.WriteContext
import kotlinx.metadata.klib.KlibHeader
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf

private fun wrapModuleName(moduleName: String): String =
    moduleName
        .let { if (it.startsWith("<")) it else "<$it" }
        .let { if (it.endsWith(">")) it else "$it>" }

internal fun KlibHeader.writeHeader(context: WriteContext): KlibMetadataProtoBuf.Header.Builder =
    KlibMetadataProtoBuf.Header.newBuilder().also { proto ->
        proto.moduleName = wrapModuleName(moduleName)
        proto.addAllPackageFragmentName(packageFragmentName)
        proto.addAllEmptyPackage(emptyPackage)
    }
