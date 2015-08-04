licenses(["notice"])

package(default_visibility = ["//visibility:public"])

cc_library(
    name = "gmock",
    hdrs = [
        "gmock.h",
        "gtest.h",
    ],
    srcs = ["gmock-gtest-all.cc"],
    linkstatic = 1,
)

cc_library(
    name = "gmock_main",
    srcs = ["gmock_main.cc"],
    deps = [":gmock"],
    linkstatic = 1,
    alwayslink = 1,
)
