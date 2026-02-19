/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.CommonizerTarget
import org.jetbrains.kotlin.commonizer.LeafCommonizerTarget
import org.jetbrains.kotlin.commonizer.SharedCommonizerTarget
import org.jetbrains.kotlin.commonizer.cir.CirRoot
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test

class RootCommonizerTest : AbstractCommonizerTest<CirRoot, CirRoot>() {

    @Test
    fun allAreNative() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget(KonanTarget.IOS_X64),
                LeafCommonizerTarget(KonanTarget.IOS_ARM64),
                LeafCommonizerTarget(KonanTarget.WATCHOS_ARM32)
            )
        ).toMock(),
        LeafCommonizerTarget(KonanTarget.IOS_X64).toMock(),
        LeafCommonizerTarget(KonanTarget.IOS_ARM64).toMock(),
        LeafCommonizerTarget(KonanTarget.WATCHOS_ARM32).toMock()
    )

    @Test
    fun jvmAndNative1() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget("jvm1"),
                LeafCommonizerTarget(KonanTarget.IOS_X64),
                LeafCommonizerTarget("jvm2")
            )
        ).toMock(),
        LeafCommonizerTarget("jvm1").toMock(),
        LeafCommonizerTarget(KonanTarget.IOS_X64).toMock(),
        LeafCommonizerTarget("jvm2").toMock()
    )

    @Test
    fun jvmAndNative2() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget(KonanTarget.IOS_X64),
                LeafCommonizerTarget("jvm"),
                LeafCommonizerTarget(KonanTarget.IOS_ARM64)
            )
        ).toMock(),
        LeafCommonizerTarget(KonanTarget.IOS_X64).toMock(),
        LeafCommonizerTarget("jvm").toMock(),
        LeafCommonizerTarget(KonanTarget.IOS_ARM64).toMock()
    )

    @Test
    fun noNative1() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget("default1"),
                LeafCommonizerTarget("default2"),
                LeafCommonizerTarget("default3")
            )
        ).toMock(),
        LeafCommonizerTarget("default1").toMock(),
        LeafCommonizerTarget("default2").toMock(),
        LeafCommonizerTarget("default3").toMock()
    )

    @Test
    fun noNative2() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget("jvm1"),
                LeafCommonizerTarget("default"),
                LeafCommonizerTarget("jvm2")
            )
        ).toMock(),
        LeafCommonizerTarget("jvm1").toMock(),
        LeafCommonizerTarget("default").toMock(),
        LeafCommonizerTarget("jvm2").toMock()
    )

    @Test
    fun noNative3() = doTestSuccess(
        expected = SharedCommonizerTarget(
            setOf(
                LeafCommonizerTarget("jvm1"),
                LeafCommonizerTarget("jvm2"),
                LeafCommonizerTarget("jvm3")
            )
        ).toMock(),
        LeafCommonizerTarget("jvm1").toMock(),
        LeafCommonizerTarget("jvm2").toMock(),
        LeafCommonizerTarget("jvm3").toMock()
    )

    override fun createCommonizer() = RootCommonizer()

    override fun areEqual(a: CirRoot?, b: CirRoot?) = (a === b) || (a != null && b != null && a.target == b.target)

    private companion object {
        fun CommonizerTarget.toMock() = CirRoot.create(target = this)
    }
}
