/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

//
// Here we check generated ObjC bridges, not original declarations.
//

// CHECK-LABEL: zeroext i16 @"objc2kotlin_kfun:#heyIJustMetYou(kotlin.Char){}kotlin.Char"(i8* %0, i8* %1, i16 zeroext %2)
fun heyIJustMetYou(arg: Char): Char {
    // CHECK: invoke zeroext i16 @"kfun:#heyIJustMetYou(kotlin.Char){}kotlin.Char"(i16 zeroext %2)
    return arg
}

// CHECK-LABEL: signext i8 @"objc2kotlin_kfun:#andThisIsCrazy(kotlin.Byte){}kotlin.Byte"(i8* %0, i8* %1, i8 signext %2)
fun andThisIsCrazy(arg: Byte): Byte {
    // CHECK: invoke signext i8 @"kfun:#andThisIsCrazy(kotlin.Byte){}kotlin.Byte"(i8 signext %2)
    return arg
}

// CHECK-LABEL: signext i16 @"objc2kotlin_kfun:#butHereIsMyNumber(kotlin.Short){}kotlin.Short"(i8* %0, i8* %1, i16 signext %2)
fun butHereIsMyNumber(arg: Short): Short {
    // CHECK: invoke signext i16 @"kfun:#butHereIsMyNumber(kotlin.Short){}kotlin.Short"(i16 signext %2)
    return arg
}

// CHECK-LABEL: signext i8 @"objc2kotlin_kfun:#soCallMeMaybe(kotlin.Boolean){}kotlin.Boolean"(i8* %0, i8* %1, i8 signext %2)
fun soCallMeMaybe(arg: Boolean): Boolean {
    //CHECK: invoke zeroext i1 @"kfun:#soCallMeMaybe(kotlin.Boolean){}kotlin.Boolean"(i1 zeroext {{.*}})
    return arg
}

// CHECK-LABEL: zeroext i8 @"objc2kotlin_kfun:#itsHardToLook(kotlin.UByte){}kotlin.UByte"(i8* %0, i8* %1, i8 zeroext %2)
fun itsHardToLook(arg: UByte): UByte {
    // CHECK: invoke zeroext i8 @"kfun:#itsHardToLook(kotlin.UByte){}kotlin.UByte"(i8 zeroext %2)
    return arg
}

// CHECK-LABEL: zeroext i16 @"objc2kotlin_kfun:#rightAtYouBaby(kotlin.UShort){}kotlin.UShort"(i8* %0, i8* %1, i16 zeroext %2)
fun rightAtYouBaby(arg: UShort): UShort {
    // CHECK: invoke zeroext i16 @"kfun:#rightAtYouBaby(kotlin.UShort){}kotlin.UShort"(i16 zeroext %2)
    return arg
}

// CHECK-LABEL: float @"objc2kotlin_kfun:#butHereIsMyNumber1(kotlin.Float){}kotlin.Float"(i8* %0, i8* %1, float %2)
fun butHereIsMyNumber1(arg: Float): Float {
    // CHECK: invoke float @"kfun:#butHereIsMyNumber1(kotlin.Float){}kotlin.Float"(float %2)
    return arg
}

// CHECK-LABEL: i8* @"objc2kotlin_kfun:#soCallMeMaybe1(kotlin.Any?){}kotlin.Any?"(i8* %0, i8* %1, i8* %2)
fun soCallMeMaybe1(arg: Any?): Any? {
    // CHECK: invoke %struct.ObjHeader* @"kfun:#soCallMeMaybe1(kotlin.Any?){}kotlin.Any?"(%struct.ObjHeader* {{.*}}, %struct.ObjHeader** {{.*}})
    return arg
}

