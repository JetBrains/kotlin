/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.internal.IdeaSyncDetector
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

internal val Project.isInIdeaSync
    get() = variantImplementationFactory<IdeaSyncDetector.IdeaSyncDetectorVariantFactory>()
        .getInstance(this)
        .isInIdeaSync