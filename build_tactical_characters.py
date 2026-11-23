"""
Blender 5.3 Script: Build 4 High-Fidelity Tactical Low-Poly Humanoid Characters
Each limb is a unified mesh with preserved materials and exact anatomical pivots:
- Char_Pelvis
- Char_Torso (with vest, pouches, and backpack joined)
- Char_Head (with helmet and glowing tactical visor joined)
- Char_Arm_L & Char_Arm_R (with gloves joined, pivoting at shoulder)
- Char_Leg_L & Char_Leg_R (with boots joined, pivoting at hip, resting flat at Z=0)
"""

import bpy
import os

OUTPUT_DIR = r"D:\finplay pro\public\warland\assets\models"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    if not bpy.data.scenes:
        bpy.data.scenes.new("Scene")

def create_pbr_material(name, base_color, roughness=0.6, metalness=0.1, emission=(0,0,0,1)):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs['Base Color'].default_value = base_color
        bsdf.inputs['Roughness'].default_value = roughness
        bsdf.inputs['Metallic'].default_value = metalness
        if 'Emission Color' in bsdf.inputs:
            bsdf.inputs['Emission Color'].default_value = emission
        elif 'Emission' in bsdf.inputs:
            bsdf.inputs['Emission'].default_value = emission
    return mat

def assign_material(obj, mat):
    if obj.data.materials:
        obj.data.materials[0] = mat
    else:
        obj.data.materials.append(mat)

def join_objects(objs, new_name):
    bpy.ops.object.select_all(action='DESELECT')
    for o in objs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = objs[0]
    bpy.ops.object.join()
    joined = bpy.context.active_object
    joined.name = new_name
    return joined

def build_character(char_type, camo_color, vest_color, skin_color, accent_color, emission_color=(0,0,0,1)):
    reset_scene()
    root = bpy.data.objects.new(f"Char_{char_type}", None)
    bpy.context.scene.collection.objects.link(root)

    mat_camo = create_pbr_material(f"Mat_{char_type}_Camo", camo_color, roughness=0.7, metalness=0.0)
    mat_vest = create_pbr_material(f"Mat_{char_type}_Vest", vest_color, roughness=0.5, metalness=0.2)
    mat_skin = create_pbr_material(f"Mat_{char_type}_Skin", skin_color, roughness=0.8, metalness=0.0)
    mat_gear = create_pbr_material(f"Mat_{char_type}_Gear", (0.12, 0.14, 0.16, 1.0), roughness=0.4, metalness=0.4)
    mat_accent = create_pbr_material(f"Mat_{char_type}_Accent", accent_color, roughness=0.3, metalness=0.6, emission=emission_color)

    # 1. Pelvis / Hips (Z = 0.92m)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.92))
    pelvis = bpy.context.active_object
    pelvis.scale = (0.34, 0.22, 0.18)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(pelvis, mat_camo)
    pelvis.name = "Char_Pelvis"
    pelvis.parent = root

    # 2. Torso (Vest + Pouches + Backpack)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 1.30))
    torso_main = bpy.context.active_object
    torso_main.scale = (0.44, 0.26, 0.52)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(torso_main, mat_vest)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.15, 1.28))
    pouches = bpy.context.active_object
    pouches.scale = (0.36, 0.08, 0.28)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(pouches, mat_gear)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0.16, 1.30))
    backpack = bpy.context.active_object
    backpack.scale = (0.32, 0.14, 0.38)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(backpack, mat_camo if char_type != 'Medic' else mat_accent)

    torso = join_objects([torso_main, pouches, backpack], "Char_Torso")
    torso.parent = root

    # 3. Head (Face + Helmet + Visor)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 1.74))
    head_main = bpy.context.active_object
    head_main.scale = (0.24, 0.26, 0.26)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(head_main, mat_skin if char_type not in ['Cyborg', 'Mercenary'] else mat_gear)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 1.84))
    helmet = bpy.context.active_object
    helmet.scale = (0.28, 0.30, 0.16)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(helmet, mat_gear)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.14, 1.76))
    visor = bpy.context.active_object
    visor.scale = (0.22, 0.04, 0.07)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(visor, mat_accent)

    head = join_objects([head_main, helmet, visor], "Char_Head")
    head.parent = root

    # 4. Arms (Pivoting at Shoulder: Z = 1.48m)
    # Left Arm
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.28, 0, 1.26))
    arm_l_mesh = bpy.context.active_object
    arm_l_mesh.scale = (0.13, 0.13, 0.44)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(arm_l_mesh, mat_camo)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.28, 0, 1.02))
    glove_l = bpy.context.active_object
    glove_l.scale = (0.12, 0.12, 0.10)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(glove_l, mat_gear)

    arm_l = join_objects([arm_l_mesh, glove_l], "Char_Arm_L")
    # Set pivot to shoulder
    bpy.context.scene.cursor.location = (-0.28, 0, 1.48)
    bpy.ops.object.origin_set(type='ORIGIN_CURSOR')
    arm_l.parent = root

    # Right Arm
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0.28, 0, 1.26))
    arm_r_mesh = bpy.context.active_object
    arm_r_mesh.scale = (0.13, 0.13, 0.44)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(arm_r_mesh, mat_camo)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0.28, 0, 1.02))
    glove_r = bpy.context.active_object
    glove_r.scale = (0.12, 0.12, 0.10)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(glove_r, mat_gear)

    arm_r = join_objects([arm_r_mesh, glove_r], "Char_Arm_R")
    bpy.context.scene.cursor.location = (0.28, 0, 1.48)
    bpy.ops.object.origin_set(type='ORIGIN_CURSOR')
    arm_r.parent = root

    # 5. Legs (Pivoting at Hips: Z = 0.84m)
    # Left Leg
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.14, 0, 0.46))
    leg_l_mesh = bpy.context.active_object
    leg_l_mesh.scale = (0.15, 0.17, 0.74)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(leg_l_mesh, mat_camo)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-0.14, -0.04, 0.08))
    boot_l = bpy.context.active_object
    boot_l.scale = (0.16, 0.24, 0.16)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(boot_l, mat_gear)

    leg_l = join_objects([leg_l_mesh, boot_l], "Char_Leg_L")
    bpy.context.scene.cursor.location = (-0.14, 0, 0.84)
    bpy.ops.object.origin_set(type='ORIGIN_CURSOR')
    leg_l.parent = root

    # Right Leg
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0.14, 0, 0.46))
    leg_r_mesh = bpy.context.active_object
    leg_r_mesh.scale = (0.15, 0.17, 0.74)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(leg_r_mesh, mat_camo)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0.14, -0.04, 0.08))
    boot_r = bpy.context.active_object
    boot_r.scale = (0.16, 0.24, 0.16)
    bpy.ops.object.transform_apply(scale=True)
    assign_material(boot_r, mat_gear)

    leg_r = join_objects([leg_r_mesh, boot_r], "Char_Leg_R")
    bpy.context.scene.cursor.location = (0.14, 0, 0.84)
    bpy.ops.object.origin_set(type='ORIGIN_CURSOR')
    leg_r.parent = root

    # Reset 3D Cursor to origin
    bpy.context.scene.cursor.location = (0, 0, 0)

    # Export clean GLB
    filename = f"character_{char_type.lower()}.glb"
    out_file = os.path.join(OUTPUT_DIR, filename)
    bpy.ops.export_scene.gltf(filepath=out_file, export_format='GLB', use_selection=False)
    print(f"Exported: {out_file} ({os.path.getsize(out_file)} bytes)")

if __name__ == "__main__":
    build_character("commando", (0.75, 0.62, 0.42, 1.0), (0.22, 0.24, 0.20, 1.0), (0.85, 0.65, 0.50, 1.0), (0.95, 0.55, 0.15, 1.0))
    build_character("mercenary", (0.18, 0.20, 0.22, 1.0), (0.12, 0.13, 0.15, 1.0), (0.35, 0.35, 0.35, 1.0), (0.85, 0.15, 0.15, 1.0))
    build_character("medic", (0.88, 0.88, 0.88, 1.0), (0.25, 0.45, 0.75, 1.0), (0.85, 0.65, 0.50, 1.0), (0.90, 0.10, 0.10, 1.0))
    build_character("cyborg", (0.28, 0.30, 0.34, 1.0), (0.15, 0.16, 0.20, 1.0), (0.45, 0.48, 0.52, 1.0), (0.10, 0.80, 0.95, 1.0), (0.10, 0.85, 1.0, 1.0))
    print("ALL 4 CHARACTERS EXPORTED WITH ANATOMICAL PIVOTS & UNIFIED LIMBS!")
