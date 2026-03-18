/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin.io

// Stubbed because the Minimal stdlib requires manual expect/actual bridges for core types like Serializable
// to bypass missing dependencies without bringing in the full standard library common sources.
// jvm-minimal-for-test provides this via its own common-src stubs.
expect interface Serializable
