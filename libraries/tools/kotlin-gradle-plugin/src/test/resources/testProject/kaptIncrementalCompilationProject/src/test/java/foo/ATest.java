package foo;

import org.junit.Test;
import org.junit.Assert;

public class ATest {
    @Test
    public void testValA() {
        A a = new A();
        Assert.assertEquals("text", a.getValA());
    }
}