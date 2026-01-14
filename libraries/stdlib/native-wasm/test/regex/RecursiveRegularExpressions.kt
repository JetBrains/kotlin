/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.text.regex

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecursiveRegularExpressions {
    @Test
    fun eagerRegex() {
        val match = Regex("([ab])*").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun repeatedNewLine() {
        val res = Regex("(^$)*LINE", RegexOption.MULTILINE).findAll("\n\n\nLINE").toList()
        assertEquals(1, res.size)
        assertEquals("LINE", res[0].value)
    }

    @Test
    fun repeatedEmptySet() {
        assertTrue(Regex("(){3,5}S").matches("S"))
        assertTrue(Regex("(){3,}S").matches("S"))
        assertTrue(Regex("()+S").matches("S"))
        assertTrue(Regex("()?S").matches("S"))
        assertTrue(Regex("()*S").matches("S"))
    }

    @Test
    fun possessiveRegex() {
        val match = Regex("([ab])*+").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun reluctantRegex() {
        val match = Regex("([ab])*?").matchEntire("a".repeat(100_000) + "b")!!
        assertEquals("b", match.groupValues[1])
    }

    @Test
    fun repeatedReluctantEmptySet() {
        assertTrue(Regex("(){3,5}?S").matches("S"))
        assertTrue(Regex("(){3,}?S").matches("S"))
        assertTrue(Regex("()+?S").matches("S"))
        assertTrue(Regex("()??S").matches("S"))
        assertTrue(Regex("()*?S").matches("S"))
    }

    @Test
    fun repeatedReluctantNewLine() {
        val res = Regex("(^$)*?LINE", RegexOption.MULTILINE).findAll("\n\n\nLINE").toList()
        assertEquals(1, res.size)
        assertEquals("LINE", res[0].value)
    }

    @Test
    fun reluctantNonTrivialGroup() {
        val re = Regex("(aa|a)+?a")
        assertTrue(re.matches("aa"))
    }

    @Test
    fun kt78089() {
        val reg = """^\Qmultiplatform-app://androidx.navigation/org.company.app.Screen\E(${'$'}|(\?(.)*)|(#(.)*))""".toRegex()
        assertTrue(reg.matches("""multiplatform-app://androidx.navigation/org.company.app.Screen?p=%7B%22message%22%3A%22%3Cdiv%3E%3Cstrong%3EMonday%2016%20to%20Friday%2020%20December%3C%2Fstrong%3E%20%3C%2Fdiv%3E%5Cn%3Cul%3E%5Cn%3Cli%3EDue%20to%20trackwork%20at%20Central%2C%20platforms%201-12%2C%20trains%20may%20run%20to%20a%20reduced%20frequency%20or%20changed%20timetable%20and%20stopping%20pattern.%20%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3E%3Cstrong%3EFriday%20from%2011.30pm%3C%2Fstrong%3E%2C%20trains%20run%20between%20Blue%20Mountains%20Line%20stations%20and%20the%20City%20Circle.%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3EReplacement%20buses%20for%20trackwork%20may%20be%20affected%20by%20driver%20shortages.%20We%20are%20doing%20all%20we%20can%20to%20minimise%20the%20impact%20to%20your%20journey%20and%20provide%20a%20safe%20service%2C%20however%20cancellations%20or%20delays%20may%20occur.%3C%2Fli%3E%5Cn%3Cli%3E%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftrip%23%2Ftrip%5C%22%3EPlan%20your%20trip%3C%2Fa%3E%20before%20you%20travel%20for%20up-to-date%20real%20time%20information.%20You%20can%20also%20subscribe%20to%20%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftravel-info%2Fways-to-get-around%2Ftrain%2Fabout-trackwork%23subscribe%5C%22%3Eplanned%20trackwork%20alerts%3C%2Fa%3E.%3C%2Fli%3E%5Cn%3C%2Ful%3E%22%7D&p=%7B%22message%22%3A%22%3Cdiv%3E%3Cstrong%3EMonday%2016%20to%20Friday%2020%20December%3C%2Fstrong%3E%20%3C%2Fdiv%3E%5Cn%3Cul%3E%5Cn%3Cli%3EDue%20to%20trackwork%20at%20Central%2C%20platforms%201-12%2C%20trains%20may%20run%20to%20a%20reduced%20frequency%20or%20changed%20timetable%20and%20stopping%20pattern.%20%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3E%3Cstrong%3EFriday%20from%2011.30pm%3C%2Fstrong%3E%2C%20trains%20run%20between%20Blue%20Mountains%20Line%20stations%20and%20the%20City%20Circle.%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3EReplacement%20buses%20for%20trackwork%20may%20be%20affected%20by%20driver%20shortages.%20We%20are%20doing%20all%20we%20can%20to%20minimise%20the%20impact%20to%20your%20journey%20and%20provide%20a%20safe%20service%2C%20however%20cancellations%20or%20delays%20may%20occur.%3C%2Fli%3E%5Cn%3Cli%3E%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftrip%23%2Ftrip%5C%22%3EPlan%20your%20trip%3C%2Fa%3E%20before%20you%20travel%20for%20up-to-date%20real%20time%20information.%20You%20can%20also%20subscribe%20to%20%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftravel-info%2Fways-to-get-around%2Ftrain%2Fabout-trackwork%23subscribe%5C%22%3Eplanned%20trackwork%20alerts%3C%2Fa%3E.%3C%2Fli%3E%5Cn%3C%2Ful%3E%22%7D&p=%7B%22message%22%3A%22%3Cdiv%3E%3Cstrong%3EMonday%2016%20to%20Friday%2020%20December%3C%2Fstrong%3E%20%3C%2Fdiv%3E%5Cn%3Cul%3E%5Cn%3Cli%3EDue%20to%20trackwork%20at%20Central%2C%20platforms%201-12%2C%20trains%20may%20run%20to%20a%20reduced%20frequency%20or%20changed%20timetable%20and%20stopping%20pattern.%20%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3E%3Cstrong%3EFriday%20from%2011.30pm%3C%2Fstrong%3E%2C%20trains%20run%20between%20Blue%20Mountains%20Line%20stations%20and%20the%20City%20Circle.%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3EReplacement%20buses%20for%20trackwork%20may%20be%20affected%20by%20driver%20shortages.%20We%20are%20doing%20all%20we%20can%20to%20minimise%20the%20impact%20to%20your%20journey%20and%20provide%20a%20safe%20service%2C%20however%20cancellations%20or%20delays%20may%20occur.%3C%2Fli%3E%5Cn%3Cli%3E%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftrip%23%2Ftrip%5C%22%3EPlan%20your%20trip%3C%2Fa%3E%20before%20you%20travel%20for%20up-to-date%20real%20time%20information.%20You%20can%20also%20subscribe%20to%20%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftravel-info%2Fways-to-get-around%2Ftrain%2Fabout-trackwork%23subscribe%5C%22%3Eplanned%20trackwork%20alerts%3C%2Fa%3E.%3C%2Fli%3E%5Cn%3C%2Ful%3E%22%7D&p=%7B%22message%22%3A%22%3Cdiv%3E%3Cstrong%3EMonday%2016%20to%20Friday%2020%20December%3C%2Fstrong%3E%20%3C%2Fdiv%3E%5Cn%3Cul%3E%5Cn%3Cli%3EDue%20to%20trackwork%20at%20Central%2C%20platforms%201-12%2C%20trains%20may%20run%20to%20a%20reduced%20frequency%20or%20changed%20timetable%20and%20stopping%20pattern.%20%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3E%3Cstrong%3EFriday%20from%2011.30pm%3C%2Fstrong%3E%2C%20trains%20run%20between%20Blue%20Mountains%20Line%20stations%20and%20the%20City%20Circle.%3Cbr%20%2F%3E%3Cbr%20%2F%3E%3C%2Fli%3E%5Cn%3Cli%3EReplacement%20buses%20for%20trackwork%20may%20be%20affected%20by%20driver%20shortages.%20We%20are%20doing%20all%20we%20can%20to%20minimise%20the%20impact%20to%20your%20journey%20and%20provide%20a%20safe%20service%2C%20however%20cancellations%20or%20delays%20may%20occur.%3C%2Fli%3E%5Cn%3Cli%3E%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftrip%23%2Ftrip%5C%22%3EPlan%20your%20trip%3C%2Fa%3E%20before%20you%20travel%20for%20up-to-date%20real%20time%20information.%20You%20can%20also%20subscribe%20to%20%3Ca%20href%3D%5C%22https%3A%2F%2Ftransportnsw.info%2Ftravel-info%2Fways-to-get-around%2Ftrain%2Fabout-trackwork%23subscribe%5C%22%3Eplanned%20trackwork%20alerts%3C%2Fa%3E.%3C%2Fli%3E%5Cn%3C%2Ful%3E%22%7D"""))
    }
}
