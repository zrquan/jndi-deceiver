package util

import sun.reflect.ReflectionFactory
import java.lang.reflect.Field

fun getField(clazz: Class<*>, fieldName: String): Field {
    try {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        return field
    } catch (e: NoSuchFieldException) {
        if (clazz.superclass != Any::class.java) {
            return getField(clazz.superclass, fieldName)
        }
        throw e
    }
}

fun setFieldVal(obj: Any, fieldName: String, value: Any) =
    getField(obj::class.java, fieldName).run {
        set(obj, value)
    }

@Suppress("UNCHECKED_CAST")
fun <T> createWithoutConstructor(clazz: Class<T>): T {
    val consArgTypes = emptyArray<Class<*>>()
    val consArgs = emptyArray<Any>()

    val cons = Any::class.java.getDeclaredConstructor(*consArgTypes).apply { isAccessible = true }
    val sc = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(clazz, cons)
        .apply { isAccessible = true }
    return sc.newInstance(*consArgs) as T
}
