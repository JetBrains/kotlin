/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.kapt.classloaders


internal fun ClassLoader.rootOrSelf(): ClassLoader {
    tailrec fun parentOrSelf(classLoader: ClassLoader): ClassLoader {
        val parent = classLoader.parent ?: return classLoader
        return parentOrSelf(parent)
    }

    return parentOrSelf(this)
}
