package org.jetbrains.eval4j.test

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.MethodNode
import java.lang.reflect.Modifier
import org.jetbrains.eval4j.*
import org.junit.Assert.*
import junit.framework.TestSuite
import junit.framework.TestCase

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
                expected = when (returnType.getSort()) {
                    Type.VOID -> ValueReturned(VOID_VALUE)
                    Type.BOOLEAN -> ValueReturned(boolean(result as Boolean))
                    Type.BYTE -> ValueReturned(byte(result as Byte))
                    Type.SHORT -> ValueReturned(short(result as Short))
                    Type.CHAR -> ValueReturned(char(result as Char))
                    Type.INT -> ValueReturned(int(result as Int))
                    Type.LONG -> ValueReturned(long(result as Long))
                    Type.DOUBLE -> ValueReturned(double(result as Double))
                    Type.FLOAT -> ValueReturned(float(result as Float))
                    Type.OBJECT -> ValueReturned(obj(result))
                    else -> {
                        println("Unsupported result type: $returnType")
                        return null
                    }
                }
                println("Testing $method")
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

object REFLECTION_EVAL : Eval {
    override fun loadClass(classType: Type): Value {
        throw UnsupportedOperationException()
    }
    override fun newInstance(classType: Type): Value {
        throw UnsupportedOperationException()
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
    override fun getStaticField(fieldDesc: String): Value {
        throw UnsupportedOperationException()
    }
    override fun setStaticField(fieldDesc: String, newValue: Value) {
        throw UnsupportedOperationException()
    }
    override fun invokeStaticMethod(methodDesc: String, arguments: List<Value>): Value {
        throw UnsupportedOperationException()
    }
    override fun getField(instance: Value, fieldDesc: String): Value {
        throw UnsupportedOperationException()
    }
    override fun setField(instance: Value, fieldDesc: String, newValue: Value) {
        throw UnsupportedOperationException()
    }
    override fun invokeMethod(instance: Value, methodDesc: String, arguments: List<Value>, invokespecial: Boolean): Value {
        throw UnsupportedOperationException()
    }
}