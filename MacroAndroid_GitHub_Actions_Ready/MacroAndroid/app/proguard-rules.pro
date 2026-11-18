-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.** { *; }
-keep,includedescriptorclasses class com.macro.app.model.**$$serializer { *; }
-keepclassmembers class com.macro.app.model.** {
    *** Companion;
}
