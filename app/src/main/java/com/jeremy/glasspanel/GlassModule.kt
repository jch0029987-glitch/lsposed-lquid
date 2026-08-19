package com.jeremy.glasspanel

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.View
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import java.lang.reflect.Method
import java.util.WeakHashMap

class GlassModule : XposedModule() {

    companion object {
        private const val TAG = "GlassPanel"
        private val processedViews = WeakHashMap<View, Boolean>()
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return

        Log.d(TAG, "Initialized inside SystemUI via LibXposed API 102")

        try {
            val targetClassLoader = param.classLoader ?: Thread.currentThread().contextClassLoader

            val targetClass = findNotificationShadeWindowView(targetClassLoader)
            if (targetClass == null) {
                Log.e(TAG, "Failed to locate NotificationShadeWindowView class across all known namespaces.")
                return
            }

                                                               Log.d(TAG, "Successfully resolved target window class: ${targetClass.name}")

            // Safely find a reliable lifecycle method to hook
            val targetMethod = findTargetMethod(targetClass)
            if (targetMethod == null) {
                Log.e(TAG, "Failed to find onFinishInflate or onAttachedToWindow on ${targetClass.name}")
                return
            }

            Log.d(TAG, "Successfully hooked method: ${targetMethod.name}")

            hook(targetMethod).intercept { chain ->
                val result = chain.proceed()

                try {
                    val view = chain.thisObject as? View
                    if (view != null) {
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

    private fun findNotificationShadeWindowView(classLoader: ClassLoader): Class<*>? {
        val candidatePaths = arrayOf(
            "com.android.systemui.shade.NotificationShadeWindowView",
            "com.android.systemui.statusbar.phone.NotificationShadeWindowView",
            "com.android.systemui.scene.ui.composable.SceneContainerWindowView",
            "com.android.systemui.statusbar.phone.StatusBarWindowView"
        ]

        for (path in candidatePaths) {
            try {
                return classLoader.loadClass(path)
            } catch (_: ClassNotFoundException) {
                // Try next candidate path
            }
        }
        return null
    }

    private fun findTargetMethod(clazz: Class<*>): Method? {
        val methodsToTry = arrayOf("onFinishInflate", "onAttachedToWindow")
        for (methodName in methodsToTry) {
            try {
                val method = clazz.getDeclaredMethod(methodName)
                method.isAccessible = true
                return method
            } catch (_: NoSuchMethodException) {
                // Check parent classes or next method name
            }
        }
        
        // Fallback: search declared methods for either name if signatures differ
        var current: Class<*>? = clazz
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (method.name in methodsToTry) {
                    method.isAccessible = true
                    return method
                }
            }
            current = current.superclass
        }
        return null
    }
}
