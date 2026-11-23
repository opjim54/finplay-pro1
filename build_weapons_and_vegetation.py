import bpy, os, math

model_dir = r"d:\finplay pro\3dmodel"
extracted_dir = os.path.join(model_dir, "extracted")
out_dir = r"d:\finplay pro\public\warland\assets\models"
os.makedirs(out_dir, exist_ok=True)

def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)

def export_glb(filepath):
    bpy.ops.export_scene.gltf(
        filepath=filepath,
        export_format='GLB',
        use_selection=False,
        export_apply=True
    )
    print(f"✓ Saved {filepath}: {os.path.getsize(filepath)} bytes")

# ==============================================================================
# 1. AK47 EXPORT (From user's AK47.fbx + 1P_PW_AK47_D.png)
# ==============================================================================
print(">>> EXPORTING HIGH-QUALITY AK-47...")
reset()
ak_fbx = os.path.join(model_dir, "AK47.fbx")
bpy.ops.import_scene.fbx(filepath=ak_fbx)

# Find meshes and join or parent
ak_meshes = [o for o in bpy.data.objects if o.type == 'MESH']
if ak_meshes:
    # Scale correction: check current dimension
    # Current dimension from inspect: Vector (0.0054, 0.0017, 0.0003) -> scaled by 0.01 or similar
    # Real AK-47 is ~0.88m long
    max_d = max(ak_meshes[0].dimensions)
    if max_d < 0.1:
        s = 0.88 / max_d
        ak_meshes[0].scale = (s, s, s)
        bpy.context.view_layer.objects.active = ak_meshes[0]
        bpy.ops.object.transform_apply(scale=True)
    
    # Orient forward along -Z or -Y as standard for weapons in Three.js
    # Let's inspect orientation
    ak_mat = bpy.data.materials.new(name="M_AK47_PBR")
    ak_mat.use_nodes = True
    bsdf = ak_mat.node_tree.nodes.get("Principled BSDF")
    
    d_tex = os.path.join(extracted_dir, "1P_PW_AK47_D.png")
    n_tex = os.path.join(extracted_dir, "1P_PW_AK47_N.png")
    s_tex = os.path.join(extracted_dir, "1P_PW_AK47_S.png")
    
    if os.path.exists(d_tex) and bsdf:
        img_d = bpy.data.images.load(d_tex)
        img_d.pack()
        node_d = ak_mat.node_tree.nodes.new("ShaderNodeTexImage")
        node_d.image = img_d
        ak_mat.node_tree.links.new(node_d.outputs["Color"], bsdf.inputs["Base Color"])
        
    if os.path.exists(n_tex) and bsdf:
        img_n = bpy.data.images.load(n_tex)
        img_n.colorspace_settings.name = 'Non-Color'
        img_n.pack()
        node_n = ak_mat.node_tree.nodes.new("ShaderNodeTexImage")
        node_n.image = img_n
        node_norm = ak_mat.node_tree.nodes.new("ShaderNodeNormalMap")
        ak_mat.node_tree.links.new(node_n.outputs["Color"], node_norm.inputs["Color"])
        ak_mat.node_tree.links.new(node_norm.outputs["Normal"], bsdf.inputs["Normal"])
        
    if os.path.exists(s_tex) and bsdf:
        img_s = bpy.data.images.load(s_tex)
        img_s.colorspace_settings.name = 'Non-Color'
        img_s.pack()
        node_s = ak_mat.node_tree.nodes.new("ShaderNodeTexImage")
        node_s.image = img_s
        ak_mat.node_tree.links.new(node_s.outputs["Color"], bsdf.inputs["Roughness"])
        
    for m in ak_meshes:
        m.data.materials.clear()
        m.data.materials.append(ak_mat)

ak_out = os.path.join(out_dir, "gun_ak47.glb")
export_glb(ak_out)

# ==============================================================================
# 2. AWM SNIPER RIFLE EXPORT (From user's awm.fbx + UV_L96A1_Body_BaseColor.png)
# ==============================================================================
print(">>> EXPORTING HIGH-QUALITY AWM SNIPER...")
reset()
awm_fbx = os.path.join(model_dir, "awm.fbx")
bpy.ops.import_scene.fbx(filepath=awm_fbx)

# Materials for AWM
body_tex = os.path.join(extracted_dir, "Textures", "UV_L96A1_Body_BaseColor.png")
if not os.path.exists(body_tex):
    body_tex = os.path.join(extracted_dir, "UV_L96A1_Body_BaseColor.png")

scope_tex = os.path.join(extracted_dir, "Textures", "UV_L96A1_Scope_BaseColor.png")
if not os.path.exists(scope_tex):
    scope_tex = os.path.join(extracted_dir, "UV_L96A1_Scope_BaseColor.png")

mat_body = bpy.data.materials.new(name="M_AWM_Body")
mat_body.use_nodes = True
bsdf_body = mat_body.node_tree.nodes.get("Principled BSDF")
if os.path.exists(body_tex) and bsdf_body:
    img_b = bpy.data.images.load(body_tex)
    img_b.pack()
    node_b = mat_body.node_tree.nodes.new("ShaderNodeTexImage")
    node_b.image = img_b
    mat_body.node_tree.links.new(node_b.outputs["Color"], bsdf_body.inputs["Base Color"])
    bsdf_body.inputs["Metallic"].default_value = 0.4
    bsdf_body.inputs["Roughness"].default_value = 0.35

mat_scope = bpy.data.materials.new(name="M_AWM_Scope")
mat_scope.use_nodes = True
bsdf_scope = mat_scope.node_tree.nodes.get("Principled BSDF")
if os.path.exists(scope_tex) and bsdf_scope:
    img_sc = bpy.data.images.load(scope_tex)
    img_sc.pack()
    node_sc = mat_scope.node_tree.nodes.new("ShaderNodeTexImage")
    node_sc.image = img_sc
    mat_scope.node_tree.links.new(node_sc.outputs["Color"], bsdf_scope.inputs["Base Color"])
    bsdf_scope.inputs["Metallic"].default_value = 0.7
    bsdf_scope.inputs["Roughness"].default_value = 0.25

# Scale correction: L96A1 is ~126 units (cm), so scale to ~1.18m
for o in bpy.data.objects:
    if o.type == 'MESH':
        if 'scope' in o.name.lower():
            o.data.materials.clear()
            o.data.materials.append(mat_scope)
        else:
            o.data.materials.clear()
            o.data.materials.append(mat_body)

# Join or scale
bpy.ops.object.select_all(action='SELECT')
scale_awm = 0.01  # cm to meters
for o in bpy.data.objects:
    o.scale = (o.scale[0] * scale_awm, o.scale[1] * scale_awm, o.scale[2] * scale_awm)
bpy.ops.object.transform_apply(scale=True)

awm_out = os.path.join(out_dir, "gun_awm.glb")
export_glb(awm_out)

# ==============================================================================
# 3. SHOTGUN EXPORT (From user's Remigton_870_Shotgun.fbx)
# ==============================================================================
print(">>> EXPORTING HIGH-QUALITY REMINGTON 870 SHOTGUN...")
reset()
shotgun_fbx = os.path.join(model_dir, "Remigton_870_Shotgun.fbx")
bpy.ops.import_scene.fbx(filepath=shotgun_fbx)

mat_m870 = bpy.data.materials.new(name="M_M870")
mat_m870.use_nodes = True
bsdf_m870 = mat_m870.node_tree.nodes.get("Principled BSDF")
if bsdf_m870:
    bsdf_m870.inputs["Base Color"].default_value = (0.08, 0.09, 0.10, 1.0)
    bsdf_m870.inputs["Metallic"].default_value = 0.85
    bsdf_m870.inputs["Roughness"].default_value = 0.28

mat_wood = bpy.data.materials.new(name="M_M870_Wood")
mat_wood.use_nodes = True
bsdf_wood = mat_wood.node_tree.nodes.get("Principled BSDF")
if bsdf_wood:
    bsdf_wood.inputs["Base Color"].default_value = (0.28, 0.14, 0.06, 1.0)
    bsdf_wood.inputs["Metallic"].default_value = 0.05
    bsdf_wood.inputs["Roughness"].default_value = 0.45

for o in bpy.data.objects:
    if o.type == 'MESH':
        if any(k in o.name.lower() for k in ['culata', 'corredera']):
            o.data.materials.clear()
            o.data.materials.append(mat_wood)
        else:
            o.data.materials.clear()
            o.data.materials.append(mat_m870)

# Check scale: Remington 870 is ~1.0m
max_dim = 0
for o in bpy.data.objects:
    if o.type == 'MESH':
        max_dim = max(max_dim, max(o.dimensions))

if max_dim < 0.1:  # currently in fractions of a meter
    scale_shotgun = 1.0 / 0.46
    for o in bpy.data.objects:
        o.scale = (scale_shotgun, scale_shotgun, scale_shotgun)
    bpy.ops.object.select_all(action='SELECT')
    bpy.ops.object.transform_apply(scale=True)

shotgun_out = os.path.join(out_dir, "gun_shotgun.glb")
export_glb(shotgun_out)

# ==============================================================================
# 4. DEAGLE / MAGNUM 357 EXPORT (From user's Magnum357.fbx)
# ==============================================================================
print(">>> EXPORTING HIGH-QUALITY MAGNUM 357 PISTOL...")
reset()
magnum_fbx = os.path.join(extracted_dir, "Magnum357.fbx")
if not os.path.exists(magnum_fbx):
    magnum_fbx = os.path.join(model_dir, "Magnum357.fbx")

bpy.ops.import_scene.fbx(filepath=magnum_fbx)

mat_silver = bpy.data.materials.new(name="M_MagnumSilver")
mat_silver.use_nodes = True
bsdf_sil = mat_silver.node_tree.nodes.get("Principled BSDF")
if bsdf_sil:
    bsdf_sil.inputs["Base Color"].default_value = (0.82, 0.84, 0.88, 1.0)
    bsdf_sil.inputs["Metallic"].default_value = 0.95
    bsdf_sil.inputs["Roughness"].default_value = 0.18

mat_grip = bpy.data.materials.new(name="M_MagnumGrip")
mat_grip.use_nodes = True
bsdf_grip = mat_grip.node_tree.nodes.get("Principled BSDF")
if bsdf_grip:
    bsdf_grip.inputs["Base Color"].default_value = (0.12, 0.06, 0.03, 1.0)
    bsdf_grip.inputs["Metallic"].default_value = 0.05
    bsdf_grip.inputs["Roughness"].default_value = 0.55

for o in bpy.data.objects:
    if o.type == 'MESH':
        if 'grip' in o.name.lower() or 'handle' in o.name.lower():
            o.data.materials.clear()
            o.data.materials.append(mat_grip)
        else:
            o.data.materials.clear()
            o.data.materials.append(mat_silver)

# Scale pistol to ~0.26m length
bpy.ops.object.select_all(action='SELECT')
max_d = 0
for o in bpy.data.objects:
    if o.type == 'MESH':
        max_d = max(max_d, max(o.dimensions))
if max_d > 0.01:
    s = 0.28 / max_d
    for o in bpy.data.objects:
        o.scale = (s, s, s)
    bpy.ops.object.transform_apply(scale=True)

deagle_out = os.path.join(out_dir, "gun_deagle.glb")
export_glb(deagle_out)

# ==============================================================================
# 5. HIGH-QUALITY TROPICAL PALM / BANANA PLANT EXPORT
# (Replaces bad procedural cone palm tree with user's Banana_Plant_01.fbx)
# ==============================================================================
print(">>> EXPORTING HIGH-QUALITY TROPICAL PALM/BANANA PLANT...")
reset()
plant_fbx = os.path.join(model_dir, "Banana_Plant_01.fbx")
bpy.ops.import_scene.fbx(filepath=plant_fbx)

# Remove LOD1, LOD2, LOD3 and UCX collision mesh to keep only pristine LOD0
for o in list(bpy.data.objects):
    if any(k in o.name for k in ['LOD1', 'LOD2', 'LOD3', 'UCX', 'Billboard']):
        bpy.data.objects.remove(o, do_unlink=True)

# Setup PBR Banana Palm Material
palm_tex_col = os.path.join(extracted_dir, "Textures", "Banana_Plants_BaseColor.png")
palm_tex_op  = os.path.join(extracted_dir, "Textures", "Banana_Plants_Opacity.png")
palm_tex_norm = os.path.join(extracted_dir, "Textures", "Banana_Plants_Normal_OpenGL.png")
palm_tex_rgh = os.path.join(extracted_dir, "Textures", "Banana_Plants_Roughness.png")

mat_palm = bpy.data.materials.new(name="M_BananaPalm_PBR")
mat_palm.use_nodes = True
bsdf_palm = mat_palm.node_tree.nodes.get("Principled BSDF")

if os.path.exists(palm_tex_col) and bsdf_palm:
    img_col = bpy.data.images.load(palm_tex_col)
    img_col.pack()
    node_c = mat_palm.node_tree.nodes.new("ShaderNodeTexImage")
    node_c.image = img_col
    mat_palm.node_tree.links.new(node_c.outputs["Color"], bsdf_palm.inputs["Base Color"])

if os.path.exists(palm_tex_norm) and bsdf_palm:
    img_norm = bpy.data.images.load(palm_tex_norm)
    img_norm.colorspace_settings.name = 'Non-Color'
    img_norm.pack()
    node_n = mat_palm.node_tree.nodes.new("ShaderNodeTexImage")
    node_n.image = img_norm
    node_nmap = mat_palm.node_tree.nodes.new("ShaderNodeNormalMap")
    mat_palm.node_tree.links.new(node_n.outputs["Color"], node_nmap.inputs["Color"])
    mat_palm.node_tree.links.new(node_nmap.outputs["Normal"], bsdf_palm.inputs["Normal"])

if os.path.exists(palm_tex_rgh) and bsdf_palm:
    img_rgh = bpy.data.images.load(palm_tex_rgh)
    img_rgh.colorspace_settings.name = 'Non-Color'
    img_rgh.pack()
    node_r = mat_palm.node_tree.nodes.new("ShaderNodeTexImage")
    node_r.image = img_rgh
    mat_palm.node_tree.links.new(node_r.outputs["Color"], bsdf_palm.inputs["Roughness"])

if os.path.exists(palm_tex_op) and bsdf_palm:
    img_op = bpy.data.images.load(palm_tex_op)
    img_op.colorspace_settings.name = 'Non-Color'
    img_op.pack()
    node_op = mat_palm.node_tree.nodes.new("ShaderNodeTexImage")
    node_op.image = img_op
    mat_palm.node_tree.links.new(node_op.outputs["Color"], bsdf_palm.inputs["Alpha"])
    mat_palm.blend_method = 'CLIP'
    mat_palm.alpha_threshold = 0.35

for o in bpy.data.objects:
    if o.type == 'MESH':
        o.data.materials.clear()
        o.data.materials.append(mat_palm)

# Scale up slightly so it serves as a full tropical palm canopy (~4.2m height)
bpy.ops.object.select_all(action='SELECT')
for o in bpy.data.objects:
    if o.type == 'MESH':
        max_h = o.dimensions.z
        if max_h > 0.1:
            scale_factor = 4.2 / max_h
            o.scale = (scale_factor, scale_factor, scale_factor)
bpy.ops.object.transform_apply(scale=True)

# Replace the low quality prop_palm_tree.glb
palm_out = os.path.join(out_dir, "prop_palm_tree.glb")
export_glb(palm_out)

print(">>> ALL WEAPONS AND VEGETATION ASSETS CONVERTED SUCCESSFULLY!")
