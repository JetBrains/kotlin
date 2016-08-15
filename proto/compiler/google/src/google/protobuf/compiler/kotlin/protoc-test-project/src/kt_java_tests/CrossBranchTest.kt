package tests

import java_msg.CrossBranchOuterClass
import main.kotlin.CodedInputStream
import main.kotlin.CrossBranch
import java.io.ByteArrayOutputStream

/**
 * Created by user on 8/11/16.
 */

object CrossBranchTest {
    fun generateKtGrandFather(): main.kotlin.CrossBranch.Grandfather {
        fun generateKtRightFather(): main.kotlin.CrossBranch.Grandfather.RightFather {
            fun generateKtRightLeftSon(): main.kotlin.CrossBranch.Grandfather.RightFather.RightLeftSon {
                fun generateKtLeftLeftSon(): main.kotlin.CrossBranch.Grandfather.LeftFather.LeftLeftSon {
                    return CrossBranch.Grandfather.LeftFather.LeftLeftSon.BuilderLeftLeftSon(RandomGen.rnd.nextInt()).build()
                }

                val msg = CrossBranch.Grandfather.RightFather.RightLeftSon.BuilderRightLeftSon(
                        generateKtLeftLeftSon()
                    ).build()
                return msg
            }

            fun generateKtRightRightSon(): CrossBranch.Grandfather.RightFather.RightRightSon {
                fun generateKtLeftRightSon(): main.kotlin.CrossBranch.Grandfather.LeftFather.LeftRightSon {
                    return CrossBranch.Grandfather.LeftFather.LeftRightSon.BuilderLeftRightSon(RandomGen.rnd.nextInt()).build()
                }

                return CrossBranch.Grandfather.RightFather.RightRightSon.BuilderRightRightSon(
                    generateKtLeftRightSon()
                ).build()
            }

            val msg = main.kotlin.CrossBranch.Grandfather.RightFather.BuilderRightFather(
                    generateKtRightLeftSon(),
                    generateKtRightRightSon()
            ).build()

            return msg
        }

        return CrossBranch.Grandfather.BuilderGrandfather(generateKtRightFather()).build()
    }

    fun generateJvGrandFather(): CrossBranchOuterClass.CrossBranch.Grandfather {
        fun generateJvRightFather(): CrossBranchOuterClass.CrossBranch.Grandfather.RightFather {
            fun generateJvRightLeftSon(): CrossBranchOuterClass.CrossBranch.Grandfather.RightFather.RightLeftSon {
                fun generateJvLeftLeftSon(): CrossBranchOuterClass.CrossBranch.Grandfather.LeftFather.LeftLeftSon {
                    return CrossBranchOuterClass.CrossBranch.Grandfather.LeftFather.LeftLeftSon.newBuilder()
                            .setSonField(RandomGen.rnd.nextInt()).build()
                }

                return CrossBranchOuterClass.CrossBranch.Grandfather.RightFather.RightLeftSon.newBuilder()
                            .setSonField(generateJvLeftLeftSon()).build()
            }

            fun generateJvRightRightSon(): CrossBranchOuterClass.CrossBranch.Grandfather.RightFather.RightRightSon {
                fun generateJvLeftRightSon(): CrossBranchOuterClass.CrossBranch.Grandfather.LeftFather.LeftRightSon {
                    return CrossBranchOuterClass.CrossBranch.Grandfather.LeftFather.LeftRightSon.newBuilder()
                            .setSonField(RandomGen.rnd.nextInt()).build()
                }

                return CrossBranchOuterClass.CrossBranch.Grandfather.RightFather.RightRightSon.newBuilder()
                            .setSonField(generateJvLeftRightSon()).build()
            }

            return CrossBranchOuterClass.CrossBranch.Grandfather.RightFather.newBuilder()
                    .setRls(
                        generateJvRightLeftSon()
                    )
                    .setRrs(
                            generateJvRightRightSon()
                    )
                    .build()
        }

        return CrossBranchOuterClass.CrossBranch.Grandfather.newBuilder().setRf(generateJvRightFather()).build()
    }

    fun compareGrandFathers(kt: CrossBranch.Grandfather, jv: CrossBranchOuterClass.CrossBranch.Grandfather): Boolean {
        return kt.rf.rls.son_field.son_field == jv.rf.rls.sonField.sonField &&
                kt.rf.rrs.son_field.son_field == jv.rf.rrs.sonField.sonField
    }

    val testRuns = 10

    fun KtToJavaOnce() {
        val kt = generateKtGrandFather()
        val outs = Util.getKtOutputStream(kt.getSizeNoTag())
        kt.writeTo(outs)

        val ins = Util.KtOutputStreamToInputStream(outs)
        val jv = CrossBranchOuterClass.CrossBranch.Grandfather.parseFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareGrandFathers(kt, jv))
    }

    fun KtToJava() {
        for (i in 0..testRuns) {
            KtToJavaOnce()
        }
    }

    fun JavaToKtOnce() {
        val outs = ByteArrayOutputStream(100000)

        val jv = generateJvGrandFather()
        jv.writeTo(outs)
        val ins = CodedInputStream(outs.toByteArray())
        val kt = generateKtGrandFather()
        kt.mergeFrom(ins)

        Util.assert(kt.errorCode == 0)
        Util.assert(compareGrandFathers(kt, jv))
    }

    fun JavaToKT() {
        for (i in 0..testRuns) {
            JavaToKtOnce()
        }
    }

    fun runTests() {
        KtToJava()
        JavaToKT()
    }
}