# Keep `common-configuration` out of the `plugins {}` block

Applies to: `src/**/*.gradle.kts`

`common-configuration` must stay applied with `apply(plugin = "common-configuration")` in the script body.
Listing it in `plugins {}` makes accessor generation apply it to a synthetic project without the
repository's build properties, which fails the conventions build.

# Plugins applied by id are dependencies of this project

Applies to:
```
src/**/*.gradle.kts
/build.gradle.kts
```

Every plugin the precompiled script applies by id in `plugins {}` must be on this project's classpath
through a dependency in `build.gradle.kts` (`libs.kotlinx.bcv`, `project(":test-inputs-check")`, …),
and a dependency removed there must not still be applied.

# Stay lazy and inside the project

Applies to: `src/**/*.gradle.kts`

Use `tasks.register` and providers; no `afterEvaluate`, no `tasks.getByName`, no access to other
projects. Per-library configuration is the `description`; a second knob calls for a typed extension,
not a project property.

# Document behaviour changes

Applies to: `src/**/*.gradle.kts`

A change to what the plugin sets up is reflected in `README.md` next to it and verified with `check`
and `publish` of a library (or a throwaway module while none exists), checking the POM and the API dump.
