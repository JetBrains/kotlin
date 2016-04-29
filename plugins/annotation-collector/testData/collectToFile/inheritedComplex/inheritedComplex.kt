package org.test

import java.lang.annotation.Inherited

@Inherited
annotation class Ann1

@Inherited
annotation class Ann2

@Ann1
interface A

@Ann2
open class B : A

open class C : B()

class D : C()