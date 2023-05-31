/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.api.logging.Logging
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.extra
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

private const val DOMAIN_NAME = "kotlin-build-properties.labs.jb.gg"
private const val SETUP_JSON_URL = "https://$DOMAIN_NAME/setup.json"

private const val PLUGIN_SWITCH_PROPERTY = "kotlin.build.internal.gradle.setup"

abstract class InternalGradleSetupSettingsPlugin : Plugin<Settings> {
    private val log = Logging.getLogger(javaClass)

    override fun apply(target: Settings) {
        // `kotlin-build-gradle-plugin` is not used here intentionally, as it caches properties, we don't want to cache them before modification
        val shouldApplyPlugin = target.providers.gradleProperty(PLUGIN_SWITCH_PROPERTY).orElse("false").map(String::toBoolean)
        if (!shouldApplyPlugin.get()) return // the plugin is disabled, do nothing at all
        val isTeamCityBuild = (target as? ExtensionAware)?.extra?.has("teamcity") == true || System.getenv("TEAMCITY_VERSION") != null
        if (isTeamCityBuild) {
            log.info("TeamCity build detected. Skipping automatic local.properties configuration")
            return
        }
        try {
            val modifier = LocalPropertiesModifier(target.rootDir.resolve("local.properties"))
            val consentManager = ConsentManager(modifier)
            val initialDecision = consentManager.getUserDecision()
            if (initialDecision == false) {
                log.debug("Skipping automatic local.properties configuration as you've opted out")
                return
            }
            val connection = URL(SETUP_JSON_URL).run { openConnection().apply { connectTimeout = 300 } }
            val setupFile = connection.getInputStream().buffered().use {
                parseSetupFile(it)
            }
            if (initialDecision == null && !consentManager.askForConsent(setupFile.consentDetailsLink)) {
                log.debug("Skipping automatic local.properties configuration as the consent wasn't given")
                return
            }
            modifier.applySetup(setupFile)
            log.info("Automatic local.properties setup has been applied.")
        } catch (e: UnknownHostException) {
            log.debug("Cannot connect to the internal properties storage", e)
        } catch (e: SocketTimeoutException) {
            log.debug("Cannot connect to the internal properties storage", e)
        } catch (e: Throwable) {
            log.warn("Something went wrong during the automatic local.properties setup attempt", e)
        }
    }
}