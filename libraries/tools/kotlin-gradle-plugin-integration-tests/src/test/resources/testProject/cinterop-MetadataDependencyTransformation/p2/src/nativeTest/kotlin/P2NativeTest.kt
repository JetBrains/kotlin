import kotlinx.cinterop.pointed
import simple.simpleInterop
import withPosix.getMyStructPointer
import kotlin.test.Test

class P2NativeTest {

    @Test
    fun runTest() {
        simpleInterop()
        getMyStructPointer()?.pointed?.posixProperty

        NativeMain.structFromPosix
        NativeMain.structPointerFromPosix
        NativeMain.simple

        P2NativeMain.structFromPosix
        P2NativeMain.structPointerFromPosix
        P2NativeMain.simple
    }
}