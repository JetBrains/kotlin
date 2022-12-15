/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs.extras

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.lazyProperty

/* Sources (-sources.jar) */

val sourcesClasspathKey = extrasKeyOf<IdeaKotlinClasspath>("sourcesClasspath")

/**
 * Contains all resolved -sources.jar artifacts associated with this dependency
 */
val IdeaKotlinBinaryDependency.sourcesClasspath by sourcesClasspathKey.lazyProperty { IdeaKotlinClasspath() }

/* Documentation (-javadoc.jar) */

val documentationClasspathKey = extrasKeyOf<IdeaKotlinClasspath>("documentationClasspath")

/**
 * Contains all resolved -javadoc.jar artifacts associated with this dependency
 */
val IdeaKotlinBinaryDependency.documentationClasspath by documentationClasspathKey.lazyProperty { IdeaKotlinClasspath() }
