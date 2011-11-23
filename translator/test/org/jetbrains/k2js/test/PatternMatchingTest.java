package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class PatternMatchingTest extends TranslationTest {

    final private static String MAIN = "patternMatching/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void whenType() throws Exception {
        testFooBoxIsTrue("whenType.kt");
    }

    @Test
    public void whenNotType() throws Exception {
        testFooBoxIsTrue("whenNotType.kt");
    }

    @Test
    public void whenExecutesOnlyOnce() throws Exception {
        testFooBoxIsTrue("whenExecutesOnlyOnce.kt");
    }

    @Test
    public void whenValue() throws Exception {
        testFooBoxIsTrue("whenValue.kt");
    }

    @Test
    public void whenNotValue() throws Exception {
        testFooBoxIsTrue("whenNotValue.kt");
    }

    @Test
    public void whenValueOrType() throws Exception {
        testFooBoxIsTrue("whenValueOrType.kt");
    }

    @Test
    public void multipleCases() throws Exception {
        testFunctionOutput("multipleCases.kt", "foo", "box", 2.0);
    }

    @Test
    public void matchNullableType() throws Exception {
        testFooBoxIsTrue("matchNullableType.kt");
    }

    @Test
    public void whenConditionMethodCall() throws Exception {
        testFooBoxIsTrue("whenConditionMethodCall.kt");
    }

    @Test
    public void whenConditionPropertyAccess() throws Exception {
        testFooBoxIsTrue("whenConditionPropertyAccess.kt");
    }

}
