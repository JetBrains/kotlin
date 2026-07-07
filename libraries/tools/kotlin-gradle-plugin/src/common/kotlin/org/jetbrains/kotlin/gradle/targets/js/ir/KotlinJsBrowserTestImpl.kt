/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Action
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.mapProperty
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinBrowserTestRunnerDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsBrowserTestDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTestsLocation
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinDefaultJsTestLocation
import org.jetbrains.kotlin.gradle.targets.js.testing.locateOrRegisterBrowserTestBundleTask
import org.jetbrains.kotlin.gradle.utils.listProperty
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.propertyWithConvention
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

    override val allBrowserRunners: Provider<Map<String, KotlinBrowserTestRunnerDsl>> = providers.provider {
        chromiumRunners + firefoxRunners + webkitRunners
    }

    override val defaultTestsLocationProvider: Provider<KotlinDefaultJsTestLocation> = testCompilation
        .locateOrRegisterBrowserTestBundleTask {
            // enabled when at least one browser runner is enabled. So the user has an intention to test via the browser pipeline.
            browserRunnersDeclared.set(allBrowserRunners.map { it.isNotEmpty() })
        }.map { it.kotlinJsTestLocation }

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
    }

    override val testsLocation: Property<KotlinJsTestsLocation> =
        objects.propertyWithConvention<KotlinJsTestsLocation>(defaultTestsLocationProvider)

    override val headless: Property<Boolean> = objects.propertyWithConvention<Boolean>(true)

    override val timeout: Property<Duration> = objects.propertyWithConvention<Duration>(30L.seconds)

    private fun connectTopLevelConfigDslWithBrowserTestDsl(browserLevelDsl: KotlinBrowserTestRunnerDsl) {
        browserLevelDsl.testsLocation.convention(testsLocation)
        browserLevelDsl.headless.convention(headless)
        browserLevelDsl.timeout.convention(timeout)
        browserLevelDsl.launchEnvironmentVariables.convention(launchEnvironmentVariables)
    }
}
