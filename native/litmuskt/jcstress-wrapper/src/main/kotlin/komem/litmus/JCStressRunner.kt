package komem.litmus

import komem.litmus.barriers.JvmCyclicBarrier
import java.nio.file.Path

/**
 * Note that this 'runner' is severely different from all others.
 */
class JCStressRunner(
    private val jcstressDirectory: Path,
    private val jcstressFreeArgs: List<String>,
) : LitmusRunner() {

    companion object {
        val DEFAULT_LITMUSKT_PARAMS = LitmusRunParams(0, 0, null, ::JvmCyclicBarrier)
    }

    override fun <S : Any> startTest(
        test: LitmusTest<S>,
        states: List<S>,
        barrierProducer: BarrierProducer,
        syncPeriod: Int,
        affinityMap: AffinityMap?
    ): () -> LitmusResult {
        throw NotImplementedError("jcstress runner should not be called like this")
    }

    override fun <S : Any> LitmusRunner.startTestParallel(
        instances: Int,
        params: LitmusRunParams,
        test: LitmusTest<S>
    ): List<() -> LitmusResult> {
        throw NotImplementedError(
            "jcstress runs tests in parallel by default; asking for parallelism explicitly is meaningless"
        )
    }

    // TODO: optimize for many tests (do not invoke jcstress many times)
    override fun <S : Any> startTest(params: LitmusRunParams, test: LitmusTest<S>): () -> LitmusResult {
        val mvn = ProcessBuilder("mvn", "install", "verify")
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        mvn.waitFor()
        if (mvn.exitValue() != 0) {
            error("mvn exited with code ${mvn.exitValue()}")
        }

        val jcsParams = if (params != DEFAULT_LITMUSKT_PARAMS) {
            arrayOf("strideSize", "${params.syncPeriod}", "strideCount", "${params.batchSize / params.syncPeriod}")
        } else emptyArray()
        val jcs = ProcessBuilder(
            "java",
            "-jar",
            "target/jcstress.jar",
            *(jcsParams + jcstressFreeArgs),
            "-t",
            test.javaClassName,
        )
            .directory(jcstressDirectory.toFile())
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()

        return {
            jcs.waitFor()

            // TODO: collect and return results
            emptyList()
        }
    }
}
