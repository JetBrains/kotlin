import kotlinx.cinterop.pointed
import simple.simpleInterop
import withPosix.getMyStructPointer
import kotlin.test.Test

class P2MacosTest {

    @Test
    fun runTest() {
        simpleInterop()
        getMyStructPointer()?.pointed?.posixProperty
        getMyStructPointer()?.pointed?.appleOnlyProperty

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

        MacosMain.MyStruct.appleOnlyProperty
        MacosMain.MyStruct.appleOnlyProperty
    }
}