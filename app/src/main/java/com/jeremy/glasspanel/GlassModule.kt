package com.jeremy.glasspanel

import android.content.res.Resources
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

        Log.d(TAG, "Initialized inside SystemUI via LibXposed API 102 - Hooking LayoutInflater")

        try {
            val targetClassLoader = param.classLoader ?: Thread.currentThread().contextClassLoader
            val layoutInflaterClass = targetClassLoader.loadClass("android.view.LayoutInflater")

            // Hook the primary inflate overload: View inflate(int resource, ViewGroup root, boolean attachToRoot)
            val inflateMethod = layoutInflaterClass.getDeclaredMethod(
                "inflate",
                Int::class.javaPrimitiveType,
                ViewGroup::class.java,
                Boolean::class.javaPrimitiveType
            )

            hook(inflateMethod).intercept { chain ->
                val result = chain.proceed()

                try {
                    val resourceId = chain.args[0] as? Int ?: 0
                    val inflatedView = result as? View

                    if (inflatedView != null && resourceId != 0) {
                        try {
                            val resName = inflatedView.resources.getResourceEntryName(resourceId) ?: ""
                            
                            // Target shade, quick settings, status bar, or notification panel layout identifiers
                            if (resName.contains("notification_shade") || 
                                resName.contains("status_bar") || 
                                resName.contains("quick_settings") ||
                                resName.contains("keyguard") ||
                                resName.contains("shade_window")) {
                                
                                applyGlassEffect(inflatedView, resName)
                            }
                        } catch (_: Resources.NotFoundException) {
                            // Resource ID might not have a friendly entry name in this context, ignore safely
                        }
                    }
                } catch (innerE: Throwable) {
                    Log.e(TAG, "Error inside inflate interception -> ${innerE.message}", innerE)
                }

                result
            }

            Log.d(TAG, "Successfully hooked LayoutInflater globally for SystemUI.")
        } catch (e: Throwable) {
            Log.e(TAG, "Critical failure hooking LayoutInflater -> ${e.message}", e)
        }
    }

    private fun applyGlassEffect(view: View, layoutName: String) {
        if (processedViews[view] == true) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                view.setBackgroundColor(Color.argb(45, 15, 15, 15))

                val blurEffect = RenderEffect.createBlurEffect(
                    30f,
                    30f,
                    Shader.TileMode.CLAMP
                )
                view.setRenderEffect(blurEffect)
                processedViews[view] = true

                Log.d(TAG, "Successfully applied liquid glass blur to layout resource: $layoutName")
            }
        } catch (innerE: Throwable) {
            Log.e(TAG, "Failed to apply blur effect to $layoutName -> ${innerE.message}", innerE)
        }
    }
}
