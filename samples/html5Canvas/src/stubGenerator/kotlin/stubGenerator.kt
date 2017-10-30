package org.jetbrains.kotlin.konan.jsinterop.tool

import platform.posix.*

// This is (as of now) a poor man's IDL representation.

interface Type 
interface Member

object Void: Type
object Integer: Type
object Floating: Type
object idlString: Type
object Object: Type
object Function: Type

data class Attribute(val name: String, val type: Type, 
    val hasGetter: Boolean = false, val hasSetter: Boolean = false): Member

data class Arg(val name: String, val type: Type)

class Operation(val name: String, val returnType: Type, vararg val args: Arg): Member

class InterfaceRef(val name: String): Type
class Interface(val name: String, vararg val members: Member)

fun kotlinHeader(): String {
    val packageName = "html5.minimal"
    return  "package $packageName\n" +
            "import kotlinx.wasm.jsinterop.*\n"
}

fun Type.toKotlinType(argName: String? = null): String {
    return when (this) {
        is Void -> "Unit"
        is Integer -> "Int"
        is Floating -> "Float"
        is idlString -> "String"
        is Object -> "JsValue"
        is Function -> "KtFunction<R${argName!!}>"
        is InterfaceRef -> name
        else -> error("Unexpected type")
    }
}

fun Arg.wasmMapping(): String {
    return when (type) {
        is Void -> error("An arg can not be Void")
        is Integer -> name
        is Floating -> name
        is idlString -> "stringPointer($name), stringLengthBytes($name)"
        is Object -> TODO("implement me")
        is Function -> "wrapFunction<R$name>($name), ArenaManager.currentArena"
        is InterfaceRef -> TODO("Implement me")
        else -> error("Unexpected type")
    }
}

fun Interface.wasmReturnArg(): String = "ArenaManager.currentArena"

fun Arg.wasmArgNames(): List<String> {
    return when (type) {
        is Void -> error("An arg can not be Void")
        is Integer -> listOf(name)
        is Floating -> listOf(name)
        is idlString -> listOf("${name}Ptr", "${name}Len")
        is Object -> TODO("implement me (Object)")
        is Function -> listOf("${name}Index", "${name}ResultArena")
        is InterfaceRef -> TODO("Implement me (InterfaceRef)")
        else -> error("Unexpected type")
    }
}

fun Type.wasmReturnMapping(value: String): String {
    return when (this) {
        is Void -> ""
        is Integer -> value
        is Floating -> value
        is idlString -> TODO("Implement me")
        is Object -> "JsValue(ArenaManager.currentArena, $value)"
        is Function -> TODO("Implement me")
        is InterfaceRef -> "$name(ArenaManager.currentArena, $value)"
        else -> error("Unexpected type")
    }
}

fun wasmFunctionName(functionName: String, interfaceName: String)
    = "knjs_${interfaceName}_$functionName"

fun wasmSetterName(propertyName: String, interfaceName: String)
    = "knjs_${interfaceName}_set_$propertyName"

fun wasmGetterName(propertyName: String, interfaceName: String)
    = "knjs_${interfaceName}_get_$propertyName"

val Operation.kotlinTypeParameters: String get() {
    val lambdaRetTypes = args.filter { it.type is Function }
        .map { "R${it.name}" }. joinToString(", ")
    return if (lambdaRetTypes == "") "" else "<$lambdaRetTypes>"
}

val Interface.wasmReceiverArgs get() = 
    if (this.name != "__Global") 
        listOf("this.arena", "this.index")
    else emptyList()

fun Operation.generateKotlin(parent: Interface): String {
    val argList = args.map {
        "${it.name}: ${it.type.toKotlinType(it.name)}"
    }.joinToString(",")

    val wasmArgList = (parent.wasmReceiverArgs + args.map{it.wasmMapping()} + parent.wasmReturnArg()).joinToString(",")

    // TODO: there can be multiple Rs.
    return "\tfun $kotlinTypeParameters $name(" + 
    argList + 
    "): ${returnType.toKotlinType()} {\n" +

    "\t\tval wasmRetVal = ${wasmFunctionName(name, parent.name)}($wasmArgList)\n" +

    "\t\treturn ${returnType.wasmReturnMapping("wasmRetVal")}\n"+
    "\t}\n"
}

fun Attribute.generateKotlinSetter(parent: Interface): String {
    val kotlinType = type.toKotlinType(name)
    return "\t\tset(value: $kotlinType) {\n" +
    "\t\t\t${wasmSetterName(name, parent.name)}(" +
        (parent.wasmReceiverArgs + Arg("value", type).wasmMapping()).joinToString(", ") + 
        ")\n" + 
    "\t\t}\n"
}

fun Attribute.generateKotlinGetter(parent: Interface): String {
    //val kotlinType = type.toKotlinType()
    return "\t\tget() {\n" +
    "\t\t\tval wasmRetVal = ${wasmGetterName(name, parent.name)}(${(parent.wasmReceiverArgs + parent.wasmReturnArg()).joinToString(", ")})\n" + 
    "\t\t\treturn ${type.wasmReturnMapping("wasmRetVal")}\n"+
    "\t\t}\n" +
    "\t\n"
}

fun Attribute.generateKotlin(parent: Interface): String {
    val kotlinType = type.toKotlinType(name)
    return "\tvar $name: $kotlinType\n" +
    (if (hasGetter) generateKotlinGetter(parent) else "\t\tget() = error(\"There is no getter for $name\")\n") + 
    (if (hasSetter) generateKotlinSetter(parent) else "\t\tset(_) = error(\"There is no setter for $name\")\n")
}

val Interface.wasmTypedReceiverArgs get() = 
    if (this.name != "__Global") 
        listOf("arena: Int", "index: Int")
    else emptyList()

fun Operation.generateWasmStub(parent: Interface): String {
    val wasmName = wasmFunctionName(this.name, parent.name)
    val allArgs = (parent.wasmTypedReceiverArgs + args.toList().wasmTypedMapping() + parent.wasmTypedReturnMapping()).joinToString(", ")
    return "@SymbolName(\"$wasmName\")\n" +
    "external public fun $wasmName($allArgs): ${returnType.wasmReturnTypeMapping()}\n\n"
}
fun Attribute.generateWasmSetterStub(parent: Interface): String {
    val wasmSetter = wasmSetterName(this.name, parent.name)
    val allArgs = (parent.wasmTypedReceiverArgs + Arg("value", this.type).wasmTypedMapping()).joinToString(", ")
    return "@SymbolName(\"$wasmSetter\")\n" +
    "external public fun $wasmSetter($allArgs): Unit\n\n"
}
fun Attribute.generateWasmGetterStub(parent: Interface): String {
    val wasmGetter = wasmGetterName(this.name, parent.name)
    val allArgs = (parent.wasmTypedReceiverArgs + parent.wasmTypedReturnMapping()).joinToString(", ")
    return "@SymbolName(\"$wasmGetter\")\n" +
    "external public fun $wasmGetter($allArgs): Int\n\n"
}
fun Attribute.generateWasmStubs(parent: Interface) =
    (if (hasGetter) generateWasmGetterStub(parent) else "") +
    (if (hasSetter) generateWasmSetterStub(parent) else "")

// TODO: consider using virtual mathods
fun Member.generateKotlin(parent: Interface): String {
    return when (this) {
        is Operation -> this.generateKotlin(parent)
        is Attribute -> this.generateKotlin(parent)
        else -> error("Unexpected member")
    }
}

// TODO: consider using virtual mathods
fun Member.generateWasmStub(parent: Interface) =
    when (this) {
        is Operation -> this.generateWasmStub(parent)
        is Attribute -> this.generateWasmStubs(parent)
        else -> error("Unexpected member")

    }

fun Arg.wasmTypedMapping() 
    = this.wasmArgNames().map { "$it: Int" } .joinToString(", ")

fun Interface.wasmTypedReturnMapping() 
    = "resultArena: Int"

fun List<Arg>.wasmTypedMapping()
    = this.map{ it.wasmTypedMapping() }

// TODO: more complex return types, such as returning a pair of Ints
// will require a more complex approach.
fun Type.wasmReturnTypeMapping()
    = if (this == Void) "Unit" else "Int"

fun Interface.generateMemberWasmStubs() =
    members.map {
        it.generateWasmStub(this)
    }.joinToString("")

fun Interface.generateKotlinMembers() =
    members.map {
        it.generateKotlin(this)
    }.joinToString("")

fun Interface.generateKotlinClassHeader() =
    "open class $name(arena: Int, index: Int): JsValue(arena, index) {\n" +
    "\tconstructor(jsValue: JsValue): this(jsValue.arena, jsValue.index)\n"
    
fun Interface.generateKotlinClassFooter() =
    "}\n"

fun Interface.generateKotlinClassConverter() =
    "val JsValue.as$name: $name\n" +
    "\tget() {\n" +
    "\t\treturn $name(this.arena, this.index)\n"+
    "\t}\n"

fun Interface.generateKotlin(): String {

    fun String.skipForGlobal() = 
        if (this@generateKotlin.name != "__Global") this 
        else ""

    return generateMemberWasmStubs() + 
        generateKotlinClassHeader().skipForGlobal() +
        generateKotlinMembers() + 
        generateKotlinClassFooter().skipForGlobal() +
        generateKotlinClassConverter().skipForGlobal()
}

fun generateKotlin(interfaces: List<Interface>) =
    kotlinHeader() + 
    interfaces.map {
        it.generateKotlin()
    }.joinToString("\n") +
    "fun <R> setInterval(interval: Int, lambda: KtFunction<R>) = setInterval(lambda, interval)\n"

/////////////////////////////////////////////////////////

fun Arg.composeWasmArgs(): String {
    return when (type) {
        is Void -> error("An arg can not be Void")
        is Integer -> ""
        is Floating -> ""
        is idlString -> "\t\t$name = toUTF16String(${name}Ptr, ${name}Len);\n"
        is Object -> TODO("implement me")
        is Function -> "\t\t$name = konan_dependencies.env.Konan_js_wrapLambda(lambdaResultArena, ${name}Index);\n"

        is InterfaceRef -> TODO("Implement me")
        else -> error("Unexpected type")
    }
}

val Interface.receiver get() = 
    if (this.name == "__Global") "" else  "kotlinObject(arena, obj)."

fun Operation.generateJs(parent: Interface): String {
    val allArgs = parent.wasmReceiverArgName + args.map { it.wasmArgNames() }.flatten() + wasmReturnArgName
    val wasmMapping = allArgs.joinToString(", ")
    val argList = args.map { it.name }. joinToString(", ")
    val composedArgsList = args.map { it.composeWasmArgs() }. joinToString("")

    return "\n\t${wasmFunctionName(this.name, parent.name)}: function($wasmMapping) {\n" +
        composedArgsList +
        "\t\tvar result = ${parent.receiver}$name($argList);\n" +
        (if (returnType == Void) "" else  "\t\treturn toArena(resultArena, result);\n") +
    "\t}"
}

val Interface.wasmReceiverArgName get() =
    if (this.name != "__Global") listOf("arena", "obj") else emptyList()

val Operation.wasmReturnArgName get() =
    if (this.returnType != Void) listOf("resultArena") else emptyList()

val Attribute.wasmReturnArgName get() =
    if (this.type != Void) listOf("resultArena") else emptyList()

fun Attribute.generateJsSetter(parent: Interface): String {
    val valueArg = Arg("value", type)
    val allArgs = parent.wasmReceiverArgName + valueArg.wasmArgNames()
    val wasmMapping = allArgs.joinToString(", ")
    return "\n\t${wasmSetterName(name, parent.name)}: function($wasmMapping) {\n" +
        valueArg.composeWasmArgs() +
        "\t\t${parent.receiver}$name = value;\n" +
    "\t}"
}

fun Attribute.generateJsGetter(parent: Interface): String {
    val allArgs = parent.wasmReceiverArgName + wasmReturnArgName
    val wasmMapping = allArgs.joinToString(", ")
    return "\n\t${wasmGetterName(name, parent.name)}: function($wasmMapping) {\n" +
        "\t\tvar result = ${parent.receiver}$name;\n" +
        "\t\treturn toArena(resultArena, result);\n" +
    "\t}"
}

fun Attribute.generateJs(parent: Interface) =
    (if (hasGetter) generateJsGetter(parent) else "") + 
    (if (hasSetter) generateJsSetter(parent) else "") 

fun Member.generateJs(parent: Interface): String {
    return when (this) {
        is Operation -> this.generateJs(parent)
        is Attribute -> this.generateJs(parent)
        else -> error("Unexpected member")
    }
}

fun generateJs(interfaces: List<Interface>): String =
    "konan.libraries.push ({\n" +
    interfaces.map { interf ->
        interf.members.map { member -> 
            member.generateJs(interf) 
        }
    }.flatten() .joinToString(",\n") + 
    "\n})\n"

fun String.writeToFile(name: String) {
    val file = fopen(name, "wb")!!
    fputs(this, file)
    fclose(file)
}
fun main(args: Array<String>) {
    generateKotlin(all).writeToFile("kotlin_stubs.kt")
    generateJs(all).writeToFile("js_stubs.js")
}

