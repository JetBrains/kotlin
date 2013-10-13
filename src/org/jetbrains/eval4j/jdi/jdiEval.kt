package org.jetbrains.eval4j.jdi

import org.jetbrains.eval4j.*
import org.objectweb.asm.Type
import com.sun.jdi

val CLASS = Type.getType(javaClass<Class<*>>())

class JDIEval(
        private val vm: jdi.VirtualMachine,
        private val classLoader: jdi.ClassLoaderReference,
        private val thread: jdi.ThreadReference
) : Eval {
    override fun loadClass(classType: Type): Value {
        return invokeStaticMethod(
                MethodDescription(
                        CLASS.getInternalName(),
                        "forName",
                        "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/Class;",
                        true
                ),
                listOf(classLoader.asValue())
        )
    }

    override fun loadString(str: String): Value = vm.mirrorOf(str)!!.asValue()

    override fun newInstance(classType: Type): Value {
        return NewObjectValue(classType)
    }

    override fun isInstanceOf(value: Value, targetType: Type): Boolean {
        assert(targetType.getSort() == Type.OBJECT, "Can't check isInstanceOf() for non-object type $targetType")

        val _class = loadClass(targetType)
        return invokeMethod(
                _class,
                MethodDescription(
                        CLASS.getInternalName(),
                        "isInstance",
                        "(Ljava/lang/Object;)Z",
                        false
                ),
                listOf(value)).boolean
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

    private fun findField(fieldDesc: FieldDescription): jdi.Field {
        val _class = loadClass(fieldDesc.ownerType).jdiClass!!.reflectedType()
        val field = _class.fieldByName(fieldDesc.name)
        if (field == null) {
            throwException(NoSuchFieldError("Field not found: $fieldDesc"))
        }
        return field
    }

    private fun findStaticField(fieldDesc: FieldDescription): jdi.Field {
        val field = findField(fieldDesc)
        if (!field.isStatic()) {
            throwException(NoSuchFieldError("Field is not static: $fieldDesc"))
        }
        return field
    }

    override fun getStaticField(fieldDesc: FieldDescription): Value {
        val field = findStaticField(fieldDesc)
        return field.declaringType().getValue(field).asValue()
    }

    override fun setStaticField(fieldDesc: FieldDescription, newValue: Value) {
        val field = findStaticField(fieldDesc)

        if (field.isFinal()) {
            throwException(NoSuchFieldError("Can't modify a final field: $field"))
        }

        val _class = field.declaringType()
        if (_class !is jdi.ClassType) {
            throwException(NoSuchFieldError("Can't a field in a non-class: $field"))
        }

        _class.setValue(field, newValue.asJdiValue(vm))
    }

    private fun findMethod(methodDesc: MethodDescription): jdi.Method {
        val _class = loadClass(methodDesc.ownerType).jdiClass!!.reflectedType()
        val method = when (_class) {
            is jdi.ClassType -> {
                val m = _class.concreteMethodByName(methodDesc.name, methodDesc.desc)
                if (m == null) listOf() else listOf(m)
            }
            else -> _class.methodsByName(methodDesc.name, methodDesc.desc)
        }
        if (method.isEmpty()) {
            throwException(NoSuchMethodError("Method not found: $methodDesc"))
        }
        return method[0]
    }

    override fun invokeStaticMethod(methodDesc: MethodDescription, arguments: List<Value>): Value {
        val method = findMethod(methodDesc)
        if (!method.isStatic()) {
            throwException(NoSuchMethodError("Method is not static: $methodDesc"))
        }
        val _class = method.declaringType()
        if (_class !is jdi.ClassType) throwException(NoSuchMethodError("Static method is a non-class type: $method"))

        return _class.invokeMethod(thread, method, arguments.map {v -> v.asJdiValue(vm)}, 0).asValue()
    }

    override fun getField(instance: Value, fieldDesc: FieldDescription): Value {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        return obj.getValue(field).asValue()
    }

    override fun setField(instance: Value, fieldDesc: FieldDescription, newValue: Value) {
        val field = findField(fieldDesc)
        val obj = instance.jdiObj.checkNull()

        return obj.setValue(field, newValue.asJdiValue(vm))
    }

    override fun invokeMethod(instance: Value, methodDesc: MethodDescription, arguments: List<Value>, invokespecial: Boolean): Value {
        if (invokespecial) {
            if (methodDesc.name == "<init>") {
                // Constructor call
                val ctor = findMethod(methodDesc)
                val _class = (instance as NewObjectValue).asmType.asReferenceType() as jdi.ClassType
                val result = _class.newInstance(thread, ctor, arguments.map { v -> v.asJdiValue(vm) }, 0)
                instance.value = result
                return result.asValue()
            }
            else {
                // TODO
                throw UnsupportedOperationException("invokespecial is not suported yet")
            }
        }
        val method = findMethod(methodDesc)

        val obj = instance.jdiObj.checkNull()
        return obj.invokeMethod(thread, method, arguments.map { v -> v.asJdiValue(vm) }, 0).asValue()
    }
}