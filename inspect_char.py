import bpy

def test_glb(p):
    print("=== Testing:", p)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=p)
    for o in bpy.data.objects:
        if o.type == 'ARMATURE':
            print('Armature:', o.name, 'Bones count:', len(o.data.bones))
            print('Bones:', [b.name for b in o.data.bones if not any(x in b.name for x in ['thumb', 'index', 'middle', 'pinky', 'ring', 'twist'])][:20])

test_glb(r"d:\finplay pro\public\warland\assets\models\character_asian_agent.glb")
test_glb(r"d:\finplay pro\public\warland\assets\models\test_zane_raw.glb")
