/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ir.CirModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test

class DefaultModuleCommonizerTest : AbstractCommonizerTest<CirModule, CirModule>() {

    @Test
    fun allAreNative() = doTestSuccess(
        KONAN_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock()
    )

    @Test
    fun jvmAndNative1() = doTestSuccess(
        DEFAULT_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock()
    )

    @Test
    fun jvmAndNative2() = doTestSuccess(
        DEFAULT_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock(),
        KONAN_BUILT_INS.toMock()
    )

    @Test
    fun noNative1() = doTestSuccess(
        DEFAULT_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock()
    )

    @Test
    fun noNative2() = doTestSuccess(
        DEFAULT_BUILT_INS.toMock(),
        DEFAULT_BUILT_INS.toMock(),
        DEFAULT_BUILT_INS.toMock(),
        DEFAULT_BUILT_INS.toMock()
    )

    @Test
    fun noNative3() = doTestSuccess(
        DEFAULT_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock(),
        DEFAULT_BUILT_INS.toMock(),
        JVM_BUILT_INS.toMock()
    )

    override fun createCommonizer() = ModuleCommonizer.default()

    override fun isEqual(a: CirModule?, b: CirModule?) =
        (a === b) || (a != null && b != null && a.name == b.name && a.builtIns::class.java.name == b.builtIns::class.java.name)

    private companion object {
        inline val KONAN_BUILT_INS get() = KonanBuiltIns(LockBasedStorageManager.NO_LOCKS)
        inline val JVM_BUILT_INS get() = JvmBuiltIns(LockBasedStorageManager.NO_LOCKS, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
        inline val DEFAULT_BUILT_INS get() = DefaultBuiltIns.Instance

        fun KotlinBuiltIns.toMock() = CirModule(Name.identifier("fakeModule"), this)
    }
}
