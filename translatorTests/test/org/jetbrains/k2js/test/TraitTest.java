package org.jetbrains.k2js.test;

/**
 * @author Pavel Talanov
 */
public final class TraitTest extends TranslationTest {

    final private static String MAIN = "trait/";

    @Override
    protected String mainDirectory() {
        return MAIN;
    }


    public void testTraitAddsFunctionsToClass() throws Exception {
        testFooBoxIsTrue("traitAddsFunctionsToClass.kt");
    }


    public void testClassDerivesFromClassAndTrait() throws Exception {
        testFooBoxIsTrue("classDerivesFromClassAndTrait.kt");
    }


    public void testClassDerivesFromTraitAndClass() throws Exception {
        testFooBoxIsTrue("classDerivesFromTraitAndClass.kt");
    }


    public void testExample() throws Exception {
        testFooBoxIsTrue("example.kt");
    }


    public void testTraitExtendsTrait() throws Exception {
        testFooBoxIsTrue("traitExtendsTrait.kt");
    }


    public void testTraitExtendsTwoTraits() throws Exception {
        testFooBoxIsTrue("traitExtendsTwoTraits.kt");
    }


    public void testFunDelegation() throws Exception {
        testFooBoxIsOk("funDelegation.jet");
    }


    public void testDefinitionOrder() throws Exception {
        testFooBoxIsTrue("definitionOrder.kt");
    }
}
