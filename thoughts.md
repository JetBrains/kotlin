Optimization ides/hypotheses:
1. Merge local-scoped lowerings together
2. Lowering specific incremental implementation
3. Merge plugins passes and validate only after them all
4. IR serialization/deserialization relies on resolve - meanwhile it is done via FIR - maybe keep in-memory
5. IR serialization can write directly on disk, instead of memory - if there is no consumer
6. Memory-mapped protobuf objects

Notes:
1. Lots of time spent on building/configuring/Gradle-fighting/etc. 
   Actual profiling + implementation took half or less of total time spent.
2. Konan profiling implies Gradle daemon profiling, as it is run in-process. Otherwise, JVM would not be "hot".
3. Existing compiler perf reports can only tell absolute numbers, but no comparisons, traces analysis, relative numbers, etc.
4. Reasonable cache-misses profiling (using async) can be "easily" performed only on Linux (thus, for Mac, on Linux VM).

Profiling notes:
1. "runAllLowerings" is quite "heavy" – it takes ~40% of all the time.
   1. Lots of lowerings are "local" - changing IrTree within some IrClass/IrBody/IrExpression scope.
   2. Small experiment can be conducted to check if grouping can help.
   3. There are possibilities for generalized batch lowering invocations
   4. Also, most lowerings are operating on a limited subset of nodes (say, string concat, call with some limitations, etc.) 
2. CPU is not loaded 100% all the time – it may be that there is a place for parallelization.
   1. Can be possibly be done for one lowering only or for several lowerings which are inter-independant.
3. "Linking" takes 20% or less total binary building time. 
   It is not obvious what deserialized IR part is actually used.
   It may take a lot of work to determine that.
   Additional research experiments should be conducted first (for instance, symbol tracking or nodes marking).
   Not considering as a optimization candidate for now.
4. Allocations are often noticed for building a distinct set of objects. Also for creating ArrayLists.
   Both may be considered as optimization points.
5. A lot of virtual invocations (quite heavy class hierarchies). Maybe class hierarchies can be reduced
   so JVM can inline vtable/itable calls.
6. Hot stuff noticed:
   1. FakeOverrides querying (IrFakeOverrideUtils.resolveFakeOverride) is called very often and is quite heavy. Maybe not cached?
   2. StringConcatenationLowering <init> takes considerable amount of time: `appendFunctions` init is a bit clanky.
   3. Konan configuration init (NativeSecondStageCompilationConfig) is quite noticeable (4% time total) - most time taken is for hashing participating klibs.
   4. `Autoboxing` takes ~2% total which is considerably greater than other lowerings.
7. Hadn't noticed any IO bottlenecks (but maybe I am wrong, hadn't investigated this way much).

Experiment: Build an index for 3 StringConcat-related lowerings and run all of them by index.

Index implementation notes:
1. There is an improvement that is better for StringConcatenationLowering than for FlattenStringConcatenationLowering.
   The latter operates on IrCall, which are numerous, so better index filtering (and structure) is required.
2. Index is not well suited for transforming Ir tree, because of parent absence in nodes.
3. Reindexing nodes while changing the tree can be time-consuming - for fast reindexing index should support fast parent-child queues.
