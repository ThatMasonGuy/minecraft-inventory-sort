package tempeststudios.inventorysort.compat.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class KeyBindingCompat {
    private static final String CATEGORY_TRANSLATION_KEY = "key.categories.inventorysort.general";
    private static final String CATEGORY_NAMESPACE = "inventorysort";
    private static final String CATEGORY_PATH = "general";
    private static Object cachedCategory;
    private static Class<?> cachedCategoryClass;

    private KeyBindingCompat() {
    }

    public static KeyMapping register(String translationKey, int defaultKeyCode) {
        return registerKeyMapping(createKeyMapping(translationKey, defaultKeyCode));
    }

    private static KeyMapping registerKeyMapping(KeyMapping keyMapping) {
        Throwable failure = null;
        // Fabric renamed this client helper in the 26.x lane; keep one Core source compiling across both APIs.
        for (String helperClassName : new String[]{
                "net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper",
                "net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper"
        }) {
            for (String methodName : new String[]{"registerKeyMapping", "registerKeyBinding"}) {
                try {
                    Class<?> helperClass = Class.forName(helperClassName);
                    Method register = helperClass.getMethod(methodName, KeyMapping.class);
                    Object registered = register.invoke(null, keyMapping);
                    return registered instanceof KeyMapping ? (KeyMapping) registered : keyMapping;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    failure = e;
                }
            }
        }

        IllegalStateException exception = new IllegalStateException("Failed to register keybinding");
        if (failure != null) {
            exception.addSuppressed(failure);
        }
        throw exception;
    }

    private static KeyMapping createKeyMapping(String translationKey, int defaultKeyCode) {
        Throwable categoryFailure = null;
        try {
            return createCategoryKeyMapping(translationKey, defaultKeyCode);
        } catch (ReflectiveOperationException | RuntimeException e) {
            categoryFailure = e;
        }

        try {
            Constructor<KeyMapping> constructor = KeyMapping.class.getConstructor(
                    String.class, InputConstants.Type.class, int.class, String.class);
            return constructor.newInstance(
                    translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, CATEGORY_TRANSLATION_KEY);
        } catch (ReflectiveOperationException e) {
            IllegalStateException failure = new IllegalStateException("Failed to create keybinding", e);
            if (categoryFailure != null) {
                failure.addSuppressed(categoryFailure);
            }
            throw failure;
        }
    }

    private static KeyMapping createCategoryKeyMapping(String translationKey, int defaultKeyCode)
            throws ReflectiveOperationException {
        Class<?> categoryClass = findCategoryClass();
        Object category = createCategory(categoryClass);
        Constructor<KeyMapping> constructor = KeyMapping.class.getConstructor(
                String.class, InputConstants.Type.class, int.class, categoryClass);
        return constructor.newInstance(translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, category);
    }

    private static Class<?> findCategoryClass() throws ReflectiveOperationException {
        for (Constructor<?> constructor : KeyMapping.class.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length >= 4
                    && parameterTypes[0] == String.class
                    && parameterTypes[1] == InputConstants.Type.class
                    && parameterTypes[2] == int.class
                    && parameterTypes[3] != String.class) {
                return parameterTypes[3];
            }
        }

        throw new NoSuchMethodException("No KeyMapping category constructor found");
    }

    private static Object createCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        if (cachedCategory != null && cachedCategoryClass == categoryClass) {
            return cachedCategory;
        }

        Object category;
        Object categoryId = createIdentifierForCategory(categoryClass);
        if (categoryId != null) {
            category = invokeCategoryFactory(categoryClass, categoryId);
            if (category == null) {
                category = constructCategory(categoryClass, categoryId);
            }
        } else {
            category = null;
        }
        if (category == null) {
            category = findExistingCategory(categoryClass);
        }
        cachedCategory = category;
        cachedCategoryClass = categoryClass;
        return category;
    }

    private static Object createIdentifierForCategory(Class<?> categoryClass) {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    && method.getReturnType() == categoryClass
                    && method.getParameterCount() == 1) {
                Object identifier = createIdentifier(method.getParameterTypes()[0]);
                if (identifier != null) {
                    return identifier;
                }
            }
        }

        for (Constructor<?> constructor : categoryClass.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 1) {
                Object identifier = createIdentifier(constructor.getParameterTypes()[0]);
                if (identifier != null) {
                    return identifier;
                }
            }
        }

        return null;
    }

    private static Object invokeCategoryFactory(Class<?> categoryClass, Object categoryId) {
        for (Method method : categoryClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != categoryClass
                    || method.getParameterCount() != 1
                    || !method.getParameterTypes()[0].isInstance(categoryId)) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method.invoke(null, categoryId);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static Object constructCategory(Class<?> categoryClass, Object categoryId) {
        for (Constructor<?> constructor : categoryClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 1 || !parameterTypes[0].isInstance(categoryId)) {
                continue;
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(categoryId);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            }
        }

        return null;
    }

    private static Object findExistingCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        for (Field field : categoryClass.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != categoryClass) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(null);
            if (value != null) {
                return value;
            }
        }

        throw new NoSuchFieldException("No existing KeyMapping category found");
    }

    private static Object createIdentifier(Class<?> identifierClass) {
        for (Method method : identifierClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != identifierClass
                    || method.getParameterCount() != 2
                    || method.getParameterTypes()[0] != String.class
                    || method.getParameterTypes()[1] != String.class) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method.invoke(null, CATEGORY_NAMESPACE, CATEGORY_PATH);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            }
        }

        for (Constructor<?> constructor : identifierClass.getDeclaredConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 2
                    || parameterTypes[0] != String.class
                    || parameterTypes[1] != String.class) {
                continue;
            }
            try {
                constructor.setAccessible(true);
                return constructor.newInstance(CATEGORY_NAMESPACE, CATEGORY_PATH);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            }
        }

        for (Method method : identifierClass.getDeclaredMethods()) {
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != identifierClass
                    || method.getParameterCount() != 1
                    || method.getParameterTypes()[0] != String.class) {
                continue;
            }
            try {
                method.setAccessible(true);
                return method.invoke(null, CATEGORY_NAMESPACE + ":" + CATEGORY_PATH);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
            }
        }

        return null;
    }
}
