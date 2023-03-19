val buildProperties = getKotlinBuildPropertiesForSettings(settings)

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled
        if (buildProperties.localBuildCacheDirectory != null) {
            directory = buildProperties.localBuildCacheDirectory
        }
    }

    val remoteBuildCacheUrl = buildProperties.buildCacheUrl
    if (remoteBuildCacheUrl != null) {
        remote<HttpBuildCache> {
            url = uri(remoteBuildCacheUrl)
            isPush = buildProperties.pushToBuildCache
            if (buildProperties.buildCacheUser != null &&
                buildProperties.buildCachePassword != null
            ) {
                credentials.username = buildProperties.buildCacheUser
                credentials.password = buildProperties.buildCachePassword
            }
        }
    }
}
