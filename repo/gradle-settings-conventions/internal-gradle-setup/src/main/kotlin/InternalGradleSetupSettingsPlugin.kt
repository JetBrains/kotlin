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
import kotlin.concurrent.thread

private const val DOMAIN_NAME = "kotlin-build-properties.labs.jb.gg"
private const val SETUP_JSON_URL = "https://$DOMAIN_NAME/setup.json"

abstract class InternalGradleSetupSettingsPlugin : Plugin<Settings> {
    private val log = Logging.getLogger(javaClass)

    override fun apply(target: Settings) {
        val isTeamCityBuild = (target as? ExtensionAware)?.extra?.has("teamcity") == true || System.getenv("TEAMCITY_VERSION") != null
        if (isTeamCityBuild) {
            log.info("TeamCity build detected. Skipping automatic local.properties configuration")
            return
        }
        val rootDir = target.rootDir
        // invoke this logic in a separate thread to not pause the build
        // the properties will be configured for the future builds
        thread {
            try {
                val modifier = LocalPropertiesModifier(rootDir.resolve("local.properties"))
                val consentManager = ConsentManager(modifier)
                val initialDecision = consentManager.getUserDecision()
                if (initialDecision == false) {
                    log.debug("Skipping automatic local.properties configuration as you've opted out")
                    return@thread
                }
                val connection = URL(SETUP_JSON_URL).run { openConnection().apply { connectTimeout = 300 } }
                val setupFile = connection.getInputStream().buffered().use {
                    parseSetupFile(it)
                }
                if (initialDecision == null && !consentManager.askForConsent()) {
                    log.debug("Skipping automatic local.properties configuration as the consent wasn't given")
                    return@thread
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
}