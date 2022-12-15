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

val IdeaKotlinProjectArtifactDependency.artifactsClasspath by projectArtifactsClasspathKey.lazyProperty { IdeaKotlinClasspath() }