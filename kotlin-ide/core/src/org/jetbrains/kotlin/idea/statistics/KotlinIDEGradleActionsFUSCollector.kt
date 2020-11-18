package org.jetbrains.kotlin.idea.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.internal.statistic.utils.getPluginInfoById
import org.jetbrains.kotlin.idea.KotlinPluginUtil

class KotlinIDEGradleActionsFUSCollector : CounterUsagesCollector() {
    override fun getGroup(): EventLogGroup = GROUP

    companion object {
        private val GROUP = EventLogGroup("kotlin.ide.gradle", 1)

        private val pluginInfo = getPluginInfoById(KotlinPluginUtil.KOTLIN_PLUGIN_ID)

        private val allowedTargets = listOf(
            "kotlin-android",
            "kotlin-platform-common",
            "kotlin-platform-js",
            "kotlin-platform-jvm",
            "MPP.androidJvm",
            "MPP.androidJvm.android",
            "MPP.common",
            "MPP.common.metadata",
            "MPP.js",
            "MPP.js.js",
            "MPP.jvm",
            "MPP.jvm.jvm",
            "MPP.jvm.jvmWithJava",
            "MPP.native",
            "MPP.native.androidNativeArm32",
            "MPP.native.androidNativeArm64",
            "MPP.native.iosArm32",
            "MPP.native.iosArm64",
            "MPP.native.iosX64",
            "MPP.native.linuxArm32Hfp",
            "MPP.native.linuxArm64",
            "MPP.native.linuxMips32",
            "MPP.native.linuxMipsel32",
            "MPP.native.linuxX64",
            "MPP.native.macosX64",
            "MPP.native.mingwX64",
            "MPP.native.mingwX86",
            "MPP.native.wasm32",
            "MPP.native.zephyrStm32f4Disco",
            "unknown"
        )

        private val importEvent = GROUP.registerEvent(
            "Import",
            EventFields.String("target", allowedTargets),
            EventFields.PluginInfo
        )

        fun logImport(target: String) = importEvent.log(target, pluginInfo)
    }
}
