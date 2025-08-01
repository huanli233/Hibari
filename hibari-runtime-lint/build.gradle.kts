plugins {
    alias(libs.plugins.kotlin.jvm)
}

val projectGroup = "com.huanli233.hibari"
group = projectGroup
val lintClass = "${projectGroup}.runtime.lint.HibariIssueRegistry"

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.javaVersion.get())
    targetCompatibility =  JavaVersion.toVersion(libs.versions.javaVersion.get())
}

kotlin {
    jvmToolchain(libs.versions.javaVersion.get().toInt())
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Lint-Registry-V2" to lintClass
        )
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}")
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)
    testImplementation(libs.lint)
    testImplementation(libs.lint.tests)
}