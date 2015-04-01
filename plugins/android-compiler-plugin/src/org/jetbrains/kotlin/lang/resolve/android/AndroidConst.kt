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

package org.jetbrains.kotlin.lang.resolve.android

import com.intellij.openapi.util.Key
import org.jetbrains.kotlin.lexer.JetKeywordToken
import org.jetbrains.kotlin.lexer.JetTokens

public object AndroidConst {
    val ANDROID_USER_PACKAGE: Key<String> = Key.create<String>("ANDROID_USER_PACKAGE")
    val SYNTHETIC_FILENAME_PREFIX: String = "ANDROIDXML_"
    val LAYOUT_POSTFIX: String = "_LAYOUT"
    val VIEW_LAYOUT_POSTFIX: String = "_VIEW"
    val SYNTHETIC_PACKAGE: String = "kotlinx.android.synthetic"
    val SYNTHETIC_PACKAGE_PATH_LENGTH = SYNTHETIC_PACKAGE.count { it == '.' } + 1

    val ANDROID_NAMESPACE: String = "android"
    val ID_ATTRIBUTE_NO_NAMESPACE: String = "id"
    val ID_ATTRIBUTE: String = "$ANDROID_NAMESPACE:$ID_ATTRIBUTE_NO_NAMESPACE"
    val CLASS_ATTRIBUTE_NO_NAMESPACE: String = "class"

    val ID_DECLARATION_PREFIX = "@+id/"
    val ID_USAGE_PREFIX = "@id/"

    val CLEAR_FUNCTION_NAME = "clearFindViewByIdCache"

    val IGNORED_XML_WIDGET_TYPES = setOf("requestFocus", "merge", "tag", "check")

    val ESCAPED_IDENTIFIERS = (JetTokens.KEYWORDS.getTypes() + JetTokens.SOFT_KEYWORDS.getTypes())
            .map { it as? JetKeywordToken }.filterNotNull().map { it.getValue() }.toSet()
}

public fun nameToIdDeclaration(name: String): String = AndroidConst.ID_DECLARATION_PREFIX + name

public fun idToName(id: String): String? {
    val unescaped =
            if (isResourceIdDeclaration(id)) id.replace(AndroidConst.ID_DECLARATION_PREFIX, "")
            else if (isResourceIdUsage(id)) id.replace(AndroidConst.ID_USAGE_PREFIX, "")
            else null
    return if (unescaped != null) escapeAndroidIdentifier(unescaped) else null
}

public fun isResourceIdDeclaration(str: String?): Boolean = str?.startsWith(AndroidConst.ID_DECLARATION_PREFIX) ?: false

public fun isResourceIdUsage(str: String?): Boolean = str?.startsWith(AndroidConst.ID_USAGE_PREFIX) ?: false

public fun isResourceDeclarationOrUsage(id: String?): Boolean = isResourceIdDeclaration(id) || isResourceIdUsage(id)

public fun isWidgetTypeIgnored(xmlType: String): Boolean {
    return (xmlType.isEmpty() || xmlType in AndroidConst.IGNORED_XML_WIDGET_TYPES)
}

public fun getRealWidgetType(xmlType: String): String = when (xmlType) {
    "fragment", "include" -> "View"
    else -> xmlType
}

fun escapeAndroidIdentifier(id: String): String {
    return if (id in AndroidConst.ESCAPED_IDENTIFIERS) "`$id`" else id
}