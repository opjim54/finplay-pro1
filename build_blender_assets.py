
import bpy, bmesh, math, os

out_dir = r'd:\finplay pro\public\warland\assets\models'
os.makedirs(out_dir, exist_ok=True)

def reset():
    bpy.ops.wm.read_factory_settings(use_empty=True)

def export_glb(filename):
    path = os.path.join(out_dir, filename)
    bpy.ops.export_scene.gltf(
        filepath=path,
        export_format='GLB',
        use_selection=False,
        export_apply=True
    )
    print(f'Exported {filename}: {os.path.getsize(path)} bytes')

# --- 1. FREE FIRE GLOO WALL (SM_GlooWall.glb) ---
reset()
# Curved shield wall
bm = bmesh.new()
# Create curved arc of segments (12 segments across 120 degree arc)
segments = 14
radius = 3.2
height = 2.6
thickness = 0.35
angle_span = math.radians(110)
half_angle = angle_span / 2.0

for i in range(segments):
    t0 = -half_angle + (i / segments) * angle_span
    t1 = -half_angle + ((i + 1) / segments) * angle_span
    
    # Inner and outer points
    x0_in, z0_in = math.sin(t0) * (radius - thickness/2), math.cos(t0) * (radius - thickness/2)
    x1_in, z1_in = math.sin(t1) * (radius - thickness/2), math.cos(t1) * (radius - thickness/2)
    x0_out, z0_out = math.sin(t0) * (radius + thickness/2), math.cos(t0) * (radius + thickness/2)
    x1_out, z1_out = math.sin(t1) * (radius + thickness/2), math.cos(t1) * (radius + thickness/2)
    
    v0 = bm.verts.new((x0_in, z0_in, 0))
    v1 = bm.verts.new((x1_in, z1_in, 0))
    v2 = bm.verts.new((x1_in, z1_in, height))
    v3 = bm.verts.new((x0_in, z0_in, height))
    
    v4 = bm.verts.new((x0_out, z0_out, 0))
    v5 = bm.verts.new((x1_out, z1_out, 0))
    v6 = bm.verts.new((x1_out, z1_out, height))
    v7 = bm.verts.new((x0_out, z0_out, height))
    
    # Faces
    bm.faces.new((v0, v1, v2, v3)) # Inner
    bm.faces.new((v5, v4, v7, v6)) # Outer
    bm.faces.new((v3, v2, v6, v7)) # Top
    bm.faces.new((v4, v5, v1, v0)) # Bottom

# Cap ends
bm.verts.ensure_lookup_table()
bm.faces.new((bm.verts[0], bm.verts[3], bm.verts[7], bm.verts[4]))
bm.faces.new((bm.verts[-7], bm.verts[-6], bm.verts[-2], bm.verts[-3]))

mesh_data = bpy.data.meshes.new('GlooWall_Mesh')
bm.to_mesh(mesh_data)
bm.free()

gloo_ob = bpy.data.objects.new('SM_GlooWall', mesh_data)
bpy.context.collection.objects.link(gloo_ob)

# Glow Ice Material
mat = bpy.data.materials.new(name='Mat_GlooIce')
mat.use_nodes = True
bsdf = mat.node_tree.nodes.get('Principled BSDF')
if bsdf:
    bsdf.inputs['Base Color'].default_value = (0.05, 0.75, 0.95, 1.0) # Vibrant Ice Cyan
    bsdf.inputs['Roughness'].default_value = 0.15
    bsdf.inputs['Metallic'].default_value = 0.2
    if 'Transmission Weight' in bsdf.inputs:
        bsdf.inputs['Transmission Weight'].default_value = 0.4
    if 'Emission Color' in bsdf.inputs:
        bsdf.inputs['Emission Color'].default_value = (0.02, 0.4, 0.8, 1.0)
        bsdf.inputs['Emission Strength'].default_value = 1.8
gloo_ob.data.materials.append(mat)

# Add side pillars for reinforced look
for sign in [-1, 1]:
    t = sign * half_angle
    px = math.sin(t) * radius
    pz = math.cos(t) * radius
    bpy.ops.mesh.primitive_cylinder_add(radius=0.28, depth=height + 0.3, location=(px, pz, (height+0.3)/2))
    pillar = bpy.context.active_object
    pillar.name = f'Pillar_{sign}'
    pillar_mat = bpy.data.materials.new(name='Mat_GlooSteel')
    pillar_mat.use_nodes = True
    p_bsdf = pillar_mat.node_tree.nodes.get('Principled BSDF')
    p_bsdf.inputs['Base Color'].default_value = (0.1, 0.15, 0.25, 1.0)
    p_bsdf.inputs['Metallic'].default_value = 0.9
    pillar.data.materials.append(pillar_mat)

export_glb('prop_gloowall.glb')

# --- 2. STYLIZED OAK TREE (SM_Tree_Oak.glb) ---
reset()
# Stylized Trunk with organic tapering
bpy.ops.mesh.primitive_cylinder_add(vertices=10, radius1=0.7, radius2=0.35, depth=4.5, location=(0, 0, 2.25))
trunk = bpy.context.active_object
trunk.name = 'Trunk'
trunk_mat = bpy.data.materials.new(name='Mat_Bark')
trunk_mat.use_nodes = True
t_bsdf = trunk_mat.node_tree.nodes.get('Principled BSDF')
t_bsdf.inputs['Base Color'].default_value = (0.35, 0.2, 0.1, 1.0)
trunk.data.materials.append(trunk_mat)

# Foliage clouds (multi-level overlapping soft spheres)
leaf_mat = bpy.data.materials.new(name='Mat_Foliage')
leaf_mat.use_nodes = True
l_bsdf = leaf_mat.node_tree.nodes.get('Principled BSDF')
l_bsdf.inputs['Base Color'].default_value = (0.18, 0.55, 0.15, 1.0) # Rich natural green
l_bsdf.inputs['Roughness'].default_value = 0.8

leaf_clusters = [
    (0, 0, 4.8, 2.4, 2.0),
    (-0.9, -0.6, 4.0, 1.8, 1.6),
    (0.8, 0.7, 4.2, 1.9, 1.7),
    (0.7, -0.8, 4.4, 1.7, 1.5),
    (-0.6, 0.8, 4.6, 1.6, 1.4),
    (0, 0, 5.8, 1.7, 1.5)
]

for i, (lx, ly, lz, sx, sz) in enumerate(leaf_clusters):
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2, radius=1.0, location=(lx, ly, lz))
    leaf = bpy.context.active_object
    leaf.name = f'Foliage_{i}'
    leaf.scale = (sx, sx, sz)
    leaf.data.materials.append(leaf_mat)

export_glb('prop_tree_stylized.glb')

# --- 3. TROPICAL PALM TREE (SM_Palm_Tree.glb) ---
reset()
# Curved segmented palm trunk
segments = 8
curve_x = 0
curve_y = 0
for s in range(segments):
    cz = s * 0.9 + 0.45
    cx = math.sin(s * 0.2) * 0.4
    bpy.ops.mesh.primitive_cylinder_add(vertices=8, radius1=0.45 - s*0.025, radius2=0.42 - s*0.025, depth=0.95, location=(cx, 0, cz))
    seg = bpy.context.active_object
    seg.name = f'Palm_Trunk_{s}'
    seg_mat = bpy.data.materials.new(name='Mat_PalmTrunk')
    seg_mat.use_nodes = True
    seg_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.45, 0.35, 0.22, 1.0)
    seg.data.materials.append(seg_mat)

# Palm Fronds radiating in circle
frond_mat = bpy.data.materials.new(name='Mat_PalmFrond')
frond_mat.use_nodes = True
frond_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.15, 0.65, 0.2, 1.0)

crown_z = segments * 0.9
crown_x = math.sin((segments-1)*0.2) * 0.4
for f in range(8):
    angle = (f / 8.0) * math.pi * 2
    bpy.ops.mesh.primitive_cylinder_add(vertices=6, radius1=0.35, radius2=0.08, depth=3.2, location=(crown_x + math.cos(angle)*1.3, math.sin(angle)*1.3, crown_z - 0.4))
    frond = bpy.context.active_object
    frond.name = f'Frond_{f}'
    frond.rotation_euler = (math.sin(angle)*0.45, -math.cos(angle)*0.45, angle)
    frond.scale = (1.2, 0.15, 1.0)
    frond.data.materials.append(frond_mat)

export_glb('prop_palm_tree.glb')

# --- 4. HIGH-DETAIL AK-47 ASSAULT RIFLE (SM_Gun_AK47.glb) ---
reset()
steel_mat = bpy.data.materials.new(name='Mat_GunSteel')
steel_mat.use_nodes = True
s_bsdf = steel_mat.node_tree.nodes.get('Principled BSDF')
s_bsdf.inputs['Base Color'].default_value = (0.15, 0.17, 0.2, 1.0)
s_bsdf.inputs['Metallic'].default_value = 0.85
s_bsdf.inputs['Roughness'].default_value = 0.25

wood_mat = bpy.data.materials.new(name='Mat_GunWood')
wood_mat.use_nodes = True
w_bsdf = wood_mat.node_tree.nodes.get('Principled BSDF')
w_bsdf.inputs['Base Color'].default_value = (0.45, 0.22, 0.08, 1.0)
w_bsdf.inputs['Roughness'].default_value = 0.4

# Receiver
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0))
rec = bpy.context.active_object
rec.name = 'AK_Receiver'
rec.scale = (0.08, 0.45, 0.12)
rec.data.materials.append(steel_mat)

# Barrel
bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.025, depth=0.6, location=(0, 0.45, 0.02))
barrel = bpy.context.active_object
barrel.rotation_euler = (math.pi/2, 0, 0)
barrel.name = 'AK_Barrel'
barrel.data.materials.append(steel_mat)

# Banana Magazine
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0.08, -0.16))
mag = bpy.context.active_object
mag.scale = (0.065, 0.12, 0.24)
mag.rotation_euler = (-0.25, 0, 0)
mag.name = 'AK_Mag'
mag.data.materials.append(steel_mat)

# Wood Buttstock
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.38, -0.04))
stock = bpy.context.active_object
stock.scale = (0.075, 0.32, 0.14)
stock.rotation_euler = (0.12, 0, 0)
stock.name = 'AK_Stock'
stock.data.materials.append(wood_mat)

# Wood Handguard
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0.24, 0.01))
guard = bpy.context.active_object
guard.scale = (0.085, 0.22, 0.09)
guard.name = 'AK_Handguard'
guard.data.materials.append(wood_mat)

export_glb('gun_ak47.glb')

# --- 5. AWM SNIPER RIFLE (SM_Gun_AWM.glb) ---
reset()
green_mat = bpy.data.materials.new(name='Mat_AwmGreen')
green_mat.use_nodes = True
green_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.18, 0.28, 0.16, 1.0) # Arctic Warfare Green

# Body
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.1, 0))
awm_body = bpy.context.active_object
awm_body.scale = (0.09, 0.8, 0.14)
awm_body.name = 'AWM_Body'
awm_body.data.materials.append(green_mat)

# Long Sniper Barrel
bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.022, depth=0.95, location=(0, 0.65, 0.02))
awm_barrel = bpy.context.active_object
awm_barrel.rotation_euler = (math.pi/2, 0, 0)
awm_barrel.name = 'AWM_Barrel'
awm_barrel.data.materials.append(steel_mat)

# Muzzle Brake
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 1.12, 0.02))
awm_brake = bpy.context.active_object
awm_brake.scale = (0.06, 0.08, 0.05)
awm_brake.name = 'AWM_Brake'
awm_brake.data.materials.append(steel_mat)

# Sniper Scope
bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.038, depth=0.36, location=(0, 0.05, 0.16))
awm_scope = bpy.context.active_object
awm_scope.rotation_euler = (math.pi/2, 0, 0)
awm_scope.name = 'AWM_Scope'
awm_scope.data.materials.append(steel_mat)

export_glb('gun_awm.glb')

print('ALL BLENDER WEAPONS AND PROPS BUILT SUCCESSFULLY!')
