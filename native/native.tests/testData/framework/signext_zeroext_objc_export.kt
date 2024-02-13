/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

//
// Here we check generated ObjC bridges, not original declarations.
//
// f$n function name prefixes help the compiler keep the function order
// whenever it decides to sort the functions alphabetically.
//

// CHECK-LABEL: zeroext i16 @"objc2kotlin_kfun:#f0HeyIJustMetYou(kotlin.Char){}kotlin.Char"(i8* %0, i8* %1, i16 zeroext %2)
fun f0HeyIJustMetYou(arg: Char): Char {
    // CHECK: invoke zeroext i16 @"kfun:#f0HeyIJustMetYou(kotlin.Char){}kotlin.Char"(i16 zeroext %2)
    return arg
}

// CHECK-LABEL: signext i8 @"objc2kotlin_kfun:#f1AndThisIsCrazy(kotlin.Byte){}kotlin.Byte"(i8* %0, i8* %1, i8 signext %2)
fun f1AndThisIsCrazy(arg: Byte): Byte {
    // CHECK: invoke signext i8 @"kfun:#f1AndThisIsCrazy(kotlin.Byte){}kotlin.Byte"(i8 signext %2)
    return arg
}

// CHECK-LABEL: signext i16 @"objc2kotlin_kfun:#f2ButHereIsMyNumber(kotlin.Short){}kotlin.Short"(i8* %0, i8* %1, i16 signext %2)
fun f2ButHereIsMyNumber(arg: Short): Short {
    // CHECK: invoke signext i16 @"kfun:#f2ButHereIsMyNumber(kotlin.Short){}kotlin.Short"(i16 signext %2)
    return arg
}

// CHECK-LABEL: signext i8 @"objc2kotlin_kfun:#f3SoCallMeMaybe(kotlin.Boolean){}kotlin.Boolean"(i8* %0, i8* %1, i8 signext %2)
fun f3SoCallMeMaybe(arg: Boolean): Boolean {
    //CHECK: invoke zeroext i1 @"kfun:#f3SoCallMeMaybe(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext {{.*}})
    return arg
}

// CHECK-LABEL: zeroext i8 @"objc2kotlin_kfun:#f4ItsHardToLook(kotlin.UByte){}kotlin.UByte"(i8* %0, i8* %1, i8 zeroext %2)
fun f4ItsHardToLook(arg: UByte): UByte {
    // CHECK: invoke zeroext i8 @"kfun:#f4ItsHardToLook(kotlin.UByte){}kotlin.UByte"(i8 zeroext %2)
    return arg
}

// CHECK-LABEL: zeroext i16 @"objc2kotlin_kfun:#f5RightAtYouBaby(kotlin.UShort){}kotlin.UShort"(i8* %0, i8* %1, i16 zeroext %2)
fun f5RightAtYouBaby(arg: UShort): UShort {
    // CHECK: invoke zeroext i16 @"kfun:#f5RightAtYouBaby(kotlin.UShort){}kotlin.UShort"(i16 zeroext %2)
    return arg
}

// CHECK-LABEL: float @"objc2kotlin_kfun:#f6ButHereIsMyNumber1(kotlin.Float){}kotlin.Float"(i8* %0, i8* %1, float %2)
fun f6ButHereIsMyNumber1(arg: Float): Float {
    // CHECK: invoke float @"kfun:#f6ButHereIsMyNumber1(kotlin.Float){}kotlin.Float"(float %2)
    return arg
}

// CHECK-LABEL: i8* @"objc2kotlin_kfun:#f7SoCallMeMaybe1(kotlin.Any?){}kotlin.Any?"(i8* %0, i8* %1, i8* %2)
fun f7SoCallMeMaybe1(arg: Any?): Any? {
    // CHECK: invoke %struct.ObjHeader* @"kfun:#f7SoCallMeMaybe1(kotlin.Any?){}kotlin.Any?"(%struct.ObjHeader* {{.*}}, %struct.ObjHeader** {{.*}})
    return arg
}

