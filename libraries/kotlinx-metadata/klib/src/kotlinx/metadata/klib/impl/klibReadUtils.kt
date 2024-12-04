/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.klib.impl

import kotlin.metadata.internal.readAnnotation
import kotlinx.metadata.klib.KlibHeader
import kotlinx.metadata.klib.KlibSourceFile
import kotlinx.metadata.klib.UniqId
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver

internal fun KlibMetadataProtoBuf.DescriptorUniqId.readUniqId(): UniqId =
    UniqId(index)

internal fun KlibMetadataProtoBuf.Header.readHeader(strings: NameResolver): KlibHeader =
    KlibHeader(
        moduleName,
        fileList.map(KlibMetadataProtoBuf.File::readFile),
        packageFragmentNameList,
        emptyPackageList,
        annotationList.map { it.readAnnotation(strings) }
    )

internal fun KlibMetadataProtoBuf.File.readFile(): KlibSourceFile =
    KlibSourceFile(name)
