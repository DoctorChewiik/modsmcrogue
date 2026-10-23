import random, math, os
from PIL import Image

ROOT = "/home/claude/modsmcrogue/src"
JAVA_DIR = ROOT + "/client/java/fr/nico/roguetower/client/render/model"
TEX_DIR = ROOT + "/main/resources/assets/roguetower/textures/entity"
os.makedirs(JAVA_DIR, exist_ok=True)
os.makedirs(TEX_DIR, exist_ok=True)

def clamp(v): return max(0, min(255, int(v)))
def jitter(c, amt, rnd):
    d = rnd.randint(-amt, amt)
    return (clamp(c[0]+d), clamp(c[1]+d), clamp(c[2]+d), 255)

# ---------- painters: f(px, x, y, w, h, rnd) draws on a face rect ----------
def solid(base, amt=12):
    def p(img, X, Y, w, h, rnd):
        for x in range(w):
            for y in range(h):
                img[X+x, Y+y] = jitter(base, amt, rnd)
    return p

def stack(*painters):
    def p(img, X, Y, w, h, rnd):
        for q in painters: q(img, X, Y, w, h, rnd)
    return p

def spots(color, chance):
    def p(img, X, Y, w, h, rnd):
        for x in range(w):
            for y in range(h):
                if rnd.random() < chance: img[X+x, Y+y] = jitter(color, 10, rnd)
    return p

def stripes(color, every, horizontal=True):
    def p(img, X, Y, w, h, rnd):
        for x in range(w):
            for y in range(h):
                k = y if horizontal else x
                if k % every == 0: img[X+x, Y+y] = jitter(color, 8, rnd)
    return p

def cracks(color, n):
    def p(img, X, Y, w, h, rnd):
        for _ in range(n):
            x, y = rnd.randrange(w), rnd.randrange(h)
            for _ in range(rnd.randint(2, 5)):
                if 0 <= x < w and 0 <= y < h: img[X+x, Y+y] = jitter(color, 15, rnd)
                x += rnd.choice((-1, 0, 1)); y += rnd.choice((0, 1))
    return p

def eyes(color, pupil=None, size=1, y_frac=0.35, spread=0.25):
    def p(img, X, Y, w, h, rnd):
        cy = int(h * y_frac)
        for side in (-1, 1):
            cx = int(w / 2 + side * w * spread) - (1 if side < 0 else 0)
            for dx in range(size):
                for dy in range(size):
                    xx, yy = cx + dx - size // 2, cy + dy
                    if 0 <= xx < w and 0 <= yy < h:
                        img[X+xx, Y+yy] = color + (255,)
            if pupil and size >= 2:
                img[X+cx, Y+cy+size//2] = pupil + (255,)
    return p

def big_eye(iris, pupil=(5, 0, 10)):
    def p(img, X, Y, w, h, rnd):
        cx, cy = (w-1)/2, (h-1)/2
        r = min(w, h) / 2
        for x in range(w):
            for y in range(h):
                d = math.hypot(x-cx, y-cy)
                if d < r*0.22: img[X+x, Y+y] = pupil + (255,)
                elif d < r*0.55: img[X+x, Y+y] = jitter(iris, 15, rnd)
    return p

def mouth(color, teeth=(240, 235, 220), y_frac=0.7):
    def p(img, X, Y, w, h, rnd):
        y = int(h * y_frac)
        for x in range(1, w-1):
            img[X+x, Y+y] = color + (255,)
            if x % 2 == 0 and y+1 < h: img[X+x, Y+y+1] = teeth + (255,)
    return p

def glyphs(color, n):
    def p(img, X, Y, w, h, rnd):
        for _ in range(n):
            x, y = rnd.randrange(max(1, w-2)), rnd.randrange(max(1, h-2))
            shape = rnd.choice([[(0,0),(1,0),(1,1)], [(0,0),(0,1),(1,1)], [(0,0),(1,1)], [(1,0),(0,1),(1,1),(0,0)]])
            for dx, dy in shape:
                if x+dx < w and y+dy < h: img[X+x+dx, Y+y+dy] = color + (255,)
    return p

def gradient(top, bottom):
    def p(img, X, Y, w, h, rnd):
        for y in range(h):
            t = y / max(1, h-1)
            c = tuple(top[i]*(1-t) + bottom[i]*t for i in range(3))
            for x in range(w): img[X+x, Y+y] = jitter(c, 8, rnd)
    return p

# ---------- model spec ----------
class Box:
    def __init__(self, u, v, x, y, z, w, h, d, paint, faces=None, mirror=False):
        self.u, self.v, self.x, self.y, self.z, self.w, self.h, self.d = u, v, x, y, z, w, h, d
        self.paint, self.faces, self.mirror = paint, faces or {}, mirror
    def rects(self):
        u, v, w, h, d = self.u, self.v, int(math.ceil(self.w)), int(math.ceil(self.h)), int(math.ceil(self.d))
        return {
            "top": (u+d, v, w, d), "bottom": (u+d+w, v, w, d),
            "right": (u, v+d, d, h), "front": (u+d, v+d, w, h),
            "left": (u+d+w, v+d, d, h), "back": (u+2*d+w, v+d, w, h),
        }

class Part:
    def __init__(self, name, parent, pivot, boxes, rot=(0, 0, 0)):
        self.name, self.parent, self.pivot, self.boxes, self.rot = name, parent, pivot, boxes, rot

def f(v):
    s = ("%.4f" % v).rstrip("0").rstrip(".")
    if s in ("-0", ""): s = "0"
    return s + "f"

def build(name, cls, entity, tw, th, parts, anim, seed=1):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
    px = img.load()
    used = {}
    for part in parts:
        for b in part.boxes:
            for face, (X, Y, w, h) in b.rects().items():
                if w <= 0 or h <= 0: continue
                if X + w > tw or Y + h > th:
                    raise SystemExit(f"{name}: box {part.name} face {face} out of texture")
                for x in range(X, X+w):
                    for y in range(Y, Y+h):
                        key = (b.u, b.v, b.w, b.h, b.d)
                        if (x, y) in used and used[(x, y)] != key and not b.mirror:
                            raise SystemExit(f"{name}: overlap at {x},{y} {part.name} vs {used[(x,y)]}")
                        used[(x, y)] = key
                b.paint(px, X, Y, w, h, rnd)
                if face in b.faces:
                    b.faces[face](px, X, Y, w, h, rnd)
    img.save(f"{TEX_DIR}/{name}.png")

    lines = []
    lines.append("package fr.nico.roguetower.client.render.model;\n")
    lines.append("import fr.nico.roguetower.RogueTower;")
    lines.append(f"import fr.nico.roguetower.entity.{entity};")
    lines.append("import net.minecraft.client.model.*;")
    lines.append("import net.minecraft.client.render.entity.model.EntityModelLayer;")
    lines.append("import net.minecraft.client.render.entity.model.SinglePartEntityModel;")
    lines.append("import net.minecraft.util.math.MathHelper;\n")
    lines.append("/** Modèle original généré (formes et texture créées pour Rogue Tower). */")
    lines.append(f"public class {cls} extends SinglePartEntityModel<{entity}> {{")
    lines.append(f"\tpublic static final EntityModelLayer LAYER = new EntityModelLayer(RogueTower.id(\"{name}\"), \"main\");")
    lines.append("\tprivate final ModelPart root;")
    for p in parts:
        lines.append(f"\tprivate final ModelPart {p.name};")
    lines.append("")
    lines.append(f"\tpublic {cls}(ModelPart root) {{")
    lines.append("\t\tthis.root = root;")
    for p in parts:
        par = "root" if p.parent is None else p.parent
        lines.append(f"\t\tthis.{p.name} = {par}.getChild(\"{p.name}\");")
    lines.append("\t}\n")
    lines.append("\tpublic static TexturedModelData getTexturedModelData() {")
    lines.append("\t\tModelData data = new ModelData();")
    lines.append("\t\tModelPartData root = data.getRoot();")
    for p in parts:
        par = "root" if p.parent is None else p.parent
        builder = "ModelPartBuilder.create()"
        for b in p.boxes:
            if b.mirror: builder += ".mirrored()"
            builder += f".uv({b.u}, {b.v}).cuboid({f(b.x)}, {f(b.y)}, {f(b.z)}, {f(b.w)}, {f(b.h)}, {f(b.d)})"
            if b.mirror: builder += ".mirrored(false)"
        px_, py_, pz_ = p.pivot
        rp, ry, rr = p.rot
        lines.append(f"\t\tModelPartData {p.name} = {par}.addChild(\"{p.name}\", {builder},")
        lines.append(f"\t\t\tModelTransform.of({f(px_)}, {f(py_)}, {f(pz_)}, {f(rp)}, {f(ry)}, {f(rr)}));")
    lines.append(f"\t\treturn TexturedModelData.of(data, {tw}, {th});")
    lines.append("\t}\n")
    lines.append("\t@Override\n\tpublic ModelPart getPart() {\n\t\treturn root;\n\t}\n")
    lines.append("\t@Override")
    lines.append(f"\tpublic void setAngles({entity} entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {{")
    lines.append("\t\troot.traverse().forEach(ModelPart::resetTransform);")
    lines.append("\t\tfloat walk = MathHelper.cos(limbAngle * 0.6662f) * 1.2f * limbDistance;")
    lines.append("\t\tfloat t = animationProgress;")
    lines.append("\t\tfloat action = entity.getActionProgress();")
    for l in anim.strip().splitlines():
        lines.append("\t\t" + l.strip())
    lines.append("\t}")
    lines.append("}")
    # the unused-variable-safe ModelPartData locals: suppress via usage
    src = "\n".join(lines) + "\n"
    open(f"{JAVA_DIR}/{cls}.java", "w").write(src)
    print("generated", cls)

# ================= MODELS =================
# 1. Glouton : sac gélatineux avec une énorme gueule à charnière et deux yeux sur tiges
skin = (150, 40, 150); skin2 = (200, 90, 210); maw = (170, 20, 40)
build("glutton", "GluttonModel", "GluttonEntity", 64, 64, [
    Part("jaw", None, (0, 24, 0), [Box(0, 19, -6, -5, -6, 12, 5, 12, stack(solid(skin), spots(skin2, 0.12)),
        faces={"top": stack(solid(maw), spots((230, 230, 210), 0.05))})]),
    Part("upper", None, (0, 19, 6), [Box(0, 0, -6, -7, -12, 12, 7, 12, stack(solid(skin), spots(skin2, 0.15)),
        faces={"bottom": solid(maw), "front": stack(stripes((90, 20, 90), 3))})]),
    Part("teeth", "upper", (0, 0, 0), [Box(0, 36, -5.5, 0, -11.5, 11, 1, 1, solid((245, 240, 225), 6))]),
    Part("eyeStalkR", "upper", (-3, -7, -7), [Box(48, 2, -1, -4, -1, 2, 4, 2, solid(skin))]),
    Part("eyeR", "eyeStalkR", (0, -4, 0), [Box(48, 8, -1.5, -3, -1.5, 3, 3, 3, solid((250, 230, 60)), faces={"front": solid((10, 10, 10), 0)})]),
    Part("eyeStalkL", "upper", (3, -7, -7), [Box(48, 2, -1, -4, -1, 2, 4, 2, solid(skin), mirror=True)]),
    Part("eyeL", "eyeStalkL", (0, -4, 0), [Box(48, 8, -1.5, -3, -1.5, 3, 3, 3, solid((250, 230, 60)), faces={"front": solid((10, 10, 10), 0)}, mirror=True)]),
], """
float hop = Math.abs(MathHelper.sin(limbAngle * 0.5f)) * limbDistance * 3f;
jaw.pivotY -= hop;
upper.pivotY -= hop;
upper.pitch = -0.15f - Math.abs(MathHelper.sin(t * 0.08f)) * 0.15f - action * 0.9f;
eyeStalkR.roll = MathHelper.sin(t * 0.13f) * 0.3f - 0.2f;
eyeStalkL.roll = -MathHelper.sin(t * 0.11f) * 0.3f + 0.2f;
eyeStalkR.pitch = headPitch * 0.01f;
upper.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.3f;
""", seed=11)

# 2. Mantelame : insecte dressé à faux
chit = (70, 170, 60); chit2 = (30, 90, 30); blade = (225, 230, 235)
build("blade_mantis", "BladeMantisModel", "BladeMantisEntity", 64, 32, [
    Part("abdomen", None, (0, 13, 2), [Box(0, 0, -2.5, -2, 0, 5, 4, 9, stack(solid(chit), stripes(chit2, 2, horizontal=False)))], rot=(-0.25, 0, 0)),
    Part("thorax", None, (0, 13, 0), [Box(0, 13, -1.5, -10, -1.5, 3, 10, 3, stack(solid(chit), stripes(chit2, 3)))]),
    Part("head", "thorax", (0, -10, 0), [Box(28, 0, -2.5, -4, -2.5, 5, 4, 4, solid(chit),
        faces={"front": eyes((230, 30, 30), size=2, y_frac=0.1, spread=0.32)})]),
    Part("armR", "thorax", (-1.5, -8, -1), [Box(12, 13, -1, 0, -1, 2, 7, 2, solid(chit))]),
    Part("bladeR", "armR", (0, 7, 0), [Box(28, 8, -0.5, -1, -9, 1, 2, 9, stack(solid(blade, 6), stripes((160, 165, 170), 2, horizontal=False)))]),
    Part("armL", "thorax", (1.5, -8, -1), [Box(12, 13, -1, 0, -1, 2, 7, 2, solid(chit), mirror=True)]),
    Part("bladeL", "armL", (0, 7, 0), [Box(28, 8, -0.5, -1, -9, 1, 2, 9, solid(blade, 6), mirror=True)]),
    Part("legFR", None, (-1.5, 13, -1), [Box(48, 0, -0.5, 0, -0.5, 1, 11, 1, solid(chit2))], rot=(0, 0, 0.45)),
    Part("legFL", None, (1.5, 13, -1), [Box(48, 0, -0.5, 0, -0.5, 1, 11, 1, solid(chit2), mirror=True)], rot=(0, 0, -0.45)),
    Part("legBR", None, (-1.5, 13, 1.5), [Box(52, 0, -0.5, 0, -0.5, 1, 11, 1, solid(chit2))], rot=(0, 0, 0.45)),
    Part("legBL", None, (1.5, 13, 1.5), [Box(52, 0, -0.5, 0, -0.5, 1, 11, 1, solid(chit2), mirror=True)], rot=(0, 0, -0.45)),
], """
legFR.pitch = walk; legBL.pitch = walk;
legFL.pitch = -walk; legBR.pitch = -walk;
head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
head.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
float idle = MathHelper.sin(t * 0.1f) * 0.1f;
float raise = entity.isWindingUp() ? action : 0f;
float slash = entity.isStriking() ? action : 0f;
armR.pitch = -0.9f + idle - raise * 1.6f + slash * 1.8f;
armL.pitch = -0.9f - idle - raise * 1.6f + slash * 1.8f;
bladeR.pitch = 0.6f + raise * 0.6f;
bladeL.pitch = 0.6f + raise * 0.6f;
abdomen.yaw = MathHelper.sin(t * 0.07f) * 0.1f;
""", seed=22)

# 3. Totem Hurleur : trois visages empilés, ailes de bois
wood = (120, 80, 50); wood2 = (90, 55, 30); glow = (80, 230, 255)
face = lambda: stack(eyes(glow, size=2, y_frac=0.3, spread=0.25), mouth((30, 15, 10), teeth=glow, y_frac=0.68))
build("howling_totem", "HowlingTotemModel", "HowlingTotemEntity", 64, 64, [
    Part("base", None, (0, 24, 0), [Box(0, 0, -5, -8, -5, 10, 8, 10, stack(solid(wood), stripes(wood2, 3)), faces={"front": face()})]),
    Part("middle", None, (0, 16, 0), [Box(0, 18, -4, -8, -4, 8, 8, 8, stack(solid((140, 60, 45)), stripes(wood2, 4)), faces={"front": face()})]),
    Part("top", None, (0, 8, 0), [Box(0, 34, -4.5, -9, -4.5, 9, 9, 9, stack(solid(wood), glyphs(glow, 3)), faces={"front": face()})]),
    Part("wingR", "top", (-4.5, -6, 0), [Box(40, 0, -7, -3, -0.5, 7, 6, 1, stack(solid((200, 60, 40)), stripes((240, 200, 60), 2, horizontal=False)))]),
    Part("wingL", "top", (4.5, -6, 0), [Box(40, 0, 0, -3, -0.5, 7, 6, 1, solid((200, 60, 40)), mirror=True)]),
], """
float charge = entity.isWindingUp() ? action : 0f;
top.pivotY -= charge * 3f;
top.yaw = MathHelper.sin(t * 0.05f) * 0.15f + MathHelper.sin(t * 1.3f) * 0.12f * charge;
middle.yaw = -MathHelper.sin(t * 0.05f) * 0.1f;
wingR.roll = -0.2f - MathHelper.sin(t * 0.2f) * 0.1f - charge * 0.6f;
wingL.roll = 0.2f + MathHelper.sin(t * 0.2f) * 0.1f + charge * 0.6f;
""", seed=33)

# 4. Rampecroc : mille-pattes cuirassé
shell = (150, 70, 30); band = (240, 200, 40)
segs = []
for i in range(4):
    segs.append(Part(f"seg{i}", None, (0, 21, -3 + i * 4), [
        Box(0, 10, -2.5, -2.5, 0, 5, 5, 4, stack(solid(shell), stripes(band, 4, horizontal=False))),
        Box(18, 10, -0.5, -4.5, 1, 1, 2, 1, solid((40, 25, 15))),
        Box(22, 10, -3.5, 1.5, 1.5, 7, 1, 1, solid((40, 25, 15))),
    ]))
build("crawler", "CrawlerModel", "CrawlerEntity", 64, 32, [
    Part("head", None, (0, 21, -3), [Box(0, 0, -3, -3, -5, 6, 5, 5, solid((120, 50, 20)),
        faces={"front": eyes((120, 255, 80), size=1, y_frac=0.25, spread=0.3)})]),
    Part("jaw", "head", (0, 1.5, -5), [Box(22, 0, -2.5, 0, -2, 5, 1, 2, solid((60, 30, 15)))]),
] + segs, """
for (int i = 0; i < 4; i++) {
    ModelPart s = switch (i) { case 0 -> seg0; case 1 -> seg1; case 2 -> seg2; default -> seg3; };
    s.pivotX = MathHelper.sin(limbAngle * 0.8f - i * 0.9f) * 1.6f * Math.min(1f, limbDistance * 2f + 0.15f);
    s.yaw = MathHelper.cos(limbAngle * 0.8f - i * 0.9f) * 0.25f;
}
head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE * 0.5f;
head.pitch = entity.isWindingUp() ? -action * 0.6f : 0f;
jaw.pitch = 0.2f + MathHelper.sin(t * 0.4f) * 0.15f + (entity.isWindingUp() ? action * 0.6f : 0f);
""", seed=44)

# 5. Sporelin : champignon marcheur bleu luminescent
cap = (40, 120, 150); capspot = (150, 255, 230); stem = (225, 215, 190)
build("sporeling", "SporelingModel", "SporelingEntity", 64, 32, [
    Part("cap", None, (0, 14, 0), [Box(0, 0, -6, -5, -6, 12, 5, 12, stack(solid(cap), spots(capspot, 0.12)))]),
    Part("stem", None, (0, 14, 0), [Box(0, 17, -3, 0, -3, 6, 6, 6, solid(stem, 8),
        faces={"front": stack(eyes((20, 20, 30), size=1, y_frac=0.3, spread=0.2), mouth((90, 60, 50), teeth=(90, 60, 50), y_frac=0.7))})]),
    Part("legR", None, (-1.5, 20, 0), [Box(24, 17, -1, 0, -1, 2, 4, 2, solid((200, 190, 160)))]),
    Part("legL", None, (1.5, 20, 0), [Box(24, 17, -1, 0, -1, 2, 4, 2, solid((200, 190, 160)), mirror=True)]),
], """
legR.pitch = walk * 1.4f;
legL.pitch = -walk * 1.4f;
float wobble = MathHelper.sin(limbAngle * 0.66f) * limbDistance * 0.3f;
cap.roll = wobble;
stem.roll = wobble * 0.5f;
float swell = entity.isWindingUp() ? 1f + action * 0.6f + MathHelper.sin(t * 2f) * 0.05f : 1f;
cap.xScale = swell; cap.yScale = swell; cap.zScale = swell;
""", seed=55)

# 6. Gargouille de Braise : pierre noire fissurée de lave, ailes
stone = (45, 42, 48); lava = (255, 120, 20)
build("ember_gargoyle", "EmberGargoyleModel", "EmberGargoyleEntity", 64, 32, [
    Part("body", None, (0, 12, 0), [Box(0, 0, -3, -4, -2, 6, 8, 4, stack(solid(stone), cracks(lava, 4)))]),
    Part("head", "body", (0, -4, -0.5), [Box(20, 0, -2.5, -5, -3, 5, 5, 5, stack(solid(stone), cracks(lava, 2)),
        faces={"front": eyes((255, 170, 30), size=1, y_frac=0.4, spread=0.25)})]),
    Part("hornR", "head", (-2, -5, 0), [Box(40, 0, -0.5, -3, -0.5, 1, 3, 1, solid((220, 210, 190)))], rot=(0, 0, -0.3)),
    Part("hornL", "head", (2, -5, 0), [Box(40, 0, -0.5, -3, -0.5, 1, 3, 1, solid((220, 210, 190)), mirror=True)], rot=(0, 0, 0.3)),
    Part("wingR", "body", (-3, -3, 1.5), [Box(0, 12, -10, -1, 0, 10, 8, 1, stack(solid((70, 30, 30)), stripes(stone, 3, horizontal=False)))]),
    Part("wingL", "body", (3, -3, 1.5), [Box(0, 12, 0, -1, 0, 10, 8, 1, solid((70, 30, 30)), mirror=True)]),
    Part("armR", "body", (-3.5, -3, 0), [Box(44, 0, -1, 0, -1, 2, 5, 2, solid(stone))]),
    Part("armL", "body", (3.5, -3, 0), [Box(44, 0, -1, 0, -1, 2, 5, 2, solid(stone), mirror=True)]),
    Part("legR", "body", (-1.5, 4, 0), [Box(44, 7, -1, 0, -1, 2, 5, 2, solid(stone))]),
    Part("legL", "body", (1.5, 4, 0), [Box(44, 7, -1, 0, -1, 2, 5, 2, solid(stone), mirror=True)]),
    Part("tail", "body", (0, 3, 2), [Box(22, 12, -0.5, 0, 0, 1, 1, 6, stack(solid(stone), cracks(lava, 1)))], rot=(0.5, 0, 0)),
], """
float flap = MathHelper.sin(t * 0.6f) * 0.6f;
float dive = entity.isStriking() ? action : 0f;
float charge = entity.isWindingUp() ? action : 0f;
wingR.yaw = 0.35f + flap * (1f - dive) - charge * 0.5f;
wingL.yaw = -0.35f - flap * (1f - dive) + charge * 0.5f;
body.pitch = 0.2f + dive * 0.9f;
body.pivotY += MathHelper.sin(t * 0.1f) * 1.0f;
head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
armR.pitch = -0.4f - dive * 1.2f; armL.pitch = -0.4f - dive * 1.2f;
legR.pitch = 0.4f; legL.pitch = 0.4f;
tail.yaw = MathHelper.sin(t * 0.15f) * 0.4f;
""", seed=66)

# 7. Œil du Néant (refait avec le même générateur)
build("void_eye", "VoidEyeModel", "VoidEyeEntity", 64, 64, [
    Part("eye", None, (0, 12, 0), [Box(0, 0, -5, -5, -5, 10, 10, 10, stack(solid((230, 225, 225), 10), cracks((160, 20, 40), 6)),
        faces={"front": big_eye((130, 40, 210))})]),
    Part("lid", "eye", (0, 0, 0), [Box(0, 33, -5.5, -5.5, -5.5, 11, 2, 11, stack(solid((60, 15, 90)), spots((120, 40, 160), 0.2)))]),
    Part("t0", None, (-3, 16, -3), [Box(40, 0, -1, 0, -1, 2, 8, 2, gradient((70, 20, 110), (20, 5, 40)))]),
    Part("t1", None, (3, 16, -3), [Box(48, 0, -1, 0, -1, 2, 8, 2, gradient((70, 20, 110), (20, 5, 40)))]),
    Part("t2", None, (-3, 16, 3), [Box(56, 0, -1, 0, -1, 2, 8, 2, gradient((70, 20, 110), (20, 5, 40)))]),
    Part("t3", None, (3, 16, 3), [Box(40, 23, -1, 0, -1, 2, 7, 2, gradient((70, 20, 110), (20, 5, 40)))]),
], """
float bob = MathHelper.sin(t * 0.1f) * 1.2f;
eye.pivotY += bob;
eye.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
eye.pitch = headPitch * MathHelper.RADIANS_PER_DEGREE;
float charge = entity.isWindingUp() ? action : 0f;
float size = 1f - charge * 0.35f + MathHelper.sin(t * 1.5f) * 0.05f * charge;
eye.xScale = size; eye.yScale = size; eye.zScale = size;
lid.pivotY = -charge * 0.5f;
ModelPart[] ts = {t0, t1, t2, t3};
for (int i = 0; i < 4; i++) {
    ts[i].pivotY += bob;
    ts[i].pitch = MathHelper.sin(t * 0.2f + i * 1.7f) * 0.35f;
    ts[i].roll = MathHelper.cos(t * 0.17f + i) * 0.25f;
}
""", seed=77)

# 8. Golem Runique (boss)
slate = (60, 64, 75); rune = (70, 230, 255)
build("rune_golem", "RuneGolemModel", "RuneGolemEntity", 128, 64, [
    Part("body", None, (0, 14, 0), [Box(0, 0, -9, -14, -6, 18, 14, 12, stack(solid(slate), glyphs(rune, 8)))]),
    Part("head", "body", (0, -14, -2), [Box(60, 0, -4, -7, -4, 8, 7, 8, solid((50, 54, 64)),
        faces={"front": stack(eyes(rune, size=2, y_frac=0.35, spread=0.22), glyphs(rune, 1))})]),
    Part("armR", "body", (-9, -12, 0), [Box(64, 15, -6, -2, -3, 6, 18, 6, stack(solid(slate), glyphs(rune, 4)))]),
    Part("armL", "body", (9, -12, 0), [Box(64, 15, 0, -2, -3, 6, 18, 6, stack(solid(slate), glyphs(rune, 4)), mirror=True)]),
    Part("legR", None, (-5, 14, 0), [Box(0, 26, -3, 0, -3, 6, 10, 6, solid((55, 58, 68)))]),
    Part("legL", None, (5, 14, 0), [Box(0, 26, -3, 0, -3, 6, 10, 6, solid((55, 58, 68)), mirror=True)]),
    Part("core", "body", (0, -8, -6), [Box(24, 26, -2, -2, -1, 4, 4, 1, solid(rune, 20))]),
], """
legR.pitch = walk * 0.6f;
legL.pitch = -walk * 0.6f;
head.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
float raise = entity.isWindingUp() ? action : 0f;
float slam = entity.isStriking() ? action : 0f;
armR.pitch = -walk * 0.6f - raise * 2.6f + slam * 2.6f;
armL.pitch = walk * 0.6f - raise * 2.6f + slam * 2.6f;
core.xScale = 1f + MathHelper.sin(t * 0.3f) * 0.15f + raise * 0.5f;
core.yScale = core.xScale;
body.pitch = slam * 0.3f;
""", seed=88)

# 9. Tisseuse d'Ombres (boss) : capuche flottante et quatre mains
robe = (35, 20, 50); robe2 = (70, 30, 100); shadoweye = (210, 120, 255)
hand = lambda u, v: Box(u, v, -1.5, -1.5, -1.5, 3, 3, 3, solid((200, 190, 230), 10))
build("shadow_weaver", "ShadowWeaverModel", "ShadowWeaverEntity", 128, 64, [
    Part("robe", None, (0, 18, 0), [Box(0, 0, -5, -14, -4, 10, 14, 8, stack(gradient(robe2, robe), stripes((120, 60, 160), 5)))]),
    Part("hood", "robe", (0, -14, 0), [Box(36, 0, -5, -10, -5, 10, 10, 10, solid(robe),
        faces={"front": stack(solid((10, 5, 15), 4), eyes(shadoweye, size=2, y_frac=0.45, spread=0.22))})]),
    Part("crown", "hood", (0, -10, 0), [Box(76, 0, -4, -3, -4, 8, 3, 8, stack(solid((60, 20, 90)), glyphs(shadoweye, 2)))]),
    Part("hand0", None, (-9, 6, -2), [hand(0, 22)]),
    Part("hand1", None, (9, 6, -2), [hand(12, 22)]),
    Part("hand2", None, (-8, 12, 3), [hand(24, 22)]),
    Part("hand3", None, (8, 12, 3), [hand(36, 22)]),
], """
float bob = MathHelper.sin(t * 0.08f) * 1.5f;
robe.pivotY += bob;
hood.yaw = headYaw * MathHelper.RADIANS_PER_DEGREE;
float gather = entity.isWindingUp() ? action : 0f;
ModelPart[] hs = {hand0, hand1, hand2, hand3};
for (int i = 0; i < 4; i++) {
    float a = t * 0.05f + i * MathHelper.HALF_PI;
    float r = 9f * (1f - gather * 0.6f);
    hs[i].pivotX = MathHelper.cos(a) * r;
    hs[i].pivotZ = MathHelper.sin(a) * r;
    hs[i].pivotY = 6f + (i % 2) * 6f + bob + MathHelper.sin(t * 0.2f + i) * 1.5f - gather * 6f;
    hs[i].yaw = -a;
}
""", seed=99)
