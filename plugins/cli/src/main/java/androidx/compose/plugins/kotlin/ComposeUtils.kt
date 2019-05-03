/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.plugins.kotlin

import org.jetbrains.kotlin.name.FqName

object ComposeUtils {

    fun generateComposePackageName() = "androidx.compose"

    fun composeFqName(cname: String) = FqName("${generateComposePackageName()}.$cname")

    fun setterMethodFromPropertyName(name: String): String {
        return "set${name[0].toUpperCase()}${name.slice(1 until name.length)}"
    }

    fun propertyNameFromSetterMethod(name: String): String {
        return if (name.startsWith("set")) "${
            name[3].toLowerCase()
        }${name.slice(4 until name.length)}" else name
    }

    fun isSetterMethodName(name: String): Boolean {
        // use !lower to capture non-alpha chars
        return name.startsWith("set") && name.length > 3 && !name[3].isLowerCase()
    }
}