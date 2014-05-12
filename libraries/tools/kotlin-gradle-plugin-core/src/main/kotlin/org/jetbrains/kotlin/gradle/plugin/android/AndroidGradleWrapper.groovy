package org.jetbrains.kotlin.gradle.plugin.android

import com.android.build.gradle.BasePlugin

class AndroidGradleWrapper {
    static def getRuntimeJars(BasePlugin basePlugin) {
        if (basePlugin.getMetaClass().getMetaMethod("getRuntimeJarList")) {
            return basePlugin.getRuntimeJarList()
        }
        else {
            return basePlugin.getBootClasspath()
        }
    }
}
