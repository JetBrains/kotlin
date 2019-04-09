/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.r4a.analysis

import org.jetbrains.kotlin.r4a.AbstractR4aDiagnosticsTest

class UnionCheckerTests : AbstractR4aDiagnosticsTest() {

    fun testUnionTypeReporting001() {
        doTest(
            """
            import com.google.r4a.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar() {
                <foo value=1 />
                <foo value="1" />
                <foo value=<!ILLEGAL_ASSIGN_TO_UNIONTYPE!>1f<!> />
            }
        """)
    }

    fun testUnionTypeReporting002() {
        doTest(
            """
            import com.google.r4a.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class) Any) {
                <foo value />
            }
        """)
    }

    fun testUnionTypeReporting003() {
        doTest(
            """
            import com.google.r4a.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class, Float::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class) Any) {
                <foo value />
            }
        """)
    }

    fun testUnionTypeReporting004() {
        doTest(
            """
            import com.google.r4a.*;

            @Composable fun foo(value: @UnionType(Int::class, String::class) Any) {
                System.out.println(value)
            }

            @Composable
            fun bar(value: @UnionType(Int::class, String::class, Float::class) Any) {
                <foo <!ILLEGAL_ASSIGN_TO_UNIONTYPE!>value<!> />
            }
        """)
    }
}
