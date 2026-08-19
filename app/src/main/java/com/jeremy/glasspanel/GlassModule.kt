package com.jeremy.glasspanel

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

class GlassModule : XposedModule() {

    companion object {
        private const val TAG = "GlassScanner"
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != "com.android.systemui") return

        Log.w(TAG, "=== SYSTEMUI DIAGNOSTIC SCANNER STARTED ===")

        try {
            val classLoader = param.classLoader ?: Thread.currentThread().contextClassLoader

            // 1. Intercept ClassLoader to catch classes containing shade, panel, statusbar, scene, or window
            val loadClassMethod = ClassLoader::class.java.getDeclaredMethod("loadClass", String::class.java, Boolean::class.javaPrimitiveType)
            loadClassMethod.isAccessible = true

            hook(loadClassMethod).intercept { chain ->
                val className = chain.args[0] as? String ?: ""
                val result = chain.proceed()

                if (className.contains("Shade", ignoreCase = true) ||
                    className.contains("Panel", ignoreCase = true) ||
                    className.contains("Scene", ignoreCase = true) ||
                    className.contains("StatusBar", ignoreCase = true) ||
                    className.contains("Window", ignoreCase = true)) {
                    Log.w(TAG, "DISCOVERED CLASS: $className")
                }

                result
            }

            // 2. Intercept LayoutInflater to catch actual layout resource names and their root views
            val layoutInflaterClass = classLoader.loadClass("android.view.LayoutInflater")
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
                    val view = result as? View
                    if (resourceId != 0 && view != null) {
                        try {
                            val resName = view.resources.getResourceEntryName(resourceId)
                            Log.w(TAG, "INFLATED LAYOUT -> Name: $resName | ViewClass: ${view.javaClass.name}")
                        } catch (_: Resources.NotFoundException) {
                            Log.w(TAG, "INFLATED LAYOUT -> ResId: $resourceId | ViewClass: ${view.javaClass.name}")
                        }
                    }
                } catch (_: Throwable) {
                }
                result
            }

            Log.w(TAG, "Successfully installed diagnostic scanner hooks.")
        } catch (e: Throwable) {
            Log.e(TAG, "Scanner installation failed -> ${e.message}", e)
        }
    }
}
