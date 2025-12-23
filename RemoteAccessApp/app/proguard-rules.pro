# R8 rules for Ktor & Netty
-keep class io.netty.** { *; }
-keepnames class io.netty.** { *; }
-dontwarn io.netty.**
-dontwarn io.ktor.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn org.eclipse.jetty.alpn.**
-dontwarn org.conscrypt.**
-dontwarn java.lang.invoke.**
-dontwarn com.sun.**

# R8 rules for Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Specific fix for "Missing class io.netty.internal.tcnative.AsyncSSLPrivateKeyMethod" and others
-dontwarn io.netty.internal.tcnative.**

# R8 rules for Reactor
-dontwarn reactor.blockhound.integration.BlockHoundIntegration
