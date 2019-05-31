/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.Test

import org.junit.Assert.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SemVerFixTest(val expected: String, val actual: String) {
    companion object {
        @Parameterized.Parameters(name = "fix({1}) == {0}")
        @JvmStatic
        fun data() = listOf(
            arrayOf("1.3.0-SNAPSHOT", "1.3-SNAPSHOT"),
            arrayOf("1.2.3-RC1-1234", "1.2.3-RC1-1234"),
            arrayOf("0.0.0-abc-qx", "abc-qx"),
            arrayOf("1.0.0-o.p-qx", "1.o.p-qx"),
            arrayOf("1.2.3-o.p-qa", "1.o2.3p-qa"),
            arrayOf("1.0.0", "1.0.0+"),
            arrayOf("1.2.3-beta.11+sha.0nsfgkjkjsdf", "1.2.3-beta.11+sha.0nsfgkjkjsdf"),
            arrayOf("1.2.0-beta.11+sha.0nsfgkjkjsdf", "1.2-beta.11+sha.0nsfgkjkjsdf"),
            arrayOf("1.0.0-beta.11+sha.0nsfgkjkjsdf", "1-beta.11+sha.0nsfgkjkjsdf"),
            arrayOf("1.2.3-beta.1-1.ab-c+sha.0nsfgkjkjs-df", "1.2.3-beta.1-1.ab-c+sha.0nsfgkjkjs-df"),
            arrayOf("1.0.0-alpha.1", "1.0.0-alpha.1"),
            arrayOf("1.0.0-alpha.beta", "1.0.0-alpha.beta"),
            arrayOf("1.0.0-alpha.12.ab-c", "1.0.0-alpha.12.ab-c"),
            arrayOf("1.0.0-alpha.12.x-yz", "1.0.0-alpha.12.x-yz"),
            arrayOf("1.0.0", "1"),
            arrayOf("1.2.0", "1.2"),
            arrayOf("1.2.3", "1.2.3"),
            arrayOf("1.2.3", "01.2.3"),
            arrayOf("0.2.3", "0.2.3"),
            arrayOf("0.0.3", "0.0.3"),
            arrayOf("0.0.0", "0.0.0"),
            arrayOf("1.2.3", "1.2.3-"),
            arrayOf("1.2.3-x", "1.2.3-x"),
            arrayOf("1.2.3-xy", "1.2.3-xy"),
            arrayOf("1.2.3-xy-z", "1.2.3-xy-z"),
            arrayOf("1.2.3-xy-z", "1.2.3-xy-z+"),
            arrayOf("1.2.3-xy-z+1", "1.2.3-xy-z+1"),
            arrayOf("1.2.3-x.xy-z+1", "1x.2.3-xy-z+1"),
            arrayOf("1.2.3-x.y.xy-z+1", "1x.2y.3-xy-z+1"),
            arrayOf("1.2.3-x.y.z-xy-z+1", "1x.2y.3z-xy-z+1"),
            arrayOf("1.2.3-x.y.z-xy-z+1u", "1x.2y.3z-xy-z+1u"),
            arrayOf("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z-x.y-z+1u"),
            arrayOf("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z!-x.y-z+1u"),
            arrayOf("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z-x.y!-z+1u"),
            arrayOf("1.2.3-x.y.z-x.y-z+1u", "!1x.2y.3z-x.y!-z+1u"),
            arrayOf("1.2.3-x.y.z-x.y-z+1u", "1x.!2y.3z-x.y!-z+1u")
        )
    }


    @Test
    fun testFixSemver() {
        assertEquals(expected, fixSemver(actual))
    }
}