/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;
import kotlin.collections.ArraysKt;
import kotlin.reflect.*;

import java.util.Arrays;
import java.util.Collections;

/**
 * This class is a copy of {@link Reflection} with some slight modifications; it serves as a facade to the Stdlib reflection
 * implementation. JVM back-end generates calls to static methods of this class on any reflection-using construct if the Stdlib Reflection
 * is forced, see option `-Xforce-stdlib-only-reflection`.
 * <p/>
 * Unlike {@link Reflection}, there is no full reflection support for created constructs, even if the full Reflection implementation is
 * present in the classpath.
 * <p/>
 * The changes compared to the {@link Reflection} class are:
 * <ul>
 *     <li>the factory used is always {@link ReflectionFactory} that only provides Stdlib reflection</li>
 *     <li>there is no Stdlib-only versions of {@link Reflection#renderLambdaToString(Lambda)} and
 *     {@link Reflection#renderLambdaToString(FunctionBase)} methods</li>
 *     <li>there is no Stdlib-only version of the {@link Reflection#getOrCreateKotlinPackage(Class, String)} method, but only
 *     {@link StdlibOnlyReflection#getOrCreateKotlinPackage(Class)}</li>
 *     <li>there is no Stdlib-only version of the {@link Reflection#areTypesEqual(TypeReference, KType)} method, as it performs
 *     cross-universe reflection types comparison</li>
 *     <li>there is no Stdlib-only version of the {@link Reflection#notSupportedError()}</li>
 * </ul>
 */
@SuppressWarnings({"unused", "rawtypes"})
@SinceKotlin(version = "2.4")
public class StdlibOnlyReflection {
    private static final ReflectionFactory factory = new ReflectionFactory();

    private static final KClass[] EMPTY_K_CLASS_ARRAY = new KClass[0];

    public static KClass createKotlinClass(Class javaClass) {
        return factory.createKotlinClass(javaClass);
    }

    public static KClass createKotlinClass(Class javaClass, String internalName) {
        return factory.createKotlinClass(javaClass, internalName);
    }

    public static KDeclarationContainer getOrCreateKotlinPackage(Class javaClass) {
        return factory.getOrCreateKotlinPackage(javaClass, "");
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

    public static KType typeOf(KClassifier classifier) {
        return factory.typeOf(classifier, Collections.<KTypeProjection>emptyList(), false);
    }

    public static KType typeOf(Class klass) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.<KTypeProjection>emptyList(), false);
    }

    public static KType typeOf(Class klass, KTypeProjection arg1) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.singletonList(arg1), false);
    }

    public static KType typeOf(Class klass, KTypeProjection arg1, KTypeProjection arg2) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Arrays.asList(arg1, arg2), false);
    }

    public static KType typeOf(Class klass, KTypeProjection... arguments) {
        return factory.typeOf(getOrCreateKotlinClass(klass), ArraysKt.<KTypeProjection>toList(arguments), false);
    }

    public static KType nullableTypeOf(KClassifier classifier) {
        return factory.typeOf(classifier, Collections.<KTypeProjection>emptyList(), true);
    }

    public static KType nullableTypeOf(Class klass) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.<KTypeProjection>emptyList(), true);
    }

    public static KType nullableTypeOf(Class klass, KTypeProjection arg1) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Collections.singletonList(arg1), true);
    }

    public static KType nullableTypeOf(Class klass, KTypeProjection arg1, KTypeProjection arg2) {
        return factory.typeOf(getOrCreateKotlinClass(klass), Arrays.asList(arg1, arg2), true);
    }

    public static KType nullableTypeOf(Class klass, KTypeProjection... arguments) {
        return factory.typeOf(getOrCreateKotlinClass(klass), ArraysKt.<KTypeProjection>toList(arguments), true);
    }

    // Support of non-reified type parameters for typeOf

    public static KTypeParameter typeParameter(Object container, String name, KVariance variance, boolean isReified) {
        return factory.typeParameter(container, name, variance, isReified);
    }

    public static void setUpperBounds(KTypeParameter typeParameter, KType bound) {
        factory.setUpperBounds(typeParameter, Collections.singletonList(bound));
    }

    public static void setUpperBounds(KTypeParameter typeParameter, KType... bounds) {
        factory.setUpperBounds(typeParameter, ArraysKt.toList(bounds));
    }

    // Features of stable typeOf

    public static KType platformType(KType lowerBound, KType upperBound) {
        return factory.platformType(lowerBound, upperBound);
    }

    public static KType mutableCollectionType(KType type) {
        return factory.mutableCollectionType(type);
    }

    public static KType nothingType(KType type) {
        return factory.nothingType(type);
    }
}
