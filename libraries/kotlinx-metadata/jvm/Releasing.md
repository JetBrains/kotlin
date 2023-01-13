# kotlinx-metadata-jvm releasing guide

Release is usually done from `master` branch, unless specific circumstances occur (e.g. incompatibility of protobuf between master and current Kotlin's release branch).

To release version `N` of `kotlinx-metadata-jvm`:

1. Update `ChangeLog.md` and other documentation (`ReadMe.md`, `Migration.md`) if necessary.
If release is done with different version of Kotlin than the previous one, add note about metadata version update.

2. If changes are large: Send changes for the review as a separate branch and merge them into `master` after approve.

3. Run the [TeamCity build](https://teamcity.jetbrains.com/buildConfiguration/KotlinTools_KotlinxMetadata_PublishJvm?mode=builds) with parameters:
   * `deployVersion` is a version of Kotlin and kotlin-stdlib which `kotlinx-metadata-jvm` should depend on.
   * `kotlinxMetadataDeployVersion` is a version `N` you want to release.

4. In [Sonatype](https://oss.sonatype.org/#stagingRepositories) admin interface:
    * Close the repository and wait for it to verify.
    * Release it.

5. Announce new version in [forum topic](https://discuss.kotlinlang.org/t/announcing-kotlinx-metadata-jvm-library-for-reading-modifying-metadata-of-kotlin-jvm-class-files/7980).
Additionally, you may announce it in the #compiler [public Slack channel](https://kotlinlang.slack.com) and in the internal channel #ext-google-compiler.