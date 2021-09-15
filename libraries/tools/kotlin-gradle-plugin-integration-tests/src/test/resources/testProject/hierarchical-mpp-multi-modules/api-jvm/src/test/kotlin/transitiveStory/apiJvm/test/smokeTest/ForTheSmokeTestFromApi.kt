package transitiveStory.apiJvm.test.smokeTest

import org.junit.Test
import transitiveStory.apiJvm.beginning.tlAPIval
import kotlin.test.assertEquals

class KClassForTheSmokeTestFromApi {
}

class SomeTestInApiJVM {
    @Test
    fun some() {
        println("I'm simple test in `api-jvm` module")
        assertEquals(tlAPIval, 42)
    }

    // KT-33573
    @Test
    fun `function with spaces`() {}
}
