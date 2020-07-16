/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.compiler

import org.jetbrains.kotlin.analyzer.moduleInfo
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.resolve.LanguageVersionSettingsProvider

class IdeLanguageVersionSettingsProvider : LanguageVersionSettingsProvider {
    override fun getModuleLanguageVersionSettings(module: ModuleDescriptor): LanguageVersionSettings? {
        // downcast to module source info, as we're not interested in non-source classes
        val moduleInfo = module.moduleInfo as? ModuleSourceInfo ?: return null
        return moduleInfo.module.languageVersionSettings
    }
}