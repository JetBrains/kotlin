package language.scala

import kotlin.nullable.*

import junit.framework.TestCase
import kotlin.test.assertEquals

class Request(val value: String?) {
    fun getParameter(name: String): String? {
        return value
    }
}

/**
 * This test case shows how we can use T?, the Kotlin nullable type instead of Option[T] in Scala
 *
 * Its worth saying that nullable types have 2 huge benefits over Option:
 *
 *   * Already works with any Java or JVM based API which can return nulls
 *   * No extra object construction to wrap non-null values
 *
 * Examples taken from the [Scala API docs for Option](http://www.scala-lang.org/api/current/scala/Option.html)
 *
 * Note that currently the Kotlin library doesn't support the composition API of collections on T? like Scala's Option[T] does...
 */
class OptionTest: TestCase() {

    fun testPatternMatching() {
        fun foo(request: Request): String {

            /* Scala:

             val nameMaybe = request.getParameter("name")
             nameMaybe match {
               case Some(name) => {
                 name.trim.toUppercase
               }
               case None => {
                 "No name value"
               }
             }
            */

            // Kotlin version:
            val name = request.getParameter("name")
            return when (name) {
                is String -> {
                    name.trim().toUpperCase()
                }
                else -> {
                    "No name value"
                }
            }
        }

        assertEquals("No name value", foo(Request(null)))
        assertEquals("BAR", foo(Request("BAR")))
        assertEquals("BAR", foo(Request("  bar ")))

        println("foo(null) = ${foo(Request(null))}")
        println("foo(\"  bar   \") = ${foo(Request("  bar   "))}")
    }

    fun testPatternMatchingUsingIf() {
        fun foo2(request: Request): String {

            /* Scala:

             val nameMaybe = request.getParameter("name")
             nameMaybe match {
               case Some(name) => {
                 name.trim.toUppercase
               }
               case None => {
                 "No name value"
               }
             }
            */

            // Kotlin version
            val name = request.getParameter("name")
            return if (name != null)  {
                name.trim().toUpperCase()
            } else {
                "No name value"
            }
        }

        assertEquals("No name value", foo2(Request(null)))
        assertEquals("BAR", foo2(Request("BAR")))
        assertEquals("BAR", foo2(Request("  bar ")))

        println("foo2(Request(null)) = ${foo2(Request(null))}")
        println("foo2(Request(\"  bar   \")) = ${foo2(Request("  bar   "))}")
    }


    fun testFunctionComposition() {
        assertEquals("", foo3(Request(null)))
        assertEquals("", foo3(Request("  ")))
        assertEquals("BAR", foo3(Request("  bar ")))
    }

    fun foo3(request: Request): String {
        /* Scala:

        val name:Option[String] = request.getParameter("name")
        val upper = name map { _.trim } filter { _.length != 0 } map { _.toUpperCase }
        println(upper.getOrElse(""))
        */

        val name = request.getParameter("name")
        val upper = name.map<String, String>{ it.trim() }.filter{ it.length != 0 }.map<String, String>{ it.toUpperCase() }
        return upper ?: ""

        // TODO when http://youtrack.jetbrains.com/issue/KT-1145 is fixed
        // we can get rid of the unnecessary <String, String> on map
    }


    fun testCompositionWithFor() {
        fun foo4(request: Request): String {
            /* Scala:

            val upper = for {
                name <- request.getParameter("name")
                trimmed <- Some(name.trim)
                upper <- Some(trimmed.toUpperCase) if trimmed.length != 0
            } yield upper
            println(upper.getOrElse(""))
            */

            // Kotlin version
            // not as clean as we've no way to compose if statements so have
            // to cheat and use returns
            val name = request.getParameter("name")
            if (name != null) {
                val trimmed = name.trim()
                if (trimmed.length() != 0) {
                    return trimmed.toUpperCase()
                }
            }
            return ""
        }

        assertEquals("", foo4(Request(null)))
        assertEquals("", foo4(Request("")))
        assertEquals("BAR", foo4(Request("  bar  ")))
    }
}