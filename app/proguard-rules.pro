# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class id.algaffar.jwstv.**$$serializer { *; }
-keepclassmembers class id.algaffar.jwstv.** {
    *** Companion;
}
-keepclasseswithmembers class id.algaffar.jwstv.** {
    kotlinx.serialization.KSerializer serializer(...);
}
