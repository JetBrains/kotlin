/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object DisabledNativeTargetsReporter {
    private const val EXTRA_PROPERTY_NAME = "org.jetbrains.kotlin.native.disabledTargets"

    internal const val WARNING_PREFIX = "Some Kotlin/Native targets cannot be built on this "

    internal const val DISABLE_WARNING_PROPERTY_NAME = "kotlin.native.ignoreDisabledTargets"

    fun reportDisabledTarget(project: Project, target: KotlinNativeTarget, supportedHosts: Collection<KonanTarget>) {
        val disabledTargetsList = getOrRegisterDisabledTargets(project)
        disabledTargetsList.add(DisabledTarget(project, target, supportedHosts))
    }

    private data class DisabledTarget(val project: Project, val target: KotlinNativeTarget, val supportedHosts: Collection<KonanTarget>)

    @Suppress("UNCHECKED_CAST")
    private fun getOrRegisterDisabledTargets(project: Project) =
        project.rootProject.extensions.getByType(ExtraPropertiesExtension::class.java).run {
            if (!has(EXTRA_PROPERTY_NAME)) {
                set(EXTRA_PROPERTY_NAME, mutableListOf<DisabledTarget>())
                printWarningWhenTaskGraphIsReady(project)
            }
            get(EXTRA_PROPERTY_NAME)
        } as MutableList<DisabledTarget>

    private fun printWarningWhenTaskGraphIsReady(project: Project) {
        project.gradle.taskGraph.whenReady {
            if (PropertiesProvider(project).ignoreDisabledNativeTargets == true) {
                return@whenReady
            }

            val disabledTargetsList = getOrRegisterDisabledTargets(project)

            @Suppress("UselessCallOnCollection") // filterIsInstance helps against potential class loaders conflict or misconfiguration
            val disabledTargetGroups = disabledTargetsList
                .filterIsInstance<DisabledTarget>()
                .groupBy { it.project }
                .mapValues { (_, disabledTargetsInProject) -> disabledTargetsInProject.groupBy { it.supportedHosts } }
                .toSortedMap(compareBy { it.path })

            project.logger.warn(buildString {
                appendln("\n$WARNING_PREFIX${HostManager.host} machine and are disabled:")

                disabledTargetGroups.forEach { (targetProject, targetsBySupportedHosts) ->
                    appendln(
                        "    * In project '${targetProject.path}':"
                    )
                    targetsBySupportedHosts.forEach { (supportedHosts, disabledTargets) ->
                        append("        * target" + "s".takeIf { disabledTargets.size > 1 }.orEmpty() + " ")
                        append(disabledTargets.joinToString { "'${it.target.name}'" })
                        
                        val supportedHostsString = when (supportedHosts.size) {
                            1 -> "a ${supportedHosts.single()} host"
                            else -> "one of the hosts: ${supportedHosts.joinToString(", ")}"
                        }
                        appendln(" (can be built with $supportedHostsString)")
                    }
                }
                appendln("To hide this message, add '$DISABLE_WARNING_PROPERTY_NAME=true' to the Gradle properties.")
            })
        }
    }
}