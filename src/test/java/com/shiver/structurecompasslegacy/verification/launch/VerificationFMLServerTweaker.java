package com.shiver.structurecompasslegacy.verification.launch;

import java.io.File;
import java.util.Collections;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.launch.MixinTweaker;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

/** 独立 JVM 的真实 Mixin 加载验证，不启动客户端或玩家服务器。 */
public final class VerificationFMLServerTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> arguments, File gameDirectory, File assetsDirectory, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        classLoader.addClassLoaderExclusion("org.spongepowered.");
        classLoader.addClassLoaderExclusion("zone.rong.mixinbooter.service.");
        classLoader.addClassLoaderExclusion("zone.rong.mixinbooter.util.");
        classLoader.addClassLoaderExclusion("zone.rong.mixinbooter.MixinBooter");
        classLoader.addClassLoaderExclusion("zone.rong.mixinbooter.MixinLoader");
        classLoader.addClassLoaderExclusion("zone.rong.mixinbooter.Context");
        classLoader.addClassLoaderExclusion("org.objectweb.asm.");
        classLoader.addClassLoaderExclusion("com.llamalad7.mixinextras.");
        MixinTweaker mixin = new MixinTweaker();
        mixin.acceptOptions(Collections.emptyList(), null, null, "verification");
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.SERVER);
        Mixins.addConfiguration("mixins.structurecompasslegacy.json");
        mixin.injectIntoClassLoader(classLoader);
    }

    @Override
    public String getLaunchTarget() {
        return "com.shiver.structurecompasslegacy.verification.RuntimeVerificationMain";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
