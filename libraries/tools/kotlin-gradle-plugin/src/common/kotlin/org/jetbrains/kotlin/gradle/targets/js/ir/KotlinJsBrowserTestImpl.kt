/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.mapProperty
import org.jetbrains.kotlin.gradle.targets.js.dsl.BrowserTestRunnerConfigDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserTestRunnerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.testing.FileBasedKotlinJsTestDevServer
import org.jetbrains.kotlin.gradle.targets.js.testing.WebpackBundleKotlinJsTests
import org.jetbrains.kotlin.gradle.targets.js.testing.locateOrRegisterBrowserTestBundleTask
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import java.time.Duration
import javax.inject.Inject

internal abstract class KotlinBrowserTestRunner(
    private val name: String,
    objects: ObjectFactory,
): KotlinBrowserTestRunnerDsl {
    override fun getName(): String = name

    override val testsLocation: Property<KotlinJsTestsLocation> = objects.property()
    override val headless: Property<Boolean> = objects.property()
    override val timeout: Property<Duration> = objects.property()
    override val launchArgs: ListProperty<String> = objects.listProperty()
    override val customBrowserExecutable: RegularFileProperty = objects.fileProperty()
    override val launchEnvironmentVariables: MapProperty<String, String> = objects.mapProperty()
}

internal class KotlinChromiumTestRunner(
    name: String,
    objects: ObjectFactory
) : KotlinBrowserTestRunner(name, objects), KotlinJsBrowserTestDsl.ChromiumTestRunnerDsl

internal class KotlinFirefoxTestRunner(
    name: String,
    objects: ObjectFactory
) : KotlinBrowserTestRunner(name, objects), KotlinJsBrowserTestDsl.FirefoxTestRunnerDsl

internal class KotlinWebkitTestRunner(
    name: String,
    objects: ObjectFactory
) : KotlinBrowserTestRunner(name, objects), KotlinJsBrowserTestDsl.WebkitTestRunnerDsl

internal fun ObjectFactory.createKotlinJsBrowserTestImpl(
    testCompilation: KotlinJsIrCompilation
) = newInstance(KotlinJsBrowserTestImpl::class.java, testCompilation)

internal abstract class KotlinJsBrowserTestImpl
@Inject constructor(
    testCompilation: KotlinJsIrCompilation,
    private val objects: ObjectFactory,
    providers: ProviderFactory,
) : KotlinJsBrowserTestDsl {
    override val defaultBundleTask: TaskProvider<WebpackBundleKotlinJsTests> = testCompilation
        .locateOrRegisterBrowserTestBundleTask()

    private fun enableBundleTask() {
        defaultBundleTask.configure {
            it.enabled = true
        }
    }

    override val defaultTestsLocation: Provider<KotlinDefaultJsTestLocation> =
        defaultBundleTask.map { KotlinDefaultJsTestLocation(it.outputBundleDir) }

    override val allBrowserRunners: Provider<Map<String, KotlinBrowserTestRunnerDsl>> = providers.provider {
        chromiumRunners + firefoxRunners + webkitRunners
    }

    val chromiumRunners = mutableMapOf<String, KotlinChromiumTestRunner>()
    override fun chromium(
        name: String,
        body: Action<KotlinJsBrowserTestDsl.ChromiumTestRunnerDsl>,
    ) {
        val runner = chromiumRunners.getOrPut(name) {
            KotlinChromiumTestRunner(name, objects).also {
                connectTopLevelConfigDslWithBrowserTestDsl(it)
            }
        }
        body.execute(runner)
        enableBundleTask()
    }

    val firefoxRunners = mutableMapOf<String, KotlinFirefoxTestRunner>()
    override fun firefox(
        name: String,
        body: Action<KotlinJsBrowserTestDsl.FirefoxTestRunnerDsl>,
    ) {
        val runner = firefoxRunners.getOrPut(name) {
            KotlinFirefoxTestRunner(name, objects).also {
                connectTopLevelConfigDslWithBrowserTestDsl(it)
            }
        }
        body.execute(runner)
        enableBundleTask()
    }

    val webkitRunners = mutableMapOf<String, KotlinWebkitTestRunner>()
    override fun webkit(
        name: String,
        body: Action<KotlinJsBrowserTestDsl.WebkitTestRunnerDsl>,
    ) {
        val runner = webkitRunners.getOrPut(name) {
            KotlinWebkitTestRunner(name, objects).also {
                connectTopLevelConfigDslWithBrowserTestDsl(it)
            }
        }
        body.execute(runner)
        enableBundleTask()
    }

    override val browserDefaults: BrowserTestRunnerConfigDsl = objects
        .newInstance(BrowserTestRunnerConfigDsl::class.java)
        .apply {
            testsLocation.convention(defaultTestsLocation)
            headless.convention(true)
            timeout.convention(Duration.ofSeconds(2))
        }

    override fun browserDefaults(configure: Action<BrowserTestRunnerConfigDsl>) =
        browserDefaults.also { configure.execute(it) }

    private fun connectTopLevelConfigDslWithBrowserTestDsl(browserLevelDsl: KotlinBrowserTestRunnerDsl) {
        with(browserDefaults) {
            browserLevelDsl.testsLocation.convention(testsLocation)
            browserLevelDsl.headless.convention(headless)
            browserLevelDsl.timeout.convention(timeout)
            browserLevelDsl.launchArgs.convention(launchArgs)
            browserLevelDsl.customBrowserExecutable.convention(customBrowserExecutable)
            browserLevelDsl.launchEnvironmentVariables.convention(launchEnvironmentVariables)
        }
    }
}

internal class KotlinDefaultJsTestLocation(
    @get:InputDirectory
    override val bundleLocation: Provider<Directory>
) : KotlinJsTestsLocation {

    @get:Internal
    override val devServer: Provider<FileBasedKotlinJsTestDevServer> = bundleLocation
        .map { it.file("test.html") }
        .map { FileBasedKotlinJsTestDevServer(it.asFile.toPath()) }
}
