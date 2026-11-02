plugins {
    id("rush.kotlin-conventions")
}

dependencies {
    implementation(libs.spring.cloud.starter.gateway)
    implementation(libs.spring.cloud.starter.config)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)
}
