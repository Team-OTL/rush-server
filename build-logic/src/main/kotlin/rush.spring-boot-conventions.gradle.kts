import org.gradle.api.artifacts.VersionCatalogsExtension

// 모든 Spring Boot 모듈의 공통 설정
plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

group = "com.rush"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.findVersion("java").get().requiredVersion.toInt())
    }
}

dependencyManagement {
    imports {
        mavenBom(libs.findLibrary("spring-cloud-dependencies").get().get().toString())
    }
}

// 실행 jar(bootJar)만 만들고 plain jar는 만들지 않습니다.
tasks.named("jar") {
    enabled = false
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
