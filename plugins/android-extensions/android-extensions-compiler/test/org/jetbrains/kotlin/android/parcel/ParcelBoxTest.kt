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

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith

@RunWith(JUnit3RunnerWithInners::class)
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
    fun testKt19749() = doTest("kt19749")
    fun testKt19747() = doTest("kt19747")
    fun testKt19747_2() = doTest("kt19747_2")
    fun test20002() = doTest("kt20002")
    fun test20021() = doTest("kt20021")
    fun testCustomSerializerSimple() = doTest("customSerializerSimple")
    fun testCustomSerializerWriteWith() = doTest("customSerializerWriteWith")
    fun testCustomSerializerBoxing() = doTest("customSerializerBoxing")
    fun testKt20717() = doTest("kt20717")
    fun testEnumObject() = doTest("enumObject")
    fun testIntArray() = doTest("intArray")
    fun testOpenParcelize() = doTest("openParcelize")
    fun testKt25839() = doTest("kt25839")
}

class ParcelBoxTestWithSerializableLikeExtension : AbstractParcelBoxTest() {
    fun testSimple() = doTest("simple")

    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        super.setupEnvironment(environment)
        SyntheticResolveExtension.registerExtension(environment.project, SerializableLike())
    }

    private class SerializableLike : SyntheticResolveExtension {
        override fun getSyntheticCompanionObjectNameIfNeeded(thisDescriptor: ClassDescriptor): Name? {
            fun ClassDescriptor.isSerializableLike() = annotations.hasAnnotation(FqName("test.SerializableLike"))

            return when {
                thisDescriptor.kind == ClassKind.CLASS && thisDescriptor.isSerializableLike() -> Name.identifier("Companion")
                else -> return null
            }
        }
    }
}