/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.konan.util.KlibMetadataFactories
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.StorageManager

fun createKonanBuiltIns(storageManager: StorageManager) = KonanBuiltIns(storageManager)
/**
 * The default Kotlin/Native factories.
 */
object KonanFactories : KlibMetadataFactories(::createKonanBuiltIns, NullFlexibleTypeDeserializer)