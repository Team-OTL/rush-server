plugins {
    id("rush.kotlin-conventions")
}

dependencies {
    implementation(libs.spring.cloud.config.server)
    implementation(libs.spring.boot.starter.actuator)
}
