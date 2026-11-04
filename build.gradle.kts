plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.services) apply false
}

val backendUnitTest = tasks.register<Exec>("backendUnitTest") {
    group = "verification"
    description = "Runs the API's pure financial, job-status, and score unit tests (Node.js 18+)."
    workingDir(layout.projectDirectory.dir("backend"))
    commandLine(providers.gradleProperty("nodeExecutable").getOrElse("node"), "--test")
}

tasks.register("test") {
    group = "verification"
    description = "Runs JVM app tests and the production API's business-rule tests without a device."
    dependsOn(":app:test", backendUnitTest)
}
