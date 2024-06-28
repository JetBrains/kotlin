/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinBinaryAttributesProto
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.ideaKotlinBinaryAttributesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryAttributes

internal fun IdeaKotlinBinaryAttributesProto(
    attributes: IdeaKotlinBinaryAttributes,
): IdeaKotlinBinaryAttributesProto {
    return ideaKotlinBinaryAttributesProto {
        this.attributes.putAll(attributes)
    }
}

internal fun IdeaKotlinBinaryAttributes(proto: IdeaKotlinBinaryAttributesProto): IdeaKotlinBinaryAttributes {
    return IdeaKotlinBinaryAttributes(proto.attributesMap)
}
