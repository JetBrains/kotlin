package tests

import org.junit.*
import org.junit.runners.AllTests
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.util.List

/*
[RunWith(javaClass<Parameterized>())]
public class SeleniumTest(val id: String) {

    Test public fun checkQUnitTest(): Unit {
        println("Testing $id")
    }

    class object {
        Parameters public fun findSeleniumUris(): List<String> {
            return arrayList("a", "b")
        }
    }
}
*/