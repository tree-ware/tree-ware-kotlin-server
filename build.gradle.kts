import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tree-ware"
version = "1.0-SNAPSHOT"

val ktorVersion = "1.6.1"

val log4j2Version = "2.14.1"

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.5.21")
    id("idea")
    id("java-library")
}

repositories {
    jcenter()
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    // Compile for Java 8 (default is Java 6)
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    implementation(project(":tree-ware-kotlin-core"))

    implementation(kotlin("stdlib"))

    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")

    testImplementation(testFixtures(project(":tree-ware-kotlin-core")))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
