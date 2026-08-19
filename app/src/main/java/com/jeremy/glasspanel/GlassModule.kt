package com.jeremy.glasspanel

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class GlassModule : XposedModule() {

    companion object {
        private const val TAG = "GlassPanel"
    }

    override fun onPackageLoaded(param: PackageLoadedParam) {
        if (param.packageName != "com.android.systemui") return

        Log.d(TAG, "Initialized inside SystemUI via LibXposed API 102")

        try {
            val classLoader = param.classLoader

            // Attempt to resolve target class with multi-version fallback support
            val targetClass = try {
                classLoader.loadClass("com.android.systemui.shade.NotificationShadeWindowView")
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Modern shade path not found, falling back to legacy path...")
                classLoader.loadClass("com.android.systemui.statusbar.phone.NotificationShadeWindowView")
            }

            val targetMethod = targetClass.getDeclaredMethod("onFinishInflate")

            // Install the hook using modern API 102 chain interceptor
            hook(targetMethod).intercept { chain ->
                val result = chain.proceed()

                try {
                    val view = chain.thisObject as? View
                    if (view != null) {
                        // Apply background tint and render effect conditionally for API 31+
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (view.renderEffect == null) {
                                view.setBackgroundColor(Color.argb(45, 15, 15, 15))

                                val blurEffect = RenderEffect.createBlurEffect(
                                    30f,
                                    30f,
                                    Shader.TileMode.CLAMP
                                )
                                view.setRenderEffect(blurEffect)

                                Log.d(TAG, "Successfully applied liquid glass blur filter.")
                            }
                        }
                    }
                } catch (innerE: Throwable) {
                    Log.e(TAG, "Non-fatal error during hook execution -> ${innerE.message}")
                }

                result
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Critical failure hooking notification shade -> ${e.message}")
        }
    }
}
