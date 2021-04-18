plugins {
    java
}

repositories {
    mavenCentral()
    mavenLocal()

    maven {
        name = "Quilt"
        url = uri("https://maven.quiltmc.org/repository/release")
    }

    maven {
        name = "JitPack"
        url = uri("https://jitpack.io")
        content {
            includeGroup("com.github.Anuken")
            includeGroup("com.github.Anuken.Arc")
            includeGroup("com.github.Anuken.Mindustry")
        }
    }
}

val asm_version: String by project.ext
val mindustry_version: String by project.ext

dependencies {
    implementation("de.kb1000.quiltmd:quilt-loader:0.12.0.1+local")
    implementation("org.ow2.asm:asm:$asm_version")
    runtimeOnly("org.ow2.asm:asm-analysis:$asm_version")
    runtimeOnly("org.ow2.asm:asm-commons:$asm_version")
    implementation("org.ow2.asm:asm-tree:$asm_version")
    runtimeOnly("org.ow2.asm:asm-util:$asm_version")
    runtimeOnly("com.google.code.gson:gson:2.8.6")
    runtimeOnly("com.google.guava:guava:30.1.1-jre")
    runtimeOnly("org.quiltmc:quilt-json5:1.0.0-rc.3")
    runtimeOnly("org.quiltmc:sponge-mixin:0.9.2+mixin.0.8.2") {
        exclude(module = "launchwrapper")
        exclude(module = "guava")
    }
    runtimeOnly("org.quiltmc:tiny-mappings-parser:0.3.0")
    runtimeOnly("org.quiltmc:tiny-remapper:0.3.2")
    runtimeOnly("org.quiltmc:access-widener:1.0.2")
    runtimeOnly("com.google.jimfs:jimfs:1.2") {
        exclude(module = "guava")
    }
    runtimeOnly("org.quiltmc:quilt-loader-sat4j:2.3.5")
    compileOnly("com.github.Anuken.Mindustry:core:$mindustry_version")
}

tasks.processResources {
    from(configurations.runtimeClasspath)
}

tasks.jar {
    manifest {
        attributes("de.kb1000.fabricmd.hackloader", "Sealed" to "true")
        attributes("de.kb1000.fabricmd.hackloader.accessor", "Sealed" to "true")
        attributes("de.kb1000.fabricmd.hackloader.post", "Sealed" to "true")
    }

    from(file("LICENSE"))
}
