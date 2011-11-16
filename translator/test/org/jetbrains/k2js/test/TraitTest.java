package org.jetbrains.k2js.test;

import org.junit.Test;

/**
 * @author Talanov Pavel
 */
public final class TraitTest extends AbstractClassTest {

    final private static String MAIN = "trait/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }

    @Test
    public void traitAddsFunctionsToClass() throws Exception {
        testFooBoxIsTrue("traitAddsFunctionsToClass.kt");
    }

    @Test
    public void classDerivesFromClassAndTrait() throws Exception {
        testFooBoxIsTrue("classDerivesFromClassAndTrait.kt");
    }

    @Test
    public void classDerivesFromTraitAndClass() throws Exception {
        testFooBoxIsTrue("classDerivesFromTraitAndClass.kt");
    }

    @Test
    public void example() throws Exception {
        testFooBoxIsTrue("example.kt");
    }

    @Test
    public void traitExtendsTrait() throws Exception {
        testFooBoxIsTrue("traitExtendsTrait.kt");
    }

}
