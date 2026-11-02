import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

// convention 플러그인 안에서 외부 플러그인을 쓰려면 marker 아티팩트를 의존성으로 추가해야 합니다.
fun Provider<PluginDependency>.marker() = map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

dependencies {
    implementation(libs.plugins.spring.boot.marker())
    implementation(libs.plugins.spring.dependency.management.marker())
    implementation(libs.plugins.kotlin.jvm.marker())
    implementation(libs.plugins.kotlin.spring.marker())
    implementation(libs.plugins.kotlin.jpa.marker())
    implementation(libs.plugins.ktlint.marker())
}
