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

import android.os.Bundle
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// This class is not generated because it uses the custom test runner
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ParcelBoxTest : AbstractParcelBoxTest() {
    @Test fun simple() = doTest("simple")
    @Test fun primitiveTypes() = doTest("primitiveTypes")
    @Test fun boxedTypes() = doTest("boxedTypes")
    @Test fun nullableTypesSimple() = doTest("nullableTypesSimple")
    @Test fun nullableTypes() = doTest("nullableTypes")
    @Test fun listSimple() = doTest("listSimple")
    @Test fun lists() = doTest("lists")
    @Test fun listKinds() = doTest("listKinds")
    @Test fun arraySimple() = doTest("arraySimple")
    @Test fun arrays() = doTest("arrays")
    @Test fun mapSimple() = doTest("mapSimple")
    @Test fun maps() = doTest("maps")
    @Test fun mapKinds() = doTest("mapKinds")
    @Test fun sparseBooleanArray() = doTest("sparseBooleanArray")
    @Test fun bundle() = doTest("bundle")
    @Test fun sparseArrays() = doTest("sparseArrays")
}