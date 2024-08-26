/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.utils.property
import javax.inject.Inject

@Suppress("unused")
@CacheableTask
internal abstract class AbstractXcodeTask @Inject constructor(
    providerFactory: ProviderFactory,
    objectFactory: ObjectFactory,
) : DefaultTask() {
    @get:Optional
    @get:Input
    val targetDeviceIdentifier: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("TARGET_DEVICE_IDENTIFIER")
    )

    @get:Optional
    @get:Input
    val sdkName: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("SDK_NAME")
    )

    @get:Optional
    @get:Input
    val archs: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("ARCHS")
    )

    @get:Optional
    @get:Input
    val defaultCompiler: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("DEFAULT_COMPILER")
    )

    @get:Optional
    @get:Input
    val swiftVersion: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("SWIFT_VERSION")
    )

    @get:Optional
    @get:Input
    val xcodeProductBuildVersion: Property<String> = objectFactory.property<String>().convention(
        providerFactory.environmentVariable("XCODE_PRODUCT_BUILD_VERSION")
    )
}