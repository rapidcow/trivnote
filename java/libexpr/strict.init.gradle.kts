/*
 * Strict lint
 *
 * To use these options, run (in ..):
 * ./gradlew --offline --no-configuration-cache -I libexpr/strict.init.gradle.kts :libexpr:compileJava :libexpr:compileTestJava :libexpr:javadoc --rerun-tasks
 */

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xdoclint:all/protected", "-Werror"))
    }
    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            addBooleanOption("Xwerror", true)
        }
    }
}
