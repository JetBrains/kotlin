import java.awt.image.AreaAveragingScaleFilter

class TestInterfaceStaticFieldReference(width: Int, height: Int) : AreaAveragingScaleFilter(width, height) {
    fun test() {
        println(TOPDOWNLEFTRIGHT)
    }
}