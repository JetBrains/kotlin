/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package gradle

import KotlinBuildProperties

// Enabling publishing docs jars only on CI build by default
// Currently dokka task runs non-incrementally and takes big amount of time
val KotlinBuildProperties.publishGradlePluginsJavadoc: Boolean
    get() = booleanProperty("kotlin.build.gradle.publish.javadocs", isTeamcityBuild).get()
