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
import java.util.concurrent.atomic.AtomicInteger
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    }

    @Test
    fun testJmxServerWorks() {
        val callsCount = AtomicInteger(0)
        val instance = object : KotlinBuildStatsMXBean {
            override fun reportBoolean(name: String, value: Boolean, subprojectName: String?, weight: Long?): Boolean {
                callsCount.incrementAndGet()
                return true
            }

            override fun reportNumber(name: String, value: Long, subprojectName: String?, weight: Long?): Boolean {
                callsCount.incrementAndGet()
                return true
            }

            override fun reportString(name: String, value: String, subprojectName: String?, weight: Long?): Boolean {
                callsCount.incrementAndGet()
                return true
            }

        }

        val beanName = ObjectName(KotlinBuildStatsService.JMX_BEAN_NAME)
        val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
        mbs.registerMBean(StandardMBean(instance, KotlinBuildStatsMXBean::class.java), beanName)

        try {
            val jmxService = JMXKotlinBuildStatsService(mbs, beanName)
            assertTrue(jmxService.report(StringMetrics.KOTLIN_COMPILER_VERSION, "1.2.3"))
            assertTrue(jmxService.report(NumericalMetrics.NUMBER_OF_SUBPROJECTS, 10))
            assertTrue(jmxService.report(BooleanMetrics.ENABLED_DATABINDING, true))
            assertEquals(3, callsCount.get())
        } finally {
            mbs.unregisterMBean(beanName)
        }

    }
}
