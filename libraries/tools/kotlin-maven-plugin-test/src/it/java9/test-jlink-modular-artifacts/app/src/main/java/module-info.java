module org.test.modularApp {
    requires transitive kotlin.stdlib;
    requires kotlin.stdlib.jdk8;

    exports org.test.modularApp;
}