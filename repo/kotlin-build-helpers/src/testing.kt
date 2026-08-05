/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Task

/**
 * A workaround to treat the task same way as [org.gradle.api.tasks.testing.Test]
 * to provide compatibility with IDEA's test runner.
 */
fun Task.markAsIdeaTestTask() {
    if (project.providers.systemProperty("idea.active").isPresent) {
        extensions.extraProperties["idea.internal.test"] = true
    }
}
