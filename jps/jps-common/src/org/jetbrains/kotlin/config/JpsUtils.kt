/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import com.intellij.openapi.application.ApplicationManager

private const val JPS_STANDALONE_CLASS_NAME = "org.jetbrains.jps.build.Standalone"

val isJps: Boolean by lazy {
    val jpsStandaloneClassName = JPS_STANDALONE_CLASS_NAME.replace('.', '/') + ".class"

    (object {}.javaClass.classLoader.getResource(jpsStandaloneClassName) != null)
            || ApplicationManager.getApplication() == null
}