package tests

import CodedInputStream
import FirstMessage
import SecondMessage
import ThirdMessage

object MultipleMessagesTest {
    fun generateKtFirstMessage(): FirstMessage {
        return FirstMessage.BuilderFirstMessage(Util.nextInt()).build()
    }

    fun generateKtSecondMessage(): SecondMessage {
        return SecondMessage.BuilderSecondMessage(generateKtFirstMessage()).build()
    }

    fun generateKtThirdMessage(): ThirdMessage {
        return ThirdMessage.BuilderThirdMessage(generateKtSecondMessage()).build()
    }

    fun compareThirdMessages(kt1: ThirdMessage, kt2: ThirdMessage): Boolean {
        return kt1.second_msg.first_msg.int_field == kt2.second_msg.first_msg.int_field
    }

    fun ktToKtOnce() {
        val msg = generateKtThirdMessage()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = ThirdMessage.BuilderThirdMessage(SecondMessage.BuilderSecondMessage(FirstMessage.BuilderFirstMessage(0).build()).build()).build()
        readMsg.mergeFrom(ins)

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareThirdMessages(msg, readMsg))
    }


    val testRuns = 1000

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}