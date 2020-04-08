/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction.nonBlocking
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.kotlin.idea.configuration.ui.notifications.ConfigureKotlinNotification
import java.util.concurrent.Callable

fun notify(manager: ConfigureKotlinNotificationManager, project: Project, excludeModules: List<Module>) {
    nonBlocking(Callable {
        ConfigureKotlinNotification.getNotificationState(project, excludeModules)
    })
        .expireWith(project)
        .coalesceBy(manager)
        .finishOnUiThread(ModalityState.any()) { notificationState ->
            notificationState?.let {
                manager.notify(project, ConfigureKotlinNotification(project, excludeModules, it))
            }
        }
        .submit(AppExecutorUtil.getAppExecutorService())
}