#!/usr/bin/env python3
"""Audit a converted Sift 1.1.2 pack. Usage: verify-sift-pack.py /path/to/the_sift.mcpack"""
import json
import sys
from zipfile import ZipFile

with ZipFile(sys.argv[1]) as pack:
    names = set(pack.namelist())
    for name in names:
        if name.endswith('.json'):
            json.loads(pack.read(name))
    icons = json.loads(pack.read('textures/item_texture.json'))['texture_data']
    assert 'the_sift:siftite_ingot' in icons
    assert 'the_sift:ichor_glass_pane' in icons
    for definition in icons.values():
        textures = definition['textures']
        if isinstance(textures, str):
            textures = [textures]
        for texture in textures:
            assert texture + '.png' in names, f'Missing item texture: {texture}'

    attachables = [name for name in names if name.startswith('attachables/') and name.endswith('.json')]
    assert len(attachables) == 4, f'Expected 4 armor attachables, found {len(attachables)}'
    for name in attachables:
        description = json.loads(pack.read(name))['minecraft:attachable']['description']
        binding = description.get('item', description['identifier'])
        assert description['identifier'] in binding, name
        assert description['textures']['default'] + '.png' in names, name

    multipart = [name for name in names if name.startswith('models/blocks/') and 'hydraulic_multipart_' in name and name.endswith('.json')]
    assert multipart, 'No multipart geometry was exported'
    assert len(multipart) < 1000, 'Equivalent states must reuse their geometry'
    largest_parts = 0
    for name in multipart:
        geometry = json.loads(pack.read(name))['minecraft:geometry'][0]
        bones = geometry['bones']
        bone_names = {bone['name'] for bone in bones}
        assert len(bone_names) == len(bones), f'Duplicate bone name: {name}'
        parts = sum(bone['name'].endswith('_root') for bone in bones)
        largest_parts = max(largest_parts, parts)
        assert parts >= 2, f'Dropped multipart pieces: {name}'
        for bone in bones:
            if 'parent' in bone:
                assert bone['parent'] in bone_names, f'Missing parent: {name}'
            for cube in bone.get('cubes', []):
                for face in cube.get('uv', {}).values():
                    material = face.get('material_instance', '')
                    assert material.startswith('part_'), f'Unscoped material: {name}'
    assert largest_parts >= 5, 'Connected pane/wall geometry lost parts'

    sounds = json.loads(pack.read('sounds/sound_definitions.json'))['sound_definitions']
    for definition in sounds.values():
        for sound in definition.get('sounds', []):
            path = sound if isinstance(sound, str) else sound['name']
            assert path + '.ogg' in names, f'Missing sound: {path}'
    geometries = {}
    for name in names:
        if name.startswith('models/') and name.endswith('.json'):
            document = json.loads(pack.read(name))
            for geometry in document.get('minecraft:geometry', []):
                geometries[geometry['description']['identifier']] = geometry
                if name.startswith('models/blocks/'):
                    for bone in geometry.get('bones', []):
                        for cube in bone.get('cubes', []):
                            assert cube.get('uv') != {}, f'Invisible cube with empty faces: {name}'
                    identifier = geometry['description']['identifier']
                    if identifier.startswith('geometry.the_sift.'):
                        culling_path = 'block_culling/cull_' + identifier.replace('.', '_').replace(':', '_') + '.json'
                        assert culling_path in names, f'Missing shape culling: {identifier}'
    shape_culling = 0
    for name in names:
        if not name.startswith('block_culling/cull_geometry_the_sift_') or not name.endswith('.json'):
            continue
        body = json.loads(pack.read(name))['minecraft:block_culling_rules']
        candidates = [g for identifier, g in geometries.items() if body['description']['identifier'] == 'hydraulic:cull_' + identifier.replace('.', '_').replace(':', '_')]
        assert len(candidates) == 1, f'Culling geometry reference: {name}'
        bones = {b['name']: b for b in candidates[0]['bones']}
        for rule in body['rules']:
            part = rule['geometry_part']
            assert part['bone'] in bones, f'Culling bone missing: {name}'
            cubes = bones[part['bone']].get('cubes', [])
            assert 0 <= part['cube'] < len(cubes), f'Culling cube missing: {name}'
            assert part['face'] in cubes[part['cube']]['uv'], f'Culling face missing: {name}'
            assert rule['direction'] == part['face'], f'Culling direction mismatch: {name}'
            shape_culling += 1
    assert shape_culling > 0, 'No usable Sift shape culling rules'
    for name in attachables:
        description = json.loads(pack.read(name))['minecraft:attachable']['description']
        assert description['geometry']['default'] in geometries, f'Missing armor geometry: {name}'
    assert 'geometry.hydraulic.structure_cube' in geometries
    assert 'geometry.hydraulic.sift_portal' not in geometries
    import struct
    bitmap = pack.read('textures/hydraulic/the_sift/portal.png')
    assert bitmap.startswith(b'\x89PNG')
    assert struct.unpack('>II', bitmap[16:24]) == (32, 512)
    culling = json.loads(pack.read('block_culling/hydraulic_solid_cube.json'))['minecraft:block_culling_rules']
    assert culling['description']['identifier'] == 'hydraulic:solid_cube'
    assert len(culling['rules']) == 6
    for rule in culling['rules']:
        assert rule['geometry_part']['bone'] == 'frame'
        assert rule['geometry_part']['face'] == rule['direction']
    manifest = json.loads(pack.read('manifest.json'))
    assert manifest['header']['min_engine_version'] >= [1, 26, 0]
    flipbooks = json.loads(pack.read('textures/flipbook_textures.json'))
    assert len(flipbooks) == 3
    for flipbook in flipbooks:
        bitmap = pack.read(flipbook['flipbook_texture'] + '.png')
        portal = flipbook['flipbook_texture'].endswith('/portal')
        assert struct.unpack('>II', bitmap[16:24]) == ((32, 512) if portal else (32, 1024))
        assert flipbook['ticks_per_frame'] == (3 if portal else 10)
    terrain = json.loads(pack.read('textures/terrain_texture.json'))['texture_data']
    for definition in terrain.values():
        textures = definition['textures']
        if isinstance(textures, (str, dict)): textures = [textures]
        for texture in textures:
            if isinstance(texture, dict): texture = texture['path']
            assert texture + '.png' in names, f'Missing terrain texture: {texture}'
    assert 'hydraulic_sift_portal' in terrain
    assert 'textures/hydraulic/the_sift/portal.png' in names
    entities = [name for name in names if name.startswith('entity/') and name.endswith('.json')]
    assert len(entities) == 9, f'Expected 9 entity appearances, found {len(entities)}'
    animations = {}
    for name in names:
        if name.startswith('animations/') and name.endswith('.json'):
            animations.update(json.loads(pack.read(name))['animations'])
    controllers = {}
    for name in names:
        if name.startswith("animation_controllers/") and name.endswith(".json"):
            controllers.update(json.loads(pack.read(name))["animation_controllers"])
    assert len(controllers) == 3
    for name in entities:
        description = json.loads(pack.read(name))['minecraft:client_entity']['description']
        geometry = description['geometry']['default']
        assert geometry in geometries or geometry in {'geometry.sniffer', 'geometry.boat', 'geometry.chest_boat'}, name
        assert description['textures']['default'] + '.png' in names, name
        for animation in description.get('animations', {}).values():
            assert animation in animations or animation in controllers, f'Missing entity animation: {animation}'
    particles = [name for name in names if name.startswith('particles/') and name.endswith('.json')]
    assert len(particles) == 10
    for name in particles:
        description = json.loads(pack.read(name))['particle_effect']['description']
        assert description['basic_render_parameters']['texture'] + '.png' in names, name
    print(f'PASS: portal geometry/texture, {len(entities)} entity appearances, {len(animations)} animations, {len(particles)} effects')
    print(f'PASS: {len(icons)} item icons, {len(attachables)} armor attachables, '
          f'{len(multipart)} shared multipart geometries, {len(sounds)} sound events')
