/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener

//Available since Gradle 6.1
class KotlinGradleBuildListener(
//    val gradle: Gradle,
    val services: KotlinGradleFinishBuildHandler
) : OperationCompletionListener {

    override fun onFinish(event: FinishEvent) {
//        services.buildFinished(gradle)
    }

}