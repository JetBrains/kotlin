/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.CommonizerTarget
import org.jetbrains.kotlin.descriptors.commonizer.LeafTarget
import org.jetbrains.kotlin.descriptors.commonizer.SharedTarget
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirRoot
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirRootFactory
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.junit.Test

class RootCommonizerTest : AbstractCommonizerTest<CirRoot, CirRoot>() {

    @Test
    fun allAreNative() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("ios_x64", KonanTarget.IOS_X64),
                LeafTarget("ios_arm64", KonanTarget.IOS_ARM64),
                LeafTarget("ios_arm32", KonanTarget.IOS_ARM32)
            )
        ).toMock(),
        LeafTarget("ios_x64", KonanTarget.IOS_X64).toMock(),
        LeafTarget("ios_arm64", KonanTarget.IOS_ARM64).toMock(),
        LeafTarget("ios_arm32", KonanTarget.IOS_ARM32).toMock()
    )

    @Test
    fun jvmAndNative1() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("jvm1"),
                LeafTarget("ios_x64", KonanTarget.IOS_X64),
                LeafTarget("jvm2")
            )
        ).toMock(),
        LeafTarget("jvm1").toMock(),
        LeafTarget("ios_x64", KonanTarget.IOS_X64).toMock(),
        LeafTarget("jvm2").toMock()
    )

    @Test
    fun jvmAndNative2() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("ios_x64", KonanTarget.IOS_X64),
                LeafTarget("jvm"),
                LeafTarget("ios_arm64", KonanTarget.IOS_ARM64)
            )
        ).toMock(),
        LeafTarget("ios_x64", KonanTarget.IOS_X64).toMock(),
        LeafTarget("jvm").toMock(),
        LeafTarget("ios_arm64", KonanTarget.IOS_ARM64).toMock()
    )

    @Test
    fun noNative1() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("default1"),
                LeafTarget("default2"),
                LeafTarget("default3")
            )
        ).toMock(),
        LeafTarget("default1").toMock(),
        LeafTarget("default2").toMock(),
        LeafTarget("default3").toMock()
    )

    @Test
    fun noNative2() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("jvm1"),
                LeafTarget("default"),
                LeafTarget("jvm2")
            )
        ).toMock(),
        LeafTarget("jvm1").toMock(),
        LeafTarget("default").toMock(),
        LeafTarget("jvm2").toMock()
    )

    @Test
    fun noNative3() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("jvm1"),
                LeafTarget("jvm2"),
                LeafTarget("jvm3")
            )
        ).toMock(),
        LeafTarget("jvm1").toMock(),
        LeafTarget("jvm2").toMock(),
        LeafTarget("jvm3").toMock()
    )

    @Test(expected = ObjectsNotEqual::class)
    fun misconfiguration1() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("ios_x64", KonanTarget.IOS_X64),
                LeafTarget("ios_arm64", KonanTarget.IOS_ARM64),
                LeafTarget("ios_arm32", KonanTarget.IOS_ARM32)
            )
        ).toMock(),
        LeafTarget("ios_x64").toMock(), // KonanTarget is missing here!
        LeafTarget("ios_arm64", KonanTarget.IOS_ARM64).toMock(),
        LeafTarget("ios_arm32", KonanTarget.IOS_ARM32).toMock()
    )

    @Test(expected = ObjectsNotEqual::class)
    fun misconfiguration2() = doTestSuccess(
        expected = SharedTarget(
            setOf(
                LeafTarget("jvm1"),
                LeafTarget("jvm2"),
                LeafTarget("jvm3")
            )
        ).toMock(),
        LeafTarget("jvm1", KonanTarget.IOS_X64).toMock(), // mistakenly specified KonanTarget!
        LeafTarget("jvm2").toMock(),
        LeafTarget("jvm3").toMock()
    )

    override fun createCommonizer() = RootCommonizer()

    override fun isEqual(a: CirRoot?, b: CirRoot?) = (a === b) || (a != null && b != null && a.target == b.target)

    private companion object {
        fun CommonizerTarget.toMock() = CirRootFactory.create(target = this)
    }
}
