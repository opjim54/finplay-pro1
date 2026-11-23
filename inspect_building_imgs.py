import bpy
import os

bpy.ops.wm.open_mainfile(filepath=r"d:\finplay pro\3dmodel\Buildings.blend")
for img in bpy.data.images:
    p = bpy.path.abspath(img.filepath)
    print(f"  {img.name}: {p} -> exists: {os.path.exists(p)}")
