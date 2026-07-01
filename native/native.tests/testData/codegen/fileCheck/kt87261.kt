// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=genericSafeCasts=true

open class RTObject
class Glue : RTObject()
class StringValue : RTObject()
class Other

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#reproduce(RTObject;kotlin.Any){}kotlin.Boolean"
fun reproduce(obj: RTObject, o: Any): Boolean {
    val glue = if (obj is Glue) obj else null
    val text = if (obj is StringValue) obj else null
    if (glue == null && text != null) {
        // This block is reachable, so `o is Other` must be kept, not folded to a constant.
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:Other"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        return o is Other
    }
    return false
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#reproduce2(Glue?;kotlin.Any){}kotlin.Boolean"
fun reproduce2(v: Glue?, o: Any): Boolean {
    val x = if (v is Glue?) v else null
    if (x == null) {
        // Reachable when v == null, so `o is Other` must be kept, not folded to a constant.
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:Other"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        return o is Other
    }
    return false
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#reproduce3(RTObject;kotlin.Any){}kotlin.Boolean"
fun reproduce3(obj: RTObject, o: Any): Boolean {
    val glue = if (obj as? Glue != null) obj else null
    val text = if (obj as? StringValue != null) obj else null
    if (glue == null && text != null) {
        // This block is reachable, so `o is Other` must be kept, not folded to a constant.
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:Other"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        return o is Other
    }
    return false
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define {{i1|zeroext i1}} @"kfun:#reproduce4(kotlin.Any;kotlin.Any?){}kotlin.Boolean"
fun reproduce4(obj: Any, t: Any?): Boolean {
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:Other"
    if (obj is Other) {
        // Reachable with t == null, when the safe call evaluates to null, so the `is Other` check on
        // its result must be kept, not folded to a constant despite the safe call's result aliasing
        // to `obj`. The explicit type arguments keep the frontend from narrowing the safe call's type
        // (which would legitimately reduce the check to a null check).
// CHECK-DEBUG: @IsSubtype{{.*}}@"kclass:Other"
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
        return (t?.let<Any, Any> { obj }) is Other
    }
    return false
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    if (reproduce(StringValue(), Any())) return "FAIL KT-87261 (type check): o is Other folded to true"
    if (reproduce2(null, Any())) return "FAIL KT-87261 (nullable type operand): o is Other folded to true"
    if (reproduce3(StringValue(), Any())) return "FAIL KT-87261 (safe type check): o is Other folded to true"
    if (reproduce4(Other(), null)) return "FAIL KT-87261 (safe call with unrelated result): is Other folded to true"
    return "OK"
}
