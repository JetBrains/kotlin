@rem Executing Gradle build

gradlew.bat --info -Pkotlin_version=1.9.255-SNAPSHOT --warning-mode=fail -Dorg.gradle.unsafe.configuration-cache=false -Dorg.gradle.unsafe.configuration-cache-problems=fail --parallel --max-workers=4 --watch-fs --no-build-cache -Pkotlin.native.cacheKind=none -Pkotlin.js.compiler.nowarn=true -Ptest_fixes_version=1.9.255-SNAPSHOT -Pkotlin_performance_profile_force_validation=true -Pkotlin.daemon.useFallbackStrategy=false -Pkotlin.internal.verboseDiagnostics=true %* 
