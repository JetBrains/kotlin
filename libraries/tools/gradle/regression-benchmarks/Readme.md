## Description

Contains build regression benchmark scripts for different Kotlin projects.  

Such benchmarks comparing different build scenarios between last stable Kotlin release and current-in-progress release helping 
to identify build speed regressions with minimal user-project modifications.

All scripts should run via related Gradle task which could be found in "Gradle Regression Benchmark tasks" task group.

### Adding benchmark for new user-project

_**Before adding a new project, consider checking existing one** - they possibly may cover the case you want to check. New project
should provide unique build setup/codebase that is not covered by existing projects._

All scripts are using infrastructure provided by [template](../regression-benchmark-templates/Readme.md).

- Add new script file in `benchmarkScripts/` directory
- Add new Gradle task to run the script
- Add required `@file:BenchmarkProject` annotation and few steps that will download profiler plus project itself
- Inspect user-project and create required git patches to change Kotlin version in the project - add changes, test it 
and use `git diff --no-color > name.patch` command. Put created patches into `benchmarkScripts/files` directory.
- Update current Kotlin version patch to use Kotlin version passed as argument to script and available as `currentKotlinVersion`
- Write benchmark scenarios and run benchmark with `dryRun = true` flag
- Add final changes to script, probably convert it to use `runAllBenchmarks()` function
