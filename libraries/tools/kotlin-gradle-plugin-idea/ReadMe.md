# Kotlin Gradle Plugin Models shared between IDEA import and KGP
The classes defined in this module will be available inside the Kotlin Gradle Plugin (KGP) 
as well as in the IDE import (GradleProject).

## Binary Compatibility: 
Binary compatibility can be tested by
`./gradlew :tools:binary-compatibility-validator:clean :tools:binary-compatibility-validator:test`

Binary incompatible changes have to go through a proper deprecation cycle after releases

### Unstable APIs / KotlinGradlePluginApi
Some APIs are marked with 'KotlinGradlePluginApi' which means, that those are not kept binary compatible and are considered 'unstable'
from the IDEA perspective. Typically constructors for models are marked like this
