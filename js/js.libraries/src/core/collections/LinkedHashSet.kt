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
 * Based on GWT LinkedHashSet
 * Copyright 2008 Google Inc.
 */

package kotlin.collections

open class LinkedHashSet<E> : HashSet<E> {

    constructor() : super(LinkedHashMap<E, Any>())

    constructor(c: Collection<E>) : super(LinkedHashMap<E, Any>()) {
        addAll(c)
    }

    constructor(capacity: Int, loadFactor: Float = 0.0f) : super(LinkedHashMap<E, Any>(capacity, loadFactor))

//    public override fun clone(): Any {
//        return LinkedHashSet(this)
//    }

}
