-assumenosideeffects class java.io.PrintStream {
    public void print(...);
    public void println(...);
    public java.io.PrintStream append(...);
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkNotNull(...);
    public static void checkExpressionValueIsNotNull(...);
    public static void checkNotNullParameter(...);
    public static void checkParameterIsNotNull(...);
    public static void throw*(...);
}
-repackageclasses ""
-allowaccessmodification
-mergeinterfacesaggressively

-keep,allowoptimization public class * extends android.app.Activity
-keep,allowoptimization public class * extends android.app.Application
-keep,allowoptimization public class * extends android.app.Service
-keep,allowoptimization public class * extends android.content.BroadcastReceiver
-keep,allowoptimization public class * extends android.content.ContentProvider
-keep,allowoptimization class androidx.navigation.Navigator$Name { *; }
-keep,allowoptimization @androidx.navigation.Navigator$Name class *
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

-dontwarn androidx.window.extensions.area.ExtensionWindowAreaPresentation
-dontwarn androidx.window.extensions.core.util.function.Consumer
-dontwarn androidx.window.extensions.core.util.function.Function
-dontwarn androidx.window.extensions.core.util.function.Predicate

-keepclasseswithmembernames class * {
    native <methods>;
}