package com.intellij.configurationStore.xml

import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.xmlb.Converter
import com.intellij.util.xmlb.SkipDefaultsSerializationFilter
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.util.xmlb.annotations.Tag
import org.junit.Test

internal class XmlSerializerConversionTest {
  @Test
  fun converter() {
    val bean = BeanWithConverter()
    testSerializer("""
    <bean>
      <option name="bar" />
    </bean>""", bean)

    bean.foo = Ref.create("testValue")
    testSerializer("""
    <bean foo="testValue">
      <option name="bar" />
    </bean>""", bean)

    bean.foo = Ref.create<String>()
    bean.bar = Ref.create("testValue2")
    testSerializer("""
    <bean foo="">
      <option name="bar" value="testValue2" />
    </bean>""", bean)
  }

  @Test
  fun converterUsingSkipDefaultsFilter() {
    val bean = BeanWithConverter()
    testSerializer("<bean />", bean, SkipDefaultsSerializationFilter())

    bean.foo = Ref.create("testValue")
    testSerializer("""<bean foo="testValue" />""", bean, SkipDefaultsSerializationFilter())

    bean.foo = Ref.create<String>()
    bean.bar = Ref.create("testValue2")
    testSerializer("""
    <bean foo="">
      <option name="bar" value="testValue2" />
    </bean>""", bean)
  }

//  @Test
//  fun `bean fromString`() {
//    val bean = BeanWithConverter2()
//    testSerializer("""
//    <bean>
//      <option name="bar" />
//    </bean>""", bean)
//
//    bean.foo = StringRef("testValue")
//    testSerializer("""
//    <bean foo="testValue">
//      <option name="bar" />
//    </bean>""", bean)
//
//    bean.foo = StringRef()
//    bean.bar = StringRef("testValue2")
//    testSerializer("""
//    <bean foo="">
//      <option name="bar" value="testValue2" />
//    </bean>""", bean)
//  }
}

@Tag("bean")
private class BeanWithConverter {
  private class MyConverter : Converter<Ref<String>>() {
    override fun fromString(value: String): Ref<String> = Ref.create(value)

    override fun toString(o: Ref<String>) = StringUtil.notNullize(o.get())
  }

  @Attribute(converter = MyConverter::class)
  var foo: Ref<String>? = null

  @OptionTag(converter = MyConverter::class)
  var bar: Ref<String>? = null
}

//private class StringRef(private var value: String? = null) : Converter<StringRef>() {
//  companion object {
//    @Suppress("unused")
//    @JvmStatic
//    fun fromText(value: String): StringRef = StringRef(value)
//  }
//
//  override fun fromString(value: String) = StringRef(value)
//
//  override fun toString(value: StringRef) = value.value
//}
//
//@Tag("bean")
//private class BeanWithConverter2 {
//  @Attribute
//  var foo: StringRef? = null
//
//  @OptionTag
//  var bar: StringRef? = null
//}