module my.module {
    requires kotlin.stdlib;
    requires org.apache.logging.log4j;
    requires annotation.processor.example;

    requires dagger;
    requires javax.inject;
    //because dagger generates classes with @javax.annotation.processing.Generated
    requires java.compiler;
}
