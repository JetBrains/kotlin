/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader

import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.FqName

internal fun PackageFragmentReadingContext(packageFragmentProto: ProtoBuf.PackageFragment): PackageFragmentReadingContext? {
    val nameResolver = NameResolverImpl(packageFragmentProto.strings, packageFragmentProto.qualifiedNames)
    val packageFqName = packageFragmentProto.`package`.getExtensionOrNull(KlibMetadataProtoBuf.packageFqName)
        ?.let { packageFqNameStringIndex -> nameResolver.getPackageFqName(packageFqNameStringIndex) } ?: return null
    return PackageFragmentReadingContext(FqName(packageFqName), nameResolver)
}

internal class PackageFragmentReadingContext(
    val packageFqName: FqName,
    val nameResolver: NameResolverImpl,
)
