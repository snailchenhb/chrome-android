package(default_visibility = ["//visibility:public"])

licenses(["notice"])  # Apache 2

package_group(
    name = "sharedstream_pkgs",
    packages = ["//src/main/java/com/google/android/libraries/feed/sharedstream/..."],
)

package_group(
    name = "sharedstream_pkgs_and_users",
    packages = ["//..."],
)

android_library(
    name = "resources",
    srcs = glob(["*.java"]),
    manifest = "AndroidManifest.xml",
    resource_files = glob(["res/**"]),
    deps = [],
)
