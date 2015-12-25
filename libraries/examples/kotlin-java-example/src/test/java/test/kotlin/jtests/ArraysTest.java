package test.kotlin.jtests;

import junit.framework.TestCase;
import kotlin.*;
import kotlin.jvm.functions.Function1;

import java.util.Collection;
import java.util.List;

import static kotlin.collections.CollectionsKt.*;
import static kotlin.collections.ArraysKt.*;

/**
 * Lets try using the Kotlin standard library from Java code
 */
public class ArraysTest extends TestCase {

    public void testArrays() throws Exception {
        String[] array = {"foo", "bar", "x"};


        String text = joinToString(array, ",", "(", ")", -1, "...", null);
        System.out.println("Have text: " + text);
        assertEquals("(foo,bar,x)", text);

        Collection<String> actual = filter(array, new Function1<String, Boolean>() {
            @Override
            public Boolean invoke(String text) {
                return text.length() > 1;
            }
        });

        int[] actualArray = toIntArray(map(actual, new Function1<String, Integer>() {
            @Override
            public Integer invoke(String s) {
                return s.length();
            }
        }));

        System.out.println("Filtered list from array is " + actual);
        assertEquals("(3,3)", joinToString(actualArray, ",", "(", ")", -1, "...", null));

        int single = single(distinct(actualArray));
        assertEquals(3, single);
    }
}
