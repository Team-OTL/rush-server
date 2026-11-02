import org.gradle.api.artifacts.VersionCatalogsExtension

// Spring Boot + Kotlin 모듈 공통 설정
plugins {
    id("rush.spring-boot-conventions")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jlleitschuh.gradle.ktlint")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

ktlint {
    version.set(libs.findVersion("ktlint").get().requiredVersion)
}

dependencies {
    "implementation"(libs.findLibrary("kotlin-reflect").get())
    "implementation"(libs.findLibrary("jackson-module-kotlin").get())
    "testImplementation"(libs.findLibrary("spring-boot-starter-test").get())
    "testImplementation"(libs.findLibrary("mockk").get())
}
