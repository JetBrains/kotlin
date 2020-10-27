# Workers

This example shows how one could implement computation offload to other workers
(usually mapped to OS threads) and transfer data back and forth between workers.
Idea of workers is to avoid most common problems with concurrent programming, related
to simultaneous computations on the same data. Instead, each object belongs to
one or other worker's object graph, but could be disconnected from one worker
and connected to other worker. This relies on the fact that memory management
engine can ensure, that one worker doesn't keep references to certain object and
whatever it refers to, and so the object could be safely transferred to another worker.

Workers do not share mutable state, but share executable code of the program and some
immutable data, such as immutable blobs. But Kotlin objects can be transferred
between workers, as long, as they do not refer to objects, having external references.

The transfer is implemented with the function `execute()` having the following signature

    fun <T1, T2> execute(
            mode: TransferMode,
            producer: () -> T1,
            @VolatileLambda job: (T1) -> T2
    ): Future<T2>

Kotlin/Native runtime invokes `producer()` function, and makes sure object it produces
have a property, that no external references to subgraph rooted by this object, exists.
If property doesn't hold, either (depending on `mode` argument) exception is being thrown
or program may crash unexpectedly.
 Then, pointer to stateless lambda `job` along with the stable pointer to parameter object
is being added to the target worker's queue, and `Future` object matching to the query
is being created. Once worker peeks the job from the queue, it executes stateless lambda
with object provided, and stores stable pointer to result in future's data. Whenever
future is being consumed, object is passed to the consumer's callback.

This particular example starts several workers, and gives them some computational jobs.
Then it continues execution, and waits on future objects encapsulating the
computation results. Afterwards, worker execution termination is requested with the
`requestTermination()` operation.

To build use `../gradlew assemble`.

To run use `../gradlew runReleaseExecutableWorkers` or execute the program directly:

    ./build/bin/workers/main/release/executable/workers.kexe
