import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("io.javalin:javalin:4.3.0")
    implementation("org.ow2.asm:asm:9.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.tomcat:tomcat-catalina:8.5.38")
    implementation("com.unboundid:unboundid-ldapsdk:3.1.1")

    // suppress warning about SLF4J
    implementation("org.slf4j:slf4j-nop:1.7.35")

    testImplementation("io.kotest:kotest-runner-junit5:5.1.0")
    testImplementation("org.apache.logging.log4j:log4j-core:2.14.1")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.run {
        isFork = true
        forkOptions.executable = "javac"
        compilerArgs.plusAssign("-XDignore.symbol.file")
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    dependsOn(configurations.runtimeClasspath)
    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    )
}

// for mixing java and kotlin files
sourceSets.main {
    java.srcDir("src/main/kotlin")
}

application {
    mainClass.set("MainKt")
}
