/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.util;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;

public class ReflectionUtil {
    private static final Logger LOG = Logger.getInstance("#com.intellij.util.ReflectionUtil");

    private ReflectionUtil() {
    }

    @Nullable
    public static Type resolveVariable(TypeVariable variable, final Class classType) {
        return resolveVariable(variable, classType, true);
    }

    @Nullable
    public static Type resolveVariable(TypeVariable variable, final Class classType, boolean resolveInInterfacesOnly) {
        final Class aClass = getRawType(classType);
        int index = ArrayUtil.find(ReflectionCache.getTypeParameters(aClass), variable);
        if (index >= 0) {
            return variable;
        }

        final Class[] classes = ReflectionCache.getInterfaces(aClass);
        final Type[] genericInterfaces = ReflectionCache.getGenericInterfaces(aClass);
        for (int i = 0; i <= classes.length; i++) {
            Class anInterface;
            if (i < classes.length) {
                anInterface = classes[i];
            } else {
                anInterface = ReflectionCache.getSuperClass(aClass);
                if (resolveInInterfacesOnly || anInterface == null) {
                    continue;
                }
            }
            final Type resolved = resolveVariable(variable, anInterface);
            if (resolved instanceof Class || resolved instanceof ParameterizedType) {
                return resolved;
            }
            if (resolved instanceof TypeVariable) {
                final TypeVariable typeVariable = (TypeVariable) resolved;
                index = ArrayUtil.find(ReflectionCache.getTypeParameters(anInterface), typeVariable);
                if (index < 0) {
                    LOG.error("Cannot resolve type variable:\n" + "typeVariable = " + typeVariable + "\n" + "genericDeclaration = " +
                            declarationToString(typeVariable.getGenericDeclaration()) + "\n" + "searching in " + declarationToString(anInterface));
                }
                final Type type = i < genericInterfaces.length ? genericInterfaces[i] : aClass.getGenericSuperclass();
                if (type instanceof Class) {
                    return Object.class;
                }
                if (type instanceof ParameterizedType) {
                    return getActualTypeArguments(((ParameterizedType) type))[index];
                }
                throw new AssertionError("Invalid type: " + type);
            }
        }
        return null;
    }

    public static String declarationToString(final GenericDeclaration anInterface) {
        return anInterface.toString()
                + Arrays.asList(anInterface.getTypeParameters())
                + " loaded by " + ((Class) anInterface).getClassLoader();
    }

    public static Class<?> getRawType(Type type) {
        if (type instanceof Class) {
            return (Class) type;
        }
        if (type instanceof ParameterizedType) {
            return getRawType(((ParameterizedType) type).getRawType());
        }
        if (type instanceof GenericArrayType) {
            //todo[peter] don't create new instance each time
            return Array.newInstance(getRawType(((GenericArrayType) type).getGenericComponentType()), 0).getClass();
        }
        assert false : type;
        return null;
    }

    public static Type[] getActualTypeArguments(final ParameterizedType parameterizedType) {
        return ReflectionCache.getActualTypeArguments(parameterizedType);
    }

    @Nullable
    public static Class<?> substituteGenericType(final Type genericType, final Type classType) {
        if (genericType instanceof TypeVariable) {
            final Class<?> aClass = getRawType(classType);
            final Type type = resolveVariable((TypeVariable) genericType, aClass);
            if (type instanceof Class) {
                return (Class) type;
            }
            if (type instanceof ParameterizedType) {
                return (Class<?>) ((ParameterizedType) type).getRawType();
            }
            if (type instanceof TypeVariable && classType instanceof ParameterizedType) {
                final int index = ArrayUtil.find(ReflectionCache.getTypeParameters(aClass), type);
                if (index >= 0) {
                    return getRawType(getActualTypeArguments(((ParameterizedType) classType))[index]);
                }
            }
        } else {
            return getRawType(genericType);
        }
        return null;
    }

    public static ArrayList<Field> collectFields(Class clazz) {
        ArrayList<Field> result = new ArrayList<Field>();
        collectFields(clazz, result);
        return result;
    }

    public static Field findField(Class clazz, @Nullable Class type, String name) throws NoSuchFieldException {
        final ArrayList<Field> fields = collectFields(clazz);
        for (Field each : fields) {
            if (name.equals(each.getName()) && (type == null || each.getType().equals(type))) return each;
        }

        throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
    }

    public static Field findAssignableField(Class clazz, Class type, String name) throws NoSuchFieldException {
        final ArrayList<Field> fields = collectFields(clazz);
        for (Field each : fields) {
            if (name.equals(each.getName()) && type.isAssignableFrom(each.getType())) return each;
        }

        throw new NoSuchFieldException("Class: " + clazz + " name: " + name + " type: " + type);
    }

    private static void collectFields(final Class clazz, final ArrayList<Field> result) {
        final Field[] fields = clazz.getDeclaredFields();
        result.addAll(Arrays.asList(fields));
        final Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            collectFields(superClass, result);
        }
        final Class[] interfaces = clazz.getInterfaces();
        for (Class each : interfaces) {
            collectFields(each, result);
        }
    }

    public static void resetField(Class clazz, Class type, String name) {
        try {
            resetField(null, findField(clazz, type, name));
        } catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }

    public static void resetField(Object object, Class type, String name) {
        try {
            resetField(object, findField(object.getClass(), type, name));
        } catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }

    public static void resetField(Object object, String name) {
        try {
            resetField(object, findField(object.getClass(), null, name));
        } catch (NoSuchFieldException e) {
            LOG.info(e);
        }
    }

    public static void resetField(@Nullable final Object object, final Field field) {
//    field.setAccessible(true);
        Class<?> type = field.getType();
        try {
            if (type.isPrimitive()) {
                if (boolean.class.equals(type)) {
                    field.set(object, Boolean.FALSE);
                } else if (int.class.equals(type)) {
                    field.set(object, new Integer(0));
                } else if (double.class.equals(type)) {
                    field.set(object, new Double(0));
                } else if (float.class.equals(type)) {
                    field.set(object, new Float(0));
                }
            } else {
                field.set(object, null);
            }
        } catch (IllegalAccessException e) {
            LOG.info(e);
        }
    }

    @Nullable
    public static Method findMethod(Method[] methods, @NonNls @NotNull String name, Class... parameters) {
        for (final Method method : methods) {
            if (name.equals(method.getName()) && Arrays.equals(parameters, method.getParameterTypes())) return method;
        }
        return null;
    }

    @Nullable
    public static Method getMethod(@NotNull Class aClass, @NonNls @NotNull String name, Class... parameters) {
        return findMethod(ReflectionCache.getMethods(aClass), name, parameters);
    }

    @Nullable
    public static Method getDeclaredMethod(@NotNull Class aClass, @NonNls @NotNull String name, Class... parameters) {
        return findMethod(aClass.getDeclaredMethods(), name, parameters);
    }

    public static Object getField(Class objectClass, Object object, Class type, @NonNls String name) {
        try {
            final Field field = findAssignableField(objectClass, type, name);
//      field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException e) {
            LOG.debug(e);
            return null;
        } catch (IllegalAccessException e) {
            LOG.debug(e);
            return null;
        }
    }

    public static Type resolveVariableInHierarchy(final TypeVariable variable, final Class aClass) {
        Type type;
        Class current = aClass;
        while ((type = resolveVariable(variable, current, false)) == null) {
            current = ReflectionCache.getSuperClass(current);
            if (current == null) {
                return null;
            }
        }
        if (type instanceof TypeVariable) {
            return resolveVariableInHierarchy((TypeVariable) type, aClass);
        }
        return type;
    }

    @NotNull
    public static <T> Constructor<T> getDefaultConstructor(final Class<T> aClass) {
        try {
            final Constructor<T> constructor = aClass.getConstructor();
//      constructor.setAccessible(true);
            return constructor;
        } catch (NoSuchMethodException e) {
            LOG.error("No default constructor in " + aClass, e);
            return null;
        }
    }

    @NotNull
    public static <T> T createInstance(final Constructor<T> constructor, final Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (InstantiationException e) {
            LOG.error(e);
            return null;
        } catch (IllegalAccessException e) {
            LOG.error(e);
            return null;
        } catch (InvocationTargetException e) {
            LOG.error(e);
            return null;
        }
    }
}
