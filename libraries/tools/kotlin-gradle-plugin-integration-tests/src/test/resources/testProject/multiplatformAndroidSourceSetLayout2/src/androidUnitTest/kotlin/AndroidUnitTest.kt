import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidUnitTest {
    @Test
    fun someTest() {
        commonMainExpect()
        CommonMain.invoke()
        AndroidMain.invoke()
        AndroidMain.invoke(ApplicationProvider.getApplicationContext())
        CommonTest().someTest()
    }
}
