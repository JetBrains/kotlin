import kotlin.test.*
import test.*

@Test
fun runTest() = assertEquals(3, useActions(object : Actions { override fun run() = 3 }))
