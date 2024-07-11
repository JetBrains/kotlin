plugins {
    id("com.gradle.develocity")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildProperties = getKotlinBuildPropertiesForSettings(settings)

val buildScanServer = buildProperties.buildScanServer

if (buildProperties.buildScanServer != null) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

develocity {
    buildScan {
        if (buildScanServer != null) {
            server = buildScanServer

            capture {
                buildLogging = true
                uploadInBackground = true
            }
        } else {
            termsOfUseUrl = "https://gradle.com/help/legal-terms-of-use"
            termsOfUseAgree = "yes"
        }

        val overriddenUsername = (buildProperties.getOrNull("kotlin.build.scan.username") as? String)?.trim()
        val overriddenHostname = (buildProperties.getOrNull("kotlin.build.scan.hostname") as? String)?.trim()
        val isTeamCity = buildProperties.isTeamcityBuild
        if (buildProperties.isJpsBuildEnabled) {
            tag("JPS")
        }
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> overriddenHostname ?: "concealed" }
            username { originalUsername ->
                when {
                    isTeamCity -> "TeamCity"
                    overriddenUsername.isNullOrEmpty() -> "concealed"
                    overriddenUsername == "<default>" -> originalUsername
                    else -> overriddenUsername
                }
            }
        }
    }
}
