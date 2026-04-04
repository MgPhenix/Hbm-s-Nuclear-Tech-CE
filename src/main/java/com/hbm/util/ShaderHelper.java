package com.hbm.util;

import com.hbm.core.HbmCorePlugin;
import com.hbm.interfaces.SuppressCheckedExceptions;
import com.hbm.lib.internal.MethodHandleHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

@SideOnly(Side.CLIENT)
@SuppressCheckedExceptions
public class ShaderHelper {

    private static final MethodHandle SHADER_PACK_LOADED;
    private static final MethodHandle IS_SHADOW_PASS;

    static {
        if (HbmCorePlugin.isOptifinePresent()) {
            Class<?> shadersClass = Class.forName("net.optifine.shaders.Shaders");
            SHADER_PACK_LOADED = MethodHandleHelper.findStaticGetter(shadersClass, "shaderPackLoaded", boolean.class);
            IS_SHADOW_PASS = MethodHandleHelper.findStatic(shadersClass, "isShadowPass",
                    MethodType.methodType(boolean.class));
        } else {
            SHADER_PACK_LOADED = null;
            IS_SHADOW_PASS = null;
        }
    }

    public static boolean areShadersActive() {
        if (SHADER_PACK_LOADED == null) return false;
        return (boolean) SHADER_PACK_LOADED.invokeExact();
    }

    public static boolean isShadowPass() {
        if (IS_SHADOW_PASS == null) return false;
        return (boolean) IS_SHADOW_PASS.invokeExact();
    }
}
