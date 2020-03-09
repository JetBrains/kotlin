/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.collections.ArraysKt;
import kotlin.reflect.*;

import java.util.Arrays;
import java.util.Collections;

/**
 * This class serves as a facade to the actual reflection implementation. JVM back-end generates calls to static methods of this class
 * on any reflection-using construct.
 */
@SuppressWarnings("unused")
public class Reflection {
    private static final ReflectionFactory factory;

    static {
        ReflectionFactory impl;
        try {
            Class<?> implClass = Class.forName("kotlin.reflect.jvm.internal.ReflectionFactoryImpl");
            impl = (ReflectionFactory) implClass.newInstance();
        }
        catch (ClassCastException e) { impl = null; }
        catch (ClassNotFoundException e) { impl = null; }
        catch (InstantiationException e) { impl = null; }
        catch (IllegalAccessException e) { impl = null; }

        factory = impl != null ? impl : new ReflectionFactory();
    }

    /* package */ static final String REFLECTION_NOT_AVAILABLE = " (Kotlin reflection is not available)";

    private static final KClass[] EMPTY_K_CLASS_ARRAY = new KClass[0];

    public static KClass createKotlinClass(Class javaClass) {
        return factory.createKotlinClass(javaClass);
    }

    public static KClass createKotlinClass(Class javaClass, String internalName) {
        return factory.createKotlinClass(javaClass, internalName);
    }

    @SinceKotlin(version = "1.4")
    public static KDeclarationContainer getOrCreateKotlinPackage(Class javaClass) {
        return factory.getOrCreateKotlinPackage(javaClass, "");
    }

    public static KDeclarationContainer getOrCreateKotlinPackage(Class javaClass, String moduleName) {
        return factory.getOrCreateKotlinPackage(javaClass, moduleName);
    }

    public static KClass getOrCreateKotlinClass(Class javaClass) {
        return factory.getOrCreateKotlinClass(javaClass);
    }

    public static KClass getOrCreateKotlinClass(Class javaClass, String internalName) {
        return factory.getOrCreateKotlinClass(javaClass, internalName);
    }

    public static KClass[] getOrCreateKotlinClasses(Class[] javaClasses) {
        int size = javaClasses.length;
        if (size == 0) return EMPTY_K_CLASS_ARRAY;
        KClass[] kClasses = new KClass[size];
        for (int i = 0; i < size; i++) {
            kClasses[i] = getOrCreateKotlinClass(javaClasses[i]);
        }
        return kClasses;
    }

    @SinceKotlin(version = "1.1")
    public static String renderLambdaToString(Lambda lambda) {
        return factory.renderLambdaToString(lambda);
    }

    @SinceKotlin(version = "1.3")
    public static String renderLambdaToString(FunctionBase lambda) {
        return factory.renderLambdaToString(lambda);
    }

    // Functions

    public static KFunction function(FunctionReference f) {
        return factory.function(f);
    }

    // Properties

    public static KProperty0 property0(PropertyReference0 p) {
        return factory.property0(p);
    }

    public static KMutableProperty0 mutableProperty0(MutablePropertyReference0 p) {
        return factory.mutableProperty0(p);
    }

    public static KProperty1 property1(PropertyReference1 p) {
        return factory.property1(p);
    }

    public static KMutableProperty1 mutableProperty1(MutablePropertyReference1 p) {
        return factory.mutableProperty1(p);
    }

    public static KProperty2 property2(PropertyReference2 p) {
        return factory.property2(p);
    }

    public static KMutableProperty2 mutableProperty2(MutablePropertyReference2 p) {
        return factory.mutableProperty2(p);
    }

    // typeOf

    @SinceKotlin(version = "1.4")
    public static KType typeOf(Class klass) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.<KTypeProjection>emptyList(), false);
    }

    @SinceKotlin(version = "1.4")
    public static KType typeOf(Class klass, KTypeProjection arg1) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.singletonList(arg1), false);
    }

    @SinceKotlin(version = "1.4")
    public static KType typeOf(Class klass, KTypeProjection arg1, KTypeProjection arg2) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Arrays.asList(arg1, arg2), false);
    }

    @SinceKotlin(version = "1.4")
    public static KType typeOf(Class klass, KTypeProjection... arguments) {
        return factory.typeOf(getOrCreateKotlinClass(klass), ArraysKt.<KTypeProjection>toList(arguments), false);
    }

    @SinceKotlin(version = "1.4")
    public static KType nullableTypeOf(Class klass) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.<KTypeProjection>emptyList(), true);
    }

    @SinceKotlin(version = "1.4")
    public static KType nullableTypeOf(Class klass, KTypeProjection arg1) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.singletonList(arg1), true);
    }

    @SinceKotlin(version = "1.4")
    public static KType nullableTypeOf(Class klass, KTypeProjection arg1, KTypeProjection arg2) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Arrays.asList(arg1, arg2), true);
    }

    @SinceKotlin(version = "1.4")
    public static KType nullableTypeOf(Class klass, KTypeProjection... arguments) {
        return factory.typeOf(getOrCreateKotlinClass(klass), ArraysKt.<KTypeProjection>toList(arguments), true);
    }
}
