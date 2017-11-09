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

internal interface EqualityComparator {
    /**
     * Subclasses must override to return a value indicating
     * whether or not two keys or values are equal.
     */
    abstract fun equals(value1: Any?, value2: Any?): Boolean

    /**
     * Subclasses must override to return the hash code of a given key.
     */
    abstract fun getHashCode(value: Any?): Int


    object HashCode : EqualityComparator {
        override fun equals(value1: Any?, value2: Any?): Boolean = value1 == value2

        override fun getHashCode(value: Any?): Int = value?.hashCode() ?: 0
    }
}