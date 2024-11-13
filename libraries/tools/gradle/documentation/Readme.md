## Kotlinlang Gradle API reference

This project assembles API reference for Kotlin Gradle plugins to publish it to https://kotlinlang.org.

### Configuration

- `build/templates` dir is used for Kotlinlang website templates. Currently, they should be put there manually.

### Assembling

To assemble API reference run:
```shell
$ ./gradlew :gradle:documentation:dokkaKotlinlangDocumentation -Pteamcity=true
```

Once build is finished - API reference is available in `build/documentation/kotlinlang` directory.
