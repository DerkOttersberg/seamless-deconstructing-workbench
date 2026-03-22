import json, math, sys

def rot_matrix(rx_deg, ry_deg, rz_deg):
    """Create rotation matrix R = Rz * Ry * Rx (extrinsic XYZ order)."""
    rx = math.radians(rx_deg)
    ry = math.radians(ry_deg)
    rz = math.radians(rz_deg)
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    return [
        [cy*cz, cz*sx*sy - cx*sz, cx*cz*sy + sx*sz],
        [cy*sz, cx*cz + sx*sy*sz, cx*sy*sz - cz*sx],
        [-sy, cy*sx, cx*cy]
    ]

def mat_mul_vec(m, v):
    return [sum(m[i][j] * v[j] for j in range(3)) for i in range(3)]

def rotate_point(point, matrix, origin):
    p = [point[i] - origin[i] for i in range(3)]
    rp = mat_mul_vec(matrix, p)
    return [rp[i] + origin[i] for i in range(3)]

FACE_NORMALS = {
    'north': [0, 0, -1], 'south': [0, 0, 1],
    'east': [1, 0, 0], 'west': [-1, 0, 0],
    'up': [0, 1, 0], 'down': [0, -1, 0]
}

def get_face_dir(normal):
    abs_vals = [abs(n) for n in normal]
    max_idx = abs_vals.index(max(abs_vals))
    if max_idx == 0: return 'east' if normal[0] > 0 else 'west'
    elif max_idx == 1: return 'up' if normal[1] > 0 else 'down'
    else: return 'south' if normal[2] > 0 else 'north'

def clean_coord(v):
    """Round to integer if very close, otherwise keep 1 decimal."""
    r = round(v)
    if abs(v - r) < 0.01:
        return r
    return round(v, 1)

def process_element(elem, idx):
    rot = elem.get('rotation', {})

    if 'angle' in rot:
        angle = rot['angle']
        if angle == 0:
            return {'from': elem['from'], 'to': elem['to'], 'faces': elem['faces']}
        valid_angles = [-45, -22.5, 0, 22.5, 45]
        if angle in valid_angles:
            return {
                'from': elem['from'], 'to': elem['to'],
                'rotation': {'angle': angle, 'axis': rot['axis'], 'origin': rot['origin']},
                'faces': elem['faces']
            }
        rx = angle if rot.get('axis') == 'x' else 0
        ry = angle if rot.get('axis', 'y') == 'y' else 0
        rz = angle if rot.get('axis') == 'z' else 0
    else:
        rx = rot.get('x', 0)
        ry = rot.get('y', 0)
        rz = rot.get('z', 0)

    origin = rot.get('origin', [8, 8, 8])

    if rx == 0 and ry == 0 and rz == 0:
        return {'from': elem['from'], 'to': elem['to'], 'faces': elem['faces']}

    m = rot_matrix(rx, ry, rz)

    new_from = rotate_point(elem['from'], m, origin)
    new_to = rotate_point(elem['to'], m, origin)

    new_from = [clean_coord(v) for v in new_from]
    new_to = [clean_coord(v) for v in new_to]

    # Remap faces
    new_faces = {}
    for face_name, face_data in elem.get('faces', {}).items():
        normal = FACE_NORMALS[face_name]
        rotated_normal = mat_mul_vec(m, normal)
        new_face_name = get_face_dir(rotated_normal)
        new_faces[new_face_name] = face_data

    result = {'from': new_from, 'to': new_to, 'faces': new_faces}
    return result

with open('deconstructing workbench.json', 'r') as f:
    data = json.load(f)

elements = []
for i, e in enumerate(data['elements']):
    elements.append(process_element(e, i))

output = {
    'credit': 'Made with Blockbench',
    'parent': 'minecraft:block/block',
    'textures': {
        '0': 'minecraft:block/cracked_stone_bricks',
        '1': 'minecraft:block/cracked_stone_bricks',
        '2': 'minecraft:block/cracked_stone_bricks',
        '4': 'seamlessdeconstructor:block/oak_planks_dark',
        '5': 'minecraft:block/oak_planks',
        '6': 'seamlessdeconstructor:block/cracked_stone_bricks_dark',
        '7': 'seamlessdeconstructor:block/saw',
        '8': 'seamlessdeconstructor:block/scissor',
        '9': 'seamlessdeconstructor:block/hammer',
        'particle': 'seamlessdeconstructor:block/saw'
    },
    'elements': elements
}

print(json.dumps(output, indent='\t'))
