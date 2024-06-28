/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.gradle.plugin.statistics.old.Pre232IdeaKotlinBuildStatsMXBean
import org.jetbrains.kotlin.gradle.plugin.statistics.old.Pre232IdeaKotlinBuildStatsBeanService
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
    private val services = hashMapOf<String, KotlinBuildStatsBeanService>()

    /**
     * Registers the Kotlin build stats services for the given project.
     *
     * The registry must be closed at the end of the usage.
     */
    fun registerServices(project: Project) {
        val mbs: MBeanServer = ManagementFactory.getPlatformMBeanServer()
        registerStatsService(
            mbs,
            DefaultKotlinBuildStatsBeanService(project, getBeanName(DEFAULT_SERVICE_QUALIFIER)),
            KotlinBuildStatsMXBean::class.java,
            DEFAULT_SERVICE_QUALIFIER
        )
        // to support backward compatibility with Idea before version 232
        registerStatsService(
            mbs,
            Pre232IdeaKotlinBuildStatsBeanService(project, getBeanName(LEGACY_SERVICE_QUALIFIER)),
            Pre232IdeaKotlinBuildStatsMXBean::class.java,
            LEGACY_SERVICE_QUALIFIER
        )

    }

    fun recordBuildStart(buildId: String) {
        services.forEach { it.value.recordBuildStart(buildId) }
    }


    private fun <T : KotlinBuildStatsBeanService> registerStatsService(
        mbs: MBeanServer,
        service: T,
        beanInterfaceType: Class<in T>,
        qualifier: String,
    ) {
        val beanName = getBeanName(qualifier)
        val loggedServiceName = "${KotlinBuildStatsBeanService::class.java}" + if (qualifier.isNotEmpty()) "_$qualifier" else ""
        if (!mbs.isRegistered(beanName)) {
            mbs.registerMBean(StandardMBean(service, beanInterfaceType), beanName)
            services[qualifier] = service
            logger.debug("Instantiated $loggedServiceName: new instance $service")
        } else {
            logger.debug("$loggedServiceName is already instantiated in another classpath.")
        }
    }


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