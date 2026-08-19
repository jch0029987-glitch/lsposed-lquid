package com.jeremy.glasspanel

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.util.WeakHashMap

class GlassModule : XposedModule() {

    companion object {
        private const val TAG = "GlassPanel"
        // Prevent redundant hook logic/logging on views that have already been processed
        private val processedViews = WeakHashMap<View, Boolean>()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return

        Log.d(TAG, "Initialized inside SystemUI via LibXposed API 102")

        try {
            val targetClassLoader = param.classLoader

            val targetClass = try {
                targetClassLoader.loadClass("com.android.systemui.shade.NotificationShadeWindowView")
            } catch (e: ClassNotFoundException) {
                Log.d(TAG, "Modern shade path not found, falling back to legacy path...")
                targetClassLoader.loadClass("com.android.systemui.statusbar.phone.NotificationShadeWindowView")
            }

            val targetMethod = targetClass.getDeclaredMethod("onFinishInflate")

            hook(targetMethod).intercept { chain ->
                val result = chain.proceed()

                try {
                    val view = chain.thisObject as? View
                    if (view != null) {
                        // Ensure we only apply this once per view instance without needing getRenderEffect()
                        if (processedViews[view] != true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            view.setBackgroundColor(Color.argb(45, 15, 15, 15))

                            val blurEffect = RenderEffect.createBlurEffect(
                                30f,
                                30f,
                                Shader.TileMode.CLAMP
                            )
                            view.setRenderEffect(blurEffect)
                            processedViews[view] = true

                            Log.d(TAG, "Successfully applied liquid glass blur filter.")
                        }
                    }
                } catch (innerE: Throwable) {
                    Log.e(TAG, "Non-fatal error during hook execution -> ${innerE.message}", innerE)
                }

                result
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Critical failure hooking notification shade -> ${e.message}", e)
        }
    }
}
