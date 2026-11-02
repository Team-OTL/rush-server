plugins {
    id("rush.kotlin-conventions")
}

dependencies {
    implementation(libs.spring.cloud.starter.netflix.eureka.server)
    implementation(libs.spring.boot.starter.actuator)
}
