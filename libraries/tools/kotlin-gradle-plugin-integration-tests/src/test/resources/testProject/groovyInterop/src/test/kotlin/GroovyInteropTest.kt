import org.junit.Assert
import kotlin.reflect.full.valueParameters

class GroovyInteropTest {

    @org.junit.Test
    fun classWithReferenceToInner() {
        Assert.assertEquals("OK", ClassWithReferenceToInner().f1(null))
        Assert.assertEquals("OK", ClassWithReferenceToInner().f2(null))
    }

    @org.junit.Test
    fun groovyTraitAccessor() {
        Assert.assertEquals(1, MyTraitAccessor().myField)
    }

    @org.junit.Test
    fun parametersInInnerClassConstructor() {
        val inner = inner.Outer().Inner("123")
        Assert.assertEquals("123", inner.name)

        val valueParameters = inner::class.constructors.single().valueParameters
        Assert.assertEquals(1, valueParameters.size)
        val annotations = valueParameters[0].annotations
        Assert.assertEquals(1, annotations.size)
        Assert.assertEquals("FooInner", annotations[0].annotationClass.simpleName)
    }

    @org.junit.Test
    fun parametersInEnumConstructor() {
        val enumValue = genum.GEnum.FOO
        Assert.assertEquals("123", enumValue.value)

        val valueParameters = enumValue::class.constructors.single().valueParameters
        Assert.assertTrue(valueParameters.isNotEmpty())
        //valueParameters.last() cause Groovy doesn't mark name and ordinal as synthetic and doesn't generate signature
        val annotations = valueParameters.last().annotations
        Assert.assertEquals(1, annotations.size)
        Assert.assertEquals("FooEnum", annotations[0].annotationClass.simpleName)
    }
}
