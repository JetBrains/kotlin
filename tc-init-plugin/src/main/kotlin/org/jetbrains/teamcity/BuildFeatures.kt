package org.jetbrains.teamcity

import org.gradle.api.invocation.Gradle

object BuildFeatures {
    fun isConfigurationCacheRequested(gradle: Gradle): Boolean {
        return try {
            val buildFeatures = getBuildFeatures(gradle)
            val configCache = buildFeatures.javaClass.getMethod("getConfigurationCache").invoke(buildFeatures)
            val requested = configCache.javaClass.getMethod("getRequested").invoke(configCache)
            val getOrElseMethod = requested.javaClass.getMethod("getOrElse", Any::class.java)
            getOrElseMethod.invoke(requested, false) as Boolean
        } catch (ignored: Throwable) {
            try {
                val startParameter = gradle.startParameter
                val method = startParameter.javaClass.getMethod("isConfigurationCacheRequested")
                method.invoke(startParameter) as Boolean
            } catch (e: Throwable) {
                false
            }
        }
    }

    fun areIsolatedProjectsEnabled(gradle: Gradle): Boolean {
        return try {
            val buildFeatures = getBuildFeatures(gradle)
            val isolatedProjects = buildFeatures.javaClass.getMethod("getIsolatedProjects").invoke(buildFeatures)
            val active = isolatedProjects.javaClass.getMethod("getActive").invoke(isolatedProjects)
            val getOrElseMethod = active.javaClass.getMethod("getOrElse", Any::class.java)
            getOrElseMethod.invoke(active, false) as Boolean
        } catch (ignored: Throwable) {
            val value = System.getProperty(Constants.GRADLE_UNSAFE_ISOLATED_PROJECTS_SYSTEM_PROP)
            value != null && value.toBoolean()
        }
    }

    private fun getBuildFeatures(gradle: Gradle): Any {
        val buildFeaturesClass = Class.forName("org.gradle.api.configuration.BuildFeatures", false, gradle.javaClass.classLoader)
        val services = gradle.javaClass.getMethod("getServices").invoke(gradle)
        val getMethod = services.javaClass.getMethod("get", Class::class.java)
        return getMethod.invoke(services, buildFeaturesClass)
    }
}
