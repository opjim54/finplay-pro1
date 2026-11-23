import bpy
import os

out_dir = r"d:\finplay pro\public\warland\assets\models"
tex_dir = r"d:\finplay pro\3dmodel"

# 1. BUILDINGS
print(">>> EXPORTING BUILDINGS...")
bpy.ops.wm.open_mainfile(filepath=os.path.join(tex_dir, "Buildings.blend"))

# Delete ground plane
if "Plane" in bpy.data.objects:
    bpy.data.objects.remove(bpy.data.objects["Plane"], do_unlink=True)

# Remap missing images to tex_dir
for img in bpy.data.images:
    fname = os.path.basename(img.filepath.replace("\\", "/"))
    local_p = os.path.join(tex_dir, fname)
    if os.path.exists(local_p):
        img.filepath = local_p
        try:
            img.reload()
            img.pack()
        except:
            pass

# Ensure materials use Principled BSDF with textures
for mat in bpy.data.materials:
    if mat.use_nodes:
        # Check if texture is connected
        has_tex = any(n.type == 'TEX_IMAGE' for n in mat.node_tree.nodes)
        if not has_tex:
            # find corresponding bina image
            mname = mat.name.lower()
            target_img = None
            for iname in bpy.data.images.keys():
                if mname in iname.lower():
                    target_img = bpy.data.images[iname]
                    break
            if target_img:
                bsdf = next((n for n in mat.node_tree.nodes if n.type == 'BSDF_PRINCIPLED'), None)
                if bsdf:
                    tex_node = mat.node_tree.nodes.new('ShaderNodeTexImage')
                    tex_node.image = target_img
                    mat.node_tree.links.new(tex_node.outputs['Color'], bsdf.inputs['Base Color'])

# Export GLB
buildings_out = os.path.join(out_dir, "building_city_blocks.glb")
bpy.ops.export_scene.gltf(
    filepath=buildings_out,
    export_format='GLB',
    use_selection=False,
    export_apply=True
)
print(f"✓ Saved {buildings_out}: {os.path.getsize(buildings_out)} bytes")


# 2. MODERN HOUSES
print(">>> EXPORTING MODERN HOUSES...")
bpy.ops.wm.open_mainfile(filepath=os.path.join(tex_dir, "LowPoly_Modern_House_Collection_01_blend.blend"))

# Ensure color palette texture is packed
color_tex_p = os.path.join(tex_dir, "textures", "color_1024x1024.jpg")
if not os.path.exists(color_tex_p):
    color_tex_p = os.path.join(tex_dir, "textures", "color_128x128.jpg")

if os.path.exists(color_tex_p):
    c_img = bpy.data.images.load(color_tex_p)
    c_img.pack()
    mat = bpy.data.materials.get("01 - CoronaMtl")
    if mat and mat.use_nodes:
        for n in mat.node_tree.nodes:
            if n.type == 'TEX_IMAGE':
                n.image = c_img

houses_out = os.path.join(out_dir, "building_modern_houses.glb")
bpy.ops.export_scene.gltf(
    filepath=houses_out,
    export_format='GLB',
    use_selection=False,
    export_apply=True
)
print(f"✓ Saved {houses_out}: {os.path.getsize(houses_out)} bytes")


# 3. DETAILED TREE
print(">>> EXPORTING TREE...")
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.fbx(filepath=os.path.join(tex_dir, "Tree.fbx"))

# Setup lush green leaf and bark materials
mat_leaf1 = bpy.data.materials.new(name="M_LeafOak")
mat_leaf1.use_nodes = True
bsdf_l1 = mat_leaf1.node_tree.nodes.get("Principled BSDF")
if bsdf_l1:
    bsdf_l1.inputs["Base Color"].default_value = (0.08, 0.42, 0.12, 1.0)
    bsdf_l1.inputs["Roughness"].default_value = 0.65

mat_leaf2 = bpy.data.materials.new(name="M_LeafOak2")
mat_leaf2.use_nodes = True
bsdf_l2 = mat_leaf2.node_tree.nodes.get("Principled BSDF")
if bsdf_l2:
    bsdf_l2.inputs["Base Color"].default_value = (0.12, 0.52, 0.18, 1.0)
    bsdf_l2.inputs["Roughness"].default_value = 0.65

mat_bark = bpy.data.materials.new(name="M_Bark")
mat_bark.use_nodes = True
bsdf_b = mat_bark.node_tree.nodes.get("Principled BSDF")
if bsdf_b:
    bsdf_b.inputs["Base Color"].default_value = (0.18, 0.10, 0.05, 1.0)
    bsdf_b.inputs["Roughness"].default_value = 0.85

for o in bpy.data.objects:
    if o.name == 'Leaf Oak':
        o.data.materials.clear()
        o.data.materials.append(mat_leaf1)
    elif o.name == 'Leaf Oak2':
        o.data.materials.clear()
        o.data.materials.append(mat_leaf2)
    elif 'Bark' in o.name:
        o.data.materials.clear()
        o.data.materials.append(mat_bark)

tree_out = os.path.join(out_dir, "prop_tree_oak_detailed.glb")
bpy.ops.export_scene.gltf(
    filepath=tree_out,
    export_format='GLB',
    use_selection=False,
    export_apply=True
)
print(f"✓ Saved {tree_out}: {os.path.getsize(tree_out)} bytes")
print(">>> ALL 3D ASSETS EXPORTED SUCCESSFULLY!")
