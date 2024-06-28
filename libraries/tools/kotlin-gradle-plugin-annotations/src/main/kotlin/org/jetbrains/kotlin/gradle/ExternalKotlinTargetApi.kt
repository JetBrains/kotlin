/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

/**
 * Marker for APIs that can be used by Google to build and maintain a 'KotlinTarget' outside the kotlin.git repository:
 * Declarations marked with this annotation will be checked for binary compatibility.
 *
 * ### Stability Guarantee:
 * Despite those APIs being verified for binary compatibility, the overall 'External Kotlin Target API' surface is
 * not stabilised yet and changes might happen until 1.9.20
 *
 * After 1.9.20 this APIs will have to go through a full deprecation cycle before being broken.
 */
@RequiresOptIn(
    message = "This API can be used by Google to maintain KotlinTargets outside of kotlin.git",
    level = RequiresOptIn.Level.ERROR
)
annotation class ExternalKotlinTargetApi
