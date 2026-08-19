package com.jeremy.glasspanel

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface

class GlassModule : XposedModule() {

    override fun onPackageLoaded(packageName: String, classLoader: ClassLoader, apkPath: String) {
        if (packageName != "com.android.systemui") return

        log("GlassPanel: Initialized inside SystemUI via LibXposed API 102")

        try {
            // Resolve the target SystemUI layout container class via standard reflection
            val targetClass = classLoader.loadClass("com.android.systemui.shade.NotificationShadeWindowView")
            val targetMethod = targetClass.getDeclaredMethod("onFinishInflate")

            // Install the hook using the modern API 102 chain interceptor
            hook(targetMethod).intercept { chain ->
                // Execute original method chain first
                val result = chain.proceed()

                // Extract receiver instance using chain.thisObject
                val view = chain.thisObject as? View
                if (view != null) {
                    // Apply translucent tinted glass background (dark frosted tint)
                    view.setBackgroundColor(Color.argb(45, 15, 15, 15))

                    // Apply hardware-accelerated blur render effect
                    val blurEffect = RenderEffect.createBlurEffect(
                        30f, // Blur X
                        30f, // Blur Y
                        Shader.TileMode.CLAMP
                    )
                    view.setRenderEffect(blurEffect)

                    log("GlassPanel: Successfully applied liquid glass blur filter.")
                }

                result
            }
        } catch (e: Throwable) {
            log("GlassPanel: Failed to hook notification shade -> ${e.message}")
        }
    }
}
