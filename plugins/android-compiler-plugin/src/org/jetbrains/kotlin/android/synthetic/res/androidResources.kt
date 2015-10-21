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

package org.jetbrains.kotlin.android.synthetic.res

import org.jetbrains.kotlin.android.synthetic.AndroidConst

class AndroidVariant(val name: String, val resDirectories: List<String>) {
    val packageName: String = name
    val isMainVariant: Boolean
        get() = name == "main"

    companion object {
        fun createMainVariant(resDirectories: List<String>) = AndroidVariant("main", resDirectories)
    }
}

public class AndroidModule(val applicationPackage: String, val variants: List<AndroidVariant>) {
    override fun equals(other: Any?) = other is AndroidModule && applicationPackage == other.applicationPackage
    override fun hashCode() = applicationPackage.hashCode()
}

public abstract class AndroidResource(val id: String) {
    public abstract val className: String
    public open val supportClassName: String
        get() = className

    public abstract val mainProperties: List<Pair<String, String>>
    public open val mainPropertiesForSupportV4: List<Pair<String, String>> = listOf()
    public open val viewProperties: List<Pair<String, String>> = listOf()

    public open fun sameClass(other: AndroidResource): Boolean = false
}

public class AndroidWidget(
        id: String,
        override val className: String,
        val invalidType: String? = null // When widget type is invalid, this value is the widget tag name
) : AndroidResource(id) {
    private companion object {
        val MAIN_PROPERTIES = listOf(
                AndroidConst.ACTIVITY_FQNAME to "findViewById(0)",
                AndroidConst.FRAGMENT_FQNAME to "getView().findViewById(0)")

        val MAIN_PROPERTIES_SUPPORT_V4 = listOf(AndroidConst.SUPPORT_FRAGMENT_FQNAME to "getView().findViewById(0)")

        val VIEW_PROPERTIES = listOf(AndroidConst.VIEW_FQNAME to "findViewById(0)")
    }

    override val mainProperties = MAIN_PROPERTIES
    override val mainPropertiesForSupportV4 = MAIN_PROPERTIES_SUPPORT_V4
    override val viewProperties = VIEW_PROPERTIES

    override fun sameClass(other: AndroidResource) = other is AndroidWidget
}

public class AndroidFragment(id: String) : AndroidResource(id) {
    private companion object {
        val MAIN_PROPERTIES = listOf(
                AndroidConst.ACTIVITY_FQNAME to "getFragmentManager().findFragmentById(0)",
                AndroidConst.FRAGMENT_FQNAME to "getFragmentManager().findFragmentById(0)")

        val MAIN_PROPERTIES_SUPPORT_V4 = listOf(
                AndroidConst.SUPPORT_FRAGMENT_FQNAME to "getFragmentManager().findFragmentById(0)",
                AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME to "getSupportFragmentManager().findFragmentById(0)")
    }

    override val className = AndroidConst.FRAGMENT_FQNAME
    override val supportClassName = AndroidConst.SUPPORT_FRAGMENT_FQNAME

    override val mainProperties = MAIN_PROPERTIES
    override val mainPropertiesForSupportV4 = MAIN_PROPERTIES_SUPPORT_V4

    override fun sameClass(other: AndroidResource) = other is AndroidFragment
}