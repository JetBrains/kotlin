## Description

Provides common settings convention plugins for the repo.

### List of plugins

- `cache-redirector` - configures all repositories in `pluginManagement`, `dependencyResolutionManagement` and `buildscript` to use the proxy caching service
- `develocity` - configures build scans upload
- `internal-gradle-setup` - fetches internal Develocity configs for developers using VPN / internal network 
- `jvm-toolchain-provisioning` - configures JVM toolchain to download project JDKs via Disco API
- `kotlin-bootstrap` - applies the currently configured Kotlin bootstrap version to the project
- `kotlin-daemon-config` - common project JVM arguments for Kotlin daemon
