package org.test

import java.lang.annotation.Inherited

@Inherited
annotation class Ann

@Ann
interface A

class B : A