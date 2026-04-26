/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlinx.cinterop

@BetaInteropApi
@OptIn(ExperimentalForeignApi::class)
public inline fun <R> autoreleasepool(block: () -> R): R {
    val pool = objc_autoreleasePoolPush()
    return try {
        block()
    } finally {
        objc_autoreleasePoolPop(pool)
    }
}

// TODO: null checks
@BetaInteropApi
@ExperimentalForeignApi
public var <T> ObjCObjectVar<T>.value: T
    @Suppress("DEPRECATION") get() =
        interpretObjCPointerOrNull<T>(nativeMemUtils.getNativePtr(this)).uncheckedCast<T>()

    set(value) = nativeMemUtils.putNativePtr(this, value.objcPtr())

/**
 * Makes Kotlin method in Objective-C class accessible through Objective-C dispatch
 * to be used as action sent by control in UIKit or AppKit.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@BetaInteropApi
public annotation class ObjCAction

/**
 * Makes Kotlin property in Objective-C class settable through Objective-C dispatch
 * to be used as IB outlet.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@BetaInteropApi
public annotation class ObjCOutlet

/**
 * Makes Kotlin subclass of Objective-C class visible for runtime lookup
 * after Kotlin `main` function gets invoked.
 *
 * Note: runtime lookup can be forced even when the class is referenced statically from
 * Objective-C source code by adding `__attribute__((objc_runtime_visible))` to its `@interface`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@BetaInteropApi
public annotation class ExportObjCClass(val name: String = "")
