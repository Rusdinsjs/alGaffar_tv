# ==================== KOTLINX SERIALIZATION ====================
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
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
-keep class id.algaffar.jwstv.domain.model.** { *; }
-keep class id.algaffar.jwstv.data.location.** { *; }
-keep class id.algaffar.jwstv.data.update.** { *; }
-keep class id.algaffar.jwstv.data.media.** { *; }
-keep class id.algaffar.jwstv.data.cctv.** { *; }

# ==================== KTOR CIO SERVER ====================
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.atomicfu.**
-dontwarn org.slf4j.**

# ==================== MEDIA3 EXOPLAYER ====================
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ==================== FASTEXCEL & USB-SERIAL & ADHAN ====================
-keep class org.dhatim.fastexcel.** { *; }
-dontwarn org.dhatim.fastexcel.**
-dontwarn javax.xml.stream.**
-keep class com.hoho.android.usbserial.** { *; }
-dontwarn com.hoho.android.usbserial.**
-keep class com.batoulapps.adhan.** { *; }
-keep class com.google.zxing.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
