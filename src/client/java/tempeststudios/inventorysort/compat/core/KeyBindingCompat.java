package tempeststudios.inventorysort.compat.core;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
        Class<?> categoryClass = Class.forName("net.minecraft.client.KeyMapping$Category");
        Object category = createCategory(categoryClass);
        Constructor<KeyMapping> constructor = KeyMapping.class.getConstructor(
                String.class, InputConstants.Type.class, int.class, categoryClass);
        return constructor.newInstance(translationKey, InputConstants.Type.KEYSYM, defaultKeyCode, category);
    }

    private static Object createCategory(Class<?> categoryClass) throws ReflectiveOperationException {
        if (cachedCategory != null && cachedCategoryClass == categoryClass) {
            return cachedCategory;
        }

        Object category;
        try {
            Object categoryId = createIdentifier();
            Method register = categoryClass.getMethod("register", categoryId.getClass());
            category = register.invoke(null, categoryId);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            Field misc = categoryClass.getField("MISC");
            category = misc.get(null);
        }
        cachedCategory = category;
        cachedCategoryClass = categoryClass;
        return category;
    }

    private static Object createIdentifier() throws ReflectiveOperationException {
        ReflectiveOperationException failure = null;
        for (String className : new String[]{
                "net.minecraft.resources.ResourceLocation",
                "net.minecraft.resources.Identifier"
        }) {
            try {
                Class<?> identifierClass = Class.forName(className);
                return createIdentifier(identifierClass);
            } catch (ReflectiveOperationException e) {
                failure = e;
            }
        }

        throw failure != null ? failure : new ClassNotFoundException("No Minecraft identifier class found");
    }

    private static Object createIdentifier(Class<?> identifierClass) throws ReflectiveOperationException {
        try {
            Method fromNamespaceAndPath = identifierClass.getMethod(
                    "fromNamespaceAndPath", String.class, String.class);
            return fromNamespaceAndPath.invoke(null, CATEGORY_NAMESPACE, CATEGORY_PATH);
        } catch (NoSuchMethodException ignored) {
            Constructor<?> constructor = identifierClass.getConstructor(String.class, String.class);
            return constructor.newInstance(CATEGORY_NAMESPACE, CATEGORY_PATH);
        }
    }
}
