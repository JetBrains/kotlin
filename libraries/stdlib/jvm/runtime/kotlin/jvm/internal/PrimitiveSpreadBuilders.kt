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

package kotlin.jvm.internal

public abstract class PrimitiveSpreadBuilder<T : Any>(private val size: Int) {
    abstract protected fun T.getSize(): Int

    protected var position: Int = 0

    @Suppress("UNCHECKED_CAST")
    private val spreads: Array<T?> = arrayOfNulls<Any>(size) as Array<T?>

    public fun addSpread(spreadArgument: T) {
        spreads[position++] = spreadArgument
    }

    protected fun size(): Int {
        var totalLength = 0
        for (i in 0..size - 1) {
            totalLength += spreads[i]?.getSize() ?: 1
        }
        return totalLength
    }

    protected fun toArray(values: T, result: T): T {
        var dstIndex = 0
        var copyValuesFrom = 0
        for (i in 0..size - 1) {
            val spreadArgument = spreads[i]
            if (spreadArgument != null) {
                if (copyValuesFrom < i) {
                    System.arraycopy(values, copyValuesFrom, result, dstIndex, i - copyValuesFrom)
                    dstIndex += i - copyValuesFrom
                }
                val spreadSize = spreadArgument.getSize()
                System.arraycopy(spreadArgument, 0, result, dstIndex, spreadSize)
                dstIndex += spreadSize
                copyValuesFrom = i + 1
            }
        }
        if (copyValuesFrom < size) {
            System.arraycopy(values, copyValuesFrom, result, dstIndex, size - copyValuesFrom)
        }

        return result
    }
}

public class ByteSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<ByteArray>(size) {
    private val values: ByteArray = ByteArray(size)
    override fun ByteArray.getSize(): Int = this.size

    public fun add(value: Byte) {
        values[position++] = value
    }

    public fun toArray(): ByteArray = toArray(values, ByteArray(size()))
}

public class CharSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<CharArray>(size) {
    private val values: CharArray = CharArray(size)
    override fun CharArray.getSize(): Int = this.size

    public fun add(value: Char) {
        values[position++] = value
    }

    public fun toArray(): CharArray = toArray(values, CharArray(size()))
}

public class DoubleSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<DoubleArray>(size) {
    private val values: DoubleArray = DoubleArray(size)
    override fun DoubleArray.getSize(): Int = this.size

    public fun add(value: Double) {
        values[position++] = value
    }

    public fun toArray(): DoubleArray = toArray(values, DoubleArray(size()))
}

public class FloatSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<FloatArray>(size) {
    private val values: FloatArray = FloatArray(size)
    override fun FloatArray.getSize(): Int = this.size

    public fun add(value: Float) {
        values[position++] = value
    }

    public fun toArray(): FloatArray = toArray(values, FloatArray(size()))
}

public class IntSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<IntArray>(size) {
    private val values: IntArray = IntArray(size)
    override fun IntArray.getSize(): Int = this.size

    public fun add(value: Int) {
        values[position++] = value
    }

    public fun toArray(): IntArray = toArray(values, IntArray(size()))
}

public class LongSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<LongArray>(size) {
    private val values: LongArray = LongArray(size)
    override fun LongArray.getSize(): Int = this.size

    public fun add(value: Long) {
        values[position++] = value
    }

    public fun toArray(): LongArray = toArray(values, LongArray(size()))
}

public class ShortSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<ShortArray>(size) {
    private val values: ShortArray = ShortArray(size)
    override fun ShortArray.getSize(): Int = this.size

    public fun add(value: Short) {
        values[position++] = value
    }

    public fun toArray(): ShortArray = toArray(values, ShortArray(size()))
}

public class BooleanSpreadBuilder(size: Int) : PrimitiveSpreadBuilder<BooleanArray>(size) {
    private val values: BooleanArray = BooleanArray(size)
    override fun BooleanArray.getSize(): Int = this.size

    public fun add(value: Boolean) {
        values[position++] = value
    }

    public fun toArray(): BooleanArray = toArray(values, BooleanArray(size()))
}
