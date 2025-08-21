/*
 * Copyright 2016-2023 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package annotations

@HiddenClass
@Target(AnnotationTarget.CLASS)
annotation class HiddenClass

@HiddenClass
@Target(AnnotationTarget.FUNCTION)
annotation class HiddenFunction

@HiddenClass
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class HiddenCtor

@HiddenClass
@Target(AnnotationTarget.PROPERTY)
annotation class HiddenProperty

@HiddenClass
@Target(AnnotationTarget.FIELD)
annotation class HiddenField

@HiddenClass
@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class HiddenGetter

@HiddenClass
@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class HiddenSetter
