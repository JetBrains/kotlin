/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.invocation.Gradle
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.configuration.ProjectConfigurationFinishEvent
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName

open class KotlinBuildStatListener(val beanName: ObjectName/*, val gradle: Gradle*/) : OperationCompletionListener, AutoCloseable {

    private var projectEvaluatedTime: Long? = null

    override fun onFinish(event: FinishEvent?) {
        if (event is ProjectConfigurationFinishEvent) {
            projectEvaluatedTime = event.eventTime
        }
        //todo is it any chance to get failure exception?
        //todo nothing to do?
//        KotlinBuildStatHandler.runSafe("${KotlinBuildStatListener::class.java}.onFinish") {
//
//
//            try {
//                val finishTime = event?.result?.endTime
//                val startTime = event?.result?.startTime
//                    report(NumericalMetrics.GRADLE_BUILD_DURATION, finishTime - it.buildStartedTime)
//                    report(NumericalMetrics.GRADLE_EXECUTION_DURATION, finishTime - it.projectEvaluatedTime)
//                    report(NumericalMetrics.BUILD_FINISH_TIME, finishTime)
//            } finally {
//                val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
//                if (mbs.isRegistered(beanName)) {
//                    mbs.unregisterMBean(beanName)
//                }
//            }
//        }

    }

    override fun close() {
//        val sessionLogger = BuildSessionLogger(gradle.gradleUserHomeDir)
//        KotlinBuildStatHandler.runSafe("${KotlinBuildStatListener::class.java}.close()") {
//            try {
//                try {
//                    KotlinBuildStatHandler().reportGlobalMetrics(gradle, sessionLogger)
//                } finally {
////                    report(NumericalMetrics.GRADLE_BUILD_DURATION, finishTime - it.buildStartedTime)
////                    report(NumericalMetrics.GRADLE_EXECUTION_DURATION, finishTime - it.projectEvaluatedTime)
////                    report(NumericalMetrics.BUILD_FINISH_TIME, finishTime)
//                }
//
//            } finally {
//                val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
//                if (mbs.isRegistered(beanName)) {
//                    mbs.unregisterMBean(beanName)
//                }
//            }
//        }
    }
}