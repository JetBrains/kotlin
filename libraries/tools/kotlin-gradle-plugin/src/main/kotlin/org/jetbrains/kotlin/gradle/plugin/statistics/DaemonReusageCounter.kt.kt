/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicLong
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean

interface IDaemonReuseCounterMXBean {

    fun getOrdinal(): Long

    fun incrementAndGetOrdinal(): Long

}

/**
 * This class is used for counting builds involving Kotlin withing one Gradle daemon
 * One instance is kept available via JMX bean after build completion. If other builds are executed with other version
 * of Kotlin plugin, they are still able to reuse this build counter vis JXM interface
 */
class DaemonReuseCounter private constructor() : IDaemonReuseCounterMXBean {
    private val count = AtomicLong()

    override fun getOrdinal(): Long {
        return count.get()
    }

    override fun incrementAndGetOrdinal(): Long {
        return count.incrementAndGet()
    }

    companion object {
        // Do not rename this bean otherwise compatibility with the older Kotlin Gradle Plugins would be lost
        const val JMX_BEAN_NAME = "org.jetbrains.kotlin.gradle.plugin.statistics:type=BuildCounter"

        private fun ensureRegistered(beanName: ObjectName, mbs: MBeanServer) {
            if (!mbs.isRegistered(beanName)) {
                mbs.registerMBean(StandardMBean(DaemonReuseCounter(), IDaemonReuseCounterMXBean::class.java), beanName)
            }
        }

        fun incrementAndGetOrdinal(): Long {
            val beanName = ObjectName(JMX_BEAN_NAME)
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            ensureRegistered(beanName, mbs)
            return mbs.invoke(beanName, "incrementAndGetOrdinal", emptyArray(), emptyArray<String>()) as? Long ?: 0
        }

        fun getOrdinal(): Long {
            val beanName = ObjectName(KotlinBuildStatsService.JMX_BEAN_NAME)
            val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
            ensureRegistered(beanName, mbs)
            return mbs.invoke(beanName, "getOrdinal", emptyArray(), emptyArray<String>()) as? Long ?: 0
        }
    }
}