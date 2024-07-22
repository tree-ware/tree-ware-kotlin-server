import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// The libraries are currently published to JitPack. JitPack picks up the
// version from the repo label, resulting in all libraries from the repo
// having the same version in JitPack. Setting the version for all projects
// conveys this.
allprojects {
    group = "org.tree-ware.tree-ware-kotlin-server"
    version = "0.1.0.1"
}

val ktorApiKeyVersion = "1.1.0"
val ktorVersion = "2.0.2"
val mockkVersion = "1.12.0"

plugins {
    kotlin("jvm") version "1.7.0"
    id("idea")
    id("org.tree-ware.core") version "0.1.0.0-SNAPSHOT" // TODO #### drop -SNAPSHOT
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    mavenLocal() // TODO #### delete
}

tasks.withType<KotlinCompile> {
    // Compile for Java 8 (default is Java 6)
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(libs.treeWareKotlinCore)

    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")

    testImplementation(libs.treeWareKotlinCoreTestFixtures)
    testImplementation("dev.forst:ktor-api-key:$ktorApiKeyVersion")
    testImplementation("io.ktor:ktor-client-core:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform {
        when (System.getProperty("integrationTests", "")) {
            "include" -> includeTags("integrationTest")
            "exclude" -> excludeTags("integrationTest")
            else -> {}
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}