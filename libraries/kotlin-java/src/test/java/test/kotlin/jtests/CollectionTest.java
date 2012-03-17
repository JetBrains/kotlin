package test.kotlin.jtests;

import static kotlin.namespace.*;
import static kotlin.util.namespace.*;

import jet.Function1;
import junit.framework.TestCase;

import java.util.Collection;
import java.util.List;

/**
 * Lets try using the Kotlin standard library from Java code
 */
public class CollectionTest extends TestCase {

    public void testCollections() throws Exception {
        List<String> list = arrayList("foo", "bar");

        String text = join(list, ",", "(", ")");
        System.out.println("Have text: " + text);
        assertEquals("(foo,bar)", text);

        Collection<String> actual = filter(list, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String text) {
                return text.startsWith("b");
            }
        });

        System.out.println("Filtered list is " + actual);
        assertEquals("(bar)", join(actual, ",", "(", ")"));
    }
}
