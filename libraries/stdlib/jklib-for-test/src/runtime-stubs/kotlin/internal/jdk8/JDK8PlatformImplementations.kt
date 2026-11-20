/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal.jdk8

// Stands in for the JDK 8 implementation from `libraries/stdlib/jdk8/src`, which is not compiled into the test stdlib. Like the
// forward declaration in `libraries/stdlib/jvm/compileOnly`, it only exists for `kotlin.internal.IMPLEMENTATIONS`.
internal open class JDK8PlatformImplementations : kotlin.internal.PlatformImplementations()
