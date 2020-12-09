/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JvmBuiltIns
import org.jetbrains.kotlin.builtins.konan.KonanBuiltIns
import org.jetbrains.kotlin.commonizer.api.CommonizerTarget
import org.jetbrains.kotlin.commonizer.api.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.api.SharedCommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirRootFactory
import org.jetbrains.kotlin.descriptors.commonizer.utils.MockBuiltInsProvider
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.junit.Test

class RootCommonizerTest : AbstractCommonizerTest<CirRoot, CirRoot>() {

    @Test
    fun allAreNative() = doTestSuccess(
        expected = KONAN_BUILT_INS.toMock(
            CommonizerTarget(
                KonanTarget.IOS_X64,
                KonanTarget.IOS_ARM64,
                KonanTarget.IOS_ARM32
            )
        ),
        KONAN_BUILT_INS.toMock(CommonizerTarget(KonanTarget.IOS_X64)),
        KONAN_BUILT_INS.toMock(CommonizerTarget(KonanTarget.IOS_ARM64)),
        KONAN_BUILT_INS.toMock(CommonizerTarget(KonanTarget.IOS_ARM32))
    )

    @Test
    fun jvmAndNative1() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget("jvm1"),
                    LeafCommonizerTarget(KonanTarget.IOS_X64),
                    LeafCommonizerTarget("jvm2")
                )
            )
        ),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm1")),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget(KonanTarget.IOS_X64)),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm2"))
    )

    @Test
    fun jvmAndNative2() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget(KonanTarget.IOS_X64),
                    LeafCommonizerTarget("jvm"),
                    LeafCommonizerTarget(KonanTarget.IOS_ARM64)
                )
            )
        ),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget(KonanTarget.IOS_X64)),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm")),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget(KonanTarget.IOS_ARM64))
    )

    @Test
    fun noNative1() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget("default1"),
                    LeafCommonizerTarget("default2"),
                    LeafCommonizerTarget("default3")
                )
            )
        ),
        DEFAULT_BUILT_INS.toMock(LeafCommonizerTarget("default1")),
        DEFAULT_BUILT_INS.toMock(LeafCommonizerTarget("default2")),
        DEFAULT_BUILT_INS.toMock(LeafCommonizerTarget("default3"))
    )

    @Test
    fun noNative2() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget("jvm1"),
                    LeafCommonizerTarget("default"),
                    LeafCommonizerTarget("jvm2")
                )
            )
        ),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm1")),
        DEFAULT_BUILT_INS.toMock(LeafCommonizerTarget("default")),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm2"))
    )

    @Test
    fun noNative3() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget("jvm1"),
                    LeafCommonizerTarget("jvm2"),
                    LeafCommonizerTarget("jvm3")
                )
            )
        ),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm1")),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm2")),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm3"))
    )

    @Test(expected = IllegalStateException::class)
    fun misconfiguration1() = doTestSuccess(
        expected = KONAN_BUILT_INS.toMock(
            SharedCommonizerTarget(
                setOf(
                    LeafCommonizerTarget(KonanTarget.IOS_X64),
                    LeafCommonizerTarget(KonanTarget.IOS_ARM64),
                    LeafCommonizerTarget(KonanTarget.IOS_ARM32)
                )
            )
        ),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget("NO_KONAN_TARGET")),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget(KonanTarget.IOS_ARM64)),
        KONAN_BUILT_INS.toMock(LeafCommonizerTarget(KonanTarget.IOS_ARM32))
    )

    @Test(expected = IllegalStateException::class)
    fun misconfiguration2() = doTestSuccess(
        expected = DEFAULT_BUILT_INS.toMock(
            SharedCommonizerTarget(
                LeafCommonizerTarget("jvm1"),
                LeafCommonizerTarget("jvm2"),
                LeafCommonizerTarget("jvm3")
            )
        ),
        JVM_BUILT_INS.toMock(CommonizerTarget(KonanTarget.IOS_X64)),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm2")),
        JVM_BUILT_INS.toMock(LeafCommonizerTarget("jvm3"))
    )

    override fun createCommonizer() = RootCommonizer()

    override fun isEqual(a: CirRoot?, b: CirRoot?) =
        (a === b)
                || (a != null && b != null
                && a.target == b.target
                && a.builtInsClass == b.builtInsClass
                && a.builtInsClass == a.builtInsProvider.loadBuiltIns()::class.java.name
                && a.builtInsProvider.loadBuiltIns()::class.java == b.builtInsProvider.loadBuiltIns()::class.java)

    private companion object {
        inline val KONAN_BUILT_INS get() = KonanBuiltIns(LockBasedStorageManager.NO_LOCKS)
        inline val JVM_BUILT_INS get() = JvmBuiltIns(LockBasedStorageManager.NO_LOCKS, JvmBuiltIns.Kind.FROM_CLASS_LOADER)
        inline val DEFAULT_BUILT_INS get() = DefaultBuiltIns.Instance

        fun KotlinBuiltIns.toMock(target: CommonizerTarget) = CirRootFactory.create(
            target = target,
            builtInsClass = this::class.java.name,
            builtInsProvider = MockBuiltInsProvider(this)
        )
    }
}

