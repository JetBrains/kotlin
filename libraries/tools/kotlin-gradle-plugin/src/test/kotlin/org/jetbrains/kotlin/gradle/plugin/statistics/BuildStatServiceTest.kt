/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import org.junit.Test
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName

class BuildStatServiceTest {

    @Test
    fun testJmxDoesNotFail() {
        //Test that JMX service does not throw exception even if JMX beans are not configured
        val beanName = ObjectName(KotlinBuildStatsService.JMX_BEAN_NAME)
        val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()

        val jmxService = JMXKotlinBuildStatsService(mbs, beanName)
        jmxService.report(StringMetrics.KOTLIN_COMPILER_VERSION, "1.2.3")
        jmxService.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 10)
        jmxService.report(BooleanMetrics.ENABLED_DATABINDING, true)

        jmxService.completed()
    }
}
