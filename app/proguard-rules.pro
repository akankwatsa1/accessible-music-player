# Media3 / ExoPlayer
-dontwarn androidx.media3.**

# Keep MediaStore and session entry points referenced from the manifest.
-keep class com.akankwatsa.accessiblemusicplayer.** { *; }

# Kotlin metadata is not needed at runtime.
-dontwarn kotlin.**