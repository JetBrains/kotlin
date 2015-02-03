/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android

import com.intellij.openapi.util.Key

public object AndroidConst {
    val ANDROID_USER_PACKAGE: Key<String> = Key.create<String>("ANDROID_USER_PACKAGE")
    val SYNTHETIC_FILENAME: String = "ANDROIDXML.kt"

    val androidNamespace: String = "android"
    val idAttributeNoNamespace: String = "id"
    val idAttribute: String = "$androidNamespace:$idAttributeNoNamespace"
    val classAttributeNoNamespace: String = "class"
    val classAttribute: String = "$androidNamespace:$classAttributeNoNamespace"

    val idDeclarationPrefix = "@+id/"
    val idUsagePrefix = "@id/"
}

class WrongIdFormat(id: String) : Exception("Id \"$id\" has wrong format")

public fun nameToIdDeclaration(name: String): String = AndroidConst.idDeclarationPrefix + name

public fun nameToIdUsage(name: String): String = AndroidConst.idUsagePrefix + name

public fun idToName(id: String): String {
    return if (isResourceIdDeclaration(id)) id.replace(AndroidConst.idDeclarationPrefix, "")
    else if (isResourceIdUsage(id)) id.replace(AndroidConst.idUsagePrefix, "")
    else throw WrongIdFormat(id)
}

public fun isResourceIdDeclaration(str: String?): Boolean = str?.startsWith(AndroidConst.idDeclarationPrefix) ?: false

public fun isResourceIdUsage(str: String?): Boolean = str?.startsWith(AndroidConst.idUsagePrefix) ?: false

public fun isResourceDeclarationOrUsage(id: String?): Boolean = isResourceIdDeclaration(id) || isResourceIdUsage(id)
