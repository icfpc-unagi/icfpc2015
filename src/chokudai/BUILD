licenses(["notice"])

package(default_visibility = ["//visibility:public"])

genrule(
    name = "main",
    srcs = ["Main.cs"],
    outs = ["Main.exe"],
    cmd = "if [ \"$$(uname)\" = 'Darwin' ]; then touch $(OUTS); else dmcs -r:System,System.Net.Http,System.Runtime.Serialization -lib:/usr/lib/mono/4.5 $(locations :Main.cs) -out:$(OUTS); fi",
)

sh_binary(
    name = "chokudai",
    srcs = ["chokudai.sh"],
    data = [":Main.exe"],
)

genrule(
    name = "chokudai_ar",
    tools = [
        ":chokudai",
        "//bin:imos-package",
        "//src/buildrule:package",
    ],
    outs = ["chokudai.ar"],
    cmd = "$(location //src/buildrule:package) $(location //bin:imos-package) $(location :chokudai) $(OUTS)",
)
