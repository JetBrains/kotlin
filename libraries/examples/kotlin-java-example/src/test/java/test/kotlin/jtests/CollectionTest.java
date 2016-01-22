package test.kotlin.jtests;

import junit.framework.TestCase;
import kotlin.jvm.functions.Function1;

import java.util.Collection;
import java.util.List;

import static kotlin.collections.CollectionsKt.*;

/**
 * Lets try using the Kotlin standard library from Java code
 */
public class CollectionTest extends TestCase {

    public void testCollections() throws Exception {
        List<String> list = arrayListOf("foo", "bar");

        String text = joinToString(list, ",", "(", ")", -1, "...", null);
        System.out.println("Have text: " + text);
        assertEquals("(foo,bar)", text);

        Collection<String> actual = filter(list, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String text) {
                return text.startsWith("b");
            }
        });

        System.out.println("Filtered list is " + actual);
        assertEquals("(bar)", joinToString(actual, ",", "(", ")", -1, "...", null));
    }
}
