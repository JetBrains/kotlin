# Kotlin Gradle Plugin Models shared between IDEA import and KGP

The classes defined in this module will be available inside the Kotlin Gradle Plugin (KGP)
as well as in the IDE import (GradleProject).

## Binary Compatibility

The public API surface of this module is checked for stability
using the [binary compatibility validator](https://github.com/Kotlin/binary-compatibility-validator/) plugin
to prevent accidental public API changes.

You can execute public API validation by running `apiCheck` task (also executed when `check` task runs).

In order to overwrite the reference API snapshot, you can launch `apiDump` task.

Binary incompatible changes have to go through a proper deprecation cycle after releases

### Unstable APIs / InternalKotlinGradlePluginApi

Some APIs are marked with 'InternalKotlinGradlePluginApi' which means, that those are not kept binary compatible and are considered 'unstable' from
the IDEA perspective.
