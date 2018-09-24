/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
                   }, {
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