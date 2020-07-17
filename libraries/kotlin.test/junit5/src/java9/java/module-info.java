@SuppressWarnings("module") // suppress warning about terminal digit
module kotlin.test.junit5 {
    requires transitive kotlin.stdlib;
    requires transitive kotlin.test;

    requires org.junit.jupiter.api;

    exports kotlin.test.junit5;

    provides kotlin.test.AsserterContributor with kotlin.test.junit5.JUnit5Contributor;
}
