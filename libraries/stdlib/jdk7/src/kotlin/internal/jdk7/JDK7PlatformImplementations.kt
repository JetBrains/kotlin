/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package kotlin.internal.jdk7

import kotlin.internal.PlatformImplementations

internal open class JDK7PlatformImplementations : PlatformImplementations() {

    private object ReflectSdkVersion {
        @JvmField
        public val sdkVersion: Int? = try {
            Class.forName("android.os.Build\$VERSION").getField("SDK_INT").get(null) as? Int
        } catch (e: Throwable) {
            null
        }?.takeIf { it > 0 }
    }

    private fun sdkIsNullOrAtLeast(version: Int): Boolean = ReflectSdkVersion.sdkVersion == null || ReflectSdkVersion.sdkVersion >= version


    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun addSuppressed(cause: Throwable, exception: Throwable) =
        if (sdkIsNullOrAtLeast(19))
            (cause as java.lang.Throwable).addSuppressed(exception)
        else
            super.addSuppressed(cause, exception)

    override fun getSuppressed(exception: Throwable): List<Throwable> =
        if (sdkIsNullOrAtLeast(19))
            exception.suppressed.asList()
        else
            super.getSuppressed(exception)
}
