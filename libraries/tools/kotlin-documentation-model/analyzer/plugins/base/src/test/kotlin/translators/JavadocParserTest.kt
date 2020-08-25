package translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.DModule
import org.jetbrains.dokka.model.childrenOfType
import org.jetbrains.dokka.model.doc.*
import org.jetbrains.dokka.model.firstChildOfType
import org.jetbrains.dokka.testApi.testRunner.AbstractCoreTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import utils.text

class JavadocParserTest : AbstractCoreTest() {

    private fun performJavadocTest(testOperation: (DModule) -> Unit) {
        val configuration = dokkaConfiguration {
            sourceSets {
                sourceSet {
                    sourceRoots = listOf("src/main/java")
                }
            }
        }

        testInline(
            """
            |/src/main/java/sample/Date2.java
			|/**
            | * The class <code>Date</code> represents a specific instant
            | * in time, with millisecond precision.
            | * <p>
            | * Prior to JDK&nbsp;1.1, the class <code>Date</code> had two additional
            | * functions.  It allowed the interpretation of dates as year, month, day, hour,
            | * minute, and second values.  It also allowed the formatting and parsing
            | * of date strings.  Unfortunately, the API for these functions was not
            | * amenable to internationalization.  As of JDK&nbsp;1.1, the
            | * <code>Calendar</code> class should be used to convert between dates and time
            | * fields and the <code>DateFormat</code> class should be used to format and
            | * parse date strings.
            | * The corresponding methods in <code>Date</code> are deprecated.
            | * <p>
            | * Although the <code>Date</code> class is intended to reflect
            | * coordinated universal time (UTC), it may not do so exactly,
            | * depending on the host environment of the Java Virtual Machine.
            | * Nearly all modern operating systems assume that 1&nbsp;day&nbsp;=
            | * 24&nbsp;&times;&nbsp;60&nbsp;&times;&nbsp;60&nbsp;= 86400 seconds
            | * in all cases. In UTC, however, about once every year or two there
            | * is an extra second, called a "leap second." The leap
            | * second is always added as the last second of the day, and always
            | * on December 31 or June 30. For example, the last minute of the
            | * year 1995 was 61 seconds long, thanks to an added leap second.
            | * Most computer clocks are not accurate enough to be able to reflect
            | * the leap-second distinction.
            | * <p>
            | * Some computer standards are defined in terms of Greenwich mean
            | * time (GMT), which is equivalent to universal time (UT).  GMT is
            | * the "civil" name for the standard; UT is the
            | * "scientific" name for the same standard. The
            | * distinction between UTC and UT is that UTC is based on an atomic
            | * clock and UT is based on astronomical observations, which for all
            | * practical purposes is an invisibly fine hair to split. Because the
            | * earth's rotation is not uniform (it slows down and speeds up
            | * in complicated ways), UT does not always flow uniformly. Leap
            | * seconds are introduced as needed into UTC so as to keep UTC within
            | * 0.9 seconds of UT1, which is a version of UT with certain
            | * corrections applied. There are other time and date systems as
            | * well; for example, the time scale used by the satellite-based
            | * global positioning system (GPS) is synchronized to UTC but is
            | * <i>not</i> adjusted for leap seconds. An interesting source of
            | * further information is the U.S. Naval Observatory, particularly
            | * the Directorate of Time at:
            | * <blockquote><pre>
            | *     <a href=http://tycho.usno.navy.mil>http://tycho.usno.navy.mil</a>
            | * </pre></blockquote>
            | * <p>
            | * and their definitions of "Systems of Time" at:
            | * <blockquote><pre>
            | *     <a href=http://tycho.usno.navy.mil/systime.html>http://tycho.usno.navy.mil/systime.html</a>
            | * </pre></blockquote>
            | * <p>
            | * In all methods of class <code>Date</code> that accept or return
            | * year, month, date, hours, minutes, and seconds values, the
            | * following representations are used:
            | * <ul>
            | * <li>A year <i>y</i> is represented by the integer
            | *     <i>y</i>&nbsp;<code>-&nbsp;1900</code>.
            | * <li>A month is represented by an integer from 0 to 11; 0 is January,
            | *     1 is February, and so forth; thus 11 is December.
            | * <li>A date (day of month) is represented by an integer from 1 to 31
            | *     in the usual manner.
            | * <li>An hour is represented by an integer from 0 to 23. Thus, the hour
            | *     from midnight to 1 a.m. is hour 0, and the hour from noon to 1
            | *     p.m. is hour 12.
            | * <li>A minute is represented by an integer from 0 to 59 in the usual manner.
            | * <li>A second is represented by an integer from 0 to 61; the values 60 and
            | *     61 occur only for leap seconds and even then only in Java
            | *     implementations that actually track leap seconds correctly. Because
            | *     of the manner in which leap seconds are currently introduced, it is
            | *     extremely unlikely that two leap seconds will occur in the same
            | *     minute, but this specification follows the date and time conventions
            | *     for ISO C.
            | * </ul>
            | * <pre class="prettyprint">
            | * &lt;androidx.fragment.app.FragmentContainerView
            | *        xmlns:android="http://schemas.android.com/apk/res/android"
            | *        xmlns:app="http://schemas.android.com/apk/res-auto"
            | *        android:id="@+id/fragment_container_view"
            | *        android:layout_width="match_parent"
            | *        android:layout_height="match_parent"&gt;
            | * &lt;/androidx.fragment.app.FragmentContainerView&gt;
            | * </pre>
            | * <p>
            | * In all cases, arguments given to methods for these purposes need
            | * not fall within the indicated ranges; for example, a date may be
            | * specified as January 32 and is interpreted as meaning February 1.
            | *
            | * @author  James Gosling
            | * @author  Arthur van Hoff
            | * @author  Alan Liu
            | * @see     java.text.DateFormat
            | * @see     java.util.Calendar
            | * @since   JDK1.0
            | * @apiSince 1
            | */
            |public class Date2 implements java.io.Serializable, java.lang.Cloneable, java.lang.Comparable<java.util.Date> {
            |    void x() { }
            |}
            """.trimIndent(),
            configuration
        ) {
            documentablesMergingStage = testOperation
        }
    }

    @Test
    fun `correctly parsed list`() {
        performJavadocTest { module ->
            val dateDescription = module.descriptionOf("Date2")!!
            assertEquals(6, dateDescription.firstChildOfType<Ul>().children.filterIsInstance<Li>().size)
        }
    }

    @Test
    fun `correctly parsed author tags`() {
        performJavadocTest { module ->
            val authors = module.findClasslike().documentation.values.single().childrenOfType<Author>()
            assertEquals(3, authors.size)
            assertEquals("James Gosling", authors[0].firstChildOfType<Text>().text())
            assertEquals("Arthur van Hoff", authors[1].firstChildOfType<Text>().text())
            assertEquals("Alan Liu", authors[2].firstChildOfType<Text>().text())
        }
    }

    @Test
    fun `correctly parsed see tags`() {
        performJavadocTest { module ->
            val sees = module.findClasslike().documentation.values.single().childrenOfType<See>()
            assertEquals(2, sees.size)
            assertEquals(DRI("java.text", "DateFormat"), sees[0].address)
            assertEquals("java.text.DateFormat", sees[0].name)
            assertEquals(DRI("java.util", "Calendar"), sees[1].address)
            assertEquals("java.util.Calendar", sees[1].name)
        }
    }

    @Test
    fun `correctly parsed code block`(){
        performJavadocTest { module ->
            val dateDescription = module.descriptionOf("Date2")!!
            val preTagContent = dateDescription.firstChildOfType<Pre>().firstChildOfType<Text>()
            val expectedText = """<androidx.fragment.app.FragmentContainerView
       xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:app="http://schemas.android.com/apk/res-auto"
       android:id="@+id/fragment_container_view"
       android:layout_width="match_parent"
       android:layout_height="match_parent">
</androidx.fragment.app.FragmentContainerView>""".trimIndent()
            assertEquals(expectedText.trim(), preTagContent.body.trim())
        }
    }
}
