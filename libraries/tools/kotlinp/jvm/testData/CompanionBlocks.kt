// LANGUAGE: +CompanionBlocksAndExtensions

class Vector(val x: Double, val y: Double) {
    companion {
        val Zero: Vector = Vector(0.0, 0.0)
        val UnitY: Vector get() = Vector(0.0, 1.0)
        fun vectorOf(x: Double, y: Double) = Vector(x, y)
    }
}

companion val Vector.UnitX get() = Vector(1.0, 0.0)
companion fun Vector.mark(len: Int) = Vector(len.toDouble(), 1.0)
