package samples.text

import samples.*
import kotlin.test.*
import java.util.*

class Strings {

    @Sample
    fun capitalize() {
        assertPrints("abcd".capitalize(), "Abcd")
        assertPrints("Abcd".capitalize(), "Abcd")
    }

    @Sample
    fun decapitalize() {
        assertPrints("abcd".decapitalize(), "abcd")
        assertPrints("Abcd".decapitalize(), "abcd")
    }

    @Sample
    fun repeat() {
        assertPrints("Word".repeat(4), "WordWordWordWord")
        assertPrints("Word".repeat(0), "")
    }

    @Sample
    fun trimIndent() {
        val withoutIndent =
                """
                    ABC
                    123
                    456
                """.trimIndent()
        assertPrints(withoutIndent, "ABC\n123\n456")
    }

    @Sample
    fun trimMargin() {
        val withoutMargin1 = """ABC
                        |123
                        |456""".trimMargin()
        assertPrints(withoutMargin1, "ABC\n123\n456")

        val withoutMargin2 = """
            #XYZ
            #foo
            #bar
        """.trimMargin("#")
        assertPrints(withoutMargin2, "XYZ\nfoo\nbar")
    }

    @Sample
    fun chunkedTransform() {
        val codonTable = mapOf("ATT" to "Isoleucine", "CAA" to "Glutamine", "CGC" to "Arginine", "GGC" to "Glycine")
        val dnaFragment = "ATTCGCGGCCGCCAA"

        val proteins = dnaFragment.chunked(3) { codon: CharSequence -> codonTable[codon.toString()] ?: error("Unknown codon") }

        assertPrints(proteins, "[Isoleucine, Arginine, Glycine, Arginine, Glutamine]")
    }

    @Sample
    fun chunkedTransformToSequence() {
        val codonTable = mapOf("ATT" to "Isoleucine", "CAA" to "Glutamine", "CGC" to "Arginine", "GGC" to "Glycine")
        val dnaFragment = "ATTCGCGGCCGCCAACGG"

        val proteins = dnaFragment.chunkedSequence(3) { codon: CharSequence -> codonTable[codon.toString()] ?: error("Unknown codon") }

        // sequence is evaluated lazily, so that unknown codon is not reached
        assertPrints(proteins.take(5).toList(), "[Isoleucine, Arginine, Glycine, Arginine, Glutamine]")
    }

    @Sample
    fun stringToByteArray() {
        val charset = Charsets.UTF_8
        val byteArray = "Hello".toByteArray(charset)
        assertPrints(byteArray.contentToString(), "[72, 101, 108, 108, 111]")
        assertPrints(byteArray.toString(charset), "Hello")
    }


    //Indices is IntRange
    @Sample
    fun slice() {
        assertPrints("abcdef".slice(IntRange(1,2)),"bc")
        assertPrints("abcdef".slice(IntRange(0,0)),"a")
    }

    //Without the startIndex
    @Sample
    fun startsWith(){
        assertPrints("abcd".startsWith("ab",true),"true")
        assertPrints("abcd".startsWith("Ab",true),"true")
        assertPrints("Abcd".startsWith("Ab",false),"true")
        assertPrints("abcd".startsWith("ab",false),"true")

    }

    //With the startIndex
    @Sample
    fun startsWithStartIndex(){
        assertPrints("abcd".startsWith("B",1,true),"true")
        assertPrints("abcd".startsWith("c",0,true),"false")
        assertPrints("abcd".startsWith("Ab",0,true),"true")
        assertPrints("Abcd".startsWith("Ab",0,false),"true")
        assertPrints("abcd".startsWith("c",2,false),"true")
        assertPrints("abcd".startsWith("C",2,false),"false")
    }

    //delimiter: Char

    @Sample
    fun substringBefore(){
        assertPrints("abcd".substringBefore("c"),"ab")
        assertPrints("abcd".substringBefore("8"),"abcd")
        assertPrints("kotlinot".substringBefore("ot"),"k")
        assertPrints("kotlin".substringBefore('k'),"")

    }

    @Sample
    fun substringAfter(){
        assertPrints("abcd".substringAfter("c"),"d")
        assertPrints("abcd".substringAfter("8"),"abcd")
        assertPrints("kotlinot".substringAfter("ot"),"linot")
        assertPrints("kotlin".substringAfter('o'),"tlin")

    }

    @Sample
    fun substringAfterLast(){
        assertPrints("abcd".substringAfterLast("c"),"d")
        assertPrints("abcd".substringAfterLast("8"),"abcd")
        assertPrints("kotlinot".substringAfterLast("o"),"t")
        assertPrints("kotlin".substringAfterLast('o'),"tlin")
    }

    @Sample
    fun toBoolean(){
        assertPrints("true".toBoolean(),"true")
        assertPrints("True".toBoolean(),"true")
        assertPrints("false".toBoolean(),"false")
        assertPrints("truethis".toBoolean(),"false")
    }

    @Sample
    fun stringtoCharArray(){

        val chararray=CharArray(10)
        val expectchararray=CharArray(10)
        expectchararray.set(0,'a')
        expectchararray.set(1,'b')
        expectchararray.set(2,'c')
        expectchararray.set(3,'d')
        assertPrints("abcd".toCharArray(chararray).contentToString(),expectchararray.contentToString())

        val expectchararray1=CharArray(10)
        expectchararray1.set(2,'b')
        val chararray1=CharArray(10)
        assertPrints("abcd".toCharArray(chararray1,2,1,2).contentToString(),expectchararray1.contentToString())

    }

    @Sample
    fun stringTODoubleorNull(){
        assertPrints("abcd".toDoubleOrNull(),"null")
        assertPrints("55".toDoubleOrNull(),"55.0")
        assertPrints("-5".toDoubleOrNull(),"-5.0")

    }

    @Sample
    fun stringToDouble(){
        assertPrints('a'.toDouble(),"97.0")
        assertPrints("55".toDouble(),"55.0")
        assertPrints("-5".toDouble(),"-5.0")
    }

    @Sample
    fun stringToFLoat(){
        assertPrints('a'.toFloat(),"97.0")
        assertPrints("55".toFloat(),"55.0")
        assertPrints("-5".toFloat(),"-5.0")
    }

    @Sample
    fun stringToFLoatOrNull(){
        assertPrints("abcd".toFloatOrNull(),"null")
        assertPrints("55".toFloatOrNull(),"55.0")
        assertPrints("-5".toFloatOrNull(),"-5.0")
    }

    @Sample
    fun stringToInt(){

        //without radix argument
        assertPrints('a'.toInt(),"97")
        assertPrints("55".toInt(),"55")
        assertPrints("-5".toInt(),"-5")

    }

    @Sample
    fun stringToIntOrNull(){

        //without radix argument
        assertPrints("abcd".toIntOrNull(),"null")
        assertPrints("55".toIntOrNull(),"55")
        assertPrints("-5".toIntOrNull(),"-5")
    }

    @Sample
    fun stringToLong(){

        //without radix argument
        assertPrints('a'.toLong(),"97")
        assertPrints("55".toLong(),"55")
        assertPrints("-5".toLong(),"-5")

    }

    @Sample
    fun toLowerCase(){
        //without locale argument
        assertPrints("AbCdEf".toLowerCase(),"abcdef")
    }

    @Sample
    fun stringToShort(){

        //without radix argument
        assertPrints('a'.toShort(),"97")
        assertPrints("55".toShort(),"55")
        assertPrints("-5".toShort(),"-5")

    }

    @Sample
    fun toUpperCase(){
        //without locale argument
        assertPrints("AbCdEf".toUpperCase(),"ABCDEF")
    }

    @Sample
    fun trimString(){

        //with no arguments
        assertPrints("  a bcd  ".trim(),"a bcd")
        assertPrints("  abcd  ".trim(),"abcd")

    }

    @Sample
    fun trimEndString(){

        //with no arguments
        assertPrints("  a bcd  ".trimEnd(),"  a bcd")
        assertPrints("  abcd  ".trimEnd(),"  abcd")

    }

    @Sample
    fun trimStartString(){

        //with no arguments
        assertPrints("  a bcd  ".trimStart(),"a bcd  ")
        assertPrints("  abcd  ".trimStart(),"abcd  ")

    }

}


