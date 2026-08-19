package com.jeremy.glasspanel

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.View
import io.github.libxposed.api.XposedModule

class GlassModule : XposedModule() {

    override fun onPackageLoaded(packageName: String, classLoader: ClassLoader) {
        if (packageName != "com.android.systemui") return

        log("GlassPanel", "Initialized inside SystemUI via LibXposed API 102")

        try {
            // Attempt to resolve target class with multi-version fallback support
            val targetClass = try {
                classLoader.loadClass("com.android.systemui.shade.NotificationShadeWindowView")
            } catch (e: ClassNotFoundException) {
                log("GlassPanel", "Modern shade path not found, falling back to legacy path...")
                classLoader.loadClass("com.android.systemui.statusbar.phone.NotificationShadeWindowView")
            }

            val targetMethod = targetClass.getDeclaredMethod("onFinishInflate")

            // Install the hook using modern API 102 chain interceptor
            hook(targetMethod).intercept { chain ->
                // Execute original method chain first
                val result = chain.proceed()

                try {
                    val view = chain.thisObject as? View
                    if (view != null) {
                        // Idempotent guard: only apply if render effect hasn't been set yet
                        if (view.renderEffect == null) {
                            // Apply translucent tinted glass background (dark frosted tint)
                            view.setBackgroundColor(Color.argb(45, 15, 15, 15))

                            // Apply hardware-accelerated blur render effect
                            val blurEffect = RenderEffect.createBlurEffect(
                                30f, // Blur X
                                30f, // Blur Y
                                Shader.TileMode.CLAMP
                            )
                            view.setRenderEffect(blurEffect)

                            log("GlassPanel", "Successfully applied liquid glass blur filter.")
                        }
                    }
                } catch (innerE: Throwable) {
                    log("GlassPanel", "Non-fatal error during hook execution -> ${innerE.message}")
                }

                result
            }
        } catch (e: Throwable) {
            log("GlassPanel", "Critical failure hooking notification shade -> ${e.message}")
        }
    }
}
