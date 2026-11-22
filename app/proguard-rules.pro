# Keep Retrofit / Gson models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.yuranime.dinz.data.model.** { *; }
-keepclassmembers class com.yuranime.dinz.data.model.** { *; }

# Keep LibGDX & AndroidFragmentApplication
-dontwarn com.badlogic.gdx.**
-keep class com.badlogic.gdx.backends.android.AndroidFragmentApplication { *; }
-keep class com.badlogic.gdx.backends.android.AndroidApplicationConfiguration { *; }
-keep class com.badlogic.gdx.maps.** { *; }
-keep class com.yuranime.dinz.rpg.** { *; }
