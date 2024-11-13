## Kotlinlang Gradle API reference

This project assembles API reference for Kotlin Gradle plugins to publish it to https://kotlinlang.org.

### Configuration

- `build/templates` dir is used for Kotlinlang website templates. Currently, they should be put there manually.
- `build/documentation/kotlinlangOld` directory is used for previously generated documentation.
  Inside the structure of subdirectories 
should follow [this specification](https://github.com/Kotlin/dokka/tree/1.9.20/dokka-subprojects/plugin-versioning#directory-structure).

### Assembling

To assemble API reference run:
```shell
$ ./gradlew :gradle:documentation:dokkaKotlinlangDocumentation -Pteamcity=true
```

Once build is finished - API reference is available in `build/documentation/kotlinlang` directory.
