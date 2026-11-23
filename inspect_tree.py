import bpy

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=r"d:\finplay pro\3dmodel\Tree.fbx")
for o in bpy.data.objects:
    if o.type == 'MESH':
        print(f"Tree mesh {o.name}:")
        for m in o.data.materials:
            if m:
                print(f"  mat: {m.name}")
