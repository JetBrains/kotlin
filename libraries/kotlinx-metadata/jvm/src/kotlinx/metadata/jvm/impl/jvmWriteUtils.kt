/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlinx.metadata.jvm.impl

import kotlinx.metadata.impl.WriteContext
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable
import org.jetbrains.kotlin.protobuf.MessageLite

internal fun writeProtoBufData(message: MessageLite, c: WriteContext): Pair<Array<String>, Array<String>> {
    val strings = c.strings as JvmStringTable
    return Pair(
        JvmProtoBufUtil.writeData(message, strings),
        strings.strings.toTypedArray()
    )
}
