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

@file:Suppress("UNUSED_ANONYMOUS_PARAMETER")

package samples.properties

import kotlin.properties.Delegates
import samples.*
import kotlin.test.*

class Delegates {
    @Sample
    fun vetoableDelegate() {
        var max: Int by Delegates.vetoable(0) { property, oldValue, newValue ->
            newValue > oldValue
        }

        assertPrints(max, "0")

        max = 10
        assertPrints(max, "10")

        max = 5
        assertPrints(max, "10")
    }

    @Sample
    fun throwVetoableDelegate() {
        var max: Int by Delegates.vetoable(0) { property, oldValue, newValue ->
            if (newValue > oldValue) true else throw IllegalArgumentException("New value must be larger than old value.")
        }

        assertPrints(max, "0")

        max = 10
        assertPrints(max, "10")

        assertFailsWith<IllegalArgumentException> { max = 5 }
    }

    @Sample
    fun notNullDelegate() {
        var max: Int by Delegates.notNull()

        assertFailsWith<IllegalStateException> { println(max) }

        max = 10
        assertPrints(max, "10")
    }

    @Sample
    fun observableDelegate() {
        var observed = false
        var max: Int by Delegates.observable(0) { property, oldValue, newValue ->
            observed = true
        }

        assertPrints(max, "0")
        assertFalse(observed)

        max = 10
        assertPrints(max, "10")
        assertTrue(observed)
    }
}