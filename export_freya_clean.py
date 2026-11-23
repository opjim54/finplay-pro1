import bpy, os

blend_path = r"D:\finplay pro\3dmodels\extracted\Blend\Blend\Freya.blend"
bpy.ops.wm.open_mainfile(filepath=blend_path)

keep_names = ["Body", "Hair", "Top", "Skirt", "Stockings", "Eyes", "Eyelashes", "Eyebrows"]
for o in list(bpy.context.scene.objects):
    if o.name not in keep_names:
        bpy.data.objects.remove(o, do_unlink=True)

# Decimate hair
h = bpy.data.objects.get("Hair")
if h:
    m = h.modifiers.new(name="Decimate", type='DECIMATE')
    m.ratio = 0.35

# Decimate body slightly to keep standard GLB light
b = bpy.data.objects.get("Body")
if b:
    m = b.modifiers.new(name="Decimate", type='DECIMATE')
    m.ratio = 0.65

# Downscale images
for img in bpy.data.images:
    if img.size[0] > 512 or img.size[1] > 512:
        try:
            img.scale(512, 512)
        except Exception:
            pass

out_path = r"D:\finplay pro\public\warland\assets\models\character_freya_tactical.glb"

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
print(f"SUCCESS: Exported Freya without Draco: {sz} bytes ({sz / (1024*1024):.2f} MB)")
