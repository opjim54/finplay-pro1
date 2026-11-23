import bpy, os

files = ['AK47.fbx', 'awm.fbx', 'Remigton_870_Shotgun.fbx', 'Banana_Plant_01.fbx', 'Format1.fbx']
for name in files:
    bpy.ops.wm.read_factory_settings(use_empty=True)
    p = os.path.join(r'd:\finplay pro\3dmodel', name)
    try:
        bpy.ops.import_scene.fbx(filepath=p)
        print(f"=== {name} ===")
        for o in bpy.data.objects:
            mats = [m.name for m in o.data.materials if m] if hasattr(o, "data") and hasattr(o.data, "materials") else []
            print(f"  {o.name}: type={o.type}, dim={o.dimensions}, mats={mats}")
    except Exception as e:
        print(f"=== {name} ERROR: {e} ===")
