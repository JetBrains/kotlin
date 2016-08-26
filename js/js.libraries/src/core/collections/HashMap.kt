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
/*
 * Based on GWT HashMap
 * Copyright 2008 Google Inc.
 */

package kotlin.collections


open class HashMap<K, V> : AbstractHashMap<K, V> {

    constructor() : super()
    constructor(capacity: Int, loadFactor: Float = 0f) : super(capacity, loadFactor)
    constructor(original: Map<out K, V>) : super(original)

//    public override fun clone(): Any {
//        return HashMap<K, V>(this)
//    }

    override fun equals(value1: Any?, value2: Any?): Boolean = value1 == value2

    override fun getHashCode(key: K): Int = key?.hashCode() ?: 0
}
