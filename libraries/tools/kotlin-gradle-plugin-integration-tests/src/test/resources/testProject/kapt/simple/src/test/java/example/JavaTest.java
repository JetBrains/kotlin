package example;

import org.junit.Assert;
import org.junit.Test;

public class JavaTest {
    @Test
    public void test() {
        TestClass testClass = new TestClass();
        Assert.assertEquals("text", testClass.getTestVal());
    }
}