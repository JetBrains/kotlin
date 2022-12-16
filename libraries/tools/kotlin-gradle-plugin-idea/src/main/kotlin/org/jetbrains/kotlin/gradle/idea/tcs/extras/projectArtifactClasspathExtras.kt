/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs.extras

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.lazyProperty

val projectArtifactsClasspathKey = extrasKeyOf<IdeaKotlinClasspath>("artifactsClasspath")

/**
 * Classpath used inside IntelliJ/Kotlin to figure out the set of SourceSets that this dependency represents.
 * The files contained here are the actual dependencies for a compilation.
 * The dependency project's import will know about which SourceSets produced these artifacts which enables
 * IntelliJ/Kotlin to resolve the SourceSets it needs to depend on.
 *
 * Note: Plugins like Android might use custom/different approaches on how to resolve this [IdeaKotlinProjectArtifactDependency]
 */
val IdeaKotlinProjectArtifactDependency.artifactsClasspath by projectArtifactsClasspathKey.lazyProperty { IdeaKotlinClasspath() }