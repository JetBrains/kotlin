package coffee

open class ElectricHeater : Heater {
    override var isHot: Boolean = false

    override fun on() {
        println("~ ~ ~ heating ~ ~ ~")
        this.isHot = true
    }

    override fun off() {
        this.isHot = false
    }
}
