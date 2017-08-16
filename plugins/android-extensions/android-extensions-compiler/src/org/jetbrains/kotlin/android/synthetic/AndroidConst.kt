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

package org.jetbrains.kotlin.android.synthetic

import org.jetbrains.kotlin.android.synthetic.res.ResourceIdentifier

object AndroidConst {
    val SYNTHETIC_PACKAGE: String = "kotlinx.android.synthetic"
    val SYNTHETIC_PACKAGE_PATH_LENGTH = SYNTHETIC_PACKAGE.count { it == '.' } + 1

    val SYNTHETIC_SUBPACKAGES: List<String> = SYNTHETIC_PACKAGE.split('.').fold(arrayListOf<String>()) { list, segment ->
        val prevSegment = list.lastOrNull()?.let { "$it." } ?: ""
        list += "$prevSegment$segment"
        list
    }

    val ANDROID_NAMESPACE: String = "http://schemas.android.com/apk/res/android"
    val ID_ATTRIBUTE_NO_NAMESPACE: String = "id"
    val CLASS_ATTRIBUTE_NO_NAMESPACE: String = "class"

    val IDENTIFIER_REGEX = "^@(\\+)?(([A-Za-z0-9_\\.]+)\\:)?id\\/([A-Za-z0-9_]+)$".toRegex()

    val CLEAR_FUNCTION_NAME = "clearFindViewByIdCache"


    //TODO FqName / ClassId

    val VIEW_FQNAME = "android.view.View"
    val VIEWSTUB_FQNAME = "android.view.ViewStub"

    val ACTIVITY_FQNAME = "android.app.Activity"
    val FRAGMENT_FQNAME = "android.app.Fragment"
    val DIALOG_FQNAME = "android.app.Dialog"
    val SUPPORT_V4_PACKAGE = "android.support.v4"
    val SUPPORT_FRAGMENT_FQNAME = "$SUPPORT_V4_PACKAGE.app.Fragment"
    val SUPPORT_FRAGMENT_ACTIVITY_FQNAME = "$SUPPORT_V4_PACKAGE.app.FragmentActivity"

    val IGNORED_XML_WIDGET_TYPES = setOf("requestFocus", "merge", "tag", "check", "blink")

    val FQNAME_RESOLVE_PACKAGES = listOf("android.widget", "android.webkit", "android.view")
}

fun androidIdToName(id: String): ResourceIdentifier? {
    val values = AndroidConst.IDENTIFIER_REGEX.matchEntire(id)?.groupValues ?: return null
    val packageName = values[3]
    return ResourceIdentifier(values[4], if (packageName.isEmpty()) null else packageName)
}

fun isWidgetTypeIgnored(xmlType: String): Boolean {
    return (xmlType.isEmpty() || xmlType in AndroidConst.IGNORED_XML_WIDGET_TYPES)
}

internal fun <T> List<T>.forEachUntilLast(operation: (T) -> Unit) {
    val lastIndex = lastIndex
    forEachIndexed { i, t ->
        if (i < lastIndex) {
            operation(t)
        }
    }
}
