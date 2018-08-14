/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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