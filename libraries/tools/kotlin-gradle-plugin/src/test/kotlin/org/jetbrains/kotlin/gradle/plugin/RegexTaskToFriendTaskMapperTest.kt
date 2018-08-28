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

package org.jetbrains.kotlin.gradle.plugin

import org.junit.Assert
import org.junit.Test

class RegexTaskToFriendTaskMapperTest {
    @Test
    fun getFriendTaskNameDefault() {
        val mapper = RegexTaskToFriendTaskMapper.Default("")
        Assert.assertEquals("compileKotlin", mapper["compileTestKotlin"])
        Assert.assertEquals(null, mapper["compileKotlin"])
    }

    @Test
    fun getFriendTaskNameCustomTargetName() {
        val mapper = RegexTaskToFriendTaskMapper.Default("Foo")
        Assert.assertEquals("compileKotlinFoo", mapper["compileTestKotlinFoo"])
        Assert.assertEquals(null, mapper["compileKotlinFoo"])
        Assert.assertEquals(null, mapper["compileTestKotlinBar"])
    }

    @Test
    fun getFriendTaskNameAndroid() {
        val mapper = RegexTaskToFriendTaskMapper.Android("")
        // Unit test examples
        Assert.assertEquals("compileDebugKotlin", mapper["compileDebugUnitTestKotlin"])
        Assert.assertEquals("compileReleaseKotlin", mapper["compileReleaseUnitTestKotlin"])
        Assert.assertEquals("compileProdDebugKotlin", mapper["compileProdDebugUnitTestKotlin"])
        // Android test examples
        Assert.assertEquals("compileDebugKotlin", mapper["compileDebugAndroidTestKotlin"])
        Assert.assertEquals("compileReleaseKotlin", mapper["compileReleaseAndroidTestKotlin"])
        Assert.assertEquals("compileProdDebugKotlin", mapper["compileProdDebugAndroidTestKotlin"])

        Assert.assertEquals(null, mapper["compileDebugKotlin"])
    }

    @Test
    fun getFriendTaskNameAndroidCustomTargetName() {
        val mapper = RegexTaskToFriendTaskMapper.Android("Foo")
        // Unit test examples
        Assert.assertEquals("compileDebugKotlinFoo", mapper["compileDebugUnitTestKotlinFoo"])
        Assert.assertEquals("compileReleaseKotlinFoo", mapper["compileReleaseUnitTestKotlinFoo"])
        Assert.assertEquals("compileProdDebugKotlinFoo", mapper["compileProdDebugUnitTestKotlinFoo"])
        // Android test examples
        Assert.assertEquals("compileDebugKotlinFoo", mapper["compileDebugAndroidTestKotlinFoo"])
        Assert.assertEquals("compileReleaseKotlinFoo", mapper["compileReleaseAndroidTestKotlinFoo"])
        Assert.assertEquals("compileProdDebugKotlinFoo", mapper["compileProdDebugAndroidTestKotlinFoo"])

        Assert.assertEquals(null, mapper["compileDebugKotlinFoo"])
        Assert.assertEquals(null, mapper["compileDebugUnitTestKotlinBar"])
        Assert.assertEquals(null, mapper["compileDebugAndroidTestKotlinBar"])
    }
}