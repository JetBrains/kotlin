package org.test

import javax.inject.*

public class SomeClass {

    Named("CompanionObject")
    companion object {

    }

    Named("KotlinInnerObject")
    object SomeInnerObject {

    }

    Named("InnerClass")
    inner class InnerClass {

    }

    Named("NestedClass")
    class NestedClass {

    }

}