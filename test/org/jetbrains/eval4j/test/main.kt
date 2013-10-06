package org.jetbrains.eval4j.test

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import org.jetbrains.eval4j.*
import org.junit.Assert.*
import junit.framework.TestSuite
import junit.framework.TestCase
import java.lang.reflect.Method
import java.lang.reflect.Field
import java.lang.reflect.Constructor

fun suite(): TestSuite {
    val suite = TestSuite()

    val ownerClass = javaClass<TestData>()
    val inputStream = ownerClass.getClassLoader()!!.getResourceAsStream(ownerClass.getInternalName() + ".class")!!

    ClassReader(inputStream).accept(object : ClassVisitor(ASM4) {

        override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            return object : MethodNode(access, name, desc, signature, exceptions) {
                override fun visitEnd() {
                    val testCase = doTest(ownerClass, this)
                    if (testCase != null) {
                        suite.addTest(testCase)
                    }
                }
            }
        }
    }, 0)

    return suite
}

fun Class<*>.getInternalName(): String = Type.getType(this).getInternalName()

fun doTest(ownerClass: Class<TestData>, methodNode: MethodNode): TestCase? {
    var expected: InterpreterResult? = null
    for (method in ownerClass.getDeclaredMethods()) {
        if (method.getName() == methodNode.name) {
            if ((method.getModifiers() and Modifier.STATIC) == 0) {
                println("Skipping instance method: $method")
            }
            else if (method.getParameterTypes()!!.size > 0) {
                println("Skipping method with parameters: $method")
            }
            else {
                method.setAccessible(true)
                val result = method.invoke(null)
                val returnType = Type.getType(method.getReturnType()!!)
                try {
                    expected = ValueReturned(objectToValue(result, returnType))
                }
                catch (e: UnsupportedOperationException) {
                    println("Skipping $method: $e")
                }
            }
        }
    }

    if (expected == null) {
        println("Method not found: ${methodNode.name}")
        return null
    }

    return object : TestCase("test" + methodNode.name.capitalize()) {

        override fun runTest() {
            val value = interpreterLoop(
                    ownerClass.getInternalName(),
                    methodNode,
                    REFLECTION_EVAL
            )

            assertEquals(expected, value)
        }
    }
}

fun objectToValue(obj: Any?, expectedType: Type): Value {
    return when (expectedType.getSort()) {
        Type.VOID -> VOID_VALUE
        Type.BOOLEAN -> boolean(obj as Boolean)
        Type.BYTE -> byte(obj as Byte)
        Type.SHORT -> short(obj as Short)
        Type.CHAR -> char(obj as Char)
        Type.INT -> int(obj as Int)
        Type.LONG -> long(obj as Long)
        Type.DOUBLE -> double(obj as Double)
        Type.FLOAT -> float(obj as Float)
        Type.OBJECT -> if (obj == null) NULL_VALUE else ObjectValue(obj, expectedType)
        else -> throw UnsupportedOperationException("Unsupported result type: $expectedType")
    }
}

object REFLECTION_EVAL : Eval {

    val lookup = ReflectionLookup(javaClass<ReflectionLookup>().getClassLoader()!!)

    override fun loadClass(classType: Type): Value {
        throw UnsupportedOperationException()
    }

    override fun loadString(str: String): Value = ObjectValue(str, Type.getType(javaClass<String>()))

    override fun newInstance(classType: Type): Value {
        val _class = findClass(classType.getInternalName())
        return NewObjectValue(_class, classType)
    }

    override fun checkCast(value: Value, targetType: Type): Value {
        throw UnsupportedOperationException()
    }
    override fun isInsetanceOf(value: Value, targetType: Type): Boolean {
        throw UnsupportedOperationException()
    }

    override fun newArray(arrayType: Type, size: Int): Value {
        throw UnsupportedOperationException()
    }
    override fun newMultiDimensionalArray(arrayType: Type, dimensionSizes: List<Int>): Value {
        throw UnsupportedOperationException()
    }
    override fun getArrayLength(array: Value): Value {
        throw UnsupportedOperationException()
    }
    override fun getArrayElement(array: Value, index: Value): Value {
        throw UnsupportedOperationException()
    }
    override fun setArrayElement(array: Value, index: Value, newValue: Value) {
        throw UnsupportedOperationException()
    }

    override fun getStaticField(fieldDesc: FieldDescription): Value {
        val result = findStaticField(fieldDesc).get(null)
        return objectToValue(result, fieldDesc.fieldType)
    }

    override fun setStaticField(fieldDesc: FieldDescription, newValue: Value) {
        findStaticField(fieldDesc).set(null, newValue.obj)
    }

    fun findStaticField(fieldDesc: FieldDescription): Field {
        assertTrue(fieldDesc.isStatic)
        val field = findClass(fieldDesc).findField(fieldDesc)
        assertNotNull("Field not found: $fieldDesc", field)
        assertTrue("Field is not static: $field", (field!!.getModifiers() and Modifier.STATIC) != 0)
        return field
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        assertTrue(methodDesc.isStatic)
        val method = findClass(methodDesc).findMethod(methodDesc)
        assertNotNull("Method not found: $methodDesc", method)
        val result = method!!.invoke(null, *arguments.map { v -> v.obj }.copyToArray())
        return objectToValue(result, methodDesc.returnType)
    }

    fun findClass(memberDesc: MemberDescription): Class<Any?> = findClass(memberDesc.ownerInternalName)

    fun findClass(internalName: String): Class<Any?> {
        val owner = lookup.findClass(internalName)
        assertNotNull("Class not found: ${internalName}", owner)
        return owner!!
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val obj = instance.obj
        val field = findInstanceField(obj, fieldDesc)

        return objectToValue(field.get(obj), fieldDesc.fieldType)
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val obj = instance.obj
        val field = findInstanceField(obj, fieldDesc)

        field.set(obj, newValue.obj)
    }

    fun findInstanceField(obj: Any?, fieldDesc: FieldDescription): Field {
        if (obj == null) throw NullPointerException()

        val _class: Class<Any?> = obj.javaClass
        val field = _class.findField(fieldDesc)
        assertNotNull("Field not found: $fieldDesc", field)
        return field!!
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial) {
            if (methodDesc.name == "<init>") {
                // Constructor call
                val _class = (instance as NewObjectValue)._class
                val ctor = _class.findConstructor(methodDesc)
                assertNotNull("Constructor not found: $methodDesc", ctor)
                val args = arguments.map { v -> v.obj }.copyToArray()
                val result = ctor!!.newInstance(*args)
                instance.value = result
                return objectToValue(result, instance.asmType)
            }
            else {
                // TODO
                throw UnsupportedOperationException("invokespecial is not suported yet")
            }
        }
        val obj = instance.obj
        val method = obj.javaClass.findMethod(methodDesc)
        assertNotNull("Method not found: $methodDesc", method)
        val result = method!!.invoke(obj, *arguments.map { v -> v.obj }.copyToArray())
        return objectToValue(result, methodDesc.returnType)
    }
}

class ReflectionLookup(val classLoader: ClassLoader) {
    [suppress("UNCHECKED_CAST")]
    fun findClass(internalName: String): Class<Any?>? = classLoader.loadClass(internalName.replace('/', '.')) as Class<Any?>
}

[suppress("UNCHECKED_CAST")]
fun Class<Any?>.findMethod(methodDesc: MethodDescription): Method? {
    for (declared in getDeclaredMethods()) {
        if (methodDesc.matches(declared)) return declared
    }

    val fromSuperClass = (getSuperclass() as Class<Any?>).findMethod(methodDesc)
    if (fromSuperClass != null) return fromSuperClass

    for (supertype in getInterfaces()) {
        val fromSuper = (supertype as Class<Any?>).findMethod(methodDesc)
        if (fromSuper != null) return fromSuper
    }

    return null
}

[suppress("UNCHECKED_CAST")]
fun Class<Any?>.findConstructor(methodDesc: MethodDescription): Constructor<Any?>? {
    for (declared in getDeclaredConstructors()) {
        if (methodDesc.matches(declared)) return declared as Constructor<Any?>
    }
    return null
}

fun MethodDescription.matches(ctor: Constructor<*>): Boolean {
    val methodParams = ctor.getParameterTypes()!!
    if (parameterTypes.size() != methodParams.size) return false
    for ((i, p) in parameterTypes.withIndices()) {
        if (!p.matches(methodParams[i])) return false
    }

    return true;
}

fun MethodDescription.matches(method: Method): Boolean {
    if (name != method.getName()) return false

    val methodParams = method.getParameterTypes()!!
    if (parameterTypes.size() != methodParams.size) return false
    for ((i, p) in parameterTypes.withIndices()) {
        if (!p.matches(methodParams[i])) return false
    }

    return returnType.matches(method.getReturnType()!!)
}

[suppress("UNCHECKED_CAST")]
fun Class<Any?>.findField(fieldDesc: FieldDescription): Field? {
    for (declared in getDeclaredFields()) {
        if (fieldDesc.matches(declared)) return declared
    }

    val superclass = getSuperclass()
    if (superclass != null) {
        val fromSuperClass = (superclass as Class<Any?>).findField(fieldDesc)
        if (fromSuperClass != null) return fromSuperClass
    }

    for (supertype in getInterfaces()) {
        val fromSuper = (supertype as Class<Any?>).findField(fieldDesc)
        if (fromSuper != null) return fromSuper
    }

    return null
}

fun FieldDescription.matches(field: Field): Boolean {
    if (name != field.getName()) return false

    return fieldType.matches(field.getType()!!)
}

fun Type.matches(_class: Class<*>): Boolean = this == Type.getType(_class)