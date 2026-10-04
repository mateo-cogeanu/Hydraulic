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
        assert description['identifier'] in description['item'], name
        assert description['textures']['default'] + '.png' in names, name

    multipart = [name for name in names if 'hydraulic_multipart_' in name and name.endswith('.json')]
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
    print(f'PASS: {len(icons)} item icons, {len(attachables)} armor attachables, '
          f'{len(multipart)} shared multipart geometries, {len(sounds)} sound events')
