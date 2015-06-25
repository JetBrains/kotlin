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
    val XML_ID_PREFIXES = arrayOf(ID_DECLARATION_PREFIX, ID_USAGE_PREFIX)

    val CLEAR_FUNCTION_NAME = "clearFindViewByIdCache"

    val VIEW_FQNAME = "android.view.View"
    val ACTIVITY_FQNAME = "android.app.Activity"
    val FRAGMENT_FQNAME = "android.app.Fragment"
    val SUPPORT_V4_PACKAGE = "android.support.v4"
    val SUPPORT_FRAGMENT_FQNAME = "$SUPPORT_V4_PACKAGE.app.Fragment"
    val SUPPORT_FRAGMENT_ACTIVITY_FQNAME = "$SUPPORT_V4_PACKAGE.app.FragmentActivity"

    val IGNORED_XML_WIDGET_TYPES = setOf("requestFocus", "merge", "tag", "check")

    val ESCAPED_IDENTIFIERS = (JetTokens.KEYWORDS.getTypes() + JetTokens.SOFT_KEYWORDS.getTypes())
            .map { it as? JetKeywordToken }.filterNotNull().map { it.getValue() }.toSet()
}

public fun nameToIdDeclaration(name: String): String = AndroidConst.ID_DECLARATION_PREFIX + name

public fun idToName(id: String): String? {
    for (prefix in AndroidConst.XML_ID_PREFIXES) {
        if (id.startsWith(prefix)) return escapeAndroidIdentifier(id.replace(prefix, ""))
    }
    return null
}

public fun isWidgetTypeIgnored(xmlType: String): Boolean {
    return (xmlType.isEmpty() || xmlType in AndroidConst.IGNORED_XML_WIDGET_TYPES)
}

fun escapeAndroidIdentifier(id: String): String {
    return if (id in AndroidConst.ESCAPED_IDENTIFIERS) "`$id`" else id
}

public fun parseAndroidResource(id: String, type: String): AndroidResource {
   return  when (type) {
        "fragment" -> AndroidFragment(id)
        "include" -> AndroidWidget(id, "View")
        else -> AndroidWidget(id, type)
   }
}