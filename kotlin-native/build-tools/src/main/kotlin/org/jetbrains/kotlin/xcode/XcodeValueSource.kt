/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.konan.target.*

private class XcodeSnapshot(original: Xcode) : Xcode {
    override val additionalTools = original.additionalTools
    override val appletvosSdk = original.appletvosSdk
    override val appletvsimulatorSdk = original.appletvsimulatorSdk
    override val iphoneosSdk = original.iphoneosSdk
    override val iphonesimulatorSdk = original.iphonesimulatorSdk
    override val macosxSdk = original.macosxSdk
    override val simulatorRuntimes = original.simulatorRuntimes
    override val toolchain = original.toolchain
    override val version = original.version
    override val watchosSdk = original.watchosSdk
    override val watchsimulatorSdk = original.watchsimulatorSdk
}

abstract class XcodeValueSource: ValueSource<Xcode, ValueSourceParameters.None> {
    override fun obtain(): Xcode = XcodeSnapshot(Xcode.defaultCurrent())
}
