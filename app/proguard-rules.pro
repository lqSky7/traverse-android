# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.traverse.android.**$$serializer { *; }
-keepclassmembers class com.traverse.android.** {
    *** Companion;
}
-keepclasseswithmembers class com.traverse.android.** {
    kotlinx.serialization.KSerializer serializer(...);
}
