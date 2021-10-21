import kotlinx.cinterop.pointed
import simple.simpleInterop
import withPosix.getMyStructPointer
import kotlin.test.Test

class P3IosTest {

    @Test
    fun runTest() {
        simpleInterop()
        getMyStructPointer()?.pointed?.appleOnlyProperty
        getMyStructPointer()?.pointed?.iosOnlyProperty

        NativeMain.structFromPosix
        NativeMain.structPointerFromPosix
        NativeMain.simple

        P2NativeMain.structFromPosix
        P2NativeMain.structPointerFromPosix
        P2NativeMain.simple

        AppleAndLinuxMain.MyStruct.posixProperty
        P2AppleAndLinuxMain.MyStruct.posixProperty

        AppleMain.MyStruct.appleOnlyProperty
        P2AppleMain.MyStruct.appleOnlyProperty

        IosMain.MyStruct.iosOnly
        P3IosMain.MyStruct.iosOnly
    }
}