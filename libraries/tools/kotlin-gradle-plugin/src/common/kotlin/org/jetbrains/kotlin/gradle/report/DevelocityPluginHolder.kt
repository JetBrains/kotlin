/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.gradle.develocity.agent.gradle.scan.BuildScanConfiguration
import org.gradle.api.Action


class DevelocityPluginHolder(private val buildScan: BuildScanConfiguration) : java.io.Serializable, BuildScanApi {

    companion object {
        internal operator fun invoke(extension: Any): DevelocityPluginHolder? {
            val buildScan = try {
                extension as BuildScanConfiguration
            } catch (e: ClassNotFoundException) {
                // Build scan plugin is applied, but BuildScanExtension class is not available due to Gradle classpath isolation
                // Could be reproduced by applying Gradle enterprise plugin via init script: KT-59589
                null
            } catch (e: NoClassDefFoundError) {
                // Build scan plugin is applied, but BuildScanExtension class is not available due to Gradle classpath isolation
                // Could be reproduced by applying Gradle enterprise plugin via init script: KT-59589
                null
            }

            return buildScan?.let { DevelocityPluginHolder(it) }
        }
    }

    override fun tag(tag: String) {
        buildScan.tag(tag)
    }

    override fun value(name: String, value: String) {
        buildScan.value(name, value)
    }

    override fun buildFinished(action: () -> Unit) {
        buildScan.buildFinished { action() }
    }
}