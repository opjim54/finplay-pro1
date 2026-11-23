import bpy

bpy.ops.wm.open_mainfile(filepath=r"d:\finplay pro\3dmodel\LowPoly_Modern_House_Collection_01_blend.blend")
mat = bpy.data.materials.get("01 - CoronaMtl")
if mat and mat.node_tree:
    print("Nodes in 01 - CoronaMtl:")
    for n in mat.node_tree.nodes:
        print(f"  {n.name} ({n.type})")
        if n.type == 'TEX_IMAGE' and n.image:
            print(f"    Image: {n.image.name}, filepath: {n.image.filepath}")
