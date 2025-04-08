// KIND: STANDALONE_LLDB
// FIR_IDENTICAL
// LANGUAGE: -IrInlinerBeforeKlibSerialization
// ^^^ KT-76547: With pre-serialization inliner, there's an error:
// Failed(reason=Tested process output has not passed validation: Wrong LLDB output.
// - Command: bt
// - Expected (pattern):     frame #1: [..]`kfun:[..]main$$inlined$foo[..]invoke[..](_this=[..])[..] at kt42208-2.kt:10:11
// - Actual:
// * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
//  * frame #0: 0x00000001000b1e30 lldb_kt42208.kexe`ThrowException
//    frame #1: 0x000000010000191c lldb_kt42208.kexe`kfun:main$1.invoke#internal(_this=[]) at kt42208-1.kt:7:72
//    frame #2: 0x0000000100001968 lldb_kt42208.kexe`kfun:main$1.$<bridge-DN>invoke(_this=[]){}kotlin.Nothing#internal at kt42208-1.kt:7:60
//    frame #3: 0x0000000100065250 lldb_kt42208.kexe`kfun:kotlin.Function0#invoke(){}1:0-trampoline at [K][Suspend]Functions:1:1
//    frame #4: 0x000000010000172c lldb_kt42208.kexe`kfun:#main(){} at kt42208-1.kt:5:5
//    frame #5: 0x00000001000017ac lldb_kt42208.kexe`Konan_start(args=[]) at kt42208-1.kt:4:1
//    frame #6: 0x0000000100002dac lldb_kt42208.kexe`Init_and_run_start + 100
//    frame #7: 0x000000019f6a4274 dyld`start + 2840)

// FILE: kt42208-1.kt
fun main() {
    foo()()
}
// FILE: kt42208-2.kt
// aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
inline fun foo() = {
    throw Error()
}
