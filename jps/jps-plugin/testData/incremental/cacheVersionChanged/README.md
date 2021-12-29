The same tests are run by both `AbstractIncrementalCacheVersionChangedTest` and `AbstractDataContainerVersionChangedTest`.

## AbstractIncrementalCacheVersionChangedTest

Tests for changing targets **local** cache version for each modification step.

Individual tests can:
- can skip changing the version at any step by adding files like `module1_do-not-change-cache-versions.new.2`
- clear has-kotlin flag by adding files like `module1_clear-has-kotlin.new.1`

## AbstractDataContainerVersionChangedTest

Tests for changing version of **global** lookups cache for each modification step.
Note that `build.log` file for this test case is named as `data-container-version-build.log`.

Individual tests can can skip changing the version at any step by adding files like `module1_do-not-change-cache-versions.new.2`.