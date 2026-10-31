plugins {
    kotlin("jvm") version "2.0.21"
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("measure.demo.MainKt")
}
