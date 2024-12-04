@SuppressWarnings("requires-transitive-automatic") // testng is an auto-module
module kotlin.test.testng {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.test;

    requires transitive org.testng; // automatic module name in testng manifest since 7.0.0
    // won't work with earlier versions, where just 'testng' name was inferred from the artifact name

    exports kotlin.test.testng;
    exports kotlin.test.testng.annotations;

    provides kotlin.test.AsserterContributor with kotlin.test.testng.TestNGContributor;
}
