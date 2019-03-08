package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

android_library(
    name = "contextmenumanager",
    srcs = glob(["*.java"]),
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [
        "//src/main/java/com/google/android/libraries/feed/common",
        "//src/main/java/com/google/android/libraries/feed/sharedstream:resources",  # buildcleaner: keep
        "//src/main/java/com/google/android/libraries/feed/sharedstream/publicapi/menumeasurer",
        "@com_google_code_findbugs_jsr305//jar",
    ],
)
