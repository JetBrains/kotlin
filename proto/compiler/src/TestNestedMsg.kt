import java.io.ByteArrayOutputStream

/**
 * Created by user on 7/18/16.
 */

object NestedMsgTests {
    val msg1 = Level1.BuilderLevel1()
            .setField1(
                    Level1.Level2.BuilderLevel2()
                    .setField2(
                            Level1.Level2.Level3.BuilderLevel3()
                            .setField3(
                                    Level1.Level2.Level3.Level4.BuilderLevel4()
                                    .setField4("Hello!")
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()

    fun testMessageSerialization() {
        val s = ByteArrayOutputStream()
        val output = CodedOutputStream(s)

        msg1.writeTo(output)

        val readMsg = Level1.BuilderLevel1().build()

    }
}
fun main(args: Array<String>) {
    NestedMsgTests.testMessageSerialization()
    println("OK")
}
