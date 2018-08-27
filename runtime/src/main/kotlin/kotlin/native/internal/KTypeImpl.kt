/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.reflect.KClassifier
import kotlin.reflect.KType

internal class KTypeImpl(override val classifier: KClassifier?, override val isMarkedNullable: Boolean) : KType {
    override fun equals(other: Any?) =
            other is KTypeImpl && classifier == other.classifier && isMarkedNullable == other.isMarkedNullable

    override fun hashCode(): Int {
        return (classifier?.hashCode() ?: 0) * 31 + if (isMarkedNullable) 1 else 0
    }
}

internal class KTypeImplForGenerics : KType {
    override val classifier: KClassifier?
        get() = error("Generic types are not yet supported in reflection")

    override val isMarkedNullable: Boolean
        get() = error("Generic types are not yet supported in reflection")

    override fun equals(other: Any?) =
            error("Generic types are not yet supported in reflection")

    override fun hashCode(): Int =
            error("Generic types are not yet supported in reflection")
}