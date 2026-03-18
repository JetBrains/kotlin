package test.kotlin.jtests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import kotlin.*;
import kotlin.collections.*;
import kotlin.jvm.functions.Function0;

public class QualifiedNamesTest {

    @Test
    public void testQualified() throws Exception {
        List<Object> items = CollectionsKt.plus(CollectionsKt.plus(Collections.emptyList(), "item"), 1);
        Set<Object> set = SetsKt.<Object>setOf("a", "b", "c");

        assertTrue(Collections.disjoint(items, set));
    }

    @Test
    public void testLazy() throws Exception {
        Lazy<Random> randomLazy = LazyKt.lazy(new Function0<Random>() {
            @Override
            public Random invoke() {
                return new Random();
            }
        });
        assertFalse(randomLazy.isInitialized());

        Lazy<Random> initializedLazy = LazyKt.lazyOf(randomLazy.getValue());
        assertTrue(initializedLazy.isInitialized());
        assertTrue(randomLazy.isInitialized());
    }
}
