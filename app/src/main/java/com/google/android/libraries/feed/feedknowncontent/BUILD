package(default_visibility = ["//visibility:public"])

package_group(
    name = "feedknowncontent_pkgs",
    packages = [
        "//src/main/java/com/google/android/libraries/feed/feedknowncontent/...",
        "//src/test/java/com/google/android/libraries/feed/feedknowncontent/...",
    ],
)

licenses(["notice"])  # Apache 2

java_library(
    name = "feedknowncontent",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/api/common",
        "//src/main/java/com/google/android/libraries/feed/api/knowncontent",
        "//src/main/java/com/google/android/libraries/feed/api/sessionmanager",
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/common/concurrent",
        "//src/main/java/com/google/android/libraries/feed/common/functional",
        "//src/main/java/com/google/android/libraries/feed/common/logging",
        "//src/main/proto/com/google/android/libraries/feed/api/proto:client_feed_java_proto_lite",
        "//src/main/proto/search/now/ui/stream:stream_java_proto_lite",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
