/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.test

import kotlin.script.experimental.typeProviders.json.JSONTypeProvider


internal class JSONTypeProviderTests : TypeProviderTests(
    "libraries/scripting/type-providers-json/testData"
) {

    fun testBasic() {
        val output = runScriptOrFail("simple.kts", JSONTypeProvider).lines().filter { it.isNotBlank() }
        assertEquals(output.count(), 1)
        assertEquals(output[0], "greeting = Hello, World!")
    }

}