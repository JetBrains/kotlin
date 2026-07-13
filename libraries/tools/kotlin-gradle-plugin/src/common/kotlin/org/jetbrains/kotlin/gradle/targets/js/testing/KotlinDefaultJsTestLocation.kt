/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import java.net.URI

internal class KotlinDefaultJsTestLocation(
    @get:InputDirectory
    override val bundleLocation: Provider<Directory>,
    @get:Input
    override val testHtmlFileName: Provider<String>,
    @get:Internal
    override val url: Provider<URI>
) : KotlinJsTestsLocation
