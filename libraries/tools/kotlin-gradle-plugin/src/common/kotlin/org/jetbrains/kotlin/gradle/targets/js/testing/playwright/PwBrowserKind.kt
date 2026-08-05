package org.jetbrains.kotlin.gradle.targets.js.testing.playwright

/**
 * Kind of Playwright-supported browser.
 */
internal enum class PwBrowserKind(val browserName: String) {
    CHROMIUM("chromium"),
    FIREFOX("firefox"),
    WEBKIT("webkit"),
}
