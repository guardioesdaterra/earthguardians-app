# Earth Guardians app R8 config

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class org.earthguardians.app.**$$serializer { *; }
-keepclassmembers class org.earthguardians.app.** {
    *** Companion;
}
-keepclasseswithmembers class org.earthguardians.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- WebView JS bridge must never be stripped ---
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepclassmembers class org.earthguardians.app.ui.web.EgWebBridge {
    public *;
}

# --- Compose ---
-dontwarn org.bouncycastle.**
-dontwarn java.lang.invoke.StringConcatFactory
