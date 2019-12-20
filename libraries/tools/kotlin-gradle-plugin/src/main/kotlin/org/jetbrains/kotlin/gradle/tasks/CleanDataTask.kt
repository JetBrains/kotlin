/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.time.Duration
import java.time.Instant

/**
 * Task to clean all old loaded files based on a date of special access file.
 * The access file should be updated every time when loaded files are used
 */
open class CleanDataTask : DefaultTask() {

    /**
     * Path to folder.
     * Use path instead of file to avoid file scanning for change check
     */
    @Input
    lateinit var cleanableStore: CleanableStore

    /**
     * Time to live in days
     */
    @Input
    var timeToLiveInDays: Long = 30

    @Suppress("unused")
    @TaskAction
    fun exec() {
        val expirationDate = Instant.now().minus(Duration.ofDays(timeToLiveInDays))

        cleanableStore.cleanDir(expirationDate)

    }

    companion object {
        const val NAME: String = "KotlinClean"
    }

}