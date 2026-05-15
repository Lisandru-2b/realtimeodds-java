plugins {
    `java-library`
    `maven-publish`
}

group = "com.github.Lisandru-2b"
version = "0.1.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(17)
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
    isFailOnError = false
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("realtimeodds")
                description.set("Real-time betting odds SDK — multi-bookmaker, sport-discriminated, async with CompletableFuture.")
                url.set("https://github.com/Lisandru-2b/realtimeodds-java")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
            }
        }
    }
}
