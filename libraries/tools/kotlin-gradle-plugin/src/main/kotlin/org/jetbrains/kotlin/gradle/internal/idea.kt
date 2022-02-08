/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.utils.getSystemProperty

internal val Project.isInIdeaSync: Boolean
    get() {
        // "idea.sync.active" was introduced in 2019.1
        if (getSystemProperty("idea.sync.active")?.toBoolean() == true) return true

        // before 2019.1 there is "idea.active" that was true only on sync,
        // but since 2019.1 "idea.active" present in task execution too.
        // So let's check Idea version
        val majorIdeaVersion = getSystemProperty("idea.version")
            ?.split(".")
            ?.getOrNull(0)
        val isBeforeIdea2019 = majorIdeaVersion == null || majorIdeaVersion.toInt() < 2019

        return isBeforeIdea2019 && getSystemProperty("idea.active")?.toBoolean() == true
    }