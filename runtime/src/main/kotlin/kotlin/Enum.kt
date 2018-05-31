/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package kotlin

/**
 * The common base class of all enum classes.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/enum-classes.html) for more
 * information on enum classes.
 */
public abstract class Enum<E: Enum<E>>(public val name: String, public val ordinal: Int): Comparable<E> {
    public companion object {
    }

    public override final fun compareTo(other: E): Int { return ordinal - other.ordinal }

    /**
     * Throws an exception since enum constants cannot be cloned.
     * This method prevents enum classes from inheriting from [Cloneable].
     */
    protected final fun clone(): Any {
        throw UnsupportedOperationException()
    }

    public override final fun equals(other: Any?): Boolean {
        return this === other
    }

    public override final fun hashCode(): Int {
        return ordinal
    }

    public override fun toString(): String {
        return name
    }
}

@Suppress("UNUSED_PARAMETER")
fun <T: Enum<T>> enumValueOf(name: String): T {
    throw Exception("Call to this function should've been lowered")
}

fun <T: Enum<T>> enumValues(): Array<T> {
    throw Exception("Call to this function should've been lowered")
}
