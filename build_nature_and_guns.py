
import bpy, bmesh, math, os

out_dir = r'd:\finplay pro\public\warland\assets\models'

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

# --- 2. STYLIZED OAK TREE (SM_Tree_Oak.glb) ---
reset()
# Stylized Trunk with organic tapering
bpy.ops.mesh.primitive_cone_add(vertices=10, radius1=0.7, radius2=0.35, depth=4.5, location=(0, 0, 2.25))
trunk = bpy.context.active_object
trunk.name = 'Trunk'
trunk_mat = bpy.data.materials.new(name='Mat_Bark')
trunk_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.32, 0.18, 0.08, 1.0)
trunk.data.materials.append(trunk_mat)

leaf_mat = bpy.data.materials.new(name='Mat_Foliage')
l_bsdf = leaf_mat.node_tree.nodes.get('Principled BSDF')
l_bsdf.inputs['Base Color'].default_value = (0.16, 0.52, 0.14, 1.0) # Rich natural green
l_bsdf.inputs['Roughness'].default_value = 0.85

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
segments = 8
for s in range(segments):
    cz = s * 0.9 + 0.45
    cx = math.sin(s * 0.2) * 0.4
    bpy.ops.mesh.primitive_cone_add(vertices=8, radius1=0.45 - s*0.025, radius2=0.42 - s*0.025, depth=0.95, location=(cx, 0, cz))
    seg = bpy.context.active_object
    seg.name = f'Palm_Trunk_{s}'
    seg_mat = bpy.data.materials.new(name='Mat_PalmTrunk')
    seg_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.42, 0.32, 0.2, 1.0)
    seg.data.materials.append(seg_mat)

# Palm Fronds radiating in circle
frond_mat = bpy.data.materials.new(name='Mat_PalmFrond')
frond_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.12, 0.62, 0.18, 1.0)

crown_z = segments * 0.9
crown_x = math.sin((segments-1)*0.2) * 0.4
for f in range(8):
    angle = (f / 8.0) * math.pi * 2
    bpy.ops.mesh.primitive_cone_add(vertices=6, radius1=0.35, radius2=0.08, depth=3.2, location=(crown_x + math.cos(angle)*1.3, math.sin(angle)*1.3, crown_z - 0.4))
    frond = bpy.context.active_object
    frond.name = f'Frond_{f}'
    frond.rotation_euler = (math.sin(angle)*0.45, -math.cos(angle)*0.45, angle)
    frond.scale = (1.2, 0.15, 1.0)
    frond.data.materials.append(frond_mat)

export_glb('prop_palm_tree.glb')

# --- 4. HIGH-DETAIL AK-47 ASSAULT RIFLE (gun_ak47.glb) ---
reset()
steel_mat = bpy.data.materials.new(name='Mat_GunSteel')
s_bsdf = steel_mat.node_tree.nodes.get('Principled BSDF')
s_bsdf.inputs['Base Color'].default_value = (0.12, 0.14, 0.16, 1.0)
s_bsdf.inputs['Metallic'].default_value = 0.9
s_bsdf.inputs['Roughness'].default_value = 0.25

wood_mat = bpy.data.materials.new(name='Mat_GunWood')
w_bsdf = wood_mat.node_tree.nodes.get('Principled BSDF')
w_bsdf.inputs['Base Color'].default_value = (0.42, 0.18, 0.06, 1.0)
w_bsdf.inputs['Roughness'].default_value = 0.35

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

# --- 5. AWM SNIPER RIFLE (gun_awm.glb) ---
reset()
green_mat = bpy.data.materials.new(name='Mat_AwmGreen')
green_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.16, 0.26, 0.14, 1.0) # Arctic Warfare Green

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

# --- 6. SHOTGUN (gun_shotgun.glb) ---
reset()
bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.05, 0))
sg_rec = bpy.context.active_object
sg_rec.scale = (0.08, 0.35, 0.11)
sg_rec.data.materials.append(steel_mat)

bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.028, depth=0.65, location=(0, 0.35, 0.02))
sg_barrel = bpy.context.active_object
sg_barrel.rotation_euler = (math.pi/2, 0, 0)
sg_barrel.data.materials.append(steel_mat)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0.22, -0.04))
sg_pump = bpy.context.active_object
sg_pump.scale = (0.09, 0.18, 0.07)
sg_pump.data.materials.append(wood_mat)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.32, -0.04))
sg_stock = bpy.context.active_object
sg_stock.scale = (0.07, 0.28, 0.12)
sg_stock.rotation_euler = (0.1, 0, 0)
sg_stock.data.materials.append(wood_mat)

export_glb('gun_shotgun.glb')

# --- 7. ROCK BOULDER (prop_rock_boulder.glb) ---
reset()
rock_mat = bpy.data.materials.new(name='Mat_Rock')
r_bsdf = rock_mat.node_tree.nodes.get('Principled BSDF')
r_bsdf.inputs['Base Color'].default_value = (0.38, 0.40, 0.44, 1.0)
r_bsdf.inputs['Roughness'].default_value = 0.9

bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=2, radius=1.6, location=(0, 0, 1.2))
rock = bpy.context.active_object
rock.scale = (1.8, 1.4, 1.1)
rock.data.materials.append(rock_mat)

export_glb('prop_rock_boulder.glb')

print('ALL WEAPONS, TREES, AND ROCKS EXPORTED SUCCESSFULLY!')
