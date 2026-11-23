import bpy

bpy.ops.wm.open_mainfile(filepath=r"d:\finplay pro\3dmodel\LowPoly_Modern_House_Collection_01_blend.blend")
for i in [1, 2, 3, 4, 5]:
    h = bpy.data.objects.get(f"house{i}")
    if h:
        children = [c.name for c in h.children]
        print(f"house{i}: children count={len(children)}, sample={children[:5]}")
        # check materials of first child
        for c in h.children:
            if c.type == 'MESH':
                print(f"   child mesh {c.name} mat: {[m.name for m in c.data.materials if m]}")
                break
