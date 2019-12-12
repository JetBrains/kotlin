package org.jetbrains.kotlin.tools.projectWizard.core.service

interface Service

object Services {
    val osServices = listOf(
        OsGradleIntegration(),
        OsMavenService(),
        OsFileSystemService(),
        OsJpsService(),
        AndroidServiceImpl()
    )
}
