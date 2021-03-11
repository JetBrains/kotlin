/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.ide.IdeBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.updateSettings.impl.UpdateSettings
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinPluginUpdater
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import org.jetbrains.kotlin.idea.PluginUpdateStatus
import org.jetbrains.kotlin.idea.configuration.ui.KotlinLanguageConfigurationForm
import javax.swing.JComponent

class KotlinLanguageConfiguration : SearchableConfigurable, Configurable.NoScroll {
    companion object {
        const val ID = "preferences.language.Kotlin"

        private fun saveSelectedChannel(channelOrdinal: Int) {
            val hosts = UpdateSettings.getInstance().storedPluginHosts
            hosts.removeIf {
                it.startsWith("https://plugins.jetbrains.com/plugins/") &&
                        (it.endsWith("/6954") || it.endsWith(KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString))
            }

            UpdateChannel.values().find { it.ordinal == channelOrdinal }?.let { eapChannel ->
                if (eapChannel != UpdateChannel.STABLE) {
                    hosts.add(eapChannel.url ?: error(KotlinBundle.message("configuration.error.text.shouldn.t.add.null.urls.to.custom.repositories")))
                }
            }
        }

        enum class UpdateChannel(val url: String?, val title: String) {
            STABLE(null, KotlinBundle.message("configuration.title.stable")),
            EAP(
                "https://plugins.jetbrains.com/plugins/eap/${KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString}",
                KotlinBundle.message("configuration.title.early.access.preview.1.4.x")
            ),
            EAP_NEXT(
                "https://plugins.jetbrains.com/plugins/eap-next/${KotlinPluginUtil.KOTLIN_PLUGIN_ID.idString}",
                KotlinBundle.message("configuration.title.early.access.preview.1.5.x")
            );

            fun isInHosts(): Boolean {
                if (this == STABLE) return false
                return url in UpdateSettings.getInstance().pluginHosts
            }
        }
    }

    private val form = KotlinLanguageConfigurationForm()
    private var update: PluginUpdateStatus.Update? = null

    private var savedChannel = -1

    private var versionForInstallation: String? = null

    private var installedVersion: String? = null
    private var installingStatus: String? = null

    override fun getId(): String = ID

    override fun getDisplayName(): String = KotlinBundle.message("configuration.name.kotlin")

    override fun isModified() =
        form.experimentalFeaturesPanel.isModified()

    override fun apply() {
        // Selected channel is now saved automatically

        form.experimentalFeaturesPanel.applySelectedChanges()
    }

    private fun setInstalledVersion(installedVersion: String?, installingStatus: String?) {
        this.installedVersion = installedVersion
        this.installingStatus = installingStatus
    }

    override fun createComponent(): JComponent? {
        form.updateCheckProgressIcon.suspend()
        form.updateCheckProgressIcon.setPaintPassiveIcon(false)

        form.reCheckButton.addActionListener {
            checkForUpdates()
        }

        form.installButton.isVisible = false
        form.installButton.addActionListener {
            update?.let {
                form.hideInstallButton()

                setInstalledVersion(it.pluginDescriptor.version, KotlinBundle.message("configuration.status.text.installing"))

                form.installStatusLabel.text = installingStatus

                KotlinPluginUpdater.getInstance().installPluginUpdate(
                    it,
                    successCallback = {
                        setInstalledVersion(it.pluginDescriptor.version, IdeBundle.message("plugin.manager.installed.tooltip"))
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = installingStatus
                        }
                    },
                    cancelCallback = {
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = ""
                            form.showInstallButton()

                            setInstalledVersion(null, null)
                        }
                    },
                    errorCallback = {
                        if (versionForInstallation == it.pluginDescriptor.version) {
                            form.installStatusLabel.text = KotlinBundle.message("configuration.status.text.installation.failed")
                            form.showInstallButton()
                            setInstalledVersion(null, null)
                        }
                    }
                )
            }
        }

        form.initChannels(UpdateChannel.values().map { it.title })

        savedChannel = UpdateChannel.values().find { it.isInHosts() }?.ordinal ?: 0
        form.channelCombo.selectedIndex = savedChannel

        form.channelCombo.addActionListener {
            val newChannel = form.channelCombo.selectedIndex
            if (newChannel != savedChannel) {
                savedChannel = newChannel
                checkForUpdates()
            }
        }

        checkForUpdates()

        return form.mainPanel
    }

    private fun checkForUpdates() {
        saveChannelSettings()
        form.updateCheckProgressIcon.resume()
        form.resetUpdateStatus()
        KotlinPluginUpdater.getInstance().runUpdateCheck { pluginUpdateStatus ->
            // Need this to show something is happening when check is very fast
            Thread.sleep(30)
            form.updateCheckProgressIcon.suspend()

            when (pluginUpdateStatus) {
                PluginUpdateStatus.LatestVersionInstalled -> {
                    form.setUpdateStatus(
                        KotlinBundle.message("configuration.message.text.you.have.the.latest.version.of.the.plugin.installed"),
                        false
                    )
                }

                is PluginUpdateStatus.Update -> {
                    update = pluginUpdateStatus
                    versionForInstallation = update?.pluginDescriptor?.version
                    form.setUpdateStatus(
                        KotlinBundle.message("configuration.message.text.a.new.version.is.available",
                            pluginUpdateStatus.pluginDescriptor.version
                        ),
                        true
                    )
                    if (installedVersion != null && installedVersion == versionForInstallation) {
                        // Installation of the plugin has been started or finished
                        form.hideInstallButton()
                        form.installStatusLabel.text = installingStatus
                    }
                }

                is PluginUpdateStatus.CheckFailed ->
                    form.setUpdateStatus(
                        KotlinBundle.message("configuration.message.text.update.check.failed", pluginUpdateStatus.message),
                        false
                    )

                is PluginUpdateStatus.Unverified -> {
                    val version = pluginUpdateStatus.updateStatus.pluginDescriptor.version
                    val generalLine = KotlinBundle.message("configuration.message.text.a.new.version.is.found",
                        version,
                        pluginUpdateStatus.verifierName
                    )
                    val reasonLine = pluginUpdateStatus.reason ?: ""
                    val message = "<html>$generalLine<br/>$reasonLine</html>"
                    form.setUpdateStatus(message, false)
                }
            }

            false  // do not auto-retry update check
        }
    }

    private fun saveChannelSettings() {
        saveSelectedChannel(form.channelCombo.selectedIndex)
    }
}

