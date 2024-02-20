package komem.litmus

data class LitmusRunParams(
    val batchSize: Int,
    val syncPeriod: Int,
    val affinityMap: AffinityMap?,
    val barrierProducer: BarrierProducer,
)

fun variateRunParams(
    batchSizeSchedule: List<Int>,
    affinityMapSchedule: List<AffinityMap?>,
    syncPeriodSchedule: List<Int>,
    barrierSchedule: List<BarrierProducer>
) = sequence {
    for (batchSize in batchSizeSchedule) {
        for (affinityMap in affinityMapSchedule) {
            for (syncPeriod in syncPeriodSchedule) {
                for (barrierProducer in barrierSchedule) {
                    yield(
                        LitmusRunParams(
                            batchSize = batchSize,
                            syncPeriod = syncPeriod,
                            affinityMap = affinityMap,
                            barrierProducer = barrierProducer
                        )
                    )
                }
            }
        }
    }
}
