private const val kotlinVersion = "2.1.0"

object Plugins {
    const val KOTLIN = kotlinVersion
    const val GRGIT = "5.3.0"
    const val KTLINT = "12.1.2"
    const val DOKKA = "2.0.0"
    const val GRADLE_PLUGIN_PUBLISH = "1.3.0"
}

object Dependencies {
    const val KOTLIN = kotlinVersion
    const val STARGRAD = "0.5.2"
    const val PLAT4K = "1.6.0"
    const val TOMLJ = "1.1.1"
    const val COMMONS_IO = "2.18.0"
    const val GSON = "2.11.0"
    const val ZIP4J = "2.11.5"

    val kotlinModules = arrayOf("stdlib")
}

object Repositories {
    val mavenUrls = arrayOf(
        "https://jitpack.io/",
    )
}
