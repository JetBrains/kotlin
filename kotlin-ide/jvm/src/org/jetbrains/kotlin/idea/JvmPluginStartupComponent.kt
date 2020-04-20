/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import org.jetbrains.kotlin.idea.ThreadTrackerPatcherForTeamCityTesting.patchThreadTracker
import org.jetbrains.kotlin.idea.debugger.filter.addKotlinStdlibDebugFilterIfNeeded
import org.jetbrains.kotlin.idea.util.application.isUnitTestMode

class JvmPluginStartupComponent : BaseComponent {
    override fun getComponentName(): String = JvmPluginStartupComponent::class.java.name

    override fun initComponent() {
        if (isUnitTestMode()) {
            patchThreadTracker()
        }
        addKotlinStdlibDebugFilterIfNeeded()
    }

    override fun disposeComponent() {}

    companion object {
        fun getInstance(): JvmPluginStartupComponent =
            ApplicationManager.getApplication().getComponent(JvmPluginStartupComponent::class.java)
    }
}