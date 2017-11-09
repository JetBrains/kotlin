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

package kotlin


public interface Comparator<T> {
    @JsName("compare") fun compare(a: T, b: T): Int
}

public inline fun <T> Comparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T> = object : Comparator<T> {
    override fun compare(a: T, b: T): Int = comparison(a, b)
}
