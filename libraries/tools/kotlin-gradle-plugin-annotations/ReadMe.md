## Gradle Plugin Annotations

Contains all necessary annotations related to the Kotlin Gradle Plugin and modules around it. 
This module has no dependencies and can therefore be considered very lightweight. 

### API Surface Control Annotations

#### InternalKotlinGradlePluginApi
Can be used to mark APIs as 'internal' where an 'internal' modifier cannot be used
(e.g. when entities need to be re-used across several modules within kotlin.git)


#### ExperimentalKotlinGradlePluginApi
Can be used to mark APIs as 'experimental'. No stability guarantees can be provided
for APIs marked with this annotation