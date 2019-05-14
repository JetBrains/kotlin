/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.js.resolve

import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.resolve.TargetPlatform

@Deprecated(
    message = "This class is deprecated and will be removed soon, use API from 'org.jetbrains.kotlin.platform.*' packages instead",
    replaceWith = ReplaceWith("JsPlatforms.defaultJsPlatform", "org.jetbrains.kotlin.platform.js.JsPlatforms"),
    level = DeprecationLevel.ERROR
)
interface JsPlatform : TargetPlatform {
    @JvmDefault
    override val platformName: String
        get() = "JS"

    companion object {
        @JvmField
        val INSTANCE: JsPlatform = JsPlatforms.CompatJsPlatform
    }
}
