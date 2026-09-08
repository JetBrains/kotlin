val extension = extensions.create("projectTests", ProjectTestsExtension::class)

// Aggregate task for build related checks
tasks.register("checkBuild")

configureTestRuntime()
configureTestSemaphore()
configureTestCaching()
configureTestTaskDisabling()
configureTestInventory()
configureTestRetries()
configureTestCacheDisabling()
configureDefaultJvmArguments()
configureTestInputs()
configureTestMuting()
