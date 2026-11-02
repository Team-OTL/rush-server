plugins {
    // build-logic을 한 번만 로드해서 모든 모듈이 같은 플러그인 classpath를 쓰게 합니다.
    id("rush.spring-boot-conventions") apply false
}
