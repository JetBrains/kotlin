/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.resolve.descriptorUtil.module

private fun isEnabledIn(moduleDescriptor: ModuleDescriptor): Boolean {
    val module = moduleDescriptor.getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module ?: return false
    val facet = KotlinFacet.get(module) ?: return false
    val pluginClasspath = facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return false
    if (pluginClasspath.none(KotlinSerializationImportHandler::isPluginJarPath)) return false
    return true
}

fun <T> getIfEnabledOn(clazz: ClassDescriptor, body: () -> T): T? {
    return if (isEnabledIn(clazz.module)) body() else null
}

fun runIfEnabledOn(clazz: ClassDescriptor, body: () -> Unit) { getIfEnabledOn<Unit>(clazz, body) }

fun runIfEnabledIn(moduleDescriptor: ModuleDescriptor, block: () -> Unit) { if (isEnabledIn(moduleDescriptor)) block() }