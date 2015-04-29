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

package org.jetbrains.kotlin.idea.bare

import com.intellij.compiler.server.CompileServerPlugin
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.extensions.PluginId

public class BareJpsPluginRegistrar : ApplicationComponent {
    override fun initComponent() {
        val mainKotlinPlugin = PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin"))

        if (mainKotlinPlugin != null && mainKotlinPlugin.isEnabled()) {
            // do nothing
        }
        else {
            val compileServerPlugin = CompileServerPlugin()
            compileServerPlugin.setClasspath("jps/kotlin-jps-plugin.jar;kotlin-runtime.jar;kotlin-reflect.jar;kotlin-bare-plugin.jar")
            compileServerPlugin.setPluginDescriptor(PluginManager.getPlugin(PluginId.getId("org.jetbrains.kotlin.bare")))

            Extensions.getRootArea()
                    .getExtensionPoint(CompileServerPlugin.EP_NAME)
                    .registerExtension(compileServerPlugin)
        }
    }

    override fun disposeComponent() {
    }

    override fun getComponentName(): String {
        return javaClass.getName()
    }
}
