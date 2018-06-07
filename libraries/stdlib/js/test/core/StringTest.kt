/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package test.js

import kotlin.js.*
import kotlin.test.*

class StringTest {

    @Test
    fun getBytes() {
        var str = "Привет, мир! こんにちは！"

        var bytes = str.toByteArray()

        var res = byteArrayOf(-48, -97, -47, -128, -48, -72, -48, -78, -48, -75, -47, -126, 44, 32, -48, -68, -48, -72,
                -47, -128, 33, 32, -29, -127, -109, -29, -126, -109, -29, -127, -85, -29, -127, -95, -29, -127, -81, -17,
                -68, -127);

        assertEquals(res.size, bytes.size);
        var flag = true
        for (i in 0 until res.size) {
            if (res[i] !== bytes[i]) {
                flag = false;
                break;
            }
        }
        assertTrue(flag);

        var str2 = String(bytes)

        assertEquals(str, str2)
    }
}