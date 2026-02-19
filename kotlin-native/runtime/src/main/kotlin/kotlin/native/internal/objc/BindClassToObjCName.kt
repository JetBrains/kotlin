/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.native.internal.objc

import kotlin.native.internal.InternalForKotlinNative
import kotlin.reflect.KClass

/**
 * Mark that [kotlinClass] is exported as ObjC class [objCName].
 *
 * In the entire final binary for each [kotlinClass] there must be at most one such annotation.
 *
 * Use [KClass.objCNameOrNull] to extract [objCName] back if it was set.
 *
 * This annotation is bound to a file. The binding code and data will be generated in that file.
 */
@InternalForKotlinNative
@Target(AnnotationTarget.FILE)
@Repeatable
@Retention(AnnotationRetention.BINARY)
public annotation class BindClassToObjCName(val kotlinClass: KClass<*>, val objCName: String)