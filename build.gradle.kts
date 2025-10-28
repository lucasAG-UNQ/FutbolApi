plugins {
    kotlin("jvm") version "1.9.25"
    application
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.5"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "1.9.25"
    jacoco
    id("org.sonarqube") version "7.0.1.6134"
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
    mainClass.set("com.grupob.futbolapi.FutbolApiApplication")
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
    }
    test {
        kotlin.srcDirs("src/test/kotlin")
    }
}

// Centralized dependency versions
ext {
    set("kotlinVersion", "1.9.25")
    set("springBootVersion", "3.5.5")
    set("springDependencyManagementVersion", "1.1.7")
    set("sonarqubeVersion", "7.0.1.6134")
    set("jjwtVersion", "0.12.5")
    set("jsoupVersion", "1.16.1")
    set("okhttpVersion", "4.11.0")
    set("jsonVersion", "20230227")
    set("h2Version", "2.2.224") // Example version, h2 was unversioned
    set("mockwebserverVersion", "4.9.3")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)   // XML is needed by SonarCloud
        html.required.set(true)
    }
}

repositories {
    mavenCentral()
    google()
}


group = "com.grupob"
version = "0.0.1-SNAPSHOT"
description = "Futbol Api Project for Spring Boot"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.grupob.futbolapi.FutbolApiApplication")
}

springBoot {
    mainClass.set("com.grupob.futbolapi.FutbolApiApplication")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:${project.extra["jjwtVersion"]}")
    implementation("org.jsoup:jsoup:${project.extra["jsoupVersion"]}")
    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    implementation("org.json:json:${project.extra["jsonVersion"]}")
    implementation(kotlin("stdlib"))
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${project.extra["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${project.extra["jjwtVersion"]}")
    runtimeOnly("com.h2database:h2") // H2 version managed by Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:${project.extra["mockwebserverVersion"]}")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
    jvmToolchain(21)
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
