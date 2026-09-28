
@file:DependsOn("junit:junit:4.11")

import org.junit.Assert

fun checkedTrue(): String {
    Assert.assertTrue(true)
    return "Ok"
}
