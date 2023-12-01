import kt43265.*
import kotlin.test.*

@kotlinx.cinterop.ExperimentalForeignApi
fun main() {
    assertEquals(bcm2835FunctionSelect.BCM2835_GPIO_FSEL_ALT3, bcm2835FunctionSelect.BCM2835_GPIO_FSEL_MASK)
}