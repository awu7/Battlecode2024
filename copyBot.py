import os

copyFrom = "RushBot"
copyTo = "GCMicro"

for fname in os.listdir("./src/" + copyFrom):
    if fname.endswith(".java"):
        print("Copying " + fname)
        with open("./src/" + copyFrom + "/" + fname) as f:
            with open("./src/" + copyTo + "/" + fname, "w") as f2:
                f2.write(f.read().replace(copyFrom, copyTo))
