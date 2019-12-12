package org.jetbrains.kotlin.tools.projectWizard.core.service

import java.nio.file.Path

interface AndroidService : Service {
    fun isValidAndroidSdk(path: Path): Boolean
}

class AndroidServiceImpl : AndroidService {
    //TODO use some heuristics
    override fun isValidAndroidSdk(path: Path): Boolean = true
}