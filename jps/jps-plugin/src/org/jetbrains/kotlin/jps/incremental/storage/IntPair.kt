/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.jps.incremental.storage

import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

data class IntPair(val first: Int, val second: Int) : Comparable<IntPair> {
    override fun compareTo(other: IntPair): Int {
        val firstCmp = first.compareTo(other.first)

        if (firstCmp != 0) return firstCmp

        return second.compareTo(other.second)
    }
}

fun HashPair(a: Any, b: Any): IntPair = IntPair(a.hashCode(), b.hashCode())

internal object INT_PAIR_KEY_DESCRIPTOR : KeyDescriptor<IntPair> {
    override fun read(`in`: DataInput): IntPair {
        val first = `in`.readInt()
        val second = `in`.readInt()
        return IntPair(first, second)
    }

    override fun save(out: DataOutput, value: IntPair?) {
        if (value == null) return

        out.writeInt(value.first)
        out.writeInt(value.second)
    }

    override fun getHashCode(value: IntPair?): Int = value?.hashCode() ?: 0

    override fun isEqual(val1: IntPair?, val2: IntPair?): Boolean = val1 == val2
}