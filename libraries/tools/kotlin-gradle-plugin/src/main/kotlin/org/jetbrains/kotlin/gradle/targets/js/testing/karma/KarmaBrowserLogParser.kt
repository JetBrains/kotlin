/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.testing.karma

class KarmaBrowserLogParser {
    private var overflowConsole: Boolean = false
    private var previousType: BrowserLogType? = null

    fun parseKarmaBrowserLog(text: String): BrowserLog? {
        val logStartIndex = text.indexOf("$BROWSER_LOG_SURROUNDER[")
        if (logStartIndex == -1 && !overflowConsole) {
            return null
        }

        var browser: String? = null
        var type: String? = null
        var log: String = text

        if (logStartIndex != -1 && !overflowConsole) {
            val afterPrefix = text.substring(logStartIndex)
            browser = afterPrefix.substringAfter("browser='").substringBefore("'")
            type = afterPrefix.substringAfter("type='").substringBefore("'")

            log = afterPrefix.substringAfter("log=")
        }

        val logEndIndex = text.indexOf("]$BROWSER_LOG_SURROUNDER")
        if (logEndIndex != -1) {
            overflowConsole = false
            log = log.substringBefore("]$BROWSER_LOG_SURROUNDER")
        } else {
            overflowConsole = true
        }

        val currentType = when (type) {
            "log" -> BrowserLogType.LOG
            "info" -> BrowserLogType.INFO
            "warn" -> BrowserLogType.WARN
            "error" -> BrowserLogType.ERROR
            "debug" -> BrowserLogType.DEBUG
            else -> previousType
        }

        previousType = currentType

        return BrowserLog(
            browser = browser,
            type = currentType,
            log = log
        )
    }

    companion object {
        const val BROWSER_LOG_SURROUNDER = "##browser"
    }
}

data class BrowserLog(
    val browser: String?,
    val type: BrowserLogType?,
    val log: String
)

enum class BrowserLogType(val value: String) {
    LOG("log"),
    INFO("info"),
    WARN("warn"),
    ERROR("error"),
    DEBUG("debug")
}