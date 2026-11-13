# SLF4J — provider is loaded via ServiceLoader (META-INF/services).
-keep class org.slf4j.simple.SimpleServiceProvider { *; }
-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class org.slf4j.helpers.** { *; }

# Ktor — engines and a few internals are resolved dynamically.
-keep class io.ktor.client.engine.cio.** { *; }
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt
-keep class io.ktor.network.sockets.UnixSocketAddress { *; }

# JNA (OS keychain backends) — native structures and library interfaces are bound by reflection.
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-keep class com.github.javakeyring.** { *; }

# Linux Secret Service backend — D-Bus proxies, struct marshalling and the transport ServiceLoader use reflection.
-keep class org.freedesktop.** { *; }
-keep class org.gnome.keyring.** { *; }
