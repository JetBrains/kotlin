/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

const val APPLICATION_MANAGER_CLASS_NAME = "com.intellij.openapi.application.ApplicationManager"

val isJps: Boolean by lazy {
    val application = try {
        Class.forName("com.intellij.openapi.application.Application")
    } catch (ex: LinkageError) {
        // If any Application super class is not in the classpath
        return@lazy true
    } catch (ex: ClassNotFoundException) {
        return@lazy true
    }
    val applicationManager = try {
        Class.forName(APPLICATION_MANAGER_CLASS_NAME)
    } catch (ex: LinkageError) {
        // If any ApplicationManager super class is not in the classpath
        return@lazy true
    } catch (ex: ClassNotFoundException) {
        return@lazy true
    }
    /*
        Normally, JPS shouldn't have an ApplicationManager class in the classpath,
        but that's not true for JPS inside IDEA right now.
        Though Application is not properly initialized inside JPS so we can use it as a check.
     */
    return@lazy MethodHandles.lookup()
        .findStatic(applicationManager, "getApplication", MethodType.methodType(application))
        .invoke() == null
}