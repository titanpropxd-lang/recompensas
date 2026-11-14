#!/usr/bin/env python3
"""Genera imagenes de muestra (vista frontal de cofres, mockup de GUI y botones) en /mnt/user-data/outputs/previews."""
import json, base64, io, os, sys, yaml
from PIL import Image, ImageDraw
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__))); import art
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__))); RES=ROOT+'/src/main/resources'; OUT='/mnt/user-data/outputs/previews'; os.makedirs(OUT,exist_ok=True)
def front(name,S=10):
    d=json.load(open(f'{RES}/models/{name}.bbmodel')); tex=Image.open(io.BytesIO(base64.b64decode(d['textures'][0]['source'].split(',')[1]))).convert('RGBA')
    W,H=22*S,22*S; im=Image.new('RGBA',(W,H),(0,0,0,0))
    els=sorted(d['elements'],key=lambda e:-e['from'][2])
    for e in els:
        x0,y0,z0=e['from']; x1,y1,z1=e['to']; u=e['faces']['north']['uv']
        crop=tex.crop(tuple(int(v) for v in u)); w=max(1,int((x1-x0)*S)); h=max(1,int((y1-y0)*S))
        crop=crop.resize((w,h),Image.NEAREST); im.paste(crop,(int((x0+11)*S),int((20-y1)*S)),crop)
    return im
names=['crate_madera','crate_hierro','crate_dorado','crate_magico','crate_legendario','crate_oscuro','crate_evento']
sheet=Image.new('RGBA',(4*230,2*230),(26,16,42,255))
for i,n in enumerate(names):
    f=front(n,10); sheet.paste(f,((i%4)*230+5,(i//4)*230+25),f); ImageDraw.Draw(sheet).text(((i%4)*230+8,(i//4)*230+6),n,fill=(240,200,90,255))
sheet.save(OUT+'/cofres_vista_frontal.png')
# mockup de GUI con skin
cat=yaml.safe_load(open(RES+'/catalog.yml',encoding='utf-8'))['cosmetics']
pan=art.panel('main',6).crop((0,0,176,222)); gui=pan.copy()
ids=[k for k,v in cat.items() if v['categoria']=='SOMBREROS']+[k for k,v in cat.items() if v['categoria']=='MASCOTAS']
for n,k in enumerate(ids[:28]):
    r,c=divmod(n,7); ic=art.make_icon(dict(id=k,**cat[k])).resize((16,16),Image.NEAREST); gui.paste(ic,(8+18*(c+1),18+18*(r+1)),ic)
for i,k in enumerate(['AURAS','SOMBREROS','MASCOTAS','ALAS_CAPAS','EFECTOS','OTROS']):
    ic=art.make_cat(k).resize((16,16),Image.NEAREST); gui.paste(ic,(8+18*(i+1),18),ic)
for slot,kind in((45,'back'),(47,'filter'),(48,'search'),(50,'claim'),(53,'next')):
    ic=art.make_button(kind).resize((16,16),Image.NEAREST); gui.paste(ic,(8+18*(slot%9),18+18*(slot//9)),ic)
d=ImageDraw.Draw(gui); d.text((10,6),'Cosmeticos',fill=(255,200,60,255))
gui=gui.resize((176*4,222*4),Image.NEAREST)
adv=art.panel('advent',6).crop((0,0,176,222)).resize((176*2,222*2),Image.NEAREST); feb=art.panel('feb',6).crop((0,0,176,222)).resize((176*2,222*2),Image.NEAREST)
mock=Image.new('RGBA',(gui.width+adv.width+20,gui.height),(16,10,26,255)); mock.paste(gui,(0,0)); mock.paste(adv,(gui.width+10,0)); mock.paste(feb,(gui.width+10,adv.height+10))
mock.save(OUT+'/gui_mockup.png')
# iconos
items=[dict(id=k,**v) for k,v in cat.items()]; cols=10; sh=Image.new('RGBA',(cols*36,10*36),(20,14,30,255))
for n,c in enumerate(items): ic=art.make_icon(c); sh.paste(ic,((n%cols)*36+2,(n//cols)*36+2),ic)
sh.resize((sh.width*3,sh.height*3),Image.NEAREST).save(OUT+'/iconos_100_cosmeticos.png')
print('ok')
