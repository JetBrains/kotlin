// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

enum class COLOR {
    RED,
    GREEN,
    BLUE
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    for (i in COLOR.values()) {
        // CHECK-DEBUG: = call i32 @"kfun:kotlin.Enum#<get-ordinal>(){}kotlin.Int"(
        // We inline .ordinal property access in case of opt build, so check direct field load instead.
        // CHECK-OPT: getelementptr inbounds %"kclassbody:kotlin.Enum#internal", %"kclassbody:kotlin.Enum#internal"* %{{[0-9a-z]*}}, i32 0, i32 2
        print(when (i) {
            // we can't check the register is same, because it can be saved on stack and loaded back
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 0
            COLOR.RED -> 0xff0000
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 1
            COLOR.GREEN -> 0x00ff00
            // CHECK: icmp eq i32 %{{[0-9a-z]*}}, 2
            COLOR.BLUE -> 0x0000ff
        })
    }
    // CHECK-LABEL: epilogue:
    return "OK"
}