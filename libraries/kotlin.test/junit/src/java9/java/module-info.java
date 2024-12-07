@SuppressWarnings("requires-transitive-automatic") // junit is an auto-module
module kotlin.test.junit {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.test;

    requires transitive junit;

    exports kotlin.test.junit;
    exports kotlin.test.junit.annotations;

    provides kotlin.test.AsserterContributor with kotlin.test.junit.JUnitContributor;
}
