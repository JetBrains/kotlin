/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.xcode

import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.jetbrains.kotlin.konan.target.*

private data class XcodeSnapshot(
        override val additionalTools: String,
        override val appletvosSdk: String,
        override val appletvsimulatorSdk: String,
        override val iphoneosSdk: String,
        override val iphonesimulatorSdk: String,
        override val macosxSdk: String,
        override val simulatorRuntimes: String,
        override val toolchain: String,
        override val version: XcodeVersion,
        override val watchosSdk: String,
        override val watchsimulatorSdk: String,
) : Xcode {
    constructor(original: Xcode) : this(
            additionalTools = original.additionalTools,
            appletvosSdk = original.appletvosSdk,
            appletvsimulatorSdk = original.appletvsimulatorSdk,
            iphoneosSdk = original.iphoneosSdk,
            iphonesimulatorSdk = original.iphonesimulatorSdk,
            macosxSdk = original.macosxSdk,
            simulatorRuntimes = original.simulatorRuntimes,
            toolchain = original.toolchain,
            version = original.version,
            watchosSdk = original.watchosSdk,
            watchsimulatorSdk = original.watchsimulatorSdk,
    )
}

abstract class XcodeValueSource: ValueSource<Xcode, ValueSourceParameters.None> {
    override fun obtain(): Xcode = XcodeSnapshot(Xcode.defaultCurrent())
}
