import bpy

print("=== CHECKING Buildings.blend ===")
bpy.ops.wm.open_mainfile(filepath=r"d:\finplay pro\3dmodel\Buildings.blend")
for o in bpy.data.objects:
    if o.type == 'MESH':
        print(f"  {o.name}: dim={o.dimensions}, mat={[m.name for m in o.data.materials if m]}")

print("=== CHECKING LowPoly_Modern_House_Collection_01_blend.blend ===")
bpy.ops.wm.open_mainfile(filepath=r"d:\finplay pro\3dmodel\LowPoly_Modern_House_Collection_01_blend.blend")
for o in bpy.data.objects:
    if 'house' in o.name.lower() and o.type in ['MESH', 'EMPTY']:
        print(f"  {o.name}: type={o.type}, dim={o.dimensions}")

print("=== CHECKING Tree.fbx ===")
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=r"d:\finplay pro\3dmodel\Tree.fbx")
for o in bpy.data.objects:
    print(f"  {o.name}: type={o.type}, dim={o.dimensions}")
