fun box(stepId: Int, isWasm: Boolean): String {
    val d = Demo(15)
    when (stepId) {
        0, 2 -> {
            if (!testEquals(d, Demo(15))) return "Fail equals"
            if (testHashCode(d) != Demo(15).hashCode()) return "Fail hashCode"
            if (testToString(d) != "Demo(x=15)") return "Fail toString"
        }
        1, 3-> {
            if (testEquals(d, Demo(15))) return "Fail equals"
            if (testHashCode(d) == Demo(15).hashCode()) return "Fail hashCode"
            if (testToString(d) != "Simple Demo") return "Fail toString"
        }
        else -> return "Unknown"
    }

    return "OK"
}
