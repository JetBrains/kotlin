kotlin-stdlib-docs
===================

API documentation build for kotlin-stdlib, kotlin-reflect, and kotlin-test libraries for
kotlinlang.org web-site. 

## Building

To generate the documentation, run the following command in this project directory:
```
./gradlew dokkaGenerate
```

## Gradle parameters

The table lists Gradle parameters that can be used to configure the build. 
The default values are suitable for building the latest documentation locally.
Paths are resolved relative to the project evaluating the property.

| Property | Description | Default |
| --- | --- | --- |
| `isLatest` | Whether to build the documentation for the latest version of Kotlin, or one of previous versions to be incorporated then in the latest (include previous versions, and enable the version filter. | `true` |
| `docsBuildDir` | Root output directory for the documentation. `cleanDocs` task deletes this directory. | `build/doc` in this project |
| `docsPreviousVersionsDir` | Input directory for previous documentation versions; the combined publication reads its `all-libs` subdirectory when `isLatest` is true. | `<docsBuildDir>/previous` |
| `templatesDir` | Directory containing Dokka HTML templates. | `templates` in this project |

The version for which the documentation is built is determined by the `kotlinLanguageVersion` property from the repository root `gradle.properties` file.

When `isLatest` is `true`, the combined HTML output is written to `<docsBuildDir>/latest/all-libs`; otherwise it is written to `<docsBuildDir>/previous/all-libs/<kotlinLanguageVersion>`.

### TeamCity parameters

The build detects TeamCity when the `teamcity.version` project property is present or the `TEAMCITY_VERSION` environment variable is set. In that case, the following project properties are required:

| Property | Description | Local build value |
| --- | --- | --- |
| `githubRevision` | Specifies git branch or commit reference where source links should point to.  | `master` |
| `kotlinLibsRepo` | Additional Maven repository that can contain the built Kotlin core libraries artifacts. | `<repository root>/build/repo` |
| `deployVersion` | Version of the Kotlin core libraries artifacts. | `defaultSnapshotVersion` from the repository root `gradle.properties` |

When building on TeamCity, the `githubRevision` is set to the branch name where the build checks out the source from, but can be overriden to some other branch name.

When building locally, ensure that the required library binary artifacts have been published before in the `kotlinLibsRepo` repository. The required libraries can be published there using the `coreLibsPublish` Gradle task in the root project. 
A build on TeamCity, on the other hand, usually takes these artifacts from the snapshot dependency builds and downloads them to the default path to this directory.


