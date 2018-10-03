/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.idea

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.core.unwrapModuleSourceInfo
import org.jetbrains.kotlin.idea.facet.KotlinFacet
import org.jetbrains.kotlin.resolve.descriptorUtil.module

fun <T> getIfEnabledOn(clazz: ClassDescriptor, body: () -> T): T? {
    val module = clazz.module.getCapability(ModuleInfo.Capability)?.unwrapModuleSourceInfo()?.module ?: return null
    val facet = KotlinFacet.get(module) ?: return null
    val pluginClasspath = facet.configuration.settings.compilerArguments?.pluginClasspaths ?: return null
    if (pluginClasspath.none { it == KotlinSerializationImportHandler.PLUGIN_JPS_JAR }) return null
    return body()
}

fun runIfEnabledOn(clazz: ClassDescriptor, body: () -> Unit) { getIfEnabledOn<Unit>(clazz, body) }