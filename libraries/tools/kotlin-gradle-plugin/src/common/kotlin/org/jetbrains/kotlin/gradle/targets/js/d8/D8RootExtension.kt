/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.d8

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.internal.ConfigurationPhaseAware
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.tasks.internal.CleanableStore
import java.io.Serializable
import java.net.URL

open class D8RootExtension(@Transient val rootProject: Project) : ConfigurationPhaseAware<D8Env>(), Serializable {
    init {
        check(rootProject.rootProject == rootProject)
    }

    private val gradleHome = rootProject.gradle.gradleUserHomeDir.also {
        rootProject.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationPath by Property(gradleHome.resolve("d8"))
    var downloadBaseUrl by Property("https://storage.googleapis.com/chromium-v8/official/canary/")

    // Latest version number could be found here https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json
    // Bash script/command to check that version specified in `VER` is available for all platforms, just copy-paste and run it in terminal:
    /*
    VER=${"$(curl -s https://storage.googleapis.com/chromium-v8/official/canary/v8-linux64-rel-latest.json)":13:-2}
    echo "VER = $VER"
    echo "=================="
    for p in "mac64" "mac-arm64" "linux32" "linux64" "win32" "win64"; do
        r=$(curl -I -s -o /dev/null -w "%{http_code}" https://storage.googleapis.com/chromium-v8/official/canary/v8-$p-rel-$VER.zip)
        if [ "$r" -eq 200 ]; then
            echo "$p   \t✅";
        else
            echo "$p   \t❌";
        fi;
    done;
    */
    var version by Property("11.7.186")
    var edition by Property("rel") // rel or dbg

    val setupTaskProvider: TaskProvider<out Copy>
        get() = rootProject.tasks.withType(Copy::class.java).named(D8RootPlugin.INSTALL_TASK_NAME)

    override fun finalizeConfiguration(): D8Env {
        val requiredVersionName = "v8-${D8Platform.platform}-$edition-$version"
        val requiredZipName = "$requiredVersionName.zip"
        val cleanableStore = CleanableStore[installationPath.absolutePath]
        val targetPath = cleanableStore[requiredVersionName].use()
        val isWindows = D8Platform.name == D8Platform.WIN

        return D8Env(
            cleanableStore = cleanableStore,
            zipPath = cleanableStore[requiredZipName].use(),
            targetPath = targetPath,
            executablePath = targetPath.resolve(if (isWindows) "d8.exe" else "d8"),
            isWindows = isWindows,
            downloadUrl = URL(downloadBaseUrl),
            ivyDependency = "google.d8:v8:${D8Platform.platform}-$edition-$version@zip"
        )
    }

    companion object {
        const val EXTENSION_NAME: String = "kotlinD8"
    }
}
