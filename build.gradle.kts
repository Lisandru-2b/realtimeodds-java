import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    `java-library`
    id("com.vanniktech.maven.publish") version "0.30.0"
}

group = "xyz.realtimeodds"
version = "0.3.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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

// ─── Maven Central publishing ───────────────────────────────────────────────
//
// Coordinate: xyz.realtimeodds:realtimeodds-java:<version>
//
// Driven by the vanniktech plugin (handles sources jar, javadoc jar, signing,
// POM completeness, and upload to the Sonatype Central Portal in one go).
//
// Required environment variables at publish time:
//   ORG_GRADLE_PROJECT_mavenCentralUsername       (Sonatype User Token)
//   ORG_GRADLE_PROJECT_mavenCentralPassword       (Sonatype User Token secret)
//   ORG_GRADLE_PROJECT_signingInMemoryKey         (ASCII-armored GPG private key)
//   ORG_GRADLE_PROJECT_signingInMemoryKeyId       (last 8 hex of GPG fingerprint)
//   ORG_GRADLE_PROJECT_signingInMemoryKeyPassword (GPG passphrase)
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = true))

    coordinates("xyz.realtimeodds", "realtimeodds-java", project.version.toString())

    pom {
        name.set("realtimeodds-java")
        description.set("Real-time betting odds SDK — multi-bookmaker, sport-discriminated, async with CompletableFuture.")
        inceptionYear.set("2026")
        url.set("https://github.com/Lisandru-2b/realtimeodds-java")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("Lisandru-2b")
                name.set("Lisandru")
                email.set("barrallisandru@gmail.com")
                url.set("https://github.com/Lisandru-2b")
            }
        }
        scm {
            url.set("https://github.com/Lisandru-2b/realtimeodds-java")
            connection.set("scm:git:git://github.com/Lisandru-2b/realtimeodds-java.git")
            developerConnection.set("scm:git:ssh://git@github.com/Lisandru-2b/realtimeodds-java.git")
        }
        issueManagement {
            system.set("GitHub Issues")
            url.set("https://github.com/Lisandru-2b/realtimeodds-java/issues")
        }
    }
}
