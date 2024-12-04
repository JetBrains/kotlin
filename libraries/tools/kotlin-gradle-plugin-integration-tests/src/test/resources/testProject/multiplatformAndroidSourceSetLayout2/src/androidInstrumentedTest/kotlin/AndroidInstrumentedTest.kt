import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class AndroidInstrumentedTest {

    @Test
    fun someTest() {
        commonMainExpect()
        CommonMain.invoke()
        AndroidMain.invoke()
        AndroidMain.invoke(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext)
    }
}
