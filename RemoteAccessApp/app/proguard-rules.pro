# Ktor
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.bhikan.airtap.**$$serializer { *; }
-keepclassmembers class com.bhikan.airtap.** {
    *** Companion;
}
-keepclasseswithmembers class com.bhikan.airtap.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Netty
-dontwarn io.netty.**
-keep class io.netty.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
