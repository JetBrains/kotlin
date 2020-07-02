/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.migration

import com.intellij.codeInspection.*
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.idea.KotlinBundle

internal class ObsoleteKotlinJsPackagesInspection : ObsoleteCodeMigrationInspection() {
    override val fromVersion: LanguageVersion = LanguageVersion.KOTLIN_1_3
    override val toVersion: LanguageVersion = LanguageVersion.KOTLIN_1_4

    override val problemReporters: List<ObsoleteCodeProblemReporter> = listOf(
        KotlinBrowserImportUsageReporter,
        KotlinDomImportUsageReporter
    )
}

private object ObsoleteKotlinJsPackagesUsagesInWholeProjectFix : ObsoleteCodeInWholeProjectFix() {
    override val inspectionName: String = ObsoleteKotlinJsPackagesInspection().shortName
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.kotlin.js.packages.usage.in.whole.fix.family.name")
}

private const val KOTLIN_BROWSER_PACKAGE = "kotlin.browser"
private const val KOTLIN_DOM_PACKAGE = "kotlin.dom"

private class ObsoleteKotlinBrowserUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.package.usage.fix.family.name", KOTLIN_BROWSER_PACKAGE)
}

private class ObsoleteKotlinDomUsageFix(delegate: ObsoleteCodeFix) : ObsoleteCodeFixDelegateQuickFix(delegate) {
    override fun getFamilyName(): String = KotlinBundle.message("obsolete.package.usage.fix.family.name", KOTLIN_DOM_PACKAGE)
}

private object KotlinBrowserImportUsageReporter : ObsoleteImportsUsageReporter() {
    override val textMarker: String = "browser"
    override val packageBindings: Map<String, String> = mapOf(KOTLIN_BROWSER_PACKAGE to "kotlinx.browser")

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("package.usages.are.obsolete.since.1.4", KOTLIN_BROWSER_PACKAGE)

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinBrowserUsageFix(fix)
}

private object KotlinDomImportUsageReporter : ObsoleteImportsUsageReporter() {
    override val textMarker: String = "dom"
    override val packageBindings: Map<String, String> = mapOf(KOTLIN_DOM_PACKAGE to "kotlinx.dom")

    override val wholeProjectFix: LocalQuickFix = ObsoleteKotlinJsPackagesUsagesInWholeProjectFix
    override fun problemMessage(): String = KotlinBundle.message("package.usages.are.obsolete.since.1.4", KOTLIN_DOM_PACKAGE)

    override fun wrapFix(fix: ObsoleteCodeFix): LocalQuickFix = ObsoleteKotlinDomUsageFix(fix)
}
