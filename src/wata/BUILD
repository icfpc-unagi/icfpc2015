licenses(["notice"])

package(default_visibility = ["//visibility:public"])

java_import(
    name = "gson",
    jars = ["gson-2.3.1.jar"],
)

java_binary(
    name = "wata",
    srcs = glob(["**/*.java"]),
    main_class = "Main",
    deps = [
        ":gson",
    ],
)
