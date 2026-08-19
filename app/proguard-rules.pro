# Keep Xposed module entry point and hooks from being obfuscated or removed
-keep public class com.jeremy.glasspanel.GlassModule {
    *;
}

-keep class com.android.systemui.shade.NotificationShadeWindowView {
    *;
}

# Preserve LibXposed API hooks
-keep class io.github.libxposed.api.** { *; }
-dontwarn io.github.libxposed.api.**
