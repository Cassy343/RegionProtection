package com.kicas.rp.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ReflectionHelper {
    private static final Map<Class<?>, Class<?>> W2P = new HashMap<>();

    static {
        W2P.put(Integer.class, int.class);
        W2P.put(Long.class, long.class);
        W2P.put(Short.class, short.class);
        W2P.put(Double.class, double.class);
        W2P.put(Float.class, float.class);
        W2P.put(Boolean.class, boolean.class);
        W2P.put(Byte.class, byte.class);
        W2P.put(Character.class, char.class);
        W2P.put(Integer[].class, int[].class);
        W2P.put(Long[].class, long[].class);
        W2P.put(Short[].class, short[].class);
        W2P.put(Double[].class, double[].class);
        W2P.put(Float[].class, float[].class);
        W2P.put(Boolean[].class, boolean[].class);
        W2P.put(Byte[].class, byte[].class);
        W2P.put(Character[].class, char[].class);
    }

    private ReflectionHelper() { }

    public static boolean isNullable(Class<?> clazz) {
        return !clazz.isPrimitive();
    }

    public static Number numericCast(Class<?> newType, Number target) {
        if(target.getClass().equals(newType))
            return target;
        if(Integer.class.equals(newType))
            return target.intValue();
        else if(Double.class.equals(newType))
            return target.doubleValue();
        else if(Long.class.equals(newType))
            return target.longValue();
        else if(Short.class.equals(newType))
            return target.shortValue();
        else if(Float.class.equals(newType))
            return target.floatValue();
        else
            return target.byteValue();
    }

    public static <T> T safeCast(Class<T> newType, Object target) {
        if(newType.isAssignableFrom(target.getClass()))
            return newType.cast(target);
        else
            return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] arrayCast(T[] dest, Object[] target) {
        if(dest.length != target.length)
            return null;
        for(int i = 0;i < dest.length;++ i)
            dest[i] = safeCast((Class<T>)dest.getClass().getComponentType(), target[i]);
        return dest;
    }

    public static Object[] union(Object[] a, Object[] b) {
        Set<Object> temp = new HashSet<>();
        temp.addAll(Arrays.asList(a));
        temp.addAll(Arrays.asList(b));
        temp.remove(null); // Remove the single null element if there is one
        return temp.toArray();
    }

    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Object target) {
        Method gdecla = getMethod("getDeclaredAnnotation", target.getClass(), Class.class),
                ga = getMethod("getAnnotation", target.getClass(), Class.class);
        if(gdecla == null || ga == null)
            return null;
        T a = (T)invoke(ga, target, annotationClass);
        return a == null ? (T)invoke(gdecla, target, annotationClass) : a;
    }

    public static Method getMethod(String methodName, Class<?> clazz, Class<?>... parameterTypes) {
        return getByParameterTypesAndName(methodName, getMethods(clazz), parameterTypes);
    }

    public static List<Method> getMethods(Class<?> clazz) {
        Object[] methods = union(clazz.getDeclaredMethods(), clazz.getMethods());
        return Arrays.asList(arrayCast(new Method[methods.length], methods));
    }

    public static <T extends Annotation> List<Pair<Method, T>> getMethodsAnnotatedWith(Class<?> type, Class<T> clazz) {
        return getMethods(type).stream().filter(m -> m.isAnnotationPresent(clazz)).map(m -> new Pair<>(m, m.getAnnotation(clazz)))
                .collect(Collectors.toList());
    }

    public static <T extends Annotation> Pair<Method, T> getAnnotatedMethod(Class<?> type, Class<T> clazz) {
        List<Pair<Method, T>> methods = getMethodsAnnotatedWith(type, clazz);
        return methods.isEmpty() ? null : methods.get(0);
    }

    public static Class<?> asWrapper(Class<?> clazz) {
        if(clazz.isPrimitive()) {
            for(Map.Entry<Class<?>, Class<?>> entry : W2P.entrySet()) {
                if(entry.getValue().equals(clazz))
                    return entry.getKey();
            }
            return clazz;
        }else
            return clazz;
    }

    public static Class<?> asNative(Class<?> clazz) {
        if(clazz.isPrimitive() || String.class.equals(clazz))
            return clazz;
        Class<?> nativeClass = W2P.get(clazz);
        return nativeClass == null ? clazz : nativeClass;
    }

    public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... parameterTypes) {
        return getByParameterTypes(getConstructors(clazz), parameterTypes);
    }

    @SuppressWarnings("unchecked")
    public static <T> Constructor<T> getLowestParamCountConstructor(Class<T> clazz) {
        return Stream.of(union(clazz.getDeclaredConstructors(), clazz.getConstructors())).map(c -> (Constructor<T>)c)
                .min(Comparator.comparingInt(Constructor::getParameterCount)).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> List<Constructor<T>> getConstructors(Class<T> clazz) {
        return Stream.of(union(clazz.getDeclaredConstructors(), clazz.getConstructors())).map(c -> (Constructor<T>)c)
                .collect(Collectors.toList());
    }

    public static <T> T instantiate(Class<T> clazz, Object... parameters) {
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for(int i = 0;i < parameters.length;++ i)
            parameterTypes[i] = parameters[i].getClass();
        Constructor<T> c = getConstructor(clazz, parameterTypes);
        if(c == null)
            return null;
        boolean oas = c.isAccessible();
        c.setAccessible(true);
        try {
            T object = c.newInstance(matchParameterTypes(parameters, c.getParameterTypes()));
            c.setAccessible(oas);
            return object;
        }catch(InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static <T> T instantiate(Class<T> clazz, Function<Class<?>, Object> paramSupplier) {
        Constructor<T> c = getLowestParamCountConstructor(clazz);
        if(c == null)
            return null;
        Object[] params = new Object[c.getParameterCount()];
        Class<?>[] paramTypes = c.getParameterTypes();
        for(int i = 0;i < params.length;++ i)
            params[i] = paramSupplier.apply(paramTypes[i]);
        boolean oas = c.isAccessible();
        c.setAccessible(true);
        try {
            T object = c.newInstance(params);
            c.setAccessible(oas);
            return object;
        }catch(InstantiationException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static Object invoke(String methodName, Class<?> clazz, Object target, Object... parameters) {
        Class<?>[] parameterTypes = new Class<?>[parameters.length];
        for(int i = 0;i < parameters.length;++ i)
            parameterTypes[i] = parameters[i].getClass();
        Method m = getMethod(methodName, clazz, parameterTypes);
        if(m == null)
            return null;
        boolean oas = m.isAccessible();
        m.setAccessible(true);
        try {
            Object result = m.invoke(target, matchParameterTypes(parameters, m.getParameterTypes()));
            m.setAccessible(oas);
            return result;
        }catch(IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    public static Object invoke(Method method, Object target, Object... parameters) {
        boolean oas = method.isAccessible();
        method.setAccessible(true);
        try {
            Object result = method.invoke(target, matchParameterTypes(parameters, method.getParameterTypes()));
            method.setAccessible(oas);
            return result;
        }catch(IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }


    public static Field getFieldObject(String fieldName, Class<?> clazz) {
        try {
            return clazz.getField(fieldName);
        }catch(NoSuchFieldException ex) {
            try {
                return clazz.getDeclaredField(fieldName);
            }catch(NoSuchFieldException ex0) {
                return null;
            }
        }
    }

    public static Field[] getFields(Class<?> clazz) {
        Object[] fields = union(clazz.getDeclaredFields(), clazz.getFields());
        return arrayCast(new Field[fields.length], fields);
    }

    public static Object getFieldValue(String fieldName, Class<?> clazz, Object target) {
        return getFieldValue(getFieldObject(fieldName, clazz), target);
    }

    public static Object getFieldValue(Field field, Object target) {
        boolean oas = field.isAccessible();
        field.setAccessible(true);
        Object result;
        try {
            result = field.get(target);
        }catch(IllegalAccessException ex) {
            return null;
        }
        field.setAccessible(oas);
        return result;
    }

    public static boolean setFieldValue(String fieldName, Class<?> clazz, Object target, Object value) {
        return setFieldValue(getFieldObject(fieldName, clazz), target, value);
    }

    public static boolean setFieldValue(Field field, Object target, Object value) {
        boolean oas = field.isAccessible();
        field.setAccessible(true);
        try {
            field.set(target, value);
        }catch(IllegalAccessException ex) {
            return false;
        }
        field.setAccessible(oas);
        return true;
    }

    private static <T extends Executable> T getByParameterTypesAndName(String name, List<T> options, Class<?>[] parameterTypes) {
        outer:
        for(T option : options) {
            if(!Objects.equals(option.getName(), name))
                continue;
            Class<?>[] opts = option.getParameterTypes(); // OPTS = option parameter types
            if(opts.length == parameterTypes.length) {
                for(int i = 0;i < opts.length;++ i) {
                    if(!opts[i].isAssignableFrom(parameterTypes[i]))
                        continue outer;
                }
            }else
                continue;
            return option;
        }
        return null;
    }

    private static <T extends Executable> T getByParameterTypes(List<T> options, Class<?>[] parameterTypes) {
        outer:
        for(T option : options) {
            Class<?>[] opts = option.getParameterTypes(); // OPTS = option parameter types
            if(opts.length == parameterTypes.length) {
                for(int i = 0;i < opts.length;++ i) {
                    if(!opts[i].isAssignableFrom(parameterTypes[i]))
                        continue outer;
                }
            }else
                continue;
            return option;
        }
        return null;
    }

    private static Object[] matchParameterTypes(Object[] parameters, Class<?>[] parameterTypes) {
        Object[] matchedParams = new Object[parameters.length];
        for(int i = 0;i < parameters.length;++ i)
            matchedParams[i] = safeCast(parameterTypes[i], parameters[i]);
        return matchedParams;
    }
}