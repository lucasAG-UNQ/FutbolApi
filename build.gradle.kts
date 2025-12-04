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
    set("springdocVersion", "2.8.4")
    set("archunitVersion", "1.3.0")
    set("fuzzywuzzyVersion","1.4.0")
    set("mockitoKotlinVersion", "5.2.1")
}

configurations {
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

tasks.test {
    useJUnitPlatform {
        val runIntegration = System.getProperty("runIntegration")

        if (runIntegration == "true") {
            includeTags("integration")
        } else {
            excludeTags("integration")
        }
    }

    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)   // ensures tests run before coverage

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // Fixes the Gradle 8 "implicit dependency" error
    classDirectories.setFrom(
        fileTree("${buildDir}/classes/kotlin/main") {
            exclude(
                "**/integration/**",
                "**/aspects/**",
                "**/config/**",
                "**/dto/**",
                "**/security/**",
                "**/FutbolApiApplication*"
            )
        }
    )

    sourceDirectories.setFrom(files("src/main/kotlin"))
    executionData.setFrom(fileTree(buildDir).include("/jacoco/test.exec"))
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
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("io.jsonwebtoken:jjwt-api:${project.extra["jjwtVersion"]}")
    implementation("org.jsoup:jsoup:${project.extra["jsoupVersion"]}")
    implementation("com.squareup.okhttp3:okhttp:${project.extra["okhttpVersion"]}")
    implementation("org.json:json:${project.extra["jsonVersion"]}")
    implementation(kotlin("stdlib"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${project.extra["springdocVersion"]}")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation ("org.springframework.boot:spring-boot-starter-actuator")
    implementation ("io.micrometer:micrometer-registry-prometheus")
    implementation("me.xdrop:fuzzywuzzy:${project.extra["fuzzywuzzyVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:${project.extra["jjwtVersion"]}")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${project.extra["jjwtVersion"]}")
    runtimeOnly("com.h2database:h2") // H2 version managed by Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("com.squareup.okhttp3:mockwebserver:${project.extra["mockwebserverVersion"]}")
    testImplementation("com.tngtech.archunit:archunit-junit5:${project.extra["archunitVersion"]}")
    testImplementation("org.mockito.kotlin:mockito-kotlin:${project.extra["mockitoKotlinVersion"]}")
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
