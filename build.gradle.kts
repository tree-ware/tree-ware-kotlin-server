import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.tree-ware"
version = "1.0-SNAPSHOT"

val kotlinVersion = "1.3.72"
val ktorVersion = "1.3.2"

val log4j2Version = "2.12.1"

val cassandraUnitVersion = "4.3.1.0"
val junitVersion = "5.4.2"

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.3.72")
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
    implementation(project(":tree-ware-kotlin-cassandra"))

    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("org.apache.logging.log4j:log4j-api:$log4j2Version")
    implementation("org.apache.logging.log4j:log4j-core:$log4j2Version")

    testImplementation(testFixtures(project(":tree-ware-kotlin-cassandra")))
    testImplementation(testFixtures(project(":tree-ware-kotlin-core")))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.cassandraunit:cassandra-unit:$cassandraUnitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
