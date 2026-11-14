#!/usr/bin/env python3
"""Genera modelos .bbmodel (BetterModel), texturas, resource pack de items y catalog.yml.
Uso: python3 tools/gen_assets.py   (requiere Pillow)"""
import json, uuid, base64, io, os, math, random
from PIL import Image, ImageDraw
import sys; sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
import art
random.seed(7)
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'src/main/resources')
MODELS = os.path.join(RES, 'models'); os.makedirs(MODELS, exist_ok=True)
PACK = os.path.join(ROOT, 'resourcepack'); 
for d in ('assets/minecraft/models/item','assets/aetheria/models/item','assets/aetheria/textures/item'):
    os.makedirs(os.path.join(PACK, d), exist_ok=True)
U = lambda: str(uuid.uuid4())
def hx(h): return tuple(int(h[i:i+2],16) for i in (1,3,5))

class Model:
    """Modelo voxel simple: cada color es una celda 4x4 de una textura 16x16."""
    def __init__(self, name): self.name=name; self.colors=[]; self.elements=[]; self.groups={}; self.order=[]; self.anims=[]
    def ci(self, c, emit=0):
        k=(c,1 if emit else 0)
        if k not in self.colors:
            assert len(self.colors) < 16, self.name
            self.colors.append(k)
        return self.colors.index(k)
    def bone(self, name, origin, parent=None):
        self.groups[name]=dict(name=name, origin=list(origin), uuid=U(), children=[], parent=parent)
        self.order.append(name); return name
    def box(self, bone, x, y, z, w, h, d, color, emit=0):
        c=self.ci(color,emit); tx,ty=(c%4)*16,(c//4)*16
        dims={"north":(w,h),"south":(w,h),"east":(d,h),"west":(d,h),"up":(w,d),"down":(w,d)}
        faces={}
        for f,(a,b) in dims.items():
            cw,ch=min(16,max(1,round(a))),min(16,max(1,round(b)))
            faces[f]={"uv":[tx,ty,tx+cw,ty+ch],"texture":0}
        e=dict(name="cube",box_uv=False,rescale=False,locked=False,light_emission=emit,render_order="default",
               allow_mirror_modeling=True,**{"from":[x,y,z],"to":[x+w,y+h,z+d]},autouv=0,color=0,
               origin=self.groups[bone]['origin'],faces=faces,type="cube",uuid=U())
        self.elements.append(e); self.groups[bone]['children'].append(('e',e['uuid']))
    def anim(self, name, length, loop, tracks):
        """tracks: {bone: [(time, channel, (x,y,z)), ...]}"""
        an={}
        for b,kfs in tracks.items():
            an[self.groups[b]['uuid']]=dict(name=b,type="bone",keyframes=[dict(channel=ch,data_points=[dict(x=str(v[0]),y=str(v[1]),z=str(v[2]))],
                 uuid=U(),time=t,color=-1,interpolation="catmullrom" if loop=="loop" else "linear") for t,ch,v in kfs])
        self.anims.append(dict(uuid=U(),name=name,loop=loop,override=False,length=length,snapping=24,selected=False,
                 anim_time_update="",blend_weight="",start_delay="",loop_delay="",animators=an))
    def tex(self):
        im=Image.new('RGBA',(64,64),(0,0,0,255))
        for i,(c,e) in enumerate(self.colors): im.paste(art.paint_tile(c,e),((i%4)*16,(i//4)*16))
        return im
    def save(self):
        buf=io.BytesIO(); self.tex().save(buf,'PNG')
        def outl(name):
            g=self.groups[name]
            kids=[c[1] for c in g['children']]+[outl(n) for n in self.order if self.groups[n]['parent']==name]
            return dict(name=name,origin=g['origin'],color=0,uuid=g['uuid'],export=True,mirror_uv=False,isOpen=True,locked=False,
                        visibility=True,autouv=0,selected=False,children=kids)
        def outl_flat(name):
            return outl(name)
        roots=[outl(n) for n in self.order if self.groups[n]['parent'] is None]
        # hijos anidados: BetterModel acepta objetos anidados en 'children'
        def nest(name):
            g=self.groups[name]
            kids=[c[1] for c in g['children']]+[nest(n) for n in self.order if self.groups[n]['parent']==name]
            return dict(name=name,origin=g['origin'],color=0,uuid=g['uuid'],export=True,mirror_uv=False,isOpen=True,locked=False,
                        visibility=True,autouv=0,selected=False,children=kids)
        roots=[nest(n) for n in self.order if self.groups[n]['parent'] is None]
        data=dict(meta=dict(format_version="4.10",model_format="free",box_uv=False),name=self.name,model_identifier="",
            visible_box=[1,1,0],variable_placeholders="",variable_placeholder_buttons=[],timeline_setups=[],unhandled_root_fields={},
            resolution=dict(width=64,height=64),elements=self.elements,groups=[],outliner=roots,
            textures=[dict(path="",name=self.name+".png",folder="block",namespace="",id="0",group="",width=64,height=64,uv_width=64,uv_height=64,
              particle=False,use_as_default=False,layers_enabled=False,sync_to_project="",render_mode="default",render_sides="auto",
              pbr_channel="color",frame_time=1,frame_order_type="loop",frame_order="",frame_interpolate=False,visible=True,internal=True,
              saved=False,uuid=U(),source="data:image/png;base64,"+base64.b64encode(buf.getvalue()).decode())],
            animations=self.anims)
        json.dump(data,open(os.path.join(MODELS,self.name+'.bbmodel'),'w'))

def bob(m, bone, amp=1.0, length=2.0):
    m.anim("idle", length, "loop", {bone:[(0,"position",(0,0,0)),(length/2,"position",(0,amp,0)),(length,"position",(0,0,0))]})
def spin(m, bone, length=4.0):
    m.anim("idle", length, "loop", {bone:[(0,"rotation",(0,0,0)),(length/2,"rotation",(0,180,0)),(length,"rotation",(0,360,0))]})

# ------------------------------------------------------------------ CRATES (cofres)
CRATES = {
 'madera':    dict(nombre='Cofre de Madera', body='#6b4423', trim='#3a2410', metal='#8a8a8a', lock='#c9a227', gem='#e0b040', deco=None),
 'hierro':    dict(nombre='Cofre de Hierro', body='#5b6068', trim='#33373d', metal='#b9bec7', lock='#e6e6e6', gem='#7fd0ff', deco='studs'),
 'dorado':    dict(nombre='Cofre Dorado', body='#c8962e', trim='#7a5614', metal='#f2c14e', lock='#fff1b0', gem='#ff5a5a', deco='studs'),
 'magico':    dict(nombre='Cofre Mágico', body='#4b2a8a', trim='#25124a', metal='#9aa0b4', lock='#f2c14e', gem='#c77dff', deco='crystals'),
 'legendario':dict(nombre='Cofre Legendario', body='#2a1648', trim='#e0aa28', metal='#f2c14e', lock='#ffffff', gem='#ff9bf5', deco='crown'),
 'oscuro':    dict(nombre='Cofre Oscuro', body='#1a1622', trim='#0b0910', metal='#4d4560', lock='#7a3cff', gem='#a020f0', deco='horns'),
 'evento':    dict(nombre='Cofre de Evento', body='#9b1c31', trim='#1f6b3a', metal='#f2c14e', lock='#ffffff', gem='#ffffff', deco='ribbon'),
}
def build_crate(key, s):
    m=Model('crate_'+key)
    m.bone('body',(0,0,0)); m.bone('lid',(0,10,6)); m.bone('gem',(0,10,-7)); m.bone('fx',(0,10,0))
    b,t,mt=s['body'],s['trim'],s['metal']
    m.box('body',-8,0,-6,16,10,12,b)
    for sx in (-8,6): 
        for sz in (-6,4): m.box('body',sx,0,sz,2,10,2,t)          # postes
    m.box('body',-8,0,-6,16,1.5,12,mt); m.box('body',-8,4.5,-6.2,16,1.2,12.4,mt)   # bandas
    m.box('lid',-8,10,-6,16,4,12,b); m.box('lid',-8,14,-5,16,1.5,10,b)              # tapa
    for sx in (-8,6): m.box('lid',sx,10,-6,2,5.5,12,t)
    m.box('lid',-8,10,-6.2,16,1.2,12.4,mt)
    m.box('lid',-2.5,7.5,-6.6,5,5,0.8,s['lock'])                       # cerradura
    m.box('lid',-0.7,8.8,-6.9,1.4,2.2,0.5,t)
    m.box('gem',-1.5,10.5,-7.4,3,3,1.2,s['gem'],emit=15)
    d=s['deco']
    if d=='studs':
        for x in (-6,-2,2,6): m.box('body',x-0.5,5,-6.4,1.4,1.4,0.8,mt)
    if d=='crystals':
        for i,x in enumerate((-6,-2,2,6)): m.box('fx',x-1,15.5,-1+(i%2)*2,2,3+i%3*2,2,s['gem'],emit=12)
    if d=='crown':
        for x in (-7,-3.5,0,3.5,7-1): m.box('fx',x-0.5,15.5,-0.5,1.5,3+abs(x)%3,1.5,s['metal'],emit=6)
        m.box('fx',-1,15.5,-4,2,2,2,s['gem'],emit=15)
    if d=='horns':
        for sx in (-8,6): 
            m.box('fx',sx,14,-1,2,3,2,'#e8e0f0'); m.box('fx',sx+(0.5 if sx<0 else -0.5),17,-1,1.4,3,1.4,'#e8e0f0')
    if d=='ribbon':
        m.box('lid',-1.5,10,-6.3,3,5.5,12.6,s['trim']); m.box('body',-1.5,0,-6.3,3,10,12.6,s['trim'])
        m.box('fx',-3,15.5,-1,6,1.2,2,s['trim']); m.box('fx',-1.2,15.5,-1.2,2.4,2.4,2.4,s['metal'])
    L=1.0
    m.anim("idle",2,"loop",{'gem':[(0,"position",(0,0,0)),(1,"position",(0,L,0)),(2,"position",(0,0,0))],
                            'fx':[(0,"position",(0,0,0)),(1,"position",(0,0.5,0)),(2,"position",(0,0,0))]})
    m.anim("open",1,"once",{'lid':[(0,"rotation",(0,0,0)),(0.5,"rotation",(-70,0,0)),(1,"rotation",(-105,0,0))],
                            'gem':[(0,"position",(0,0,0)),(1,"position",(0,3,0))]})
    m.anim("reveal",2,"loop",{'lid':[(0,"rotation",(-105,0,0)),(2,"rotation",(-105,0,0))],
                            'gem':[(0,"position",(0,3,0)),(1,"position",(0,9,0)),(2,"position",(0,3,0))],
                            'fx':[(0,"rotation",(0,0,0)),(1,"rotation",(0,180,0)),(2,"rotation",(0,360,0))]})
    m.anim("close",1,"once",{'lid':[(0,"rotation",(-105,0,0)),(1,"rotation",(0,0,0))],'gem':[(0,"position",(0,3,0)),(1,"position",(0,0,0))]})
    m.save()
for k,s in CRATES.items(): build_crate(k,s)

# ------------------------------------------------------------------ LLAVAS (items 3D del resource pack)
KEYS = {
 'madera':('#6b4423','#c9a227'),'hierro':('#9aa0a8','#e6e6e6'),'dorado':('#f2c14e','#ff5a5a'),
 'magico':('#9b5cff','#f2c14e'),'legendario':('#f2c14e','#ff9bf5'),'oscuro':('#4d4560','#a020f0'),'evento':('#d13b52','#2fae5e')}
def key_model(name, c1, c2):
    im=Image.new('RGBA',(16,16)); d=ImageDraw.Draw(im)
    d.rectangle([0,0,7,15],fill=hx(c1)); d.rectangle([8,0,15,15],fill=hx(c2))
    im.save(os.path.join(PACK,'assets/aetheria/textures/item',name+'.png'))
    el=lambda f,t,u: {"from":f,"to":t,"faces":{k:{"uv":u,"texture":"#0"} for k in ("north","east","south","west","up","down")}}
    A=[0,0,7,16]; B=[8,0,15,16]
    mod={"textures":{"0":"aetheria:item/"+name,"particle":"aetheria:item/"+name},
     "elements":[el([1,7,7.5],[5,11,8.5],B)]+[el([5,8,7.5],[15,10,8.5],A)]+[el([12,5.5,7.5],[14,8,8.5],A),el([14.5,5.5,7.5],[15.8,9,8.5],A)]
       +[el([2,8,7.5],[4,10,8.5],A),el([2,6,7.5],[4,7,8.5],B)],
     "display":{"gui":{"rotation":[0,0,-45],"scale":[1.1,1.1,1.1],"translation":[0,0,0]},"ground":{"scale":[0.5,0.5,0.5]},
       "fixed":{"rotation":[0,180,-45]},"thirdperson_righthand":{"rotation":[0,0,-45],"scale":[0.7,0.7,0.7]},
       "firstperson_righthand":{"rotation":[0,-90,-45],"scale":[0.8,0.8,0.8]}}}
    json.dump(mod,open(os.path.join(PACK,'assets/aetheria/models/item',name+'.json'),'w'),indent=1)
overrides={'amethyst_shard':[], 'paper':[]}
KEY_CMD={}
for i,(k,(a,b)) in enumerate(KEYS.items()):
    key_model('key_'+k,a,b); cmd=3001+i; KEY_CMD[k]=cmd
    overrides['amethyst_shard'].append({"predicate":{"custom_model_data":cmd},"model":"aetheria:item/key_"+k})

# ------------------------------------------------------------------ HELPERS de modelos de cosmeticos
def ring_of(m, bone, n, r, y, size, color, emit=0):
    for i in range(n):
        a=2*math.pi*i/n; m.box(bone, r*math.cos(a)-size/2, y, r*math.sin(a)-size/2, size, size, size, color, emit)

def hat_crown(name,c1,c2,gem,spikes=5,tall=3):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    for (x,z,w,d) in ((-4,-4,8,1),(-4,3,8,1),(-4,-3,1,6),(3,-3,1,6)): m.box('hat',x,0,z,w,1.6,d,c1)
    m.box('hat',-4,1.6,-4,8,0.6,8,c2) if False else None
    for i in range(spikes):
        a=2*math.pi*i/spikes; x,z=3.2*math.cos(a),3.2*math.sin(a)
        m.box('hat',x-0.6,1.6,z-0.6,1.2,tall+(i%2)*1.5,1.2,c1); m.box('gem',x-0.4,1.6+tall+(i%2)*1.5,z-0.4,0.8,0.8,0.8,gem,emit=15)
    m.box('gem',-0.8,0.3,-4.4,1.6,1.6,0.6,gem,emit=15)
    bob(m,'gem',0.4); m.save()
def hat_mask(name,c1,c2,eye,horns=False):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    m.box('hat',-4,-4,-4.6,8,8,1,c1); m.box('hat',-4,2.5,-4.7,8,1.5,1.1,c2)
    m.box('gem',-3,-0.5,-4.9,2,1,0.5,eye,emit=15); m.box('gem',1,-0.5,-4.9,2,1,0.5,eye,emit=15)
    m.box('hat',-0.7,-2,-5.1,1.4,2,1,c2)
    if horns:
        for s in (-1,1): m.box('hat',s*3.5-0.7,4,-4.4,1.4,3,1.4,c2); m.box('hat',s*4.3-0.5,6.5,-4.4,1,2.5,1,c2)
    bob(m,'gem',0.2); m.save()
def hat_helm(name,c1,c2,plume,wings=False):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    m.box('hat',-4.6,-4,-4.6,9.2,5,9.2,c1); m.box('hat',-4.2,1,-4.2,8.4,2.5,8.4,c1); m.box('hat',-3.4,3.5,-3.4,6.8,1,6.8,c2)
    m.box('hat',-4.8,-4,-4.8,9.6,1,9.6,c2); m.box('hat',-0.8,-1.5,-4.9,1.6,5,0.6,c2)
    m.box('gem',-0.8,4.5,-3,1.6,3,6,plume,emit=4)
    if wings:
        for s in (-1,1):
            m.box('hat',s*5.2-0.5,0,-1,1,3,3,c2); m.box('hat',s*6-0.5,1.5,-0.5,1,3,2,c2); m.box('hat',s*6.8-0.5,3,0,1,2.5,1.5,c2)
    bob(m,'gem',0.3); m.save()
def hat_wizard(name,c1,c2,star):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    m.box('hat',-7,0,-7,14,0.8,14,c1)
    sizes=[(9,2),(7.4,2),(6,2),(4.6,2),(3.4,2),(2.2,2),(1.2,1.5)]
    y=0.8
    for i,(w,h) in enumerate(sizes):
        m.box('hat',-w/2+i*0.25,y,-w/2,w,h,w,c1); y+=h
    m.box('hat',-4.8,0.8,-4.8,9.6,1,9.6,c2)
    m.box('gem',-0.7,y,-0.7,1.4,1.4,1.4,star,emit=15)
    bob(m,'gem',0.6); m.save()
def hat_horns(name,c1,c2):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    m.box('hat',-4.3,0,-2,8.6,1,4,c2)
    for s in (-1,1):
        m.box('hat',s*3.6-0.9,1,-1,1.8,3,1.8,c1); m.box('hat',s*4.4-0.8,4,-1,1.6,3,1.6,c1)
        m.box('hat',s*5.0-0.7,7,-1,1.4,2.5,1.4,c1); m.box('gem',s*5.4-0.5,9.5,-0.9,1,1,1,c2,emit=8)
    bob(m,'gem',0.2); m.save()
def hat_float(name,kind,c1,c2,glow):
    m=Model(name); m.bone('hat',(0,0,0)); m.bone('gem',(0,0,0))
    if kind=='orb':
        m.box('hat',-2,0,-2,4,4,4,c1); m.box('gem',-1,1,-1,2,2,2,glow,emit=15); m.box('hat',-3,-1,-0.5,6,1,1,c2)
    elif kind=='tome':
        m.box('hat',-3,0,-2.2,6,0.8,4.4,c2); m.box('hat',-3,0.8,-2,6,2,4,'#efe6c8'); m.box('hat',-3,2.8,-2.2,6,0.8,4.4,c1); m.box('gem',-1,3.7,-0.5,2,0.6,1,glow,emit=15)
    elif kind=='halo':
        ring_of(m,'hat',16,4.2,0,1.2,c1,emit=10); m.box('gem',-0.5,0,-4.8,1,1,1,glow,emit=15)
    elif kind=='lantern':
        m.box('hat',-1.5,0,-1.5,3,0.6,3,c2); m.box('gem',-1,0.6,-1,2,3,2,glow,emit=15); m.box('hat',-1.5,3.6,-1.5,3,0.6,3,c2); m.box('hat',-0.3,4.2,-0.3,0.6,2,0.6,c1)
    spin(m,'gem',6) if kind in('orb','halo') else bob(m,'gem',0.8); m.save()

# ---- alas / capas
def wings(name,c1,c2,c3,style):
    m=Model(name); m.bone('spine',(0,0,0)); m.bone('wl',(1.5,0,0)); m.bone('wr',(-1.5,0,0))
    m.box('spine',-1,-6,0,2,10,1,c2)
    for bn,sg in (('wl',1),('wr',-1)):
        def X(x,w): return (x if sg>0 else -x-w)
        if style=='angel':
            for i in range(5):
                L=12-i*1.6; m.box(bn,X(1.5+i*1.5,1.4),-1.5-i*1.4,0.6+i*0.1,1.4,L,0.8,c1 if i%2==0 else c3)
            m.box(bn,X(1,2),0,0.6,2,6,0.9,c2)
        elif style=='demon':
            m.box(bn,X(1,1.2),-1,0.4,1.2,12,0.8,c2)                      # hueso
            for i in range(4):
                m.box(bn,X(2.2+i*2.4,2.2),-5+i*1.1,0.5,2.2,12-i*2.5,0.5,c1 if i%2==0 else c3)
            m.box(bn,X(1,1.2),10,0.4,1.2,1.2,0.8,c3)
        elif style=='dragon':
            m.box(bn,X(1,1.6),-2,0.4,1.6,13,1,c2); m.box(bn,X(2.5,9),9,0.4,9,1.2,1,c2)
            for i in range(3): m.box(bn,X(3+i*3,2.8),-2+i*0.5,0.5,2.8,11-i*1.5,0.5,c1)
            m.box(bn,X(11,1.2),9.5,0.4,1.2,2.5,1,c3)
        elif style=='fairy':
            for i,(w,h) in enumerate(((7,10),(5,7),(4,4))):
                m.box(bn,X(1+i*0.8,w),2-i*3.2,0.5+i*0.05,w,h,0.4,c1 if i!=1 else c3, emit=6)
    L=2.0
    for b,sg in (('wl',-1),('wr',1)):
        m.anim_tracks = getattr(m,'anim_tracks',{}); m.anim_tracks[b]=[(0,"rotation",(0,0,0)),(1,"rotation",(0,sg*22,0)),(2,"rotation",(0,0,0))]
    m.anim("idle",2,"loop",m.anim_tracks); m.save()
def cape(name,c1,c2,c3,trim_glow=False):
    m=Model(name); m.bone('cape',(0,0,0)); m.bone('tail',(0,-2,0))
    m.box('cape',-5,-2,0,10,1.5,2.2,c2); 
    for s in (-1,1): m.box('cape',s*4.2-1.2,-2,-0.2,2.4,2,2.8,c3)
    m.box('tail',-4.6,-14,0.6,9.2,12,0.9,c1); m.box('tail',-4.6,-14,0.4,9.2,1,1.3,c2); m.box('tail',-4.6,-14,1.5,9.2,12,0.2,c3,emit=8 if trim_glow else 0)
    m.box('tail',-1,-9,1.6,2,2,0.4,c3,emit=10 if trim_glow else 0)
    m.anim("idle",3,"loop",{'tail':[(0,"rotation",(2,0,0)),(1.5,"rotation",(10,0,0)),(3,"rotation",(2,0,0))]}); m.save()

# ---- mascotas
def pet_dragon(name,c1,c2,eye,fire):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('wl',(1.5,5,0)); m.bone('wr',(-1.5,5,0))
    m.box('body',-1.5,2,-2.5,3,3,5,c1); m.box('body',-1.2,3.2,-4.5,2.4,2.4,2.4,c1); m.box('body',-0.8,3.2,-5.4,1.6,1.2,1,c2)
    m.box('body',-1.2,5,-4,0.6,1,0.6,c2); m.box('body',0.6,5,-4,0.6,1,0.6,c2)
    m.box('body',-1,3.9,-5.1,0.5,0.5,0.4,eye,emit=15); m.box('body',0.5,3.9,-5.1,0.5,0.5,0.4,eye,emit=15)
    m.box('body',-0.8,2.5,2.5,1.6,1.6,3,c1); m.box('body',-0.5,2.6,5.5,1,1,2.5,c1); m.box('body',-0.3,2.8,8,0.6,0.6,1,fire,emit=12)
    for x in (-1.4,0.6): m.box('body',x,0,-1.5,0.8,2,0.8,c2); m.box('body',x,0,1.2,0.8,2,0.8,c2)
    for i in range(3): m.box('body',-0.3,5,-1.5+i*1.5,0.6,0.8,0.6,fire,emit=6)
    for bn,sg in (('wl',1),('wr',-1)):
        X=lambda x,w:(x if sg>0 else -x-w)
        m.box(bn,X(0,0.5),0,0,0.5,5,0.5,c2); m.box(bn,X(0.5,4),3.8,-0.2,4,0.4,2.4,c2); m.box(bn,X(0.5,3.4),0.6,0,3.4,3.4,0.2,c1)
    m.anim("idle",1,"loop",{'wl':[(0,"rotation",(0,0,0)),(0.5,"rotation",(0,-40,0)),(1,"rotation",(0,0,0))],
                            'wr':[(0,"rotation",(0,0,0)),(0.5,"rotation",(0,40,0)),(1,"rotation",(0,0,0))],
                            'body':[(0,"position",(0,0,0)),(0.5,"position",(0,0.8,0)),(1,"position",(0,0,0))]}); m.save()
def pet_fox(name,c1,c2,glow,tailtip):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('tail',(0,3,3))
    m.box('body',-1.5,1.5,-2,3,3,5,c1); m.box('body',-1.4,2.5,-4.2,2.8,2.6,2.6,c1); m.box('body',-0.7,2.4,-5.2,1.4,1,1,c2)
    m.box('body',-1.4,5,-3.6,0.9,1.4,0.6,c1); m.box('body',0.5,5,-3.6,0.9,1.4,0.6,c1)
    m.box('body',-1,3.5,-4.5,0.5,0.5,0.4,glow,emit=15); m.box('body',0.5,3.5,-4.5,0.5,0.5,0.4,glow,emit=15)
    for x in (-1.3,0.5):
        m.box('body',x,0,-1.5,0.8,1.5,0.8,c2); m.box('body',x,0,1.6,0.8,1.5,0.8,c2)
    m.box('tail',-1,2.5,3,2,2,3.5,c1); m.box('tail',-0.9,2.6,6.5,1.8,1.8,2,tailtip,emit=10)
    m.anim("idle",1.5,"loop",{'tail':[(0,"rotation",(0,-20,0)),(0.75,"rotation",(0,20,0)),(1.5,"rotation",(0,-20,0))],
                              'body':[(0,"position",(0,0,0)),(0.75,"position",(0,0.5,0)),(1.5,"position",(0,0,0))]}); m.save()
def pet_raven(name,c1,c2,eye):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('wl',(1,3.5,0)); m.bone('wr',(-1,3.5,0))
    m.box('body',-1.2,2,-1.5,2.4,2.4,4,c1); m.box('body',-1,3.5,-3,2,2,2,c1); m.box('body',-0.4,3.6,-4.2,0.8,0.8,1.4,c2)
    m.box('body',-0.8,4.4,-3.4,0.4,0.4,0.3,eye,emit=15); m.box('body',0.4,4.4,-3.4,0.4,0.4,0.3,eye,emit=15)
    m.box('body',-0.8,2,2.5,1.6,0.6,3,c1); m.box('body',-0.8,0.8,-0.5,0.4,1.2,0.4,c2); m.box('body',0.4,0.8,-0.5,0.4,1.2,0.4,c2)
    for bn,sg in (('wl',1),('wr',-1)):
        X=lambda x,w:(x if sg>0 else -x-w)
        m.box(bn,X(0,6),3,-1,6,0.4,3,c1); m.box(bn,X(0,3),3,2,3,0.4,2,c2)
    m.anim("idle",0.8,"loop",{'wl':[(0,"rotation",(0,0,30)),(0.4,"rotation",(0,0,-30)),(0.8,"rotation",(0,0,30))],
                              'wr':[(0,"rotation",(0,0,-30)),(0.4,"rotation",(0,0,30)),(0.8,"rotation",(0,0,-30))],
                              'body':[(0,"position",(0,0,0)),(0.4,"position",(0,1,0)),(0.8,"position",(0,0,0))]}); m.save()
def pet_golem(name,c1,c2,glow):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('core',(0,4,0))
    m.box('body',-2,2,-1.5,4,4,3,c1); m.box('body',-1.5,6,-1.5,3,3,3,c1); m.box('body',-1.2,7,-1.7,0.8,0.6,0.4,glow,emit=15); m.box('body',0.4,7,-1.7,0.8,0.6,0.4,glow,emit=15)
    for s in (-1,1): m.box('body',s*2.6-(0.5 if s<0 else 0)-0.5+0.5,2.5,-1,1.2,3.4,1.6,c2)
    for x in (-1.5,0.5): m.box('body',x,0,-0.8,1,2,1.6,c2)
    m.box('core',-0.7,3.5,-1.7,1.4,1.4,0.5,glow,emit=15)
    bob(m,'core',0.3); m.save()
def pet_spirit(name,c1,c2,glow):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('tail',(0,2,0))
    m.box('body',-2,3,-2,4,4,4,c1,emit=8); m.box('body',-1.4,3.8,-2.2,0.9,0.9,0.4,glow,emit=15); m.box('body',0.5,3.8,-2.2,0.9,0.9,0.4,glow,emit=15)
    m.box('tail',-1.5,1.5,-1.5,3,2,3,c2,emit=6); m.box('tail',-1,0,-1,2,1.6,2,c1,emit=4); m.box('tail',-0.5,-1.2,-0.5,1,1.4,1,c2,emit=4)
    m.anim("idle",2,"loop",{'body':[(0,"position",(0,0,0)),(1,"position",(0,1.4,0)),(2,"position",(0,0,0))],
                            'tail':[(0,"rotation",(0,0,-8)),(1,"rotation",(0,0,8)),(2,"rotation",(0,0,-8))]}); m.save()
def pet_creature(name,kind,c1,c2,glow):
    m=Model(name); m.bone('body',(0,0,0)); m.bone('wl',(1,4,0)); m.bone('wr',(-1,4,0))
    if kind=='slime':
        m.box('body',-2.5,0,-2.5,5,4,5,c1,emit=4); m.box('body',-1.2,1.8,-2.7,0.9,0.9,0.4,glow,emit=15); m.box('body',0.4,1.8,-2.7,0.9,0.9,0.4,glow,emit=15); m.box('body',-0.5,4,-0.5,1,1.5,1,c2)
    elif kind=='phoenix':
        m.box('body',-1.5,2,-2,3,3,4,c1,emit=6); m.box('body',-1,4.5,-3,2,2,2,c1,emit=6); m.box('body',-0.4,4.8,-4.2,0.8,0.7,1.4,c2); m.box('body',-0.5,6.5,-2.5,1,1.5,1,glow,emit=15)
        m.box('body',-0.8,2,2,1.6,1,4,glow,emit=12)
        for bn,sg in (('wl',1),('wr',-1)):
            X=lambda x,w:(x if sg>0 else -x-w); m.box(bn,X(0,5),4,-1.5,5,0.4,3.5,c1,emit=6); m.box(bn,X(0,3),4,2,3,0.4,2,glow,emit=10)
    elif kind=='wisp':
        m.box('body',-1.5,2,-1.5,3,3,3,c2,emit=15); m.box('body',-2.4,2.9,-0.4,0.8,0.8,0.8,c1,emit=10); m.box('body',1.6,2.9,-0.4,0.8,0.8,0.8,c1,emit=10); m.box('body',-0.4,5.2,-0.4,0.8,0.8,0.8,c1,emit=10)
    elif kind=='owl':
        m.box('body',-2,1,-1.5,4,4.5,3,c1); m.box('body',-1.5,5.5,-1.5,3,2.5,3,c1); m.box('body',-1.3,6.3,-1.8,1.1,1.1,0.4,glow,emit=15); m.box('body',0.2,6.3,-1.8,1.1,1.1,0.4,glow,emit=15); m.box('body',-0.4,5.8,-2,0.8,0.8,0.5,c2)
        for bn,sg in (('wl',1),('wr',-1)):
            X=lambda x,w:(x if sg>0 else -x-w); m.box(bn,X(0,0.6),0,0,0.6,4,2.2,c2)
    m.anim("idle",1.2,"loop",{'wl':[(0,"rotation",(0,0,0)),(0.6,"rotation",(0,-30,0)),(1.2,"rotation",(0,0,0))],
                              'wr':[(0,"rotation",(0,0,0)),(0.6,"rotation",(0,30,0)),(1.2,"rotation",(0,0,0))],
                              'body':[(0,"position",(0,0,0)),(0.6,"position",(0,1,0)),(1.2,"position",(0,0,0))]}); m.save()
def ring_model(name,c1,c2,n=12,rad=7,pillars=False):
    m=Model(name); m.bone('ring',(0,0,0)); m.bone('gems',(0,0,0))
    ring_of(m,'ring',n,rad,0,1.1,c1,emit=4)
    for i in range(4):
        a=math.pi/2*i; m.box('gems',rad*math.cos(a)-0.8,0.6,rad*math.sin(a)-0.8,1.6,1.6,1.6,c2,emit=15)
    if pillars:
        for i in range(4):
            a=math.pi/2*i+math.pi/4; m.box('gems',rad*math.cos(a)-0.4,1,rad*math.sin(a)-0.4,0.8,3,0.8,c1,emit=8)
    spin(m,'gems',8); m.anim("idle",8,"loop",{'ring':[(0,"rotation",(0,360,0)),(4,"rotation",(0,180,0)),(8,"rotation",(0,0,0))],
        'gems':[(0,"rotation",(0,0,0)),(4,"rotation",(0,180,0)),(8,"rotation",(0,360,0))]}); m.save()
def orbit_model(name,kind,c1,c2,glow):
    m=Model(name); m.bone('orbit',(0,0,0))
    for i in range(3):
        a=2*math.pi*i/3; x,z=6*math.cos(a),6*math.sin(a)
        if kind=='swords': m.box('orbit',x-0.4,0,z-0.4,0.8,5,0.8,c1,emit=4); m.box('orbit',x-1.2,0,z-0.4,2.4,0.8,0.8,c2)
        elif kind=='runes': m.box('orbit',x-1.2,0,z-0.4,2.4,3,0.8,c1); m.box('orbit',x-0.5,0.8,z-0.5,1,1,1,glow,emit=15)
        elif kind=='butterflies': m.box('orbit',x-1.5,0,z-0.3,3,2,0.6,c1,emit=8); m.box('orbit',x-0.3,0,z-0.3,0.6,2,0.6,c2)
        elif kind=='skulls': m.box('orbit',x-1,0,z-1,2,2,2,c1); m.box('orbit',x-0.7,0.6,z-1.1,0.5,0.5,0.3,glow,emit=15); m.box('orbit',x+0.2,0.6,z-1.1,0.5,0.5,0.3,glow,emit=15)
    m.anim("idle",5,"loop",{'orbit':[(0,"rotation",(0,0,0)),(2.5,"rotation",(0,180,0)),(5,"rotation",(0,360,0))]}); m.save()

# =================================================================== CATALOGO (100 cosmeticos)
CAT=[]   # lista de dicts
def add(id,nombre,cat,slot,rar,tier,desc,model=None,color='#b070ff',particles=None,offset=(0,0,0),scale=1.0,event=None,yawmode='head'):
    CAT.append(dict(id=id,nombre=nombre,categoria=cat,slot=slot,rareza=rar,tier=tier,desc=desc,model=model,color=color,
                    particulas=particles or [],offset=list(offset),yaw=0,scale=scale,evento=event,sigue=yawmode))
def P(t,shape,count=2,radius=0.7,height=1.8,speed=0.0,color=None,color2=None,size=1.0):
    d=dict(tipo=t,forma=shape,cantidad=count,radio=radius,altura=height,velocidad=speed,tamano=size)
    if color: d['color']=color
    if color2: d['color2']=color2
    return d
F,PR='FREE','PREMIUM'
# ---------------- AURAS (18)
A='AURAS'
add('aura_llama_viva','Llama Viva',A,'AURA','COMMON',F,'Una llama discreta danza a tu alrededor.',color='#ff7a1a',particles=[P('FLAME','SPIRAL',2,0.8,1.8)])
add('aura_infierno','Infierno Carmesí',A,'AURA','LEGENDARY',PR,'Brasas y lava en un anillo abrasador.',color='#d62828',particles=[P('FLAME','RING',6,0.9,0.2),P('LAVA','ORBIT',1,1.0,1.0)])
add('aura_escarcha','Escarcha Pálida',A,'AURA','COMMON',F,'Copos que giran con el viento del norte.',color='#bfe9ff',particles=[P('SNOWFLAKE','SPIRAL',2,0.8,1.9)])
add('aura_ventisca','Ventisca Eterna',A,'AURA','EPIC',PR,'Una ventisca ligada a tu espíritu.',color='#9fd8ff',particles=[P('SNOWFLAKE','ORBIT',3,1.0,1.0),P('CLOUD','RING',1,0.8,0.1)])
add('aura_sombra','Sombra Errante',A,'AURA','RARE',PR,'Humo oscuro que respira contigo.',color='#2b2233',particles=[P('LARGE_SMOKE','SPIRAL',1,0.7,1.7)])
add('aura_vacio','Vacío Abisal',A,'AURA','LEGENDARY',PR,'El abismo te observa de vuelta.',color='#5a1fa8',particles=[P('PORTAL','RING',8,1.0,1.0),P('SQUID_INK','ORBIT',1,0.9,1.0)])
add('aura_pulso','Pulso Arcano',A,'AURA','RARE',PR,'Energía violeta orbitando en pulsos.',color='#8a3cff',particles=[P('DUST','ORBIT',3,0.9,1.0,color='#8a3cff',size=1.2)])
add('aura_nucleo','Núcleo Estelar',A,'AURA','EPIC',PR,'Fragmentos de luz estelar.',color='#ffffff',particles=[P('END_ROD','ORBIT',2,1.0,1.1)])
add('aura_runas','Runas Danzantes',A,'AURA','EPIC',PR,'Glifos antiguos flotan a tu alrededor.',color='#c9a3ff',particles=[P('ENCHANT','SPIRAL',6,1.0,2.0)])
add('aura_hadas','Polvo de Hadas',A,'AURA','RARE',PR,'Brillos traviesos y verdes.',color='#7dff9b',particles=[P('WITCH','SPIRAL',2,0.9,1.8),P('HAPPY_VILLAGER','RAIN',1,0.9,2.2)])
add('aura_velo','Velo Violeta',A,'AURA','EPIC',PR,'Un velo de magia que cambia de tono.',color='#d27bff',particles=[P('DUST_COLOR_TRANSITION','SPIRAL',3,0.8,1.9,color='#7a3cff',color2='#ff7ad9',size=1.3)])
add('aura_halo','Halo Celestial',A,'AURA','LEGENDARY',PR,'Un halo de luz sobre tu cabeza.',color='#fff3b0',particles=[P('END_ROD','HALO',8,0.45,2.3)])
add('aura_estrellas','Lluvia de Estrellas',A,'AURA','LEGENDARY',PR,'Estrellas fugaces caen sobre ti.',color='#ffe28a',particles=[P('FIREWORK','RAIN',2,0.9,2.8)])
add('aura_chispa','Chispa Eléctrica',A,'AURA','RARE',PR,'Descargas azules chisporrotean.',color='#6ad8ff',particles=[P('ELECTRIC_SPARK','BURST',3,0.8,1.0)])
add('aura_tormenta','Tormenta Viva',A,'AURA','EPIC',PR,'Nubes y relámpagos te siguen.',color='#8fb8ff',particles=[P('ELECTRIC_SPARK','ORBIT',3,1.0,1.4),P('CLOUD','HALO',1,0.5,2.4)])
add('aura_bosque','Brisa del Bosque',A,'AURA','COMMON',F,'Hojas mecidas por la brisa.',color='#6ecb63',particles=[P('HAPPY_VILLAGER','SPIRAL',1,0.9,1.8),P('CHERRY_LEAVES','RAIN',1,0.8,2.4)])
add('aura_corazones','Aura de Corazones',A,'AURA','EPIC',PR,'Corazones que nacen del cariño.',color='#ff6fa5',particles=[P('HEART','SPIRAL',1,0.8,1.9)],event='febrero')
add('aura_nevada','Nevada Navideña',A,'AURA','EPIC',PR,'Nieve y lucecitas de Navidad.',color='#e8f6ff',particles=[P('SNOWFLAKE','RAIN',3,1.0,2.8),P('END_ROD','ORBIT',1,1.0,1.0)],event='adviento')
# ---------------- SOMBREROS (20)
H='SOMBREROS'
def hat(id,n,rar,tier,desc,model,color,off=(0,0,0),scale=1.0,event=None,part=None):
    add(id,n,H,'HAT',rar,tier,desc,model=model,color=color,offset=off,scale=scale,event=event,particles=part or [])
hat_crown('hat_corona_oro','#f2c14e','#c8962e','#ff4a4a',5,3); hat('hat_corona_oro','Corona de Oro','RARE',PR,'Un clásico de la nobleza.','hat_corona_oro','#f2c14e',(0,0.6,0))
hat_crown('hat_corona_amatista','#8a5cff','#4b2a8a','#ffffff',6,3); hat('hat_corona_amatista','Corona de Amatista','EPIC',PR,'Cristales de amatista viva.','hat_corona_amatista','#8a5cff',(0,0.6,0))
hat_crown('hat_corona_aetheria','#f2c14e','#2a1648','#c77dff',7,4); hat('hat_corona_aetheria','Corona Real de Aetheria','MYTHIC',PR,'La corona del reino, forjada con runas.','hat_corona_aetheria','#f2c14e',(0,0.6,0),part=[P('END_ROD','HALO',3,0.4,2.5)])
hat_crown('hat_corona_amor','#ff9bd0','#d6336c','#ffffff',5,3); hat('hat_corona_amor','Corona del Amor','LEGENDARY',PR,'Edición de febrero, forjada con cariño.','hat_corona_amor','#ff9bd0',(0,0.6,0),event='febrero',part=[P('HEART','HALO',1,0.4,2.5)])
hat_crown('hat_corona_hielo','#bfe9ff','#5aa9e6','#ffffff',8,4); hat('hat_corona_hielo','Corona de Escarcha','LEGENDARY',PR,'Edición de Navidad: hielo eterno.','hat_corona_hielo','#bfe9ff',(0,0.6,0),event='adviento',part=[P('SNOWFLAKE','HALO',1,0.4,2.5)])
hat_mask('hat_mascara_sombra','#2b2233','#7a3cff','#a020f0'); hat('hat_mascara_sombra','Máscara de Sombra','RARE',PR,'Rostro sin nombre.','hat_mascara_sombra','#7a3cff',(0,0.2,0))
hat_mask('hat_mascara_oro','#f2c14e','#7a5614','#ffffff'); hat('hat_mascara_oro','Máscara Dorada','EPIC',PR,'Mascarada de la corte.','hat_mascara_oro','#f2c14e',(0,0.2,0))
hat_mask('hat_mascara_demonio','#7a1020','#1a0a10','#ffcc00',True); hat('hat_mascara_demonio','Máscara de Demonio','LEGENDARY',PR,'Una sonrisa que hiela la sangre.','hat_mascara_demonio','#d62828',(0,0.2,0))
hat_helm('hat_casco_hierro','#9aa0a8','#5b6068','#8b1a1a'); hat('hat_casco_hierro','Casco de Hierro','COMMON',F,'Sencillo y fiable.','hat_casco_hierro','#9aa0a8',(0,0.4,0))
hat_helm('hat_casco_dorado','#f2c14e','#c8962e','#ffffff',True); hat('hat_casco_dorado','Casco Alado Dorado','EPIC',PR,'Alas de oro a los lados.','hat_casco_dorado','#f2c14e',(0,0.4,0))
hat_helm('hat_casco_oscuro','#2b2233','#4d4560','#a020f0',False); hat('hat_casco_oscuro','Casco del Caballero Oscuro','LEGENDARY',PR,'Un yelmo sin piedad.','hat_casco_oscuro','#4d4560',(0,0.4,0))
hat_helm('hat_casco_runico','#8a5cff','#25124a','#f2c14e',True); hat('hat_casco_runico','Casco Rúnico','LEGENDARY',PR,'Runas brillan en el acero.','hat_casco_runico','#8a5cff',(0,0.4,0))
hat_wizard('hat_mago_aprendiz','#4b2a8a','#f2c14e','#ffffff'); hat('hat_mago_aprendiz','Gorro de Mago Aprendiz','COMMON',F,'Para quien empieza en las artes arcanas.','hat_mago_aprendiz','#4b2a8a',(0,0.4,0))
hat_wizard('hat_mago_archimago','#1a0f3a','#c77dff','#ffe28a'); hat('hat_mago_archimago','Sombrero de Archimago','LEGENDARY',PR,'Sabiduría en cada pliegue.','hat_mago_archimago','#7a3cff',(0,0.4,0))
hat_wizard('hat_mago_fuego','#9b1c1c','#f2c14e','#ff7a1a'); hat('hat_mago_fuego','Sombrero del Piromante','EPIC',PR,'Huele levemente a chamusquina.','hat_mago_fuego','#d62828',(0,0.4,0),part=[P('SMALL_FLAME','COLUMN',1,0.1,0.5)])
hat_wizard('hat_mago_invierno','#d62839','#ffffff','#ffffff'); hat('hat_mago_invierno','Gorro de Invierno','RARE',PR,'Edición de Navidad.','hat_mago_invierno','#d62839',(0,0.4,0),event='adviento')
hat_horns('hat_cuernos_cabra','#8a6f4d','#5a4630'); hat('hat_cuernos_cabra','Cuernos de Cabra Montesa','COMMON',F,'Fuertes y retorcidos.','hat_cuernos_cabra','#8a6f4d',(0,0.8,0))
hat_horns('hat_cuernos_demonio','#2b0f14','#d62828'); hat('hat_cuernos_demonio','Cuernos Infernales','EPIC',PR,'Cuernos que arden por dentro.','hat_cuernos_demonio','#d62828',(0,0.8,0),part=[P('SMALL_FLAME','COLUMN',1,0.1,0.5)])
hat_float('hat_orbe_flotante','orb','#4b2a8a','#f2c14e','#c77dff'); hat('hat_orbe_flotante','Orbe Arcano Flotante','RARE',F,'Un orbe que flota sobre ti.','hat_orbe_flotante','#c77dff',(0,2.8,0))
hat_float('hat_tomo_flotante','tome','#4b2a8a','#7a5614','#c77dff'); hat('hat_tomo_flotante','Tomo Prohibido','EPIC',PR,'Sus páginas susurran.','hat_tomo_flotante','#7a5614',(0,2.8,0))
# ---------------- MASCOTAS (16)
PT='MASCOTAS'
def pet(id,n,rar,tier,desc,model,color,off=(1.2,0.2,1.0),scale=1.0,event=None,part=None):
    add(id,n,PT,'PET',rar,tier,desc,model=model,color=color,offset=off,scale=scale,event=event,particles=part or [],yawmode='pet')
pet_dragon('pet_dragon_carmesi','#c0302a','#6e1713','#ffe066','#ff7a1a'); pet('pet_dragon_carmesi','Dragón Carmesí','LEGENDARY',PR,'Cría de dragón de fuego.','pet_dragon_carmesi','#c0302a')
pet_dragon('pet_dragon_escarcha','#7fc8ff','#3b6ea8','#ffffff','#e8f6ff'); pet('pet_dragon_escarcha','Dragón de Escarcha','LEGENDARY',PR,'Su aliento congela el aire.','pet_dragon_escarcha','#7fc8ff')
pet_dragon('pet_dragon_amatista','#8a5cff','#3a1a7a','#ffe066','#ff9bf5'); pet('pet_dragon_amatista','Dragón Amatista','MYTHIC',PR,'Escamas de cristal violeta.','pet_dragon_amatista','#8a5cff',part=[P('END_ROD','BURST',1,0.3,0.6)])
pet_fox('pet_zorro_arcano','#e07a2f','#f5e6d0','#7fe0ff','#ffffff'); pet('pet_zorro_arcano','Zorro Arcano','COMMON',F,'Un zorro curioso con ojos mágicos.','pet_zorro_arcano','#e07a2f')
pet_fox('pet_zorro_reno','#8a5a35','#f5e6d0','#ff4a4a','#ff4a4a'); pet('pet_zorro_reno','Reno de Escarcha','EPIC',PR,'Edición de Navidad, con nariz brillante.','pet_zorro_reno','#8a5a35',event='adviento')
pet_fox('pet_zorro_sombrio','#2b2233','#4d4560','#c77dff','#a020f0'); pet('pet_zorro_sombrio','Zorro Sombrío','EPIC',PR,'Se mueve entre las sombras.','pet_zorro_sombrio','#2b2233',part=[P('SMOKE','COLUMN',1,0.2,0.4)])
pet_raven('pet_cuervo_nocturno','#1a1622','#f2c14e','#ff4a4a'); pet('pet_cuervo_nocturno','Cuervo Nocturno','COMMON',F,'Compañero de las noches largas.','pet_cuervo_nocturno','#1a1622')
pet_raven('pet_cuervo_runas','#3a2a5a','#c9a3ff','#c77dff'); pet('pet_cuervo_runas','Cuervo de Runas','RARE',PR,'Lleva runas bordadas en las alas.','pet_cuervo_runas','#3a2a5a')
pet_raven('pet_cuervo_espectral','#7ad8ff','#ffffff','#ffffff'); pet('pet_cuervo_espectral','Cuervo Espectral','EPIC',PR,'No proyecta sombra.','pet_cuervo_espectral','#7ad8ff',part=[P('SOUL','COLUMN',1,0.1,0.3)])
pet_golem('pet_golem_piedra','#8a8a8a','#5b5b5b','#7fe0ff'); pet('pet_golem_piedra','Golem de Piedra','COMMON',F,'Pequeño, terco y leal.','pet_golem_piedra','#8a8a8a')
pet_golem('pet_golem_cristal','#9b5cff','#4b2a8a','#ffffff'); pet('pet_golem_cristal','Golem de Cristal','LEGENDARY',PR,'Su núcleo late con magia.','pet_golem_cristal','#9b5cff')
pet_spirit('pet_espiritu_errante','#e8f6ff','#9fd8ff','#4b2a8a'); pet('pet_espiritu_errante','Espíritu Errante','RARE',PR,'Un alma curiosa.','pet_espiritu_errante','#e8f6ff')
pet_spirit('pet_espiritu_bosque','#a8f0a0','#3f9a4a','#1f4a26'); pet('pet_espiritu_bosque','Espíritu del Bosque','RARE',PR,'Guardián de los árboles.','pet_espiritu_bosque','#a8f0a0')
pet_spirit('pet_espiritu_lunar','#d8c8ff','#7a5cff','#ffffff'); pet('pet_espiritu_lunar','Espíritu Lunar','LEGENDARY',PR,'Nace con la luna llena.','pet_espiritu_lunar','#d8c8ff')
pet_creature('pet_cupido','phoenix','#ff9bd0','#ffffff','#ff4a8a'); pet('pet_cupido','Cupido','LEGENDARY',PR,'Edición de febrero: vuela con flechas de cariño.','pet_cupido','#ff9bd0',event='febrero',part=[P('HEART','COLUMN',1,0.2,0.6)])
pet_creature('pet_buho_arcano','owl','#6a4cae','#f2c14e','#ffe28a'); pet('pet_buho_arcano','Búho Arcano','EPIC',PR,'Sabe más de lo que dice.','pet_buho_arcano','#6a4cae')
# ---------------- ALAS Y CAPAS (14)
W='ALAS_CAPAS'
def back(id,n,rar,tier,desc,model,color,off=(0,0,0.5),event=None,part=None):
    add(id,n,W,'BACK',rar,tier,desc,model=model,color=color,offset=off,event=event,particles=part or [],yawmode='body')
wings('wing_angelical','#ffffff','#d8d8e8','#fff3b0','angel'); back('wing_angelical','Alas Angelicales','EPIC',PR,'Plumas de luz.','wing_angelical','#ffffff',(0,1.45,0.2))
wings('wing_serafin','#f2c14e','#c8962e','#ffffff','angel'); back('wing_serafin','Alas de Serafín','LEGENDARY',PR,'Oro y fuego sagrado.','wing_serafin','#f2c14e',(0,1.45,0.2),part=[P('END_ROD','WINGS',1,0.1,0.1)])
wings('wing_rosadas','#ff9bd0','#ffd6ea','#ffffff','fairy'); back('wing_rosadas','Alas Rosadas','LEGENDARY',PR,'Edición de febrero.','wing_rosadas','#ff9bd0',(0,1.45,0.2),event='febrero',part=[P('HEART','WINGS',1,0.1,0.1)])
wings('wing_demonio','#7a1020','#2b0f14','#d62828','demon'); back('wing_demonio','Alas Demoníacas','EPIC',PR,'Membranas de ceniza.','wing_demonio','#7a1020',(0,1.45,0.2))
wings('wing_abismo','#2b2233','#1a1622','#7a3cff','demon'); back('wing_abismo','Alas del Abismo','LEGENDARY',PR,'Fragmentos del vacío.','wing_abismo','#2b2233',(0,1.45,0.2),part=[P('PORTAL','WINGS',1,0.1,0.1)])
wings('wing_infernal','#d62828','#3a0a10','#ffb000','demon'); back('wing_infernal','Alas Infernales','MYTHIC',PR,'Ardientes y legendarias.','wing_infernal','#d62828',(0,1.45,0.2),part=[P('SMALL_FLAME','WINGS',1,0.1,0.1)])
wings('wing_dragon_verde','#3a9a4a','#1f5a2a','#c8f0a0','dragon'); back('wing_dragon_verde','Alas de Dragón Esmeralda','EPIC',PR,'Escamas verdes.','wing_dragon_verde','#3a9a4a',(0,1.45,0.2))
wings('wing_dragon_oro','#f2c14e','#7a5614','#ff7a1a','dragon'); back('wing_dragon_oro','Alas de Dragón Dorado','MYTHIC',PR,'El tesoro de un dragón.','wing_dragon_oro','#f2c14e',(0,1.45,0.2),part=[P('FLAME','WINGS',1,0.1,0.1)])
cape('cape_viajero','#5a3a22','#3a2410','#c9a227'); back('cape_viajero','Capa de Viajero','COMMON',F,'Para recorrer el reino.','cape_viajero','#5a3a22',(0,1.5,0.35))
cape('cape_real_roja','#a01028','#6e0a1a','#f2c14e'); back('cape_real_roja','Capa Real Carmesí','RARE',F,'Terciopelo y oro.','cape_real_roja','#a01028',(0,1.5,0.35))
cape('cape_real_morada','#4b2a8a','#25124a','#f2c14e'); back('cape_real_morada','Capa Real Violeta','EPIC',PR,'La capa de los reyes de Aetheria.','cape_real_morada','#4b2a8a',(0,1.5,0.35))
cape('cape_encantada_runas','#2a1648','#14101c','#c77dff',True); back('cape_encantada_runas','Capa de Runas','EPIC',PR,'Runas que brillan en la tela.','cape_encantada_runas','#2a1648',(0,1.5,0.35))
cape('cape_encantada_estelar','#14101c','#0b0910','#ffffff',True); back('cape_encantada_estelar','Capa Estelar','LEGENDARY',PR,'Un cielo nocturno tejido.','cape_encantada_estelar','#14101c',(0,1.5,0.35),part=[P('END_ROD','TRAIL',1,0.3,1.0)])
cape('cape_encantada_fuego','#7a1020','#3a0a10','#ffb000',True); back('cape_encantada_fuego','Capa de Brasas','LEGENDARY',PR,'Hilos de brasa viva.','cape_encantada_fuego','#7a1020',(0,1.5,0.35),part=[P('SMALL_FLAME','TRAIL',1,0.3,1.0)])
# ---------------- EFECTOS (20)
E='EFECTOS'
def fx(id,n,slot,rar,tier,desc,color,part,event=None): add(id,n,E,slot,rar,tier,desc,color=color,particles=part,event=event)
fx('fx_walk_polvo','Polvo de Camino','WALK','COMMON',F,'Una estela de polvo mágico.','#cfa66b',[P('CRIT','TRAIL',2,0.3,0.1)])
fx('fx_walk_chispas','Chispas de Hada','WALK','COMMON',F,'Chispas a cada paso.','#ffe28a',[P('END_ROD','TRAIL',1,0.3,0.1)])
fx('fx_walk_fuego','Huellas de Fuego','WALK','EPIC',PR,'Dejas fuego a tu paso.','#ff7a1a',[P('FLAME','TRAIL',2,0.2,0.1)])
fx('fx_walk_corazones','Huellas de Corazones','WALK','EPIC',PR,'Edición de febrero.','#ff6fa5',[P('HEART','TRAIL',1,0.3,0.2)],'febrero')
fx('fx_jump_ondas','Onda de Salto','JUMP','COMMON',F,'Un anillo al saltar.','#ffffff',[P('CLOUD','RING',10,0.6,0.1)])
fx('fx_jump_runas','Runas de Salto','JUMP','RARE',PR,'Runas bajo tus pies.','#c77dff',[P('ENCHANT','RING',14,0.9,0.1)])
fx('fx_jump_alas','Impulso Alado','JUMP','EPIC',PR,'Plumas al saltar.','#ffffff',[P('CLOUD','BURST',8,0.5,0.5),P('END_ROD','RING',10,0.8,0.1)])
fx('fx_land_impacto','Impacto Telúrico','LAND','COMMON',F,'Polvo al aterrizar.','#8a6f4d',[P('POOF','RING',16,1.0,0.1)])
fx('fx_land_estrella','Aterrizaje Estelar','LAND','EPIC',PR,'Estrellas al caer.','#fff3b0',[P('FIREWORK','BURST',14,0.8,0.5)])
fx('fx_land_sismo','Sismo Arcano','LAND','LEGENDARY',PR,'Una onda violeta al caer.','#8a3cff',[P('DUST','RING',24,1.6,0.1,color='#8a3cff',size=1.5),P('ENCHANT','BURST',20,0.6,0.5)])
fx('fx_kill_alma','Alma Liberada','KILL','COMMON',F,'Un alma se eleva.','#7ad8ff',[P('SOUL','COLUMN',6,0.2,1.8)])
fx('fx_kill_explosion','Estallido Arcano','KILL','EPIC',PR,'Un estallido violeta.','#a020f0',[P('DUST','BURST',30,0.8,0.8,color='#a020f0',size=1.4),P('WITCH','BURST',20,0.6,0.8)])
fx('fx_kill_rayo','Juicio del Rayo','KILL','LEGENDARY',PR,'Un rayo sin trueno.','#6ad8ff',[P('ELECTRIC_SPARK','COLUMN',30,0.4,3.0),P('FLASH','BURST',1,0.1,0.1)])
fx('fx_dmg_chispas','Destello de Golpe','DAMAGE','COMMON',F,'Chispas al recibir daño.','#ffd25a',[P('CRIT','BURST',12,0.5,0.8)])
fx('fx_dmg_escudo','Escudo Rúnico','DAMAGE','RARE',PR,'Un escudo se manifiesta.','#c77dff',[P('ENCHANT','RING',24,0.9,1.0),P('END_ROD','BURST',8,0.6,0.8)])
fx('fx_spawn_brillo','Aparición Brillante','SPAWN','COMMON',F,'Apareces entre destellos.','#ffffff',[P('END_ROD','COLUMN',20,0.4,2.0)])
fx('fx_spawn_portal','Entrada del Portal','SPAWN','EPIC',PR,'Un portal te escupe al mundo.','#7a3cff',[P('PORTAL','RING',40,1.0,1.0),P('REVERSE_PORTAL','COLUMN',20,0.4,2.0)])
fx('fx_win_fuegos','Fuegos de Victoria','VICTORY','RARE',PR,'Fuegos artificiales de celebración.','#ffcc33',[P('FIREWORK','BURST',40,1.0,1.5)])
fx('fx_win_corona','Lluvia de Oro','VICTORY','EPIC',PR,'Monedas de luz caen.','#f2c14e',[P('DUST','RAIN',40,1.2,3.0,color='#f2c14e',size=1.4),P('TOTEM_OF_UNDYING','BURST',20,0.8,1.5)])
fx('fx_win_leyenda','Ascensión Legendaria','VICTORY','LEGENDARY',PR,'Una columna de luz celestial.','#fff3b0',[P('END_ROD','COLUMN',60,0.6,4.0),P('TOTEM_OF_UNDYING','BURST',30,0.9,2.0)])
# ---------------- OTROS (12)
O='OTROS'
def oth(id,n,slot,rar,tier,desc,color,model=None,part=None,off=(0,0,0),event=None,yawmode='head'):
    add(id,n,O,slot,rar,tier,desc,model=model,color=color,particles=part or [],offset=off,event=event,yawmode=yawmode)
ring_model('ring_plata','#cfd3da','#7fd0ff',10,6); oth('ring_plata','Anillo de Plata','RING','COMMON',F,'Un aro simple que gira sobre tu cintura.','#cfd3da','ring_plata',off=(0,0.9,0))
ring_model('ring_oro','#f2c14e','#ff5a5a',12,7); oth('ring_oro','Anillo de Oro','RING','RARE',PR,'Brillante, pesado, ostentoso.','#f2c14e','ring_oro',off=(0,0.9,0))
ring_model('ring_arcano','#8a5cff','#ffffff',14,8,True); oth('ring_arcano','Anillo Arcano','RING','EPIC',PR,'Pilares de magia orbitan.','#8a5cff','ring_arcano',off=(0,0.9,0))
ring_model('ring_eterno','#ff9bf5','#ffe28a',16,9,True); oth('ring_eterno','Anillo Eterno','RING','LEGENDARY',PR,'Nunca deja de girar.','#ff9bf5','ring_eterno',off=(0,0.9,0),part=[P('END_ROD','RING',1,0.9,0.9)])
orbit_model('acc_espadas','swords','#cfd3da','#c9a227','#ffffff'); oth('acc_espadas','Espadas Orbitales','ORBIT','EPIC',PR,'Tres espadas guardan tu espalda.','#cfd3da','acc_espadas',off=(0,1.2,0))
orbit_model('acc_runas','runs' if False else 'runes','#4b2a8a','#c77dff','#c77dff'); oth('acc_runas','Piedras Rúnicas','ORBIT','RARE',PR,'Tres monolitos flotantes.','#4b2a8a','acc_runas',off=(0,1.2,0))
orbit_model('acc_mariposas','butterflies','#c77dff','#ffffff','#ffffff'); oth('acc_mariposas','Mariposas Mágicas','ORBIT','RARE',PR,'Mariposas de luz.','#c77dff','acc_mariposas',off=(0,1.4,0))
oth('trans_espectro','Forma Espectral','TRANSFORM','LEGENDARY',PR,'Tu cuerpo se vuelve etéreo y brillante.','#9fd8ff',part=[P('SOUL','SPIRAL',1,0.5,1.8),P('END_ROD','COLUMN',1,0.3,1.8)])
oth('trans_llama','Alma de Fuego','TRANSFORM','LEGENDARY',PR,'El fuego brota de tu cuerpo.','#ff7a1a',part=[P('FLAME','COLUMN',3,0.35,1.8),P('SMALL_FLAME','SPIRAL',1,0.5,1.8)])
oth('esp_nova','Nova Estelar','SPECIAL','LEGENDARY',PR,'Cada pocos segundos, una nova de luz.','#fff3b0',part=[P('END_ROD','RING',30,1.8,0.2)])
oth('esp_runas_pulso','Pulso de Runas','SPECIAL','EPIC',PR,'Un pulso de runas cada pocos segundos.','#c77dff',part=[P('ENCHANT','RING',30,1.6,0.2)])
oth('esp_aurora','Aurora Boreal','SPECIAL','MYTHIC',PR,'Una cortina de aurora sobre ti.','#7dffd0',part=[P('DUST_COLOR_TRANSITION','RAIN',8,1.2,3.0,color='#7dffd0',color2='#9b5cff',size=1.5)])


# ---- ajuste de colocacion (bloques). Convencion: x=derecha, y=arriba, z=detras del jugador.
#  HAT: relativo a la parte alta de la cabeza (ojos+0.18). PET: relativo al jugador. BACK: relativo al pie, y=hombros.
HATY={'mascara':-0.25,'casco':-0.25,'orbe':0.25,'tomo':0.25}
for c in CAT:
    if c['slot']=='HAT':
        y=-0.02
        for k,v in HATY.items():
            if k in c['id']: y=v
        c['offset']=[0,y,0]
    elif c['slot']=='PET': c['offset']=[0.9,0.35,0.9]
    elif c['slot']=='BACK': c['offset']=[0,1.45,0.12 if c['id'].startswith('wing') else 0.14]
    elif c['slot']=='RING': c['offset']=[0,0.9,0]
    elif c['slot']=='ORBIT': c['offset']=[0,1.2,0]

# ---- validaciones
assert len(CAT)==100, len(CAT)
ids=[c['id'] for c in CAT]; assert len(set(ids))==100
free=[c for c in CAT if c['tier']==F]; prem=[c for c in CAT if c['tier']==PR]
print('free',len(free),'premium',len(prem))
assert len(free)==20 and len(prem)==80, (len(free),len(prem))

# ---- iconos (items del resource pack): 16x16 generados + CMD 5001+
def glyph(c,cat,rar):
    im=Image.new('RGBA',(16,16),(0,0,0,0)); d=ImageDraw.Draw(im); col=hx(c['color'])
    frame={'COMMON':'#8a8a8a','RARE':'#4aa3ff','EPIC':'#b05cff','LEGENDARY':'#f2c14e','MYTHIC':'#ff5ad0'}[rar]
    d.rectangle([0,0,15,15],fill=hx('#14101c')+(255,)); d.rectangle([0,0,15,15],outline=hx(frame)+(255,)); d.rectangle([1,1,14,14],outline=hx('#3a2a5a')+(255,))
    dk=tuple(max(0,v-70) for v in col); lt=tuple(min(255,v+60) for v in col)
    if cat=='AURAS':
        for r,cc in ((6,dk),(4,col)): d.ellipse([8-r,8-r,8+r,8+r],outline=cc+(255,))
        d.ellipse([6,6,9,9],fill=lt+(255,))
    elif cat=='SOMBREROS':
        d.rectangle([3,9,12,11],fill=col+(255,)); 
        for x in (3,6,9,12): d.rectangle([x,5,x,9],fill=col+(255,)); d.point((x,4),fill=lt+(255,))
        d.rectangle([4,10,11,10],fill=dk+(255,))
    elif cat=='MASCOTAS':
        d.rectangle([4,6,11,12],fill=col+(255,)); d.rectangle([4,3,5,6],fill=col+(255,)); d.rectangle([10,3,11,6],fill=col+(255,))
        d.point((6,8),fill=lt+(255,)); d.point((9,8),fill=lt+(255,)); d.rectangle([7,10,8,10],fill=dk+(255,))
    elif cat=='ALAS_CAPAS':
        for s in (0,1):
            xs=[7,3,2,3,6] if s==0 else [8,12,13,12,9]
            d.polygon(list(zip(xs,[4,3,8,12,11])),fill=col+(255,),outline=dk+(255,))
    elif cat=='EFECTOS':
        for a in range(0,360,45):
            x=8+int(5*math.cos(math.radians(a))); y=8+int(5*math.sin(math.radians(a))); d.line([8,8,x,y],fill=col+(255,))
        d.ellipse([6,6,9,9],fill=lt+(255,))
    else:
        d.ellipse([3,3,12,12],outline=col+(255,)); d.ellipse([4,4,11,11],outline=dk+(255,)); d.rectangle([7,2,8,3],fill=lt+(255,))
    return im
for i,c in enumerate(CAT):
    c['cmd']=5001+i
    art.make_icon(c).save(os.path.join(PACK,'assets/aetheria/textures/item','cos_'+c['id']+'.png'))
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":"aetheria:item/cos_"+c['id']}},open(os.path.join(PACK,'assets/aetheria/models/item','cos_'+c['id']+'.json'),'w'))
    overrides['paper'].append({"predicate":{"custom_model_data":c['cmd']},"model":"aetheria:item/cos_"+c['id']})
# botones y iconos de GUI (CMD 9001-9008 botones, 9010-9015 categorias, 9020-9024 menu principal)
UI={}
for i,k in enumerate(['back','close','prev','next','search','filter','claim','star']): UI[9001+i]=('btn_'+k,art.make_button(k))
for i,k in enumerate(['AURAS','SOMBREROS','MASCOTAS','ALAS_CAPAS','EFECTOS','OTROS']): UI[9010+i]=('cat_'+k.lower(),art.make_cat(k))
for i,k in enumerate(['cosmeticos','pase','misiones','adviento','febrero']): UI[9020+i]=('main_'+k,art.make_main(k))
for cmd,(nm,img) in UI.items():
    img.save(os.path.join(PACK,'assets/aetheria/textures/item',nm+'.png'))
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":"aetheria:item/"+nm}},open(os.path.join(PACK,'assets/aetheria/models/item',nm+'.json'),'w'))
    overrides['paper'].append({"predicate":{"custom_model_data":cmd},"model":"aetheria:item/"+nm})
# item transparente (relleno invisible de GUIs con skin)
Image.new('RGBA',(16,16),(0,0,0,0)).save(os.path.join(PACK,'assets/aetheria/textures/item','blank.png'))
json.dump({"parent":"minecraft:item/generated","textures":{"layer0":"aetheria:item/blank"}},open(os.path.join(PACK,'assets/aetheria/models/item','blank.json'),'w'))
overrides['paper'].append({"predicate":{"custom_model_data":9000},"model":"aetheria:item/blank"})
for base,ov in overrides.items():
    json.dump({"parent":"minecraft:item/generated","textures":{"layer0":"minecraft:item/"+base},"overrides":ov},
              open(os.path.join(PACK,'assets/minecraft/models/item',base+'.json'),'w'),indent=1)
json.dump({"pack":{"pack_format":34,"description":"§6Aetheria §7- Recompensas (llaves e iconos)"}},open(os.path.join(PACK,'pack.mcmeta'),'w'),indent=1)
Image.new('RGBA',(64,64),hx('#25124a')+(255,)).save(os.path.join(PACK,'pack.png'))

# ---- catalog.yml
try:
    import yaml
    class D(yaml.SafeDumper): pass
    D.add_representer(type(None), lambda s,d: s.represent_scalar('tag:yaml.org,2002:null','null'))
    out=dict(cosmetics={c['id']:{k:v for k,v in c.items() if k!='id' and not(k=='evento' and v is None) and not(k=='model' and v is None)} for c in CAT},
             key_cmd=KEY_CMD)
    open(os.path.join(RES,'catalog.yml'),'w',encoding='utf-8').write("# Catalogo de 100 cosmeticos (20 FREE + 80 PREMIUM). Editable. 'offset' [x,y,z], 'yaw', 'scale' ajustan la colocacion.\n"+yaml.dump(out,Dumper=D,allow_unicode=True,sort_keys=False,width=200))
except ImportError:
    json.dump(dict(cosmetics={c['id']:c for c in CAT}),open(os.path.join(RES,'catalog.yml'),'w'),ensure_ascii=False,indent=1)
print('modelos:',len(os.listdir(MODELS)),'iconos:',len(CAT))

open(os.path.join(MODELS,'index.txt'),'w').write('\n'.join(sorted(f[:-8] for f in os.listdir(MODELS) if f.endswith('.bbmodel')))+'\n')
