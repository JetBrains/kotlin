/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

/**
 * General annotation to mark delicate Kotlin Gradle Plugin API.
 * API marked with this annotation most likely is not suitable for everyday use.
 * Yet it is useful and covers some corner cases that may be required by a small number of users.
 *
 * Delicate doesn't mean unstable.
 * Stable delicate API will have to go through the same deprecation cycle as any other stable API.
 * Though delicate API can also be experimental, and can be changed without notice.
 *
 * If you're unsure about using this API, feel free to ask in [Kotlin Community](https://kotl.in/build-tools-slack) or [Kotlin issue tracker](https://kotl.in/issue).
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This is delicate Kotlin Gradle Plugin API." +
            " Make sure you fully understand the use case, applicability area and read documentation about this API."
)
annotation class DelicateKotlinGradlePluginApi(val kind: String)

object DelicateKotlinGradlePluginApiKind {
    /**
     * API allows replacing the default behavior or component of KGP.
     * And can break integration with other components. Leading to incorrect or failed builds.
     */
    const val REPLACES_DEFAULTS = "DELICATE_KOTLIN_GRADLE_PLUGIN_API_REPLACES_DEFAULTS"

    /**
     * API is not intuitive and requires a deep understanding of its functionality.
     * User of the API should read related documentation.
     */
    const val REQUIRES_KNOWLEDGE = "DELICATE_KOTLIN_GRADLE_PLUGIN_API_REQUIRES_KNOWLEDGE"
}
