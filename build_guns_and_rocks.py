
import bpy, os, math

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

# AWM
reset()
green_mat = bpy.data.materials.new(name='Mat_AwmGreen')
green_mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.16, 0.26, 0.14, 1.0)
steel_mat = bpy.data.materials.new(name='Mat_AwmSteel')
s_bsdf = steel_mat.node_tree.nodes.get('Principled BSDF')
s_bsdf.inputs['Base Color'].default_value = (0.1, 0.1, 0.12, 1.0)
s_bsdf.inputs['Metallic'].default_value = 0.9

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.1, 0))
awm_body = bpy.context.active_object
awm_body.scale = (0.09, 0.8, 0.14)
awm_body.name = 'AWM_Body'
awm_body.data.materials.append(green_mat)

bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.022, depth=0.95, location=(0, 0.65, 0.02))
awm_barrel = bpy.context.active_object
awm_barrel.rotation_euler = (math.pi/2, 0, 0)
awm_barrel.name = 'AWM_Barrel'
awm_barrel.data.materials.append(steel_mat)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 1.12, 0.02))
awm_brake = bpy.context.active_object
awm_brake.scale = (0.06, 0.08, 0.05)
awm_brake.name = 'AWM_Brake'
awm_brake.data.materials.append(steel_mat)

bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.038, depth=0.36, location=(0, 0.05, 0.16))
awm_scope = bpy.context.active_object
awm_scope.rotation_euler = (math.pi/2, 0, 0)
awm_scope.name = 'AWM_Scope'
awm_scope.data.materials.append(steel_mat)

export_glb('gun_awm.glb')

# SHOTGUN
reset()
sg_steel = bpy.data.materials.new(name='Mat_SgSteel')
sg_steel.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.15, 0.16, 0.18, 1.0)
sg_wood = bpy.data.materials.new(name='Mat_SgWood')
sg_wood.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = (0.38, 0.18, 0.08, 1.0)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.05, 0))
sg_rec = bpy.context.active_object
sg_rec.scale = (0.08, 0.35, 0.11)
sg_rec.data.materials.append(sg_steel)

bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=0.028, depth=0.65, location=(0, 0.35, 0.02))
sg_barrel = bpy.context.active_object
sg_barrel.rotation_euler = (math.pi/2, 0, 0)
sg_barrel.data.materials.append(sg_steel)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, 0.22, -0.04))
sg_pump = bpy.context.active_object
sg_pump.scale = (0.09, 0.18, 0.07)
sg_pump.data.materials.append(sg_wood)

bpy.ops.mesh.primitive_cube_add(size=1.0, location=(0, -0.32, -0.04))
sg_stock = bpy.context.active_object
sg_stock.scale = (0.07, 0.28, 0.12)
sg_stock.rotation_euler = (0.1, 0, 0)
sg_stock.data.materials.append(sg_wood)

export_glb('gun_shotgun.glb')

# ROCK BOULDER
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
