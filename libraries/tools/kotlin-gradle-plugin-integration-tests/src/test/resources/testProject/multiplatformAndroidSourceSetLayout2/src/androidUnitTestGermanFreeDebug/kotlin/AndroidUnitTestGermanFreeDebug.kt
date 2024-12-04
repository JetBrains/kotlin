import androidx.test.core.app.ApplicationProvider
import okio.Path.Companion.toPath
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidUnitTestGermanFreeDebug {
    @Test
    fun someTest() {
        "".toPath()
        commonMainExpect()
        CommonMain.invoke()
        AndroidMain.invoke()
        AndroidMain.invoke(ApplicationProvider.getApplicationContext())
        CommonTest().someTest()
    }
}
