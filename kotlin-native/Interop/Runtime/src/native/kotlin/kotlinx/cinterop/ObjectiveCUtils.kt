/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.cinterop

inline fun <R> autoreleasepool(block: () -> R): R {
    val pool = objc_autoreleasePoolPush()
    return try {
        block()
    } finally {
        objc_autoreleasePoolPop(pool)
    }
}

@Deprecated("Use plain Kotlin cast", ReplaceWith("this as T"), DeprecationLevel.ERROR)
fun <T : ObjCObject> ObjCObject.reinterpret() = @Suppress("DEPRECATION") this.uncheckedCast<T>()

// TODO: null checks
var <T> ObjCObjectVar<T>.value: T
    @Suppress("DEPRECATION") get() =
        interpretObjCPointerOrNull<T>(nativeMemUtils.getNativePtr(this)).uncheckedCast<T>()

    set(value) = nativeMemUtils.putNativePtr(this, value.objcPtr())

/**
 * Makes Kotlin method in Objective-C class accessible through Objective-C dispatch
 * to be used as action sent by control in UIKit or AppKit.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ObjCAction

/**
 * Makes Kotlin property in Objective-C class settable through Objective-C dispatch
 * to be used as IB outlet.
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class ObjCOutlet

/**
 * Makes Kotlin subclass of Objective-C class visible for runtime lookup
 * after Kotlin `main` function gets invoked.
 *
 * Note: runtime lookup can be forced even when the class is referenced statically from
 * Objective-C source code by adding `__attribute__((objc_runtime_visible))` to its `@interface`.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExportObjCClass(val name: String = "")
