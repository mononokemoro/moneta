plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.pininicong"
version = "0.0.1-SNAPSHOT"
description = "미니가계부 API (Gradle + Spring Boot)"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    runtimeOnly("com.h2database:h2")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("pininicong-cashbook")
}

/**
 * `frontend` 빌드 산출물이 있으면 `classpath:/static` 에 포함해 단일 JAR 로 배포 가능.
 * 없으면 그대로두며(API 전용 JAR), 통합은 `cd frontend && npm run build` 후 다시 `bootJar`.
 */
tasks.processResources {
    val dist = layout.projectDirectory.dir("../frontend/dist").asFile
    if (dist.exists()) {
        from(dist) {
            into("static")
        }
    }
}
