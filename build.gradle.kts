import com.google.protobuf.gradle.id

plugins {
    application
    id("com.google.protobuf") version "0.9.4"
    id("org.graalvm.buildtools.native") version "0.10.1"
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
            artifact = "io.grpc:protoc-gen-grpc-java:1.62.2"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc") { }
            }
        }
    }
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("terraform-provider-test")
            mainClass.set("io.github.robertograham.tpp4j.Application")
            resources.autodetect()
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("io.grpc:grpc-bom:1.62.2"))
    implementation("io.grpc:grpc-protobuf")
    implementation("io.grpc:grpc-stub")
    implementation("io.grpc:grpc-netty")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("commons-io:commons-io:2.16.0")
}
