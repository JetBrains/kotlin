package org.jetbrains.kotlin.gradle.org.jetbrains.kotlin.gradle.targets.js.internal

import org.jetbrains.kotlin.gradle.targets.js.internal.RewriteSourceMapFilterReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.Reader
import java.io.StringReader

class RewriteSourceMapFilterReaderTest {
    @Test
    fun testShort() {
        // input json should fit in buffer (1024)
        doTest(1)
    }

    @Test
    fun testLong() {
        doTest(100)
    }

    @Test
    fun testVeryLong() {
        doTest(1000)
    }

    @Test
    fun testPrologSplitByReads() {
        // Test the case when prolog contents is splitted between reads.
        // Like, one buffer ends with `],"sourc`, and the other starts with `esContent"`
        val prolog = sample("", "").substringBefore(RewriteSourceMapFilterReader.PROLOG_END)
        doTest(1, addToProlog = "-".repeat(1024 - prolog.length - "],\"sourc".length))
    }

    private fun sample(mappings: String, addToProlog: String): String {
        //language=JSON
        return """{"version":3,"file":"single-platform${addToProlog}.js","sources":["../../../../src/main/kotlin/main.kt"],"sourcesContent":[null],"names":[],"mappings":"$mappings"}"""
    }

    private fun doTest(repeatMappings: Int, addToProlog: String = "") {
        val mappings0 =
            ";;;;;;;;;;;;;;;;;;;;;;;;;IAQc,c;EAAA,C;;IAQc,O;IACJ,W;EAAA,C;;IAFA,aAAE,wBAAF,kBAA4B,gCAA5B," +
                    "C;IAGJ,W;EAAA,C;;IAJA,uBAAI,yBAAJ,C;IAKJ,W;EAAA,C;;IANA,wBAAK,kBAAL,C;IAOJ,W;EAAA,C;;IATR," +
                    "QACqC,KAAb,WAAhB,oBAAgB,CAAa,UAAK,WAAL,CADrC,C;EAWJ,C;;;;;;;;;"
        val mappings = mappings0.repeat(repeatMappings)

        val filter = RewriteSourceMapFilterReader(
            StringReader(sample(mappings, addToProlog)),
            "/root/build/classes/kotlin/test/",
            "/root/build/test_node_modules/"
        )

        assertEquals(
            //language=JSON
            """{"version":3,"file":"single-platform${addToProlog}.js","sources":["../../src/main/kotlin/main.kt"],"sourcesContent":[null],"names":[],"mappings":"$mappings"}""",
            filter.readText()
        )
    }

    @Test
    fun testUnsupportedUnderfindProlog() {
        val filter =
            RewriteSourceMapFilterReaderMock(
                StringReader(
                    //language=JSON
                    """{"version":3,"file":"single-platform.js","sources":["../../../../src/main/kotlin/main.kt"],"names":[],"mappings":""}"""
                ),
                "/root/build/classes/kotlin/test/",
                "/root/build/test_node_modules/"
            )

        assertEquals(
            //language=JSON
            """{"version":3,"file":"single-platform.js","sources":["../../../../src/main/kotlin/main.kt"],"names":[],"mappings":""}""",
            filter.readText()
        )
        assertEquals(
            "Unsupported format. Contents should starts with `{\"version\":3,\"file\":\"...\",\"sources\":[...],\"sourcesContent\":...`. \"sourcesContent\" or \"sources\" not found",
            filter.warning
        )
    }

    @Test
    fun testUnsupportedPrologSize() {
        val repeat = "\"../../../../src/main/kotlin/main.kt\",".repeat(0xfffff / 35)
        val filter =
            RewriteSourceMapFilterReaderMock(
                StringReader(
                    //language=JSON
                    """{"version":3,"file":"single-platform.js","sources":[$repeat],"sourcesContent":[null],"names":[],"mappings":""}"""
                ),
                "/root/build/classes/kotlin/test/",
                "/root/build/test_node_modules/"
            )

        assertEquals(
            //language=JSON
            """{"version":3,"file":"single-platform.js","sources":[$repeat],"sourcesContent":[null],"names":[],"mappings":""}""",
            filter.readText()
        )
        assertEquals("Too many sources or format is not supported", filter.warning)
    }


    @Test
    fun testUnsupportedFormat() {
        val filter =
            RewriteSourceMapFilterReaderMock(
                StringReader(
                    //language=JSON
                    """{"file":"single-platform.js","version1":3,"sources":["../../../../src/main/kotlin/main.kt"],"sourcesContent":[null],"names":[],"mappings":""}"""
                ),
                "/root/build/classes/kotlin/test/",
                "/root/build/test_node_modules/"
            )

        assertEquals(
            //language=JSON
            """{"file":"single-platform.js","version1":3,"sources":["../../../../src/main/kotlin/main.kt"],"sourcesContent":[null],"names":[],"mappings":""}""",
            filter.readText()
        )
        assertEquals(
            "Unsupported format. Contents should starts with `{\"version\":3,\"file\":\"...\",\"sources\":[...],\"sourcesContent\":...`. Unknown key \"version1\" at line 1 column 40 path \$.version1 in `{\"file\":\"single-platform.js\",\"version1\":3,\"sources\":[\"../../../../src/main/kotlin/main.kt\"],\"sourcesContent\":[null],\"names\":[],\"mappings\":\"\"}",
            filter.warning
        )
    }

    @Test
    fun testInvalidJson() {
        val filter =
            RewriteSourceMapFilterReaderMock(
                StringReader("{:)],\"sourcesContent\":[null]}"),
                "/root/build/classes/kotlin/test/",
                "/root/build/test_node_modules/"
            )

        assertEquals("{:)],\"sourcesContent\":[null]}", filter.readText())
        assertEquals(
            "Unsupported format. Contents should starts with `{\"version\":3,\"file\":\"...\",\"sources\":[...],\"sourcesContent\":...`. Malformed JSON at line 1 column 3 path \$. in `{:)],\"sourcesContent\":[null]}",
            filter.warning
        )
    }

    class RewriteSourceMapFilterReaderMock(input: Reader, srcSourceRoot: String, targetSourceRoot: String) :
        RewriteSourceMapFilterReader(input) {
        init {
            setProperties(this, srcSourceRoot, targetSourceRoot)
        }

        var warning: String? = null

        override fun warnUnsupported(reason: String) {
            warning = reason
        }
    }
}

private fun RewriteSourceMapFilterReader(
    input: Reader,
    srcSourceRoot: String,
    targetSourceRoot: String
) = RewriteSourceMapFilterReader(input).also {
    setProperties(it, srcSourceRoot, targetSourceRoot)
}

private fun setProperties(
    it: RewriteSourceMapFilterReader,
    srcSourceRoot: String,
    targetSourceRoot: String
) {
    it.srcSourceRoot = srcSourceRoot
    it.targetSourceRoot = targetSourceRoot
}