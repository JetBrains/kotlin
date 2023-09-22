/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.fus.services

internal abstract class DummyGradleBuildFusStatisticsService : InternalGradleBuildFusStatisticsService() {
    override fun reportMetric(name: String, value: Any, subprojectName: String?) {
        //do nothing
    }

    override fun close() {
        //do nothing
    }
}