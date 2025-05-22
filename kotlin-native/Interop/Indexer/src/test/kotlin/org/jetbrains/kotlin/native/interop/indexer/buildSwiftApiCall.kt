/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

//fun buildSwiftApiCall(type: ObjCType.ObjectType, allTypesMap: Map<String, ObjCType.ObjectType>): String {
fun buildSwiftApiCall(type: ObjCContainer): String {

    val className = if (type is ObjCClassOrProtocol) {
        type.swiftName ?: type.name
    } else {
        error("Unsupported swift_name type: $type")
    }

    val sb = StringBuilder()

    val constructors = type.methods.filter { it.containsInstancetype() }
    val instanceMethods = type.instanceMethods()
    val staticMethods = type.methods.filter { !it.containsInstancetype() && it.isClass }
    var instanceName = ""
    val staticName = type.swiftName
    constructors.forEachIndexed { index, c ->
        instanceName = "${className.lowerCaseFirstChar()}_$index"
        val callName = c.swiftName ?: c.selector

        val declareInst = "let $instanceName = "

        if (callName.contains("()")) {
            sb.append("$declareInst$className.$callName")
        } else {
            val methodName = callName.getMethodName()
            val paramNames = callName.getParamNames()

            val paramsToValues = c.parameters.mapIndexed { paramIndex, parameter ->
                val paramName = paramNames[paramIndex]
                val paramType = parameter.type.toSwiftDefaultType()
                "$paramName: $paramType"
            }.joinToString("")

            sb.append("$declareInst$className.$methodName($paramsToValues)")
        }
    }

    if (instanceName.isEmpty()) {
        staticMethods.forEach { method ->
            val methodName = method.swiftName ?: method.selector
            val returnType = (method.returnType as? ObjCObjectPointer)?.def as? ObjCClassOrProtocol

            if (returnType != null) {

                if (method.parameters.isEmpty()) {

                    val instN = "${staticName}_${methodName.substringBefore("(")}"

                    sb.append("\n")
                    sb.append("let $instN = $staticName.$methodName")

                    val instMethods = returnType.instanceMethods()

                    buildInstanceCalls(instMethods, instN, sb)
                }


            }
        }
    } else {
        buildInstanceCalls(instanceMethods, instanceName, sb)
    }

    return sb.toString()
}

private fun ObjCContainer.instanceMethods(): List<ObjCMethod> {
    return methods.filter { !it.containsInstancetype() && !it.isClass }
}

private fun buildInstanceCalls(
        instanceMethods: List<ObjCMethod>,
        instanceName: String,
        sb: StringBuilder,
) {
    instanceMethods.forEach { method ->
        val methodName = method.swiftName
        if (method.parameters.isEmpty()) {
            sb.append("\n")
            sb.append("$instanceName.$methodName")
        }
    }
}

fun Type.toSwiftDefaultType(): String {
    val typeDef = this.unwrapTypedefs()
    return if (typeDef is PrimitiveType) {
        if (typeDef is IntegerType) {
            "42"
        } else error("Unknown primitive type: $typeDef")
    } else error("Unknown type: $typeDef")
}

fun String.getMethodName(): String {
    return this.substringBefore('(')
}

fun String.getParamNames(): List<String> {
    return this.substringAfter('(').substringBeforeLast(')').split(':').map { it.trim() }
}

fun String.lowerCaseFirstChar(): String {
    return this[0].lowercaseChar() + this.substring(1)
}

//fun buildSwiftCall(instanceName: String, type: ObjCType.ObjectType?): String? {
fun buildSwiftCall(instanceName: String, type: Type): String? {
    //if (type == null) return null
    val sb = StringBuilder()

//    type.members.forEach { member ->
//        if (member is ObjCMethod) {
//            val swiftName = member.swiftName
//            if (!member.isStatic && !member.isConstructor && swiftName != "hash" && swiftName != "isEqual") {
//
//                val args = if (member.arguments.isEmpty()) {
//                    ""
//                } else {
//                    member.arguments.joinToString(", ") { arg ->
//
//                        val argType = arg.type
//                        val argTypeStr = if (argType is ObjCType.PrimitiveType) {
//                            objCTypeToSwiftType(argType.name)
//                        } else {
//                            if (argType is ObjCType.ObjectType) {
//                                argType.swiftName ?: argType.name
//                            } else argType.name
//                        }
//
//                        arg.swiftNameOrName() + ": Some() as! " + argTypeStr
//                    }
//                }
//
//                sb.appendLine("$instanceName.$swiftName($args)")
//            }
//        }
//
//    }

    return sb.toString()
}

fun objCTypeToSwiftType(type: String): String {
    return when (type) {
        "int8_t" -> "Int8"
        "uint8_t" -> "UInt8"
        "int16_t" -> "Int16"
        "uint16_t" -> "UInt16"
        "int32_t" -> "Int32"
        "uint32_t" -> "UInt32"
        //"int64_t" -> "Int32"
        "int64_t" -> "Int64"
        "uint64_t" -> "UInt64"
        "float" -> "Float"
        "double" -> "Double"
        "BOOL" -> "Bool"
        "NSString" -> "String"
        "NSNumber" -> "NSNumber"
        "NSData" -> "Data"
        "NSArray" -> "Array"
        "NSDictionary" -> "Dictionary"
        "NSError" -> "Error"
        else -> error("Unknown Objective-C type: $type")
    }
}