/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

fun parsePackageFragment(packageMetadata: ByteArray): KonanProtoBuf.LinkDataPackageFragment =
    KonanProtoBuf.LinkDataPackageFragment.parseFrom(packageMetadata, KonanSerializerProtocol.extensionRegistry)

fun parseModuleHeader(libraryMetadata: ByteArray): KonanProtoBuf.LinkDataLibrary =
    KonanProtoBuf.LinkDataLibrary.parseFrom(libraryMetadata, KonanSerializerProtocol.extensionRegistry)
