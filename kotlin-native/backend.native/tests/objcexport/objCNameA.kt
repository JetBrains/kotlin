/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalObjCName::class)

package objCNameA

import kotlin.experimental.ExperimentalObjCName

// https://youtrack.jetbrains.com/issue/KT-50767
@ObjCName("ObjCNameC1A")
class ObjCNameC1 {
    fun foo(): String = "a"
}

// https://youtrack.jetbrains.com/issue/KT-48076
@ObjCName("with")
fun withUserId(userId: String): String = userId

// https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/3240586-supports
fun supports(@ObjCName(swiftName = "_") features: Boolean): Boolean = features

// https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/1518986-scanforperipherals
fun scanForPeripherals(@ObjCName("withServices") serviceUUIDs: Int, options: String): String = "$serviceUUIDs $options"

// https://developer.apple.com/documentation/corebluetooth/cbcentralmanager/3174844-registerforconnectionevents
fun registerForConnectionEvents(@ObjCName("withOptions", "options") options: String): String = "$options"
