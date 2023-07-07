/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.gradle.scan.plugin.BuildScanExtension

class BuildScanExtensionHolder(val buildScan: BuildScanExtension) : java.io.Serializable {

    companion object {
        internal operator fun invoke(extension: Any): BuildScanExtensionHolder? {
            val buildScanExtension = try {
                extension as BuildScanExtension
            } catch (e: ClassNotFoundException) {
                // Build scan plugin is applied, but BuildScanExtension class is not available due to Gradle classpath isolation
                // Could be reproduced by applying Gradle enterprise plugin via init script: KT-59589
                null
            } catch (e: NoClassDefFoundError) {
                // Build scan plugin is applied, but BuildScanExtension class is not available due to Gradle classpath isolation
                // Could be reproduced by applying Gradle enterprise plugin via init script: KT-59589
                null
            }

            return buildScanExtension?.let { BuildScanExtensionHolder(it) }
        }
    }
}