import subprocess


# config
TEAM_A = ["RushBot"]
TEAM_B = ["ExplosiveBot", "OldRushBot"]
MAPS = []

pairs = set()
for a in TEAM_A:
    for b in TEAM_B:
        pairs.add((a, b))
        pairs.add((b, a))
if not MAPS:
    MAPS = ["DefaultSmall", "DefaultMedium", "DefaultLarge", "DefaultHuge"]
for a, b in pairs:
    for map in MAPS:
        subprocess.run(["./gradlew", "run", f"-Pmaps={map}", f"-PteamA={a}", f"-PteamB={b}", f"-PoutputVerbose=false"])