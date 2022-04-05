import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.4")
    implementation("io.javalin:javalin:4.3.0")
    implementation("com.unboundid:unboundid-ldapsdk:3.1.1")
    implementation("org.javassist:javassist:3.28.0-GA")
    implementation("org.apache.tomcat:tomcat-catalina:8.5.38")

    // suppress warning about SLF4J
    implementation("org.slf4j:slf4j-nop:1.7.35")

    compileOnly("org.springframework:spring-web:5.2.9.RELEASE")
    compileOnly("org.springframework.webflow:spring-webflow:2.5.1.RELEASE")
    compileOnly("org.eclipse.jetty:jetty-server:9.4.43.v20210629")

    testImplementation("org.apache.logging.log4j:log4j-core:2.14.1")
    testImplementation("org.codehaus.groovy:groovy:3.0.1")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.1.51")
    testImplementation("org.springframework.boot:spring-boot-starter-web:2.0.0.RELEASE") {
        exclude(module = "spring-boot-starter-logging")
    }
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaCompile> {
    options.run {
        isFork = true
        isWarnings = false
        forkOptions.executable = "javac"
        compilerArgs.plusAssign("-XDignore.symbol.file")
    }
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
