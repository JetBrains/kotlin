module kotlin.test.junit {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.test;

    requires junit;

    exports kotlin.test.junit;

    provides kotlin.test.AsserterContributor with kotlin.test.junit.JUnitContributor;
}
