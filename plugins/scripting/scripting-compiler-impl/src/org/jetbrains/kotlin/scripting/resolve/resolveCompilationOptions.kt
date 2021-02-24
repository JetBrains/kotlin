/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.resolve

import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.api.ScriptCompilationConfigurationKeys
import kotlin.script.experimental.util.PropertiesCollection

/**
 * Classes for which instances extensions should not be resolved if they are used in implicit context
 */
val ScriptCompilationConfigurationKeys.skipExtensionsResolutionForImplicits
        by PropertiesCollection.key<Collection<KotlinType>>(emptyList())

/**
 * Extensions resolution for these classes in implicit context of their instances will be done only for innermost instance
 * in scopes chain. Instances of each class in collection are handled separately.
 */
val ScriptCompilationConfigurationKeys.skipExtensionsResolutionForImplicitsExceptInnermost
        by PropertiesCollection.key<Collection<KotlinType>>(emptyList())
