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

class Queue<T>(val maxSize: Int, val none: T) {
    val array = Array<T>(maxSize, { _ -> none})
    var head = 0
    var tail = 0

    fun push(element: T) {
        if ((tail + 1) % maxSize == head)
            throw Error("queue overflow: $tail $head")
        array[tail] = element
        tail = (tail + 1) % maxSize
    }

    fun pop() : T {
        if (tail == head)
            throw Error("queue underflow")
        val result = array[head]
        array[head] = none
        head = (head + 1) % maxSize
        return result
    }

    fun peek() : T {
        if (tail == head)
            throw Error("queue underflow")
        return array[head]
    }

    fun size() = if (tail >= head) tail - head else maxSize - (head - tail)

    fun isEmpty() = head == tail
}
