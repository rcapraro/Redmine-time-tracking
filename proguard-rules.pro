# SLF4J — provider is loaded via ServiceLoader (META-INF/services).
-keep class org.slf4j.simple.SimpleServiceProvider { *; }
-keep class * implements org.slf4j.spi.SLF4JServiceProvider { *; }
-keep class org.slf4j.helpers.** { *; }

# Ktor — engines and a few internals are resolved dynamically.
-keep class io.ktor.client.engine.cio.** { *; }
-dontwarn io.ktor.utils.io.jvm.javaio.PollersKt
-keep class io.ktor.network.sockets.UnixSocketAddress { *; }
