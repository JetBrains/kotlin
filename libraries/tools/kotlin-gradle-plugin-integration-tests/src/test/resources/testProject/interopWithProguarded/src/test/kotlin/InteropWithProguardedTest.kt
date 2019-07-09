import org.junit.Assert
import kotlin.reflect.full.valueParameters
import kotlin.reflect.KClass

class InteropWithProguardedTest {

    @org.junit.Test
    fun parametersInInnerJavaClassConstructor() {
        val inner = iclass.JInnerClass().Inner("123")
        testAnnotationsInConstructor(inner::class)
    }

    @org.junit.Test
    fun parametersInInnerKotlinClassConstructor() {
        val inner = kclass.KInnerClass().Inner("123")
        testAnnotationsInConstructor(inner::class)
    }

    @org.junit.Test
    fun parametersInJavaEnumConstructor() {
        val enumValue = jenum.JEnum.OK
        Assert.assertEquals("OK", enumValue.name)
        testAnnotationsInConstructor(enumValue::class)
    }

    @org.junit.Test
    fun parametersInKotlinEnumConstructor() {
        val enumValue = kenum.KEnum.OK
        Assert.assertEquals("OK", enumValue.name)
        testAnnotationsInConstructor(enumValue::class)
    }

    private fun testAnnotationsInConstructor(clazz: KClass<*>) {
        val valueParameters = clazz.constructors.single().valueParameters
        Assert.assertTrue(valueParameters.isNotEmpty())
        val annotations = valueParameters[0].annotations
        Assert.assertEquals(1, annotations.size)
        Assert.assertEquals("Foo", annotations[0].annotationClass.simpleName)
    }
}
