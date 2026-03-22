import json, copy

# Load the current baked model
with open(r"src\main\resources\assets\seamlessdeconstructor\models\block\reverse_deconstructor.json", "r") as f:
    model = json.load(f)

def face_dims(elem, face_name):
    """Get the physical pixel dimensions (width, height) of a face."""
    fx, fy, fz = elem["from"]
    tx, ty, tz = elem["to"]
    sx, sy, sz = tx - fx, ty - fy, tz - fz
    if face_name in ("north", "south"):
        return (sx, sy)
    elif face_name in ("east", "west"):
        return (sz, sy)
    elif face_name in ("up", "down"):
        return (sx, sz)

def auto_uv(elem, face_name):
    """Generate 1:1 UV coordinates matching face dimensions."""
    w, h = face_dims(elem, face_name)
    return [0, 0, w, h]

# Elements that need full UV fix (indices in baked model)
# These are the Z-rotated horizontal beams and shelves
full_fix_indices = [3, 4, 5, 6, 7, 11, 12]

for idx in full_fix_indices:
    elem = model["elements"][idx]
    for face_name in elem["faces"]:
        face = elem["faces"][face_name]
        old_uv = face["uv"]
        new_uv = auto_uv(elem, face_name)
        if old_uv != new_uv:
            print(f"Element {idx} ({elem['from']}->{elem['to']}) face '{face_name}': {old_uv} -> {new_uv}")
        face["uv"] = new_uv

# Fix bar top/bottom faces (elements 24, 25 - the E/W rim bars)
# These are 1x2x16 bars where up/down faces are 1x16 but UV says 16x1
for idx in [24, 25]:
    elem = model["elements"][idx]
    for face_name in ["up", "down"]:
        face = elem["faces"][face_name]
        old_uv = face["uv"]
        w, h = face_dims(elem, face_name)
        # Use UV rotation to map the texture strip along the long axis
        # Keep UV as 16x1 strip but rotate 90 so it maps correctly to 1x16 face
        face["uv"] = [0, 3, 16, 4]
        face["rotation"] = 90
        print(f"Element {idx} ({elem['from']}->{elem['to']}) face '{face_name}': {old_uv} -> {face['uv']} + rotation 90")

# Write the fixed model
output = json.dumps(model, indent="\t")
with open(r"src\main\resources\assets\seamlessdeconstructor\models\block\reverse_deconstructor.json", "w") as f:
    f.write(output)

print("\nDone! Model saved.")
print(f"Total elements: {len(model['elements'])}")

# Also show a summary of all elements for verification
print("\nElement summary:")
for i, elem in enumerate(model["elements"]):
    fx, fy, fz = elem["from"]
    tx, ty, tz = elem["to"]
    print(f"  [{i}] ({fx},{fy},{fz})->({tx},{ty},{tz}) size {tx-fx}x{ty-fy}x{tz-fz}")
