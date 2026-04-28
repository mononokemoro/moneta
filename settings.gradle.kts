pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "moneta"

// 워크스페이스 루트(moneta)를 열었을 때 IDE(Java LS)가 backend 모듈과 Spring classpath를 인식하도록 포함합니다.
include(":backend")
project(":backend").projectDir = file("backend")
