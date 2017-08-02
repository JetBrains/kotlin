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

package org.jetbrains.kotlin.android.parcel

import org.junit.Test
import org.junit.runner.RunWith
//import org.robolectric.RobolectricTestRunner
//import org.robolectric.annotation.Config

// This class is not generated because it uses the custom test runner
//@RunWith(RobolectricTestRunner::class)
//@Config(manifest = Config.NONE)
class ParcelBoxTest : AbstractParcelBoxTest() {
    fun testSimple() = doTest("simple")
    fun testPrimitiveTypes() = doTest("primitiveTypes")
    fun testBoxedTypes() = doTest("boxedTypes")
    fun testNullableTypesSimple() = doTest("nullableTypesSimple")
    fun testNullableTypes() = doTest("nullableTypes")
    fun testListSimple() = doTest("listSimple")
    fun testLists() = doTest("lists")
    fun testListKinds() = doTest("listKinds")
    fun testArraySimple() = doTest("arraySimple")
    fun testArrays() = doTest("arrays")
    fun testMapSimple() = doTest("mapSimple")
    fun testMaps() = doTest("maps")
    fun testMapKinds() = doTest("mapKinds")
    fun testSparseBooleanArray() = doTest("sparseBooleanArray")
    fun testBundle() = doTest("bundle")
    fun testSparseArrays() = doTest("sparseArrays")
    fun testCustomSimple() = doTest("customSimple")
    fun testCharSequence() = doTest("charSequence")
    fun testEnums() = doTest("enums")
    fun testObjects() = doTest("objects")
    fun testNestedParcelable() = doTest("nestedParcelable")
}