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

package org.jetbrains.kotlin.idea.testUtils

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.JetStdJSProjectDescriptor
import org.jetbrains.kotlin.idea.ProjectDescriptorWithStdlibSources

public enum class ModuleKind {
    KOTLIN_JVM_WITH_STDLIB_SOURCES
    KOTLIN_JAVASCRIPT
}

public fun Module.configureAs(descriptor: LightProjectDescriptor) {
    val module = this
    updateModel(module, object : Consumer<ModifiableRootModel> {
        override fun consume(model: ModifiableRootModel) {
            if (descriptor.getSdk() != null) {
                model.setSdk(descriptor.getSdk())
            }
            val entries = model.getContentEntries()
            val entry = if (entries.size() > 0) entries[0] else null
            descriptor.configureModule(module, model, entry)
        }
    })
}

public fun Module.configureAs(kind: ModuleKind) {
    when(kind) {
        ModuleKind.KOTLIN_JVM_WITH_STDLIB_SOURCES ->
            this.configureAs(ProjectDescriptorWithStdlibSources.INSTANCE)
        ModuleKind.KOTLIN_JAVASCRIPT ->
            this.configureAs(JetStdJSProjectDescriptor.INSTANCE)
        else -> throw IllegalArgumentException("Unknown kind=$kind")
    }
}

