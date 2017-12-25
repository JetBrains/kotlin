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

package core

import test.assertStaticAndRuntimeTypeIs
import test.assertStaticTypeIs
import kotlin.js.Promise
import kotlin.test.*

class PromiseTest {

    // To be sure that some base cases can be written in Kotlin
    fun smokeTest(p: Promise<Int>, ps: Promise<String>) {
        assertStaticTypeIs<Promise<Int>>(p.then { 1 })

        assertStaticTypeIs<Promise<Int>>(p.then({ 1 }))

        val f: (Int) -> Int = { 1 }
        val ft: (Throwable) -> Int = { 1 }

        assertStaticTypeIs<Promise<Int>>(p.then(f, ft))

        assertStaticTypeIs<Promise<Int>>(
                    p.then({
                        1
                    } , {
                        1
                    })
        )

        assertStaticTypeIs<Promise<Int>>(
                    p.then({
                        1
                    }) {
                        1
                    }
        )

        assertStaticTypeIs<Promise<Int>>(
                    p.then(onFulfilled = {
                        1
                    })
        )

        assertStaticTypeIs<Promise<Int>>(
                    p.then(onFulfilled = {
                        1
                    }) {
                        1
                    }
        )

        p.then {
            ps
        }.then {
            assertStaticAndRuntimeTypeIs<String>(it)
            ps
        }.then(
                {
                    assertStaticAndRuntimeTypeIs<String>(it)
                },
                {

                }
        )
    }
}