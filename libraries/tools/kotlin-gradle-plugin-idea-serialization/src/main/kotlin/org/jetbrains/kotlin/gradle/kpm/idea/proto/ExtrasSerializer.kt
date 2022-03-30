/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.proto

import com.google.protobuf.ByteString
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinExtras
import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.kpm.idea.SerializedIdeaKotlinExtras
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.IterableExtras

fun IterableExtras.serialize(): ByteArray {
    return ExtrasProto(this).toByteArray()
}

internal fun ExtrasProto(extras: IterableExtras): ExtrasProto {
    return extrasProto {
        val serializedEntries = extras.entries.mapNotNull { entry -> serialize(entry) }
        val ids = serializedEntries.map { (id, _) -> id.stableString }
        val values = serializedEntries.map { (_, data) -> data }.map { ByteString.copyFrom(it) } // TODO, not cool!

        this.ids.addAll(ids)
        this.values.addAll(values)
    }
}

fun IdeaKotlinExtras(data: ByteArray): IdeaKotlinExtras {
    return IdeaKotlinExtras(ExtrasProto.parseFrom(data))
}

internal fun IdeaKotlinExtras(proto: ExtrasProto): IdeaKotlinExtras {
    val serializedValues = proto.idsList.mapIndexed { index, id ->
        Extras.Id.fromString(id) to proto.valuesList[index].toByteArray()
    }.toMap().toMutableMap()

    return SerializedIdeaKotlinExtras(serializedValues)
}

private fun <T : Any> serialize(entry: Extras.Entry<T>): Pair<Extras.Id<T>, ByteArray>? {
    val serializer = entry.key.capability<IdeaKotlinExtrasSerializer<T>>() ?: return null
    return entry.key.id to serializer.serialize(entry.key, entry.value)
}

