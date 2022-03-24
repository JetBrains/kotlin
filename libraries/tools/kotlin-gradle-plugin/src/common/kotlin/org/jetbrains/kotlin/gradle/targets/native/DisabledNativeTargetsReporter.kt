/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal abstract class AggregateReporter {
    @Suppress("UNCHECKED_CAST")
    protected fun <T> getOrRegisterData(project: Project, propertyName: String): MutableList<T> =
        project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java).run {
            if (!has(propertyName)) {
                set(propertyName, mutableListOf<T>())
                project.gradle.taskGraph.whenReady { printWarning(project) }
            }
            get(propertyName)
        } as MutableList<T>

    protected abstract fun printWarning(project: Project)
}

internal object DisabledNativeTargetsReporter : AggregateReporter() {
    private const val EXTRA_PROPERTY_NAME = "org.jetbrains.kotlin.native.disabledTargets"

    internal const val WARNING_PREFIX = "Some Kotlin/Native targets cannot be built on this "

    internal const val DISABLE_WARNING_PROPERTY_NAME = "kotlin.native.ignoreDisabledTargets"

    fun reportDisabledTarget(project: Project, target: KotlinNativeTarget, supportedHosts: Collection<KonanTarget>) {
        val disabledTargetsList = getOrRegisterDisabledTargets(project)
        disabledTargetsList.add(DisabledTarget(project, target, supportedHosts))
    }

    private data class DisabledTarget(val project: Project, val target: KotlinNativeTarget, val supportedHosts: Collection<KonanTarget>)

    private fun getOrRegisterDisabledTargets(project: Project) =
        getOrRegisterData<DisabledTarget>(project, EXTRA_PROPERTY_NAME)

    override fun printWarning(project: Project) {
        if (PropertiesProvider(project).ignoreDisabledNativeTargets == true) {
            return
        }

        val disabledTargetsList = getOrRegisterDisabledTargets(project)

        @Suppress("UselessCallOnCollection") // filterIsInstance helps against potential class loaders conflict or misconfiguration.
        val disabledTargetGroups = disabledTargetsList
            .filterIsInstance<DisabledTarget>()
            .groupBy { it.project }
            .mapValues { (_, disabledTargetsInProject) -> disabledTargetsInProject.groupBy { it.supportedHosts } }
            .toSortedMap(compareBy { it.path })

        project.logger.warn(buildString {
            appendLine("\n$WARNING_PREFIX${HostManager.host} machine and are disabled:")

            disabledTargetGroups.forEach { (targetProject, targetsBySupportedHosts) ->
                appendLine(
                    "    * In project '${targetProject.path}':"
                )
                targetsBySupportedHosts.forEach { (supportedHosts, disabledTargets) ->
                    append("        * target" + "s".takeIf { disabledTargets.size > 1 }.orEmpty() + " ")
                    append(disabledTargets.joinToString { "'${it.target.name}'" })

                    val supportedHostsString = when (supportedHosts.size) {
                        1 -> "a ${supportedHosts.single()} host"
                        else -> "one of the hosts: ${supportedHosts.joinToString(", ")}"
                    }
                    appendLine(" (can be built with $supportedHostsString)")
                }
            }
            appendLine("To hide this message, add '$DISABLE_WARNING_PROPERTY_NAME=true' to the Gradle properties.")
        })
    }
}
