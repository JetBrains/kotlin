@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
package ktlib

import cinterop.*
import kotlin.native.concurrent.Worker

interface KotlinIndexAccess {
   fun loadKotlinField(index: Int): Any?
   fun storeKotlinField(index: Int, value: Any?)
}

private fun Any.loadField(index: Int) = when (this) {
    is KotlinIndexAccess -> loadKotlinField(index)
    is ObjCIndexAccessProtocol -> loadObjCField(index)
    else -> error("Invalid loadField call")
}

private fun Any.storeField(index: Int, value: Any?) = when (this) {
    is KotlinIndexAccess -> storeKotlinField(index, value)
    is ObjCIndexAccessProtocol -> storeObjCField(index, value)
    else -> error("Invalid storeField call")
}

object WorkerTerminationProcessor {
    private val processor = Worker.start()

    fun terminateCurrent() {
        val future = Worker.current.requestTermination()
        processor.executeAfter(0L) { future.result }
    }
}

private fun spawnThread(block: () -> Unit) {
   if (!tryRegisterThread())
       return
   Worker.start().executeAfter(0L) {
       block()
       WorkerTerminationProcessor.terminateCurrent()
       unregisterThread()
   }
}

private inline fun call(localsCount: Int, blockLocalsCount: Int, block: (Int) -> Any?): Any? {
    val nextLocalsCount = localsCount + blockLocalsCount
    if (nextLocalsCount > 500) {
        return null
    }
    return block(nextLocalsCount)
}

var allocBlocker: Boolean = false

fun performGC() { kotlin.native.runtime.GC.collect() }

private inline fun alloc(block: () -> Any?): Any? {
    if (!allocBlocker || !updateAllocBlocker()) return block()
    return null
}

class Class0(var f0: Any?, var f1: Any?, var f2: Any?, var f3: Any?, var f4: Any?, var f5: Any?, var f6: Any?, var f7: Any?, var f8: Any?, var f9: Any?) : KotlinIndexAccess {
    override fun loadKotlinField(index: Int): Any? {
        return when (index % 10) {
            0 -> f0
            1 -> f1
            2 -> f2
            3 -> f3
            4 -> f4
            5 -> f5
            6 -> f6
            7 -> f7
            8 -> f8
            9 -> f9
            else -> null
        }
    }

    override fun storeKotlinField(index: Int, value: Any?) {
        when (index % 10) {
            0 -> f0 = value
            1 -> f1 = value
            2 -> f2 = value
            3 -> f3 = value
            4 -> f4 = value
            5 -> f5 = value
            6 -> f6 = value
            7 -> f7 = value
            8 -> f8 = value
            9 -> f9 = value
        }
    }
}


fun fun2(localsCount: Int, l0: Any?): Any? {
    var l1: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(0, l1)
    var l2: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(1, l2)
    var l3: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(2, l3)
    var l4: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(3, l4)
    var l5: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(4, l5)
    var l6: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(5, l6)
    var l7: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(6, l7)
    var l8: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(7, l8)
    var l9: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(8, l9)
    var l10: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    l0?.storeField(9, l10)
    return l0?.loadField(9)
}

private fun mainBodyImpl(localsCount: Int) {
    var l0: Any? = alloc({ Class0(null, null, null, null, null, null, null, null, null, null) })
    var l1: Any? = alloc({ Class1(null, null, null, null, null, null, null, null, null, null) })
    var l2: Any? = call(localsCount, 11, { fun2(it, l0) })
    var l3: Any? = call(localsCount, 11, { fun3(it, l1) })
    var l4: Any? = call(localsCount, 11, { fun2(it, l2) })
    var l5: Any? = call(localsCount, 11, { fun3(it, l3) })
    var l6: Any? = call(localsCount, 11, { fun2(it, l4) })
    var l7: Any? = call(localsCount, 11, { fun3(it, l5) })
    var l8: Any? = call(localsCount, 11, { fun2(it, l6) })
    var l9: Any? = call(localsCount, 11, { fun3(it, l7) })
    var l10: Any? = call(localsCount, 11, { fun2(it, l8) })
    var l11: Any? = call(localsCount, 11, { fun3(it, l9) })
    var l12: Any? = call(localsCount, 11, { fun2(it, l10) })
    var l13: Any? = call(localsCount, 11, { fun3(it, l11) })
    var l14: Any? = call(localsCount, 11, { fun2(it, l12) })
    var l15: Any? = call(localsCount, 11, { fun3(it, l13) })
    var l16: Any? = call(localsCount, 11, { fun2(it, l14) })
    var l17: Any? = call(localsCount, 11, { fun3(it, l15) })
    var l18: Any? = call(localsCount, 11, { fun2(it, l16) })
    var l19: Any? = call(localsCount, 11, { fun3(it, l17) })
    var l20: Any? = call(localsCount, 11, { fun2(it, l18) })
    var l21: Any? = call(localsCount, 11, { fun3(it, l19) })
    var l22: Any? = call(localsCount, 11, { fun2(it, l20) })
    var l23: Any? = call(localsCount, 11, { fun3(it, l21) })
    var l24: Any? = call(localsCount, 11, { fun2(it, l22) })
    var l25: Any? = call(localsCount, 11, { fun3(it, l23) })
    var l26: Any? = call(localsCount, 11, { fun2(it, l24) })
    var l27: Any? = call(localsCount, 11, { fun3(it, l25) })
    var l28: Any? = call(localsCount, 11, { fun2(it, l26) })
    var l29: Any? = call(localsCount, 11, { fun3(it, l27) })
    var l30: Any? = call(localsCount, 11, { fun2(it, l28) })
    var l31: Any? = call(localsCount, 11, { fun3(it, l29) })
    var l32: Any? = call(localsCount, 11, { fun2(it, l30) })
    var l33: Any? = call(localsCount, 11, { fun3(it, l31) })
    var l34: Any? = call(localsCount, 11, { fun2(it, l32) })
    var l35: Any? = call(localsCount, 11, { fun3(it, l33) })
    var l36: Any? = call(localsCount, 11, { fun2(it, l34) })
    var l37: Any? = call(localsCount, 11, { fun3(it, l35) })
    var l38: Any? = call(localsCount, 11, { fun2(it, l36) })
    var l39: Any? = call(localsCount, 11, { fun3(it, l37) })
    var l40: Any? = call(localsCount, 11, { fun2(it, l38) })
    var l41: Any? = call(localsCount, 11, { fun3(it, l39) })
    var l42: Any? = call(localsCount, 11, { fun2(it, l40) })
    var l43: Any? = call(localsCount, 11, { fun3(it, l41) })
    var l44: Any? = call(localsCount, 11, { fun2(it, l42) })
    var l45: Any? = call(localsCount, 11, { fun3(it, l43) })
    var l46: Any? = call(localsCount, 11, { fun2(it, l44) })
    var l47: Any? = call(localsCount, 11, { fun3(it, l45) })
    var l48: Any? = call(localsCount, 11, { fun2(it, l46) })
    var l49: Any? = call(localsCount, 11, { fun3(it, l47) })
    var l50: Any? = call(localsCount, 11, { fun2(it, l48) })
    var l51: Any? = call(localsCount, 11, { fun3(it, l49) })
    var l52: Any? = call(localsCount, 11, { fun2(it, l50) })
    var l53: Any? = call(localsCount, 11, { fun3(it, l51) })
    var l54: Any? = call(localsCount, 11, { fun2(it, l52) })
    var l55: Any? = call(localsCount, 11, { fun3(it, l53) })
    var l56: Any? = call(localsCount, 11, { fun2(it, l54) })
    var l57: Any? = call(localsCount, 11, { fun3(it, l55) })
    var l58: Any? = call(localsCount, 11, { fun2(it, l56) })
    var l59: Any? = call(localsCount, 11, { fun3(it, l57) })
    var l60: Any? = call(localsCount, 11, { fun2(it, l58) })
    var l61: Any? = call(localsCount, 11, { fun3(it, l59) })
    var l62: Any? = call(localsCount, 11, { fun2(it, l60) })
    var l63: Any? = call(localsCount, 11, { fun3(it, l61) })
    var l64: Any? = call(localsCount, 11, { fun2(it, l62) })
    var l65: Any? = call(localsCount, 11, { fun3(it, l63) })
    var l66: Any? = call(localsCount, 11, { fun2(it, l64) })
    var l67: Any? = call(localsCount, 11, { fun3(it, l65) })
    var l68: Any? = call(localsCount, 11, { fun2(it, l66) })
    var l69: Any? = call(localsCount, 11, { fun3(it, l67) })
    var l70: Any? = call(localsCount, 11, { fun2(it, l68) })
    var l71: Any? = call(localsCount, 11, { fun3(it, l69) })
    var l72: Any? = call(localsCount, 11, { fun2(it, l70) })
    var l73: Any? = call(localsCount, 11, { fun3(it, l71) })
    var l74: Any? = call(localsCount, 11, { fun2(it, l72) })
    var l75: Any? = call(localsCount, 11, { fun3(it, l73) })
    var l76: Any? = call(localsCount, 11, { fun2(it, l74) })
    var l77: Any? = call(localsCount, 11, { fun3(it, l75) })
    var l78: Any? = call(localsCount, 11, { fun2(it, l76) })
    var l79: Any? = call(localsCount, 11, { fun3(it, l77) })
    var l80: Any? = call(localsCount, 11, { fun2(it, l78) })
    var l81: Any? = call(localsCount, 11, { fun3(it, l79) })
    var l82: Any? = call(localsCount, 11, { fun2(it, l80) })
    var l83: Any? = call(localsCount, 11, { fun3(it, l81) })
    var l84: Any? = call(localsCount, 11, { fun2(it, l82) })
    var l85: Any? = call(localsCount, 11, { fun3(it, l83) })
    var l86: Any? = call(localsCount, 11, { fun2(it, l84) })
    var l87: Any? = call(localsCount, 11, { fun3(it, l85) })
    var l88: Any? = call(localsCount, 11, { fun2(it, l86) })
    var l89: Any? = call(localsCount, 11, { fun3(it, l87) })
    var l90: Any? = call(localsCount, 11, { fun2(it, l88) })
    var l91: Any? = call(localsCount, 11, { fun3(it, l89) })
    var l92: Any? = call(localsCount, 11, { fun2(it, l90) })
    var l93: Any? = call(localsCount, 11, { fun3(it, l91) })
    var l94: Any? = call(localsCount, 11, { fun2(it, l92) })
    var l95: Any? = call(localsCount, 11, { fun3(it, l93) })
    var l96: Any? = call(localsCount, 11, { fun2(it, l94) })
    var l97: Any? = call(localsCount, 11, { fun3(it, l95) })
    var l98: Any? = call(localsCount, 11, { fun2(it, l96) })
    var l99: Any? = call(localsCount, 11, { fun3(it, l97) })
    var l100: Any? = call(localsCount, 11, { fun2(it, l98) })
    var l101: Any? = call(localsCount, 11, { fun3(it, l99) })
    var l102: Any? = call(localsCount, 11, { fun2(it, l100) })
    var l103: Any? = call(localsCount, 11, { fun3(it, l101) })
    var l104: Any? = call(localsCount, 11, { fun2(it, l102) })
    var l105: Any? = call(localsCount, 11, { fun3(it, l103) })
    var l106: Any? = call(localsCount, 11, { fun2(it, l104) })
    var l107: Any? = call(localsCount, 11, { fun3(it, l105) })
    var l108: Any? = call(localsCount, 11, { fun2(it, l106) })
    var l109: Any? = call(localsCount, 11, { fun3(it, l107) })
    var l110: Any? = call(localsCount, 11, { fun2(it, l108) })
    var l111: Any? = call(localsCount, 11, { fun3(it, l109) })
    var l112: Any? = call(localsCount, 11, { fun2(it, l110) })
    var l113: Any? = call(localsCount, 11, { fun3(it, l111) })
    var l114: Any? = call(localsCount, 11, { fun2(it, l112) })
    var l115: Any? = call(localsCount, 11, { fun3(it, l113) })
    var l116: Any? = call(localsCount, 11, { fun2(it, l114) })
    var l117: Any? = call(localsCount, 11, { fun3(it, l115) })
    var l118: Any? = call(localsCount, 11, { fun2(it, l116) })
    var l119: Any? = call(localsCount, 11, { fun3(it, l117) })
    var l120: Any? = call(localsCount, 11, { fun2(it, l118) })
    var l121: Any? = call(localsCount, 11, { fun3(it, l119) })
    var l122: Any? = call(localsCount, 11, { fun2(it, l120) })
    var l123: Any? = call(localsCount, 11, { fun3(it, l121) })
    var l124: Any? = call(localsCount, 11, { fun2(it, l122) })
    var l125: Any? = call(localsCount, 11, { fun3(it, l123) })
    var l126: Any? = call(localsCount, 11, { fun2(it, l124) })
    var l127: Any? = call(localsCount, 11, { fun3(it, l125) })
    var l128: Any? = call(localsCount, 11, { fun2(it, l126) })
    var l129: Any? = call(localsCount, 11, { fun3(it, l127) })
    var l130: Any? = call(localsCount, 11, { fun2(it, l128) })
    var l131: Any? = call(localsCount, 11, { fun3(it, l129) })
    var l132: Any? = call(localsCount, 11, { fun2(it, l130) })
    var l133: Any? = call(localsCount, 11, { fun3(it, l131) })
    var l134: Any? = call(localsCount, 11, { fun2(it, l132) })
    var l135: Any? = call(localsCount, 11, { fun3(it, l133) })
    var l136: Any? = call(localsCount, 11, { fun2(it, l134) })
    var l137: Any? = call(localsCount, 11, { fun3(it, l135) })
    var l138: Any? = call(localsCount, 11, { fun2(it, l136) })
    var l139: Any? = call(localsCount, 11, { fun3(it, l137) })
    var l140: Any? = call(localsCount, 11, { fun2(it, l138) })
    var l141: Any? = call(localsCount, 11, { fun3(it, l139) })
    var l142: Any? = call(localsCount, 11, { fun2(it, l140) })
    var l143: Any? = call(localsCount, 11, { fun3(it, l141) })
    var l144: Any? = call(localsCount, 11, { fun2(it, l142) })
    var l145: Any? = call(localsCount, 11, { fun3(it, l143) })
    var l146: Any? = call(localsCount, 11, { fun2(it, l144) })
    var l147: Any? = call(localsCount, 11, { fun3(it, l145) })
    var l148: Any? = call(localsCount, 11, { fun2(it, l146) })
    var l149: Any? = call(localsCount, 11, { fun3(it, l147) })
    var l150: Any? = call(localsCount, 11, { fun2(it, l148) })
    var l151: Any? = call(localsCount, 11, { fun3(it, l149) })
    var l152: Any? = call(localsCount, 11, { fun2(it, l150) })
    var l153: Any? = call(localsCount, 11, { fun3(it, l151) })
    var l154: Any? = call(localsCount, 11, { fun2(it, l152) })
    var l155: Any? = call(localsCount, 11, { fun3(it, l153) })
    var l156: Any? = call(localsCount, 11, { fun2(it, l154) })
    var l157: Any? = call(localsCount, 11, { fun3(it, l155) })
    var l158: Any? = call(localsCount, 11, { fun2(it, l156) })
    var l159: Any? = call(localsCount, 11, { fun3(it, l157) })
    var l160: Any? = call(localsCount, 11, { fun2(it, l158) })
    var l161: Any? = call(localsCount, 11, { fun3(it, l159) })
    var l162: Any? = call(localsCount, 11, { fun2(it, l160) })
    var l163: Any? = call(localsCount, 11, { fun3(it, l161) })
    var l164: Any? = call(localsCount, 11, { fun2(it, l162) })
    var l165: Any? = call(localsCount, 11, { fun3(it, l163) })
    var l166: Any? = call(localsCount, 11, { fun2(it, l164) })
    var l167: Any? = call(localsCount, 11, { fun3(it, l165) })
    var l168: Any? = call(localsCount, 11, { fun2(it, l166) })
    var l169: Any? = call(localsCount, 11, { fun3(it, l167) })
    var l170: Any? = call(localsCount, 11, { fun2(it, l168) })
    var l171: Any? = call(localsCount, 11, { fun3(it, l169) })
    var l172: Any? = call(localsCount, 11, { fun2(it, l170) })
    var l173: Any? = call(localsCount, 11, { fun3(it, l171) })
    var l174: Any? = call(localsCount, 11, { fun2(it, l172) })
    var l175: Any? = call(localsCount, 11, { fun3(it, l173) })
    var l176: Any? = call(localsCount, 11, { fun2(it, l174) })
    var l177: Any? = call(localsCount, 11, { fun3(it, l175) })
    var l178: Any? = call(localsCount, 11, { fun2(it, l176) })
    var l179: Any? = call(localsCount, 11, { fun3(it, l177) })
    var l180: Any? = call(localsCount, 11, { fun2(it, l178) })
    var l181: Any? = call(localsCount, 11, { fun3(it, l179) })
    var l182: Any? = call(localsCount, 11, { fun2(it, l180) })
    var l183: Any? = call(localsCount, 11, { fun3(it, l181) })
    var l184: Any? = call(localsCount, 11, { fun2(it, l182) })
    var l185: Any? = call(localsCount, 11, { fun3(it, l183) })
    var l186: Any? = call(localsCount, 11, { fun2(it, l184) })
    var l187: Any? = call(localsCount, 11, { fun3(it, l185) })
    var l188: Any? = call(localsCount, 11, { fun2(it, l186) })
    var l189: Any? = call(localsCount, 11, { fun3(it, l187) })
    var l190: Any? = call(localsCount, 11, { fun2(it, l188) })
    var l191: Any? = call(localsCount, 11, { fun3(it, l189) })
    var l192: Any? = call(localsCount, 11, { fun2(it, l190) })
    var l193: Any? = call(localsCount, 11, { fun3(it, l191) })
    var l194: Any? = call(localsCount, 11, { fun2(it, l192) })
    var l195: Any? = call(localsCount, 11, { fun3(it, l193) })
    var l196: Any? = call(localsCount, 11, { fun2(it, l194) })
    var l197: Any? = call(localsCount, 11, { fun3(it, l195) })
    var l198: Any? = call(localsCount, 11, { fun2(it, l196) })
    var l199: Any? = call(localsCount, 11, { fun3(it, l197) })
    var l200: Any? = call(localsCount, 11, { fun2(it, l198) })
    var l201: Any? = call(localsCount, 11, { fun3(it, l199) })
}

fun mainBody() {
    val localsCount = 0
    call(localsCount, 202, { mainBodyImpl(it) })
}
