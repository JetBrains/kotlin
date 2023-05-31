plugins {
    id("com.gradle.enterprise")
    id("com.gradle.common-custom-user-data-gradle-plugin") apply false
}

val buildProperties = getKotlinBuildPropertiesForSettings(settings)

val buildScanServer = buildProperties.buildScanServer

if (buildProperties.buildScanServer != null) {
    plugins.apply("com.gradle.common-custom-user-data-gradle-plugin")
}

gradleEnterprise {
    buildScan {
        if (buildScanServer != null) {
            server = buildScanServer
            publishAlways()

            capture {
                isTaskInputFiles = true
                isBuildLogging = true
                isBuildLogging = true
                isUploadInBackground = true
            }
        } else {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }

        val overridenName = (buildProperties.getOrNull("kotlin.build.scan.username") as? String)?.trim()
        val isTeamCity = buildProperties.isTeamcityBuild
        obfuscation {
            ipAddresses { _ -> listOf("0.0.0.0") }
            hostname { _ -> "concealed" }
            username { originalUsername ->
                when {
                    isTeamCity -> "TeamCity"
                    overridenName == null || overridenName.isEmpty() -> "concealed"
                    overridenName == "<default>" -> originalUsername
                    else -> overridenName
                }
            }
        }
    }
}
