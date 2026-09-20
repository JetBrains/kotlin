# Test Inventory Fixture

The tests in this module exist to be *recorded and replayed*, not to check anything themselves.

`project-tests-convention` records every test task's tests into `test-executions.json`, and replays
them as TeamCity service messages when the task is later served from the build cache. The functional
tests of that feature - `TestBuildCacheTeamCityCompatibilityFunctionalTest` in
`repo/gradle-build-conventions/project-tests-convention` - run this module's test task and assert both
the recording and the replay in full, so the tests here are chosen to cover the shapes a recording can
take: a plain test, a test in a nested class, a parameterized test, an ignored one, and a failing one.

The module has a second test task, `secondTest`, which runs `SecondTestInventoryFixtureTest` and
nothing else, while `test` runs everything else. One build service replays every test task of a
build, each under its own flow, and telling the two apart takes a build with two of them in it.

That is why they must not be changed casually: their names, and the order they run in, are asserted
verbatim. The failing one only fails when `-PtestInventoryFixture.failing=true` is passed, so that an
ordinary run of this module is green.
