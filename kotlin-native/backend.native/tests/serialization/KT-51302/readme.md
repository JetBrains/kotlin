At the time of writing, there is no infrastructure for writing klib
compatibility tests. Instead, we store klib directly in repository.
`kt51302_dependency` was generated from `lib.kt`
```kotlin
annotation class C
```
with the following command:
```commandline
~/.konan/kotlin-native-prebuilt-macos-x86_64-1.6.10/bin/konanc -p library lib.kt -nopack -o kt51302_dependency
```