import java.net.URL
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
    with(Plugins) {
        // Language Plugins
        `java-library`
        kotlin("jvm") version KOTLIN

        // Git Repo Information
        id("org.ajoberstar.grgit") version GRGIT

        // Code Quality
        id("org.jlleitschuh.gradle.ktlint") version KTLINT

        // Documentation Generation
        id("org.jetbrains.dokka") version DOKKA

        // Gradle Plugin Portal Publication
        id("com.gradle.plugin-publish") version GRADLE_PLUGIN_PUBLISH
        `java-gradle-plugin`
        `maven-publish`
        signing
    }
}

group = Coordinates.GROUP
version = Coordinates.VERSION

// What JVM version should this project compile to
val targetVersion = "17"
// What JVM version this project is written in
val sourceVersion = "17"

// Maven Repositories
repositories {
    mavenLocal()
    mavenCentral()

    Repositories.mavenUrls.forEach(::maven)
}

// Project Dependencies
dependencies {
    compileOnly(gradleApi())

    with(Dependencies) {
        kotlinModules.forEach {
            implementation("org.jetbrains.kotlin", "kotlin-$it", KOTLIN)
        }

        implementation("fr.stardustenterprises", "stargrad", STARGRAD)
        implementation("fr.stardustenterprises", "plat4k", PLAT4K)

        implementation("org.tomlj", "tomlj", TOMLJ)
        implementation("commons-io", "commons-io", COMMONS_IO)
        implementation("com.google.code.gson", "gson", GSON)
        implementation("net.lingala.zip4j", "zip4j", ZIP4J)

        testImplementation("org.jetbrains.kotlin", "kotlin-test", KOTLIN)
    }
}

configurations {
    // Makes all the configurations use the same Kotlin version.
    all {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") {
                useVersion(Dependencies.KOTLIN)
            }
        }
    }
}

// The latest commit ID
val buildRevision: String = grgit.log()[0].id ?: "dev"

// Disable unneeded rules
ktlint {
    this.disabledRules.add("no-wildcard-imports")
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Configure JVM versions
    compileKotlin {
        kotlinOptions.jvmTarget = targetVersion
    }
    compileJava {
        targetCompatibility = targetVersion
        sourceCompatibility = sourceVersion
    }

    dokkaHtml {
        val moduleFile = File(projectDir, "MODULE.temp.md")

        run {
            // In order to have a description on the rendered docs, we have to have
            // a file with the # Module thingy in it. That's what we're
            // automagically creating here.

            doFirst {
                moduleFile.writeText("# Module ${Coordinates.NAME}\n${Coordinates.DESC}")
            }

            doLast {
                moduleFile.delete()
            }
        }

        moduleName.set(Coordinates.NAME)

        dokkaSourceSets.configureEach {
            displayName.set("${Coordinates.NAME} on ${Coordinates.GIT_HOST}")
            includes.from(moduleFile.path)

            skipDeprecated.set(false)
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            reportUndocumented.set(true)
            suppressObviousFunctions.set(true)

            // Link the source to the documentation
            sourceLink {
                localDirectory.set(file("src"))
                remoteUrl.set(URL("https://${Coordinates.GIT_HOST}/${Coordinates.REPO_ID}/tree/trunk/src"))
            }

            // External documentation link template
//            externalDocumentationLink {
//                url.set(URL("https://javadoc.io/doc/net.java.dev.jna/jna/5.10.0/"))
//            }
        }
    }

    // The original artifact, we just have to add the API source output and the
    // LICENSE file.
    jar {
        fun normalizeVersion(versionLiteral: String): String {
            val regex = Regex("(\\d+\\.\\d+\\.\\d+).*")
            val match = regex.matchEntire(versionLiteral)
            require(match != null) {
                "Version '$versionLiteral' does not match version pattern, e.g. 1.0.0-QUALIFIER"
            }
            return match.groupValues[1]
        }

        val buildTimeAndDate = OffsetDateTime.now()
        val buildDate = DateTimeFormatter.ISO_LOCAL_DATE.format(buildTimeAndDate)
        val buildTime = DateTimeFormatter.ofPattern("HH:mm:ss.SSSZ").format(buildTimeAndDate)

        val javaVersion = System.getProperty("java.version")
        val javaVendor = System.getProperty("java.vendor")
        val javaVmVersion = System.getProperty("java.vm.version")

        mapOf(
            "Created-By" to "$javaVersion ($javaVendor $javaVmVersion)",
            "Build-Date" to buildDate,
            "Build-Time" to buildTime,
            "Build-Revision" to buildRevision,
            "Specification-Title" to project.name,
            "Specification-Version" to normalizeVersion(project.version.toString()),
            "Specification-Vendor" to Coordinates.VENDOR,
            "Implementation-Title" to Coordinates.NAME,
            "Implementation-Version" to Coordinates.VERSION,
            "Implementation-Vendor" to Coordinates.VENDOR,
            "Bundle-Name" to Coordinates.NAME,
            "Bundle-Description" to Coordinates.DESC,
            "Bundle-DocURL" to "https://${Coordinates.GIT_HOST}/${Coordinates.REPO_ID}",
            "Bundle-Vendor" to Coordinates.VENDOR,
            "Bundle-SymbolicName" to Coordinates.GROUP + '.' + Coordinates.NAME
        ).forEach { (k, v) ->
            manifest.attributes[k] = v
        }

        from("LICENSE")
    }

    // Source artifact, including everything the 'main' does but not compiled.
    create("sourcesJar", Jar::class) {
        group = "build"

        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)

        from("LICENSE")
    }

    // The Javadoc artifact, containing the Dokka output and the LICENSE file.
    create("javadocJar", Jar::class) {
        group = "build"

        val dokkaHtml = getByName("dokkaHtml")

        archiveClassifier.set("javadoc")
        dependsOn(dokkaHtml)
        from(dokkaHtml)

        from("LICENSE")
    }
}

// Define the default artifacts' tasks
val defaultArtifactTasks = arrayOf(
    tasks["sourcesJar"],
    tasks["javadocJar"]
)

// Declare the artifacts
artifacts {
    defaultArtifactTasks.forEach(::archives)
}

gradlePlugin {
    plugins {
        create("wrapperPlugin") {
            displayName = "Rust Wrapper"
            description = "A plugin that wraps Rust's build systems, for embedding Rust libraries in Java projects."
            id = "fr.stardustenterprises.rust.wrapper"
            implementationClass = "fr.stardustenterprises.gradle.rust.wrapper.WrapperPlugin"
        }
        create("importerPlugin") {
            displayName = "Rust Importer"
            description = "A plugin that makes it possible to import outputs from Rust from another Gradle project."
            id = "fr.stardustenterprises.rust.importer"
            implementationClass = "fr.stardustenterprises.gradle.rust.importer.ImporterPlugin"
        }
    }
}

publishing.publications {
    create<MavenPublication>("mavenJava") {
        groupId = Coordinates.GROUP
        version = Coordinates.VERSION

        pom {
            val repo = Coordinates.REPO_ID

            name.set(Coordinates.NAME)
            description.set(Coordinates.DESC)
            url.set("https://github.com/$repo")

            licenses {
                Pom.licenses.forEach {
                    license {
                        name.set(it.name)
                        url.set(it.url)
                    }
                }
            }
            developers {
                Pom.developers.forEach {
                    developer {
                        id.set(it.id)
                        name.set(it.name)
                    }
                }
            }
            scm {
                connection.set("scm:git:git://github.com/$repo.git")
                developerConnection.set("scm:git:ssh://github.com/$repo.git")
                url.set("https://github.com/$repo")
            }
        }

        // Configure the signing extension to sign this Maven artifact.
        signing {
            isRequired = project.properties["signing.keyId"] != null
            sign(this@create)
        }
    }
}
