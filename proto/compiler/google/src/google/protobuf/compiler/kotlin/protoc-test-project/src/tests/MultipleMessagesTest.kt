package tests

import java_msg.MultipleMessages
import main.kotlin.CodedInputStream
import main.kotlin.FirstMessage
import main.kotlin.SecondMessage
import main.kotlin.ThirdMessage
import java.io.ByteArrayOutputStream

/**
 * Created by user on 8/11/16.
 */

object MultipleMessagesTest {
    fun generateKtFirstMessage(): FirstMessage {
        return FirstMessage.BuilderFirstMessage(RandomGen.rnd.nextInt()).build()
    }

    fun generateKtSecondMessage(): SecondMessage {
        return SecondMessage.BuilderSecondMessage(generateKtFirstMessage()).build()
    }

    fun generateKtThirdMessage(): ThirdMessage {
        return ThirdMessage.BuilderThirdMessage(generateKtSecondMessage()).build()
    }

    fun generateJvFirstMessage(): MultipleMessages.FirstMessage {
        return MultipleMessages.FirstMessage.newBuilder().setIntField(RandomGen.rnd.nextInt()).build()
    }

    fun generateJvSecondMessage(): MultipleMessages.SecondMessage {
        return MultipleMessages.SecondMessage.newBuilder().setFirstMsg(generateJvFirstMessage()).build()
    }

    fun generateJvThirdMessage(): MultipleMessages.ThirdMessage {
        return MultipleMessages.ThirdMessage.newBuilder().setSecondMsg(generateJvSecondMessage()).build()
    }

    fun compareThirdMessages(kt: ThirdMessage, jv: MultipleMessages.ThirdMessage): Boolean {
        return kt.second_msg.first_msg.int_field == jv.secondMsg.firstMsg.intField
    }

    fun ktToJavaOnce() {
        val kt = generateKtThirdMessage()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = MultipleMessages.ThirdMessage.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareThirdMessages(kt, jv))
    }

    fun JavaToKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvThirdMessage()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = ThirdMessage.BuilderThirdMessage(SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()).build()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareThirdMessages(kt, jv))
    }

    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToJavaOnce()
            JavaToKtOnce()
        }
    }
}