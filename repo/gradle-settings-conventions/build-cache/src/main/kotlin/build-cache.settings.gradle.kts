val buildProperties = getKotlinBuildPropertiesForSettings(settings)

buildCache {
    local {
        isEnabled = buildProperties.localBuildCacheEnabled
        if (buildProperties.localBuildCacheDirectory != null) {
            directory = buildProperties.localBuildCacheDirectory
        }
    }

    val remoteBuildCacheUrl = buildProperties.buildCacheUrl?.trim()
    if (!remoteBuildCacheUrl.isNullOrEmpty()) {
        remote<HttpBuildCache> {
            val buildCacheCredentialsAvailable = buildProperties.buildCacheUser != null && buildProperties.buildCachePassword != null
            
            url = uri(remoteBuildCacheUrl)
            isPush = buildProperties.pushToBuildCache && buildCacheCredentialsAvailable
            if (buildCacheCredentialsAvailable) {
                credentials.username = buildProperties.buildCacheUser
                credentials.password = buildProperties.buildCachePassword
            }
        }
    }
}
