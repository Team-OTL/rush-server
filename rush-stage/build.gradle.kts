plugins {
    id("rush.kotlin-jpa-conventions")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.cloud.starter.config)
    implementation(libs.spring.cloud.starter.netflix.eureka.client)
    implementation(libs.springdoc.openapi.webmvc.ui)
    // DB를 붙일 때 주석을 해제하세요.
    // implementation(libs.spring.boot.starter.data.jpa)
    // runtimeOnly(libs.mysql.connector.j)
}
