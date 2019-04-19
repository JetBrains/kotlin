package com.intellij.configurationStore.xml;

import com.intellij.configurationStore.XmlSerializer;
import com.intellij.util.xmlb.annotations.Transient;
import junit.framework.TestCase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.intellij.testFramework.assertions.Assertions.assertThat;

@SuppressWarnings({"deprecation"})
public class XmlSerializerWithDefaultJDOMExternalizerCompatibilityTest extends TestCase {
  public void testCompatibility() {
    assertCompatibleSerialization(new MyBean());
  }

  private static void assertCompatibleSerialization(final Object data) {
    assertThat(serializeWithJDom(data)).isEqualTo(serialize(data));
  }

  private static Element serializeWithJDom(@NotNull Object data) {
    final Element jDomRoot = new Element("MyBean");
    if (data instanceof com.intellij.openapi.util.JDOMExternalizable) {
      ((com.intellij.openapi.util.JDOMExternalizable)data).writeExternal(jDomRoot);
    }
    else {
      com.intellij.openapi.util.DefaultJDOMExternalizer.writeExternal(data, jDomRoot);
    }

    return jDomRoot;
  }

  private static Element serialize(Object bean) {
    return XmlSerializer.serialize(bean, null);
  }

  @SuppressWarnings("unused")
  private static class MyBean {
    public int intField = 0;
    public int intField2 = 1;
    public boolean booleanField = false;
    public boolean booleanField2 = true;
    public String stringField;
    public String stringField2 = "a";
    public float floatField;
    public float floatField2 = 1;
    public transient int transientField;
    @Transient
    public int transientField2;
  }
}
