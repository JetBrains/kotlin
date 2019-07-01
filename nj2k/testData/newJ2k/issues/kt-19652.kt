import java.awt.image.AreaAveragingScaleFilter
import java.awt.image.ImageConsumer

class TestInterfaceStaticFieldReference(width: Int, height: Int) :
    AreaAveragingScaleFilter(width, height) {
    fun test() {
        println(ImageConsumer.TOPDOWNLEFTRIGHT)
    }
}