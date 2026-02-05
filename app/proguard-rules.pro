##-keepattributes !*
#-assumenosideeffects class java.io.PrintStream{*;}
#-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
#    public static void checkNotNull(...);
#    public static void checkExpressionValueIsNotNull(...);
#    public static void checkNotNullParameter(...);
#    public static void checkParameterIsNotNull(...);
#    public static void throw*(...);
#}
#-repackageclasses ""
#-allowaccessmodification
#-mergeinterfacesaggressively
#
#-keep public class * extends android.app.Activity
#-keep public class * extends android.app.Application
#-keep public class * extends android.app.Service
#-keep public class * extends android.content.BroadcastReceiver
#-keep public class * extends android.content.ContentProvider
#-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
#-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
#
#-keep class androidx.compose.ui.platform.WrappedComposition { *; }
#
#-keepattributes *Annotation*
#-keepattributes Signature
#-keepattributes InnerClasses
#-keepattributes EnclosingMethod
#-keepattributes SourceFile,LineNumberTable
#
#-keep class androidx.navigation.Navigator$Name { *; }
#-keep @androidx.navigation.Navigator$Name class *
#-keep class androidx.navigation.compose.** { *; }
