import bpy, os

blend_path = r"D:\finplay pro\3dmodels\extracted\Zane_Ryder_blender\Zane Ryder.blend"
bpy.ops.wm.open_mainfile(filepath=blend_path)

# Remove unneeded objects
for name in ["Light", "Camera", "Preview_Camera", "Boxers", "CC_Base_Tongue", "CC_Base_TearLine", "CC_Base_EyeOcclusion"]:
    o = bpy.data.objects.get(name)
    if o:
        bpy.data.objects.remove(o, do_unlink=True)

# Downscale images
for img in bpy.data.images:
    if img.size[0] > 512 or img.size[1] > 512:
        try:
            img.scale(512, 512)
        except Exception:
            pass

# Remove normal/specular links
for mat in bpy.data.materials:
    if mat.use_nodes and mat.node_tree:
        p = mat.node_tree.nodes.get("Principled BSDF")
        if p:
            for inp_name in ["Normal", "Specular IOR Level", "Roughness", "Metallic"]:
                if inp_name in p.inputs:
                    inp = p.inputs[inp_name]
                    for link in list(inp.links):
                        mat.node_tree.links.remove(link)

out_path = r"D:\finplay pro\public\warland\assets\models\character_zane_tactical.glb"

for o in bpy.context.scene.objects:
    o.select_set(True)

bpy.ops.export_scene.gltf(
    filepath=out_path,
    use_selection=True,
    export_format='GLB',
    export_apply=True,
    export_morph=False,
    export_draco_mesh_compression_enable=False,
    export_image_format='JPEG',
    export_image_quality=75
)

sz = os.path.getsize(out_path)
print(f"SUCCESS: Exported Zane Ryder without Draco: {sz} bytes ({sz / (1024*1024):.2f} MB)")
