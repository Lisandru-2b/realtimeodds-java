plugins {
    `java-library`
    `maven-publish`
}

group = "com.github.Lisandru-2b"
version = "0.3.0"

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

// Convenience task to dump a snapshot to JSON without adding the `application`
// plugin to the publication graph. Wired up by .claude/skills/snapshot-dump.
//
// Pass CLI args via -Pargs and JVM args via -PjvmArgs:
//   ./gradlew runDump \
//       -Pargs="--api-key rto_... --output snap.json --wait 2" \
//       -PjvmArgs="-Djavax.net.ssl.trustStoreType=Windows-ROOT"
tasks.register<JavaExec>("runDump") {
    group = "application"
    description = "Connect to the gateway and dump a snapshot to JSON (see src/main/java/snapshot/Dump.java)"
    mainClass.set("snapshot.Dump")
    classpath = sourceSets["main"].runtimeClasspath
    if (project.hasProperty("args")) {
        args(project.property("args").toString().trim().split("\\s+".toRegex()))
    }
    if (project.hasProperty("jvmArgs")) {
        jvmArgs(project.property("jvmArgs").toString().trim().split("\\s+".toRegex()))
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
