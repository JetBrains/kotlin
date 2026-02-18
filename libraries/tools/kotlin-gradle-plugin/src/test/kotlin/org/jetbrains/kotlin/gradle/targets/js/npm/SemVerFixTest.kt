/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import kotlin.test.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class SemVerFixTest {
    companion object {
        @JvmStatic
        fun data(): Stream<Arguments> = Stream.of(
            Arguments.of("1.3.0-SNAPSHOT", "1.3-SNAPSHOT"),
            Arguments.of("1.2.3-RC1-1234", "1.2.3-RC1-1234"),
            Arguments.of("0.0.0-abc-qx", "abc-qx"),
            Arguments.of("1.0.0-o.p-qx", "1.o.p-qx"),
            Arguments.of("1.2.3-o.p-qa", "1.o2.3p-qa"),
            Arguments.of("1.0.0", "1.0.0+"),
            Arguments.of("1.2.3-beta.11+sha.0nsfgkjkjsdf", "1.2.3-beta.11+sha.0nsfgkjkjsdf"),
            Arguments.of("1.2.0-beta.11+sha.0nsfgkjkjsdf", "1.2-beta.11+sha.0nsfgkjkjsdf"),
            Arguments.of("1.0.0-beta.11+sha.0nsfgkjkjsdf", "1-beta.11+sha.0nsfgkjkjsdf"),
            Arguments.of("1.2.3-beta.1-1.ab-c+sha.0nsfgkjkjs-df", "1.2.3-beta.1-1.ab-c+sha.0nsfgkjkjs-df"),
            Arguments.of("1.0.0-alpha.1", "1.0.0-alpha.1"),
            Arguments.of("1.0.0-alpha.beta", "1.0.0-alpha.beta"),
            Arguments.of("1.0.0-alpha.12.ab-c", "1.0.0-alpha.12.ab-c"),
            Arguments.of("1.0.0-alpha.12.x-yz", "1.0.0-alpha.12.x-yz"),
            Arguments.of("1.0.0", "1"),
            Arguments.of("1.2.0", "1.2"),
            Arguments.of("1.2.3", "1.2.3"),
            Arguments.of("1.2.3", "01.2.3"),
            Arguments.of("0.2.3", "0.2.3"),
            Arguments.of("0.0.3", "0.0.3"),
            Arguments.of("0.0.0", "0.0.0"),
            Arguments.of("1.2.3", "1.2.3-"),
            Arguments.of("1.2.3-x", "1.2.3-x"),
            Arguments.of("1.2.3-xy", "1.2.3-xy"),
            Arguments.of("1.2.3-xy-z", "1.2.3-xy-z"),
            Arguments.of("1.2.3-xy-z", "1.2.3-xy-z+"),
            Arguments.of("1.2.3-xy-z+1", "1.2.3-xy-z+1"),
            Arguments.of("1.2.3-x.xy-z+1", "1x.2.3-xy-z+1"),
            Arguments.of("1.2.3-x.y.xy-z+1", "1x.2y.3-xy-z+1"),
            Arguments.of("1.2.3-x.y.z-xy-z+1", "1x.2y.3z-xy-z+1"),
            Arguments.of("1.2.3-x.y.z-xy-z+1u", "1x.2y.3z-xy-z+1u"),
            Arguments.of("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z-x.y-z+1u"),
            Arguments.of("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z!-x.y-z+1u"),
            Arguments.of("1.2.3-x.y.z-x.y-z+1u", "1x.2y.3z-x.y!-z+1u"),
            Arguments.of("1.2.3-x.y.z-x.y-z+1u", "!1x.2y.3z-x.y!-z+1u"),
            Arguments.of("1.2.3-x.y.z-x.y-z+1u", "1x.!2y.3z-x.y!-z+1u"),
        )
    }

    @ParameterizedTest(name = "fix({1}) == {0}")
    @MethodSource("data")
    fun testFixSemver(expected: String, actual: String) {
        assertEquals(expected, fixSemver(actual))
    }
}
