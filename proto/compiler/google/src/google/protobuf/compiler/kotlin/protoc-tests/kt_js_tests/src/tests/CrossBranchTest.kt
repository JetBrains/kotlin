package tests

import CodedInputStream
import CrossBranch

object CrossBranchTest {
    fun generateKtGrandFather(): CrossBranch.Grandfather {
        fun generateKtRightFather(): CrossBranch.Grandfather.RightFather {
            fun generateKtRightLeftSon(): CrossBranch.Grandfather.RightFather.RightLeftSon {
                fun generateKtLeftLeftSon(): CrossBranch.Grandfather.LeftFather.LeftLeftSon {
                    return CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(Util.nextInt()).build()
                }

                val msg = CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(
                        generateKtLeftLeftSon()
                ).build()
                return msg
            }

            fun generateKtRightRightSon(): CrossBranch.Grandfather.RightFather.RightRightSon {
                fun generateKtLeftRightSon(): CrossBranch.Grandfather.LeftFather.LeftRightSon {
                    return CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(Util.nextInt()).build()
                }

                return CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(
                        generateKtLeftRightSon()
                ).build()
            }

            val msg = CrossBranch.Grandfather.RightFather.BuilderRightFather(
                    generateKtRightLeftSon(),
                    generateKtRightRightSon()
            ).build()

            return msg
        }

        return CrossBranch.Grandfather.BuilderGrandfather(generateKtRightFather()).build()
    }

    fun compareGrandFathers(kt1: CrossBranch.Grandfather, kt2: CrossBranch.Grandfather): Boolean {
        return kt1.rf.rls.son_field.son_field == kt2.rf.rls.son_field.son_field &&
                kt1.rf.rrs.son_field.son_field == kt2.rf.rrs.son_field.son_field
    }

    val testRuns = 1000

    fun ktToKtOnce() {
        val msg = generateKtGrandFather()
        val outs = Util.getKtOutputStream(msg.getSizeNoTag())
        msg.writeTo(outs)

        val ins = CodedInputStream(outs.buffer)
        val readMsg = generateKtGrandFather()
        readMsg.mergeFrom(ins)      // caution: not correct in presence of repeated fields. In this particular case it's ok

        Util.assert(readMsg.errorCode == 0)
        Util.assert(compareGrandFathers(msg, readMsg))
    }

    fun runTests() {
        for (i in 0..testRuns) {
            ktToKtOnce()
        }
    }
}