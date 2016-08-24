/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package kotlin.collections

abstract class AbstractSet<E> protected constructor() : AbstractCollection<E>(), MutableSet<E> {

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Set<*>) return false
        if (other.size != size) return false
        return containsAll(other)
    }

    override fun hashCode(): Int {
        var hashCode = 0
        for (element in this) {
            hashCode = (hashCode + (element?.hashCode() ?: 0)) or 0
        }
        return hashCode
    }

}