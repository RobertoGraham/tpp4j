import com.google.protobuf.gradle.id

plugins {
    application
    id("com.google.protobuf") version "0.9.4"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("io.github.robertograham.tpp4j.Application")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.25.3"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.63.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") {
                    option("jakarta_omit")
                }
            }
        }
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("terraform-provider-test")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.63.0"))
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-netty")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
