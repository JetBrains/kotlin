// EXPECTED_REACHABLE_NODES: 491
class Slot() {
    var vitality: Int = 10000

    fun increaseVitality(delta: Int) {
        vitality = vitality + delta
        if (vitality > 65535) vitality = 65535;
    }
}

fun box(): String {
    val s = Slot()
    s.increaseVitality(1000)
    if (s.vitality == 11000) return "OK" else return "fail"
}
