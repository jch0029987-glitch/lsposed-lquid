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
        private val processedViews = WeakHashMap<View, Boolean>()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return

        Log.d(TAG, "Initialized inside SystemUI via LibXposed API 102 on Android 16")

        try {
            val classLoader = param.classLoader ?: Thread.currentThread().contextClassLoader
            
            // Hook android.view.View's setBackground or layout attachment lifecycle directly
            // This bypasses any specific NotificationShadeWindowView class name changes entirely.
            val viewClass = classLoader.loadClass("android.view.View")
            val targetMethod = viewClass.getDeclaredMethod("onFinishInflate")
            targetMethod.isAccessible = true

            hook(targetMethod).intercept { chain ->
                val result = chain.proceed()
                try {
                    val view = chain.thisObject as? View
                    if (view != null) {
                        val className = view.javaClass.name
                        // Match shade, panel, or container views dynamically at runtime
                        if ((className.contains("Shade") || className.contains("Panel") || className.contains("Window")) 
                            && processedViews[view] != true 
                            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            
                            view.setBackgroundColor(Color.argb(45, 15, 15, 15))
                            view.setRenderEffect(RenderEffect.createBlurEffect(30f, 30f, Shader.TileMode.CLAMP))
                            processedViews[view] = true
                            
                            Log.d(TAG, "Applied liquid glass blur to dynamic view: $className")
                        }
                    }
                } catch (innerE: Throwable) {
                    // Suppress inner noise to keep SystemUI stable
                }
                result
            }
            Log.d(TAG, "Successfully attached dynamic runtime view monitor.")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize runtime view hook -> ${e.message}")
        }
    }
}
