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

public data class AndroidModuleInfo(val applicationPackage: String, val mainResDirectories: List<String>)

public abstract class AndroidResource(val id: String) {
    public abstract val className: String

    public abstract val mainProperties: List<Pair<String, String>>

    public open val viewProperties: List<Pair<String, String>> = listOf()

    public open fun sameClass(other: AndroidResource): Boolean = false

    public open fun supportClassName(): String = className
}

public class AndroidWidget(id: String, override val className: String) : AndroidResource(id) {
    private companion object {
        val MAIN_PROPERTIES = listOf(
                AndroidConst.ACTIVITY_FQNAME to "findViewById(0)",
                AndroidConst.FRAGMENT_FQNAME to "getView().findViewById(0)",
                AndroidConst.SUPPORT_FRAGMENT_FQNAME to "getView().findViewById(0)")

        val VIEW_PROPERTIES = listOf("android.view.View" to "findViewById(0)")
    }

    override val mainProperties = MAIN_PROPERTIES

    override val viewProperties = VIEW_PROPERTIES

    override fun sameClass(other: AndroidResource) = other is AndroidWidget
}

public class AndroidFragment(id: String) : AndroidResource(id) {
    private companion object {
        val MAIN_PROPERTIES = listOf(
                AndroidConst.ACTIVITY_FQNAME to "getFragmentManager().findFragmentById(0)",
                AndroidConst.FRAGMENT_FQNAME to "getFragmentManager().findFragmentById(0)",
                AndroidConst.SUPPORT_FRAGMENT_FQNAME to "getFragmentManager().findFragmentById(0)",
                AndroidConst.SUPPORT_FRAGMENT_ACTIVITY_FQNAME to "getSupportFragmentManager().findFragmentById(0)")
    }

    override val className = AndroidConst.FRAGMENT_FQNAME

    override val mainProperties = MAIN_PROPERTIES

    override fun sameClass(other: AndroidResource) = other is AndroidFragment

    override fun supportClassName() = AndroidConst.SUPPORT_FRAGMENT_FQNAME
}