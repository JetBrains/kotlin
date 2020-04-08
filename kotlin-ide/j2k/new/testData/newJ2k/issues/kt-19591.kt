class TestNumberConversionsInTernary {
    fun intOrDoubleAsDouble(flag: Boolean, x: Int, y: Double): Double {
        return (if (flag) x else y).toDouble()
    }
}
