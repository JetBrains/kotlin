package language.scala

import kotlin. *
import kotlin.test.assertEquals

import junit.framework.TestCase
import kotlin.util.arrayList

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
        fun foo(name: String?): String {

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
            return when (name) {
                is String -> {
                    name.trim().toUpperCase()
                }
                else -> {
                    "No name value"
                }
            }
        }

        assertEquals("No name value", foo(null))
        assertEquals("BAR", foo("BAR"))
        assertEquals("BAR", foo("  bar "))

        println("foo(null) = ${foo(null)}")
        println("foo(\"  bar   \") = ${foo("  bar   ")}")
    }

    fun testPatternMatchingUsingIf() {
        fun foo2(name: String?): String {

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
            return if (name != null)  {
                name.trim().toUpperCase()
            } else {
                "No name value"
            }
        }

        assertEquals("No name value", foo2(null))
        assertEquals("BAR", foo2("BAR"))
        assertEquals("BAR", foo2("  bar "))

        println("foo2(null) = ${foo2(null)}")
        println("foo2(\"  bar   \") = ${foo2("  bar   ")}")
    }


    fun testFunctionComposition() {
        /* Scala:

        val name:Option[String] = request.getParameter("name")
        val upper = name map { _.trim } filter { _.length != 0 } map { _.toUpperCase }
        println(upper.getOrElse(""))

        */

        /** TODO
            The following would work if we implemented the filter/map methods on T?

        fun foo(name: String?): String {
            val upper = name.map<String,String>{ it.trim() }.filter{ it.length != 0 }.map { it.toUpperCase() }
            return upper ?: ""
        }

        assertEquals("", foo(null))
        assertEquals("", foo("  "))
        assertEquals("BAR", foo("  bar "))
        */

        // TODO...
    }

    fun testCompositionWithFor() {
        fun foo3(name: String?): String {
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
            if (name != null) {
                val trimmed = name.trim()
                if (trimmed.length() != 0) {
                    return trimmed.toUpperCase()
                }
            }
            return ""
        }
    }


}