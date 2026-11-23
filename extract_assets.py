import os, zipfile, subprocess

base_dir = r"d:\finplay pro\3dmodel"
target_dir = os.path.join(base_dir, "extracted")
os.makedirs(target_dir, exist_ok=True)

# 1. Unzip
for z in ["Magnum357_fbx.zip", "Texture.zip", "Textures.zip"]:
    zp = os.path.join(base_dir, z)
    if os.path.exists(zp):
        print(f"Unzipping {z}...")
        with zipfile.ZipFile(zp, 'r') as zf:
            zf.extractall(target_dir)

# 2. Untar (handles RAR)
rars = [
    "awn gun textures.rar",
    "Grass+free+fbx.rar",
    "MangoTreePack_FBX.rar",
    "MangoTreePack_Textures.rar",
    "tree texture.rar",
    "treeFBX.rar"
]

for r in rars:
    rp = os.path.join(base_dir, r)
    if os.path.exists(rp):
        print(f"Extracting {r}...")
        subprocess.run(["tar", "-xf", rp, "-C", target_dir])

print("Extraction complete. Files in extracted:")
for root, dirs, files in os.walk(target_dir):
    rel = os.path.relpath(root, target_dir)
    for f in files:
        print(os.path.join(rel, f))
