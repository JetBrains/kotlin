/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Action
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

abstract class JfrExtension {
    abstract val jfcFile: RegularFileProperty
    abstract val jfrFile: ConfigurableFileCollection
}

fun Test.javaFlightRecorder(action: Action<JfrExtension>) {
    configure<JfrExtension> {
        action.execute(this)
    }
}

val Test.javaFlightRecorder: JfrExtension
    get() = extensions.getByType()
