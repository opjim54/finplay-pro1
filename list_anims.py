import bpy
import glob
import os

def check_fbx_folder(folder):
    files = glob.glob(os.path.join(folder, "*.fbx"))
    print(f"=== Found {len(files)} FBX files in {folder} ===")
    for f in files:
        bpy.ops.wm.read_factory_settings(use_empty=True)
        try:
            bpy.ops.import_scene.fbx(filepath=f)
            actions = [a.name for a in bpy.data.actions]
            durations = [(a.frame_range[1] - a.frame_range[0]) for a in bpy.data.actions]
            print(f"  {os.path.basename(f)}: actions={actions}, frames={durations}")
        except Exception as e:
            print(f"  {os.path.basename(f)}: ERROR: {e}")

check_fbx_folder(r"d:\finplay pro\animation")
check_fbx_folder(r"d:\finplay pro\animation\shooter_pack")
