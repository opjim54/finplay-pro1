"""
Blender 5.3 Script: Build Rich Low-Poly PBR Buildings for War Land 3D
Generates:
1. building_villa_terracotta.glb (2-story Mediterranean/tropical villa with pitched roof, chimney, entrance porch, wooden shutters, balcony)
2. building_tactical_townhouse.glb (3-story concrete/brick urban sniper building with external stairs, open balconies, and rooftop parapet)
3. building_military_barracks.glb (Military outpost barracks with corrugated metal roof, support pillars, and sandbag barriers)
4. building_hangar_depot.glb (Industrial military vehicle hangar / depot with gabled roof and reinforced sliding doors)
"""

import bpy
import bmesh
import math
import os

OUTPUT_DIR = r"D:\finplay pro\public\warland\assets\models"
os.makedirs(OUTPUT_DIR, exist_ok=True)

def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    if not bpy.data.scenes:
        bpy.data.scenes.new("Scene")

def create_pbr_material(name, base_color, roughness=0.6, metalness=0.1):
    mat = bpy.data.materials.new(name=name)
    mat.use_nodes = True
    nodes = mat.node_tree.nodes
    bsdf = nodes.get("Principled BSDF")
    if bsdf:
        bsdf.inputs['Base Color'].default_value = base_color
        bsdf.inputs['Roughness'].default_value = roughness
        bsdf.inputs['Metallic'].default_value = metalness
    return mat

def assign_material(obj, mat):
    if obj.data.materials:
        obj.data.materials[0] = mat
    else:
        obj.data.materials.append(mat)

# -------------------------------------------------------------
# 1. Mediterranean / Tropical 2-Story Villa (Terracotta Roof)
# -------------------------------------------------------------
def build_villa_terracotta():
    reset_scene()
    root = bpy.data.objects.new("Building_Villa", None)
    bpy.context.scene.collection.objects.link(root)

    mat_wall = create_pbr_material("Mat_PlasterWall", (0.92, 0.88, 0.78, 1.0), roughness=0.75, metalness=0.0)
    mat_stone_base = create_pbr_material("Mat_StoneBase", (0.45, 0.43, 0.40, 1.0), roughness=0.85, metalness=0.0)
    mat_roof = create_pbr_material("Mat_TerracottaRoof", (0.82, 0.32, 0.14, 1.0), roughness=0.6, metalness=0.0)
    mat_wood = create_pbr_material("Mat_DarkWood", (0.35, 0.20, 0.12, 1.0), roughness=0.7, metalness=0.0)
    mat_window = create_pbr_material("Mat_WindowGlass", (0.2, 0.35, 0.45, 1.0), roughness=0.15, metalness=0.8)
    mat_door = create_pbr_material("Mat_WoodDoor", (0.42, 0.24, 0.15, 1.0), roughness=0.65, metalness=0.0)

    # Main Building Body (Width: 10m, Depth: 8m, Height: 7m)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 3.5))
    body = bpy.context.active_object
    body.name = "Villa_MainBody"
    body.scale = (10.0, 8.0, 7.0)
    body.parent = root
    assign_material(body, mat_wall)

    # Stone Foundation Base (Plinth)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 0.4))
    base = bpy.context.active_object
    base.name = "Villa_Plinth"
    base.scale = (10.4, 8.4, 0.8)
    base.parent = root
    assign_material(base, mat_stone_base)

    # Floor Separator Belt (Cornice at 3.5m)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 3.5))
    cornice = bpy.context.active_object
    cornice.name = "Villa_Cornice"
    cornice.scale = (10.3, 8.3, 0.25)
    cornice.parent = root
    assign_material(cornice, mat_stone_base)

    # Pitched Terracotta Roof (Pyramid / Gable roof)
    # Built via bmesh for clean geometry
    mesh = bpy.data.meshes.new("Villa_Roof_Mesh")
    bm = bmesh.new()
    w, d, h_base, h_ridge = 5.5, 4.5, 7.0, 9.8
    # 4 eave vertices
    v1 = bm.verts.new((-w, -d, h_base))
    v2 = bm.verts.new((w, -d, h_base))
    v3 = bm.verts.new((w, d, h_base))
    v4 = bm.verts.new((-w, d, h_base))
    # 2 ridge vertices
    r1 = bm.verts.new((-w + 1.2, 0, h_ridge))
    r2 = bm.verts.new((w - 1.2, 0, h_ridge))
    # Faces
    bm.faces.new((v1, v2, r2, r1))
    bm.faces.new((v2, v3, r2))
    bm.faces.new((v3, v4, r1, r2))
    bm.faces.new((v4, v1, r1))
    bm.to_mesh(mesh)
    bm.free()
    roof = bpy.data.objects.new("Villa_Roof", mesh)
    bpy.context.scene.collection.objects.link(roof)
    roof.parent = root
    assign_material(roof, mat_roof)

    # Chimney on Roof
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(3.0, 1.8, 9.2))
    chimney = bpy.context.active_object
    chimney.name = "Villa_Chimney"
    chimney.scale = (0.9, 0.9, 2.2)
    chimney.parent = root
    assign_material(chimney, mat_stone_base)

    # Chimney Cap
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(3.0, 1.8, 10.4))
    cap = bpy.context.active_object
    cap.scale = (1.1, 1.1, 0.2)
    cap.parent = root
    assign_material(cap, mat_roof)

    # Front Entrance Porch / Balcony (Floor 2 Balcony)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -4.6, 3.5))
    balcony_floor = bpy.context.active_object
    balcony_floor.name = "Villa_BalconyFloor"
    balcony_floor.scale = (4.0, 1.5, 0.2)
    balcony_floor.parent = root
    assign_material(balcony_floor, mat_stone_base)

    # Balcony Railing
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -5.3, 4.0))
    balcony_rail = bpy.context.active_object
    balcony_rail.scale = (4.0, 0.08, 0.8)
    balcony_rail.parent = root
    assign_material(balcony_rail, mat_wood)

    # Front Door
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -4.05, 1.2))
    door = bpy.context.active_object
    door.name = "Villa_Door"
    door.scale = (1.4, 0.15, 2.3)
    door.parent = root
    assign_material(door, mat_door)

    # Windows on Ground Floor & 2nd Floor
    window_coords = [
        # Ground floor front
        (-3.0, -4.05, 1.8), (3.0, -4.05, 1.8),
        # 2nd floor front
        (-3.0, -4.05, 5.2), (0, -4.05, 5.2), (3.0, -4.05, 5.2),
        # Sides
        (-5.05, -1.5, 1.8), (-5.05, 1.5, 1.8), (-5.05, 0, 5.2),
        (5.05, -1.5, 1.8), (5.05, 1.5, 1.8), (5.05, 0, 5.2)
    ]
    for i, (wx, wy, wz) in enumerate(window_coords):
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(wx, wy, wz))
        win = bpy.context.active_object
        win.name = f"Villa_Win_{i}"
        is_side = abs(wx) > 4.5
        if is_side:
            win.scale = (0.15, 1.2, 1.4)
        else:
            win.scale = (1.2, 0.15, 1.4)
        win.parent = root
        assign_material(win, mat_window)

        # Wooden Shutters
        if not is_side:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(wx - 0.75, wy - 0.05, wz))
            s1 = bpy.context.active_object
            s1.scale = (0.28, 0.06, 1.4)
            s1.parent = root
            assign_material(s1, mat_wood)

            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(wx + 0.75, wy - 0.05, wz))
            s2 = bpy.context.active_object
            s2.scale = (0.28, 0.06, 1.4)
            s2.parent = root
            assign_material(s2, mat_wood)

    # Smooth normals and export
    out_file = os.path.join(OUTPUT_DIR, "building_villa_terracotta.glb")
    bpy.ops.export_scene.gltf(filepath=out_file, export_format='GLB', use_selection=False)
    print(f"Exported: {out_file} ({os.path.getsize(out_file)} bytes)")

# -------------------------------------------------------------
# 2. Tactical 3-Story Townhouse (Sniper Rooftop & External Stairs)
# -------------------------------------------------------------
def build_tactical_townhouse():
    reset_scene()
    root = bpy.data.objects.new("Building_TacticalTownhouse", None)
    bpy.context.scene.collection.objects.link(root)

    mat_concrete = create_pbr_material("Mat_UrbanConcrete", (0.75, 0.73, 0.70, 1.0), roughness=0.8, metalness=0.0)
    mat_brick = create_pbr_material("Mat_RedBrick", (0.65, 0.28, 0.22, 1.0), roughness=0.85, metalness=0.0)
    mat_metal = create_pbr_material("Mat_RooftopMetal", (0.25, 0.28, 0.32, 1.0), roughness=0.45, metalness=0.8)
    mat_glass = create_pbr_material("Mat_BlueGlass", (0.15, 0.30, 0.45, 1.0), roughness=0.2, metalness=0.7)

    # 3-Story Tower Body (Width: 8m, Depth: 9m, Total Height: 10.5m -> 3.5m per floor)
    # Floor 1 (Brick base)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 1.75))
    f1 = bpy.context.active_object
    f1.scale = (8.0, 9.0, 3.5)
    f1.parent = root
    assign_material(f1, mat_brick)

    # Floor 2 (Concrete mid)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 5.25))
    f2 = bpy.context.active_object
    f2.scale = (8.0, 9.0, 3.5)
    f2.parent = root
    assign_material(f2, mat_concrete)

    # Floor 3 (Upper floor)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 8.75))
    f3 = bpy.context.active_object
    f3.scale = (8.0, 9.0, 3.5)
    f3.parent = root
    assign_material(f3, mat_brick)

    # Floor Slabs (Separators)
    for slab_z in [3.5, 7.0, 10.5]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, slab_z))
        slab = bpy.context.active_object
        slab.scale = (8.4, 9.4, 0.3)
        slab.parent = root
        assign_material(slab, mat_concrete)

    # Rooftop Parapet / Bunker Walls for Sniping (Height 1.1m on top of roof slab)
    # North & South parapets
    for py in [-4.5, 4.5]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, py, 11.1))
        p = bpy.context.active_object
        p.scale = (8.4, 0.35, 1.0)
        p.parent = root
        assign_material(p, mat_concrete)

    # East & West parapets with sniper notches
    for px in [-4.0, 4.0]:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(px, 0, 11.1))
        p = bpy.context.active_object
        p.scale = (0.35, 9.0, 1.0)
        p.parent = root
        assign_material(p, mat_concrete)

    # Rooftop Water Tank & HVAC Unit
    bpy.ops.mesh.primitive_cylinder_add(radius=0.9, depth=1.8, location=(-2.0, 2.0, 11.6))
    tank = bpy.context.active_object
    tank.parent = root
    assign_material(tank, mat_metal)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(2.0, 2.0, 11.2))
    hvac = bpy.context.active_object
    hvac.scale = (1.6, 1.4, 1.0)
    hvac.parent = root
    assign_material(hvac, mat_metal)

    # External Metal Staircase Framework (Leading from Ground to 2nd & 3rd Floors)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(4.6, 0, 5.25))
    stairs_platform = bpy.context.active_object
    stairs_platform.scale = (1.2, 4.0, 10.5)
    stairs_platform.parent = root
    assign_material(stairs_platform, mat_metal)

    # Windows on all 3 floors
    for z_floor in [1.8, 5.3, 8.8]:
        for wx in [-2.5, 0.0, 2.5]:
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(wx, -4.55, z_floor))
            win = bpy.context.active_object
            win.scale = (1.3, 0.15, 1.5)
            win.parent = root
            assign_material(win, mat_glass)

            # Window Frame Outline
            bpy.ops.mesh.primitive_cube_add(size=1.0, location=(wx, -4.56, z_floor))
            frame = bpy.context.active_object
            frame.scale = (1.45, 0.05, 1.65)
            frame.parent = root
            assign_material(frame, mat_concrete)

    out_file = os.path.join(OUTPUT_DIR, "building_tactical_townhouse.glb")
    bpy.ops.export_scene.gltf(filepath=out_file, export_format='GLB', use_selection=False)
    print(f"Exported: {out_file} ({os.path.getsize(out_file)} bytes)")

# -------------------------------------------------------------
# 3. Military Outpost Barracks (Reference Image 2 style)
# -------------------------------------------------------------
def build_military_barracks():
    reset_scene()
    root = bpy.data.objects.new("Building_MilitaryBarracks", None)
    bpy.context.scene.collection.objects.link(root)

    mat_camo_wall = create_pbr_material("Mat_MilitaryKhaki", (0.42, 0.44, 0.35, 1.0), roughness=0.8, metalness=0.0)
    mat_corrugated = create_pbr_material("Mat_CorrugatedMetal", (0.35, 0.38, 0.40, 1.0), roughness=0.5, metalness=0.6)
    mat_sandbags = create_pbr_material("Mat_SandbagBurlap", (0.68, 0.58, 0.42, 1.0), roughness=0.9, metalness=0.0)
    mat_wood_beam = create_pbr_material("Mat_WoodPost", (0.32, 0.22, 0.14, 1.0), roughness=0.75, metalness=0.0)

    # Long Barracks Body (Width: 14m, Depth: 6m, Height: 3.5m)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 1.75))
    barracks = bpy.context.active_object
    barracks.scale = (14.0, 6.0, 3.5)
    barracks.parent = root
    assign_material(barracks, mat_camo_wall)

    # Slanted Gable Corrugated Roof
    mesh = bpy.data.meshes.new("Barracks_Roof_Mesh")
    bm = bmesh.new()
    bw, bd, bh_base, bh_ridge = 7.6, 3.6, 3.5, 4.8
    v1 = bm.verts.new((-bw, -bd, bh_base))
    v2 = bm.verts.new((bw, -bd, bh_base))
    v3 = bm.verts.new((bw, bd, bh_base))
    v4 = bm.verts.new((-bw, bd, bh_base))
    r1 = bm.verts.new((-bw, 0, bh_ridge))
    r2 = bm.verts.new((bw, 0, bh_ridge))
    bm.faces.new((v1, v2, r2, r1))
    bm.faces.new((v2, v3, r2))
    bm.faces.new((v3, v4, r1, r2))
    bm.faces.new((v4, v1, r1))
    bm.to_mesh(mesh)
    bm.free()
    roof = bpy.data.objects.new("Barracks_Roof", mesh)
    bpy.context.scene.collection.objects.link(roof)
    roof.parent = root
    assign_material(roof, mat_corrugated)

    # Front Sandbag Fortification Perimeter (Height: 0.9m)
    sandbag_positions = [
        (-6.0, -3.8), (-4.5, -3.8), (-3.0, -3.8),
        (3.0, -3.8), (4.5, -3.8), (6.0, -3.8)
    ]
    for sx, sy in sandbag_positions:
        bpy.ops.mesh.primitive_cube_add(size=1.0, location=(sx, sy, 0.45))
        sb = bpy.context.active_object
        sb.scale = (1.4, 0.6, 0.9)
        sb.parent = root
        assign_material(sb, mat_sandbags)

    # Front Wooden Canopy Pillars
    for px in [-6.5, -2.0, 2.0, 6.5]:
        bpy.ops.mesh.primitive_cylinder_add(radius=0.12, depth=3.2, location=(px, -3.5, 1.6))
        col = bpy.context.active_object
        col.parent = root
        assign_material(col, mat_wood_beam)

    out_file = os.path.join(OUTPUT_DIR, "building_military_barracks.glb")
    bpy.ops.export_scene.gltf(filepath=out_file, export_format='GLB', use_selection=False)
    print(f"Exported: {out_file} ({os.path.getsize(out_file)} bytes)")

# -------------------------------------------------------------
# 4. Military Vehicle Hangar & Equipment Depot
# -------------------------------------------------------------
def build_hangar_depot():
    reset_scene()
    root = bpy.data.objects.new("Building_HangarDepot", None)
    bpy.context.scene.collection.objects.link(root)

    mat_steel = create_pbr_material("Mat_HangarSteel", (0.35, 0.40, 0.45, 1.0), roughness=0.45, metalness=0.7)
    mat_gate = create_pbr_material("Mat_HangarDoor", (0.75, 0.45, 0.15, 1.0), roughness=0.5, metalness=0.3)
    mat_concrete = create_pbr_material("Mat_HangarFloor", (0.55, 0.55, 0.55, 1.0), roughness=0.85, metalness=0.0)

    # Large Hangar Structure (Width: 16m, Depth: 12m, Height: 7m)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0, 3.0))
    hangar_body = bpy.context.active_object
    hangar_body.scale = (16.0, 12.0, 6.0)
    hangar_body.parent = root
    assign_material(hangar_body, mat_steel)

    # Curved Arched / Gabled Roof
    mesh = bpy.data.meshes.new("Hangar_Roof_Mesh")
    bm = bmesh.new()
    hw, hd, h_base, h_top = 8.5, 6.5, 6.0, 8.8
    v1 = bm.verts.new((-hw, -hd, h_base))
    v2 = bm.verts.new((hw, -hd, h_base))
    v3 = bm.verts.new((hw, hd, h_base))
    v4 = bm.verts.new((-hw, hd, h_base))
    r1 = bm.verts.new((-hw, 0, h_top))
    r2 = bm.verts.new((hw, 0, h_top))
    bm.faces.new((v1, v2, r2, r1))
    bm.faces.new((v2, v3, r2))
    bm.faces.new((v3, v4, r1, r2))
    bm.faces.new((v4, v1, r1))
    bm.to_mesh(mesh)
    bm.free()
    roof = bpy.data.objects.new("Hangar_Roof", mesh)
    bpy.context.scene.collection.objects.link(roof)
    roof.parent = root
    assign_material(roof, mat_steel)

    # Double Sliding Giant Hangar Gates (Orange / Industrial Yellow Hazard)
    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(-3.2, -6.05, 2.5))
    door_l = bpy.context.active_object
    door_l.scale = (5.8, 0.15, 5.0)
    door_l.parent = root
    assign_material(door_l, mat_gate)

    bpy.ops.mesh.primitive_cube_add(size=1.0, location=(3.2, -6.05, 2.5))
    door_r = bpy.context.active_object
    door_r.scale = (5.8, 0.15, 5.0)
    door_r.parent = root
    assign_material(door_r, mat_gate)

    out_file = os.path.join(OUTPUT_DIR, "building_hangar_depot.glb")
    bpy.ops.export_scene.gltf(filepath=out_file, export_format='GLB', use_selection=False)
    print(f"Exported: {out_file} ({os.path.getsize(out_file)} bytes)")

if __name__ == "__main__":
    build_villa_terracotta()
    build_tactical_townhouse()
    build_military_barracks()
    build_hangar_depot()
    print("ALL BUILDINGS BUILT & EXPORTED SUCCESSFULLY!")
