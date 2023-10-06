/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.statistics.old.Pre232IdeaKotlinBuildStatsMXBean
import org.jetbrains.kotlin.gradle.plugin.statistics.old.Pre232IdeaKotlinBuildStatsService
import java.io.Closeable
import java.lang.management.ManagementFactory
import javax.management.MBeanServer
import javax.management.ObjectName
import javax.management.StandardMBean

/**
 * A registry of Kotlin FUS services scoped to a project.
 * In general, we use only one instance of a service either it is a real service or a JMX wrapper.
 * However, we also register legacy variants of the services so previous IDEA versions could access them.
 * We must properly manage the lifecycle of such services.
 *
 * The implementation is not thread-safe!
 */
internal class KotlinBuildStatsServicesRegistry : Closeable {
    internal val logger = Logging.getLogger(this::class.java)
    private val services = hashMapOf<String, KotlinBuildStatsService>()

    /**
     * Registers the Kotlin build stats services for the given project.
     *
     * After a call of that method, [getDefaultService] is expected to return a non-null value.
     *
     * The registry must be closed at the end of the usage.
     */
    fun registerServices(project: Project) {
        val defaultBeanName = getBeanName(DEFAULT_SERVICE_QUALIFIER)
        val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
        if (mbs.isRegistered(defaultBeanName)) {
            // because we use only the default service, and it's already registered in the MBean server, then there's no need in caring of legacy services as they're already registered
            logger.debug("${KotlinBuildStatsService::class.simpleName} $defaultBeanName is already instantiated in another classpath. Creating JMX wrapper for the main service")
            services[DEFAULT_SERVICE_QUALIFIER] = JMXKotlinBuildStatsService(mbs, defaultBeanName)
        } else {
            registerStatsService(
                mbs,
                DefaultKotlinBuildStatsService(project, getBeanName(DEFAULT_SERVICE_QUALIFIER)),
                KotlinBuildStatsMXBean::class.java,
                DEFAULT_SERVICE_QUALIFIER
            )
            // to support backward compatibility with Idea before version 232
            registerStatsService(
                mbs,
                Pre232IdeaKotlinBuildStatsService(project, getBeanName(LEGACY_SERVICE_QUALIFIER)),
                Pre232IdeaKotlinBuildStatsMXBean::class.java,
                LEGACY_SERVICE_QUALIFIER
            )
        }
    }

    private fun <T : KotlinBuildStatsService> registerStatsService(
        mbs: MBeanServer,
        service: T,
        beanInterfaceType: Class<in T>,
        qualifier: String,
    ) {
        val beanName = getBeanName(qualifier)
        if (!mbs.isRegistered(beanName)) {
            mbs.registerMBean(StandardMBean(service, beanInterfaceType), beanName)
            services[qualifier] = service
            val loggedServiceName = "${KotlinBuildStatsService::class.java}" + if (qualifier.isNotEmpty()) "_$qualifier" else ""
            logger.debug("Instantiated $loggedServiceName: new instance $service")
        }
    }

    /**
     * Retrieves the default Kotlin build stats service. That's the main service we are working with.
     */
    fun getDefaultService() = services[DEFAULT_SERVICE_QUALIFIER]

    /**
     * Unregisters all the registered JMX services and may release other resources allocated by a service.
     */
    override fun close() {
        for (service in services.values) {
            service.close()
        }
        services.clear()
    }

    companion object {
        private const val JXM_BEAN_BASE_NAME = "org.jetbrains.kotlin.gradle.plugin.statistics:type=StatsService"

        // Do not rename this bean otherwise compatibility with the older Kotlin Gradle Plugins would be lost
        private const val LEGACY_SERVICE_QUALIFIER = ""

        // Update name when API changed
        internal const val DEFAULT_SERVICE_QUALIFIER = "v2"

        internal fun getBeanName(qualifier: String) =
            ObjectName(JXM_BEAN_BASE_NAME + if (qualifier.isNotEmpty()) ",name=$qualifier" else "")
    }
}