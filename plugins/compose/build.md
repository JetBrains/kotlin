Kotlin repository is preconfigured to search for Compose modules in plugins/compose-compiler-plugin directory, 
so once compose is being checked out to the folder, it should start working immediately.

The command for check out in the Kotlin repository:

```bash
git clone ssh://git@git.jetbrains.space/kotlin/kotlin/compatible-compose.git plugins/compose-compiler-plugin
```

Run tests in compose modules (should be fast):

```bash
cd ../../
./gradlew -p plugins/compose-compiler-plugin -Pkotlin.build.disable.werror=true -Pkotlin.build.compose.publish.enabled=true cleanTest test
```

Experimental publishing:

```bash
cd ../../
./gradlew -p plugins/compose-compiler-plugin -Pkotlin.build.disable.werror=true -Pkotlin.build.compose.publish.enabled=true publishToMavenLocal
```
