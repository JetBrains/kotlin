/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import com.intellij.openapi.application.ApplicationManager

private const val APPLICATION_MANAGER_CLASS_NAME = "com.intellij.openapi.application.ApplicationManager"

val isJps: Boolean by lazy {
    /*
        Normally, JPS shouldn't have an ApplicationManager class in the classpath,
        but that's not true for JPS inside IDEA right now.
        Though Application is not properly initialized inside JPS so we can use it as a check.
     */
    return@lazy if (doesClassExist(APPLICATION_MANAGER_CLASS_NAME)) {
        ApplicationManager.getApplication() == null
    } else {
        true
    }
}

private fun doesClassExist(fqName: String): Boolean {
    val classPath = fqName.replace('.', '/') + ".class"
    return {}.javaClass.classLoader.getResource(classPath) != null
}