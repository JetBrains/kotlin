# build-logic

This project aims to share common build logic between subprojects.

In principle, this is similar to `buildSrc`, but this project utilizes [composite builds][1] to avoid various 
[inconvenient side effects][2] of `buildSrc`.

For more information, see [Sharing Build Logic between Subprojects][3]

___

Note: the filename pattern used for convention plugins is inspired by how Gradle configures its 
own convention plugins; [example project here][4].

[1]: https://docs.gradle.org/7.6/userguide/composite_builds.html
[2]: https://proandroiddev.com/stop-using-gradle-buildsrc-use-composite-builds-instead-3c38ac7a2ab3
[3]: https://docs.gradle.org/8.4/userguide/sharing_build_logic_between_subprojects.html
[4]: https://github.com/gradle/gradle/tree/b165da7de15e70afb6cac564bf4aadf16aa157b3/build-logic/jvm/src/main/kotlin
