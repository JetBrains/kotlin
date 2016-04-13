/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package com.android.tools.klint.client.api

import com.android.sdklib.IAndroidTarget
import java.io.File

class SdkWrapper(val sdk: Any) {

    companion object {
        val PROGRESS_INDICATOR_CLASS_NAME = "com.android.repository.api.ConsoleProgressIndicator"

        @JvmStatic
        fun createLocalSdk(file: File): SdkWrapper? {
            try {
                val clazz = Class.forName("com.android.sdklib.repository.local.LocalSdk")
                return SdkWrapper(clazz.getConstructor(File::class.java).newInstance(file))
            }
            catch (t: Throwable) {
                try {
                    val clazz = Class.forName("com.android.sdklib.repositoryv2.AndroidSdkHandler")
                    return SdkWrapper(clazz.getMethod("getInstance", File::class.java).invoke(null, file))
                }
                catch (t2: Throwable) {
                    return null
                }
            }
        }
    }

    fun getTargets(): Array<IAndroidTarget>? {
        try {
            @Suppress("UNCHECKED_CAST")
            return sdk.javaClass.getMethod("getTargets").invoke(sdk) as? Array<IAndroidTarget>
        } catch (t: Throwable) {
            @Suppress("UNCHECKED_CAST")
            try {
                val targets = sdk.javaClass
                        .getMethod("getAndroidTargetManager", Class.forName(PROGRESS_INDICATOR_CLASS_NAME))
                        .invoke(sdk, createProgressIndicator()) as? Collection<IAndroidTarget> ?: return null
                return targets.toTypedArray()
            }
            catch (t2: Throwable) {
                return null
            }
        }
    }

    fun getPlatformVersion(): String? {
        try {
            val pkgPlatformTools = Class.forName("com.android.sdklib.repository.descriptors.PkgType")
                    .getField("PKG_PLATFORM_TOOLS").get(null)
            val pkgInfo = sdk.javaClass.declaredMethods
                    .first { it.name == "getPkgInfo" && it.parameterTypes.size == 1 }
                    .invoke(sdk, pkgPlatformTools)
            if (pkgInfo != null) {
                val desc = pkgInfo.javaClass.getMethod("getDesc").invoke(pkgInfo)
                val version = desc.javaClass.getMethod("getFullRevision").invoke(desc)
                if (version != null) {
                    return version.javaClass.getMethod("toShortString").invoke(version) as? String
                }
            }
            else {
                return null
            }
        } catch (t: Throwable) {
            try {
                val pkgPlatformTools = Class.forName("com.android.SdkConstants")
                        .getField("FD_PLATFORM_TOOLS").get(null)
                val pkgInfo = sdk.javaClass.declaredMethods
                        .first { it.name == "getLocalPackage" && it.parameterTypes.size == 2 }
                        .invoke(sdk, pkgPlatformTools, createProgressIndicator())
                if (pkgInfo != null) {
                    val version = pkgInfo.javaClass.getMethod("getVersion").invoke(pkgInfo)
                    return version.javaClass.getMethod("toShortString").invoke(version) as? String
                }
                else {
                    return null
                }
            }
            catch (ignore: Throwable) {}
        }

        return null
    }

    private fun createProgressIndicator(): Any {
        return Class.forName(PROGRESS_INDICATOR_CLASS_NAME).newInstance()
    }
}