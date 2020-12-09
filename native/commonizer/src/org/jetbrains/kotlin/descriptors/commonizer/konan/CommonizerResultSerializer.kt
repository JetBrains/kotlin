/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.konan

import org.jetbrains.kotlin.descriptors.commonizer.Result


internal interface CommonizerResultSerializer {
    operator fun invoke(
        originalLibraries: AllNativeLibraries,
        commonizerResult: Result
    )
}

internal operator fun CommonizerResultSerializer.plus(other: CommonizerResultSerializer?): CommonizerResultSerializer {
    if (other == null) return this
    if (this is CompositeCommonizerResultSerializer) return CompositeCommonizerResultSerializer(serializers + other)
    return CompositeCommonizerResultSerializer(listOf(this, other))
}

private class CompositeCommonizerResultSerializer(
    val serializers: List<CommonizerResultSerializer>
) : CommonizerResultSerializer {
    override fun invoke(originalLibraries: AllNativeLibraries, commonizerResult: Result) {
        serializers.forEach { serializer -> serializer(originalLibraries, commonizerResult) }
    }
}
