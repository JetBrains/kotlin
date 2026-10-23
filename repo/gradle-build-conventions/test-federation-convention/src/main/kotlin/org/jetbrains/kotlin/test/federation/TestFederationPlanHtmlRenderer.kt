/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

internal class TestFederationPlanHtmlRenderer(
    private val plan: TestFederationPlan
) {
    fun renderHtml(): String =
        // language=HTML
        """
        <!DOCTYPE html>
        <html lang='en'>
            <head>
                <meta charset="utf-8">
                <title>Test Federation</title>
                ${style()}
                ${supportDarkThemeScript()}
            </head>
            <body>
                <table>
                    <tr><th>Domain</th><th>Test subsets</th><th>Reason</th></tr>
                    ${tableRows()}
                </table>
                <p>Legend:</p>
                <table>
                    <tr><th>Symbol</th><th>Meaning</th></tr>
                    <tr><td><strong>Domain*</strong></td><td>The domain is changed</td></tr>
                    <tr><td>$ALL_TESTS_EMOJI</td><td>AllTests</td></tr>
                    <tr><td>$SMOKE_TESTS_EMOJI</td><td>SmokeTests</td></tr>
                    <tr><td>$CONTRACT_TESTS_EMOJI (Domain)</td><td>ContractTestsForDomain</td></tr>
                </table>
            </body>
        </html>
        """.trimIndent()

    private fun style() =
        // language=HTML
        """
        <style>
            :root { --fg: #222; --bg: #fff; --border: #ccc; --header-bg: #f0f0f0; color-scheme: light; }
            :root.dark { --fg: #ddd; --bg: #1e1f22; --border: #4a4d52; --header-bg: #2b2d30; color-scheme: dark; }
            @media (prefers-color-scheme: dark) { :root:not(.light) { --fg: #ddd; --bg: #1e1f22; --border: #4a4d52; --header-bg: #2b2d30; color-scheme: dark; } }
            body { font-family: sans-serif; margin: 16px; color: var(--fg); background: var(--bg); }
            table { border-collapse: collapse; }
            th, td { border: 1px solid var(--border); padding: 4px 12px; text-align: left; }
            th { background: var(--header-bg); font-weight: normal; }
        </style>
        """.trimIndent()

    private fun supportDarkThemeScript() =
        // language=HTML
        """
        <script>
            (function () {
              try {
                const parentDocument = window.parent.document;
                const background = window.parent.getComputedStyle(parentDocument.body).backgroundColor;
                const channels = background.match(/\d+(\.\d+)?/g);
                if (!channels || channels.length < 3 || (channels.length > 3 && Number(channels[3]) === 0)) return;
                const luminance = 0.299 * channels[0] + 0.587 * channels[1] + 0.114 * channels[2];
                document.documentElement.classList.add(luminance < 128 ? 'dark' : 'light');
              } catch (e) {}
            })();
        </script>
        """.trimIndent()

    private fun tableRows() = buildString {
        plan.entries.forEach { row ->
            val domainName = row.domain.name.escapeHtml()
            val domain = if (row.isChanged) "<strong>$domainName</strong>*" else domainName
            val subsets = row.testSubsets.joinToString(" + ") { it.toEmoji().escapeHtml() }
            appendLine("<tr><td>$domain</td><td>$subsets</td><td>${row.reason.escapeHtml()}</td></tr>")
        }
    }
}

private fun String.escapeHtml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
