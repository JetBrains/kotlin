/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.internal.os.OperatingSystem
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestInputs() {
    tasks.withType<AbstractTestTask>().configureEach {
        inputs.property("os.name", OperatingSystem.current().name)
    }
}
