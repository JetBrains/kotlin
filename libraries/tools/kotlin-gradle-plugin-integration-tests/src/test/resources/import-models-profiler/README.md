# Task-backed Kotlin import models - profiling the POC

A [gradle-profiler](https://github.com/gradle/gradle-profiler) setup that compares three ways of serving Kotlin import
models to an IDE, without Isolated Projects and with `--configuration-cache` on:

* **baseline** - the Kotlin Gradle plugin of the parent commit: `KotlinModelBuilder` computes the models in-process on
  every request;
* **task via `forTasks`** (this commit) - one invocation: `generateKotlinImportModels` runs first
  (`BuildActionExecuter.forTasks`) and writes serialized `Result` protobuf files under `build/kotlin/import-models/`,
  then the model builder only reads them;
* **separate invocations** (this commit) - `generateKotlinImportModels` as a plain task invocation, then the sync as a
  second invocation that reads the already generated files.

The POC has no switch, so the baseline is the plugin built from the parent commit. Both have the same version
(`2.5.255-SNAPSHOT`); the baseline artifacts live in their own Maven repository, which the generated project prefers
(plugins and dependencies) when `-PkotlinBaseline` is passed (`*_baseline` scenarios).

The "sync" is `KotlinImportModelsAllProjectsBuildAction` (integration tests module): it requests every Kotlin import
model of every project sequentially, the way the IntelliJ IDEA counterpart (`KotlinImportModelsProvider`) does.

## Running

```bash
# baseline (parent commit) -> build/import-models-profiler/baseline-repo: all Kotlin artifacts of that version, so the
# baseline plugin never picks up artifacts of this commit from ~/.m2 (only kotlin-gradle-plugin differs today)
git checkout HEAD~1 && ./gradlew install && git checkout -
BASELINE=build/import-models-profiler/baseline-repo/org/jetbrains/kotlin
for m in ~/.m2/repository/org/jetbrains/kotlin/*/2.5.255-SNAPSHOT; do
    mkdir -p "$BASELINE/$(basename "$(dirname "$m")")" && cp -R "$m" "$BASELINE/$(basename "$(dirname "$m")")/"
done
# this commit -> ~/.m2, plus the build action classes
./gradlew install :kotlin-gradle-plugin-integration-tests:testClasses
brew install gradle-profiler

P=libraries/tools/kotlin-gradle-plugin-integration-tests/src/test/resources/import-models-profiler
$P/generate-project.sh build/import-models-profiler/project 30      # 30 JVM modules, baseline repo: ../baseline-repo
# sanity check: the task exists only in the POC plugin, so this must print nothing
(cd build/import-models-profiler/project && gradle -PkotlinBaseline tasks --all -q | grep generateKotlinImportModels)
$P/run-profiler.sh build/import-models-profiler/project --warmups 5 --iterations 10   # all 12 scenarios
$P/run-profiler.sh build/import-models-profiler/project no_change_task no_change_sync # selected scenarios
```

Everything after the project directory goes to gradle-profiler; `run-profiler.sh` only adds defaults for missing
arguments (`--benchmark`, `--gradle-version 9.7.0`, `--scenario-file`, `--output-dir`). Results: `benchmark.csv` /
`benchmark.html`; `profile.log` has the build output (`Reusing configuration cache.`, `UP-TO-DATE`, and the
`Kotlin import models: N projects, M compilation units, K models` line printed by the action).

## Results (2026-09-18)

Gradle 9.7.0, 30 JVM modules (60 compilation units, 240 models per sync; each module `api`-depends on its predecessor
and on ktor/spring-boot/jackson/kotlinx), Apple Silicon laptop, warm daemon, `--warmups 5 --iterations 10`, median in
ms. The "separate" total is the sum of the two invocations.

| change between builds | baseline | task via `forTasks` | separate: task | separate: sync | separate: total |
|---|---:|---:|---:|---:|---:|
| none | 457 | 387 | **56** | 259 | 315 |
| source file (`Jvm1.kt`) | 460 | 368 | **56** | 252 | 308 |
| build script (`jvm-1/build.gradle.kts`) | 591 | 530 | 632 | 336 | 968 |

Raw data: `benchmark.csv` of the run (stdev 7 ms for the task-only columns, 15-55 ms for everything that configures;
the first measured build of each series is still 50-150 ms slower than the last, so treat gaps below ~50 ms as noise).

What `profile.log` shows for every measured build:

* **baseline**, **task via `forTasks`**, **separate: sync** - no configuration cache message at all; all 30 projects
  are configured (`CONFIGURE SUCCESSFUL in ~250-500ms`, or `BUILD SUCCESSFUL` with `30 actionable tasks: 30 up-to-date`
  for `forTasks`). Gradle puts any model-building invocation without Isolated Projects into "vintage" mode regardless of
  `--configuration-cache` and `forTasks(...)` (`BuildModelParametersProvider`: "CC by itself does not yet support
  caching models or caching of the work graph that runs before model building"). With `forTasks` the tasks' `models`
  input is evaluated once per project for the up-to-date check, i.e. the models are still computed on every sync.
* **separate: task**, no change / source change - `Reusing configuration cache.`, no project configured, no model
  computed, 30 tasks `UP-TO-DATE`.
* **separate: task**, build script change - `Calculating task graph as configuration cache cannot be reused because
  file 'jvm-1/build.gradle.kts' has changed`: all projects configured, all models recomputed for the input hash, new
  entry stored; the tasks are still `UP-TO-DATE` because the mutation does not change the models.

### Takeaways

**The same model computation is a 56 ms configuration cache hit when invoked as a task and a ~255 ms full
configuration when requested as a model of the already serialized files: without Isolated Projects, the configuration
cache applies to task invocations but not to model-building ones.** If model requests were cached the way task
invocations are, a warm sync would drop from ~450 ms to the order of the "separate: task" column.

1. **Task via `forTasks` saves at most the in-process model computation** (70-90 ms here, ~0-20 % across runs). Both
   variants configure all 30 projects on every sync; the task only moves the model computation from 240 sequential
   builder calls into 30 task up-to-date checks (which run in parallel with `org.gradle.parallel`). Nothing is cached.
2. **A source change costs the same as no change** for every variant: it is neither a configuration input nor a task
   input (the task has no file inputs; the models hold paths, not contents).
3. **A build script change makes the separate invocations the worst option**: the task loses its CC hit and reconfigures
   everything (632 ms, incl. storing a new entry), then the sync reconfigures everything again (336 ms) - configured
   twice, 968 ms versus 591 ms baseline. Without Isolated Projects the CC entry is all-or-nothing, so one changed
   script reconfigures all 30 projects.
4. The separate "sync" is still ~255 ms for 240 file reads: that is pure configuration cost, paid by every model
   request no matter how cheap the model builder is.

Caveat: the POC task's single input is the serialized model map, evaluated at CC store time (or in the up-to-date
check), so on a CC miss all models of a project are recomputed to produce the input hash - the incremental build only
saves rewriting the files, never the computation.
