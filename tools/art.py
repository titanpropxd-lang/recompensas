"""Libreria de arte procedural (pixel art con relieve): texturas de materiales, iconos 32x32, botones y paneles."""
import math, random, colorsys
from PIL import Image, ImageDraw, ImageFilter

def hx(s): return tuple(int(s[i:i+2],16) for i in (1,3,5))
def clamp(v): return max(0,min(255,int(v)))
def mul(c,f): return tuple(clamp(v*f) for v in c[:3])
def mix(a,b,t): return tuple(clamp(a[i]+(b[i]-a[i])*t) for i in range(3))
def hsv(c): return colorsys.rgb_to_hsv(*(v/255 for v in c[:3]))

# ------------------------------------------------------------------ materiales (tile 16x16)
def material_of(color, emit):
    if emit>0: return 'glow'
    h,s,v=hsv(hx(color))
    if v<0.22: return 'dark'
    if s<0.22: return 'metal'
    if 0.08<=h<=0.17 and s>0.45 and v>0.62: return 'gold'
    if 0.02<=h<=0.12 and v<0.6 and s>0.35: return 'wood'
    return 'cloth'

def paint_tile(color, emit=0, seed=0):
    rnd=random.Random(hash((color,emit,seed))&0xffff)
    c=hx(color); T=16
    im=Image.new('RGBA',(T,T)); px=im.load(); m=material_of(color,emit)
    for y in range(T):
        for x in range(T):
            col=c; n=rnd.uniform(-0.05,0.05)
            if m=='wood':
                plank=y//4; col=mul(c,1.0+0.07*((plank%2)*2-1))
                if y%4==3: col=mul(c,0.62)
                elif y%4==0: col=mul(c,1.13)
                if (x*3+plank*7)%11==0 and y%4 in(1,2): col=mul(col,0.82)      # veta
                if x==7 and plank==1 and y%4 in(1,2): col=mul(c,0.45)           # nudo
                if (x+plank*5)%8==0 and y%4==1: col=mul(col,0.86)
            elif m=='metal':
                col=mix(mul(c,1.18),mul(c,0.82),y/T)
                if y%2==0: col=mul(col,1.03)
                n=rnd.uniform(-0.03,0.03)
            elif m=='gold':
                d=(x+y)/ (2*T-2); band=math.exp(-((d-0.45)/0.12)**2)
                col=mix(mul(c,0.78),mul(c,1.0),0.5+0.5*math.sin(d*6.0)); col=mix(col,(255,248,200),band*0.55)
                if rnd.random()<0.04: col=(255,255,235)
            elif m=='glow':
                r=math.hypot(x-7.5,y-7.5)/10.5; col=mix((255,255,255),c,min(1,r*1.1)); col=mul(col,1.05)
                n=0
            elif m=='dark':
                col=mix(mul(c,1.5),mul(c,0.8),y/T)
                if (x+y)%7==0: col=mul(col,1.25)
            else:  # cloth/fur/escamas
                weave=((x//2)+(y//2))%2; col=mul(c,1.06 if weave else 0.94)
                col=mix(col,mul(c,1.2),max(0,1-(x+y)/14)*0.25)
            col=mul(col,1+n)
            # bisel
            if m!='glow':
                if y==0 or x==0: col=mul(col,1.18)
                if y==T-1 or x==T-1: col=mul(col,0.72)
            px[x,y]=col+(255,)
    d=ImageDraw.Draw(im)
    if m=='metal':
        for rx,ry in((2,2),(13,2),(2,13),(13,13)):
            d.point((rx,ry),fill=mul(c,1.45)+(255,)); d.point((rx+1,ry+1),fill=mul(c,0.5)+(255,))
    return im

# ------------------------------------------------------------------ iconos 32x32
RAR={'COMMON':'#9aa0a8','RARE':'#4aa3ff','EPIC':'#b05cff','LEGENDARY':'#f2c14e','MYTHIC':'#ff5ad0'}
S=32
def icon_base(rar, accent):
    rc=hx(RAR[rar]); ac=hx(accent)
    im=Image.new('RGBA',(S,S),(0,0,0,0)); d=ImageDraw.Draw(im)
    for y in range(S):
        d.line([0,y,S-1,y],fill=mix((38,22,64),(10,6,20),y/S)+(255,))
    # resplandor del color del cosmetico
    for y in range(S):
        for x in range(S):
            r=math.hypot(x-15.5,y-15.5)/16
            if r<1:
                p=im.getpixel((x,y)); im.putpixel((x,y),mix(p,ac,(1-r)**2*0.30)+(255,))
    d.rectangle([0,0,S-1,S-1],outline=mul(rc,0.45)+(255,))
    d.rectangle([1,1,S-2,S-2],outline=rc+(255,))
    d.line([2,2,S-3,2],fill=mul(rc,1.35)+(255,)); d.line([2,2,2,S-3],fill=mul(rc,1.35)+(255,))
    d.rectangle([2,2,S-3,S-3],outline=mul(rc,0.55)+(120,))
    for x,y in((0,0),(S-2,0),(0,S-2),(S-2,S-2)): d.rectangle([x,y,x+1,y+1],fill=mul(rc,1.5)+(255,))
    if rar in('LEGENDARY','MYTHIC'):
        for x,y in((15,0),(15,S-1),(0,15),(S-1,15)): d.point((x,y),fill=(255,255,255,255))
    return im
def shade(c,hi=1.35,lo=0.55): return mul(c,hi),mul(c,lo)
def poly(d,pts,c,outline=True):
    hi,lo=shade(c); d.polygon(pts,fill=c+(255,))
    if outline: d.line(pts+[pts[0]],fill=lo+(255,))
def gem(d,x,y,c,s=2):
    d.rectangle([x,y,x+s,y+s],fill=c+(255,)); d.point((x,y),fill=(255,255,255,255))
def sparkle(d,x,y,c=(255,255,255)):
    for dx,dy in((0,0),(1,0),(-1,0),(0,1),(0,-1)): d.point((x+dx,y+dy),fill=c+(255,))

def g_crown(d,c,acc):
    hi,lo=shade(c)
    d.rectangle([7,19,24,23],fill=c+(255,)); d.rectangle([7,19,24,19],fill=hi+(255,)); d.rectangle([7,23,24,23],fill=lo+(255,))
    for x in (7,11,15,19,23):
        poly(d,[(x,19),(x+1,11+(x%8)//2),(x+3,19)],c)
        gem(d,x,10+(x%8)//2,acc,1)
    gem(d,14,20,acc,2)
def g_helm(d,c,acc):
    hi,lo=shade(c)
    d.pieslice([7,8,25,28],180,360,fill=c+(255,)); d.rectangle([7,18,25,25],fill=c+(255,)); d.rectangle([7,18,25,18],fill=hi+(255,))
    d.rectangle([9,19,23,22],fill=(12,8,20,255)); d.rectangle([15,12,17,25],fill=lo+(255,))
    d.rectangle([14,5,18,10],fill=acc+(255,)); d.point((15,5),fill=(255,255,255,255))
def g_mask(d,c,acc):
    hi,lo=shade(c); d.ellipse([8,7,24,27],fill=c+(255,),outline=lo+(255,))
    d.polygon([(10,14),(14,16),(14,18),(10,17)],fill=(10,6,16,255)); d.polygon([(22,14),(18,16),(18,18),(22,17)],fill=(10,6,16,255))
    d.point((12,16),fill=acc+(255,)); d.point((20,16),fill=acc+(255,)); d.line([16,18,16,22],fill=lo+(255,)); d.arc([12,21,20,26],20,160,fill=lo+(255,))
def g_wizard(d,c,acc):
    hi,lo=shade(c); poly(d,[(16,3),(10,21),(23,21)],c); d.rectangle([6,21,26,24],fill=lo+(255,)); d.rectangle([6,21,26,21],fill=hi+(255,))
    d.rectangle([11,17,22,19],fill=acc+(255,)); sparkle(d,16,9,acc)
def g_horns(d,c,acc):
    for sx in (-1,1):
        pts=[(16+sx*3,24),(16+sx*9,18),(16+sx*11,10),(16+sx*13,5),(16+sx*7,11),(16+sx*5,17)]
        poly(d,pts,c)
    d.rectangle([11,23,21,26],fill=mul(c,0.6)+(255,))
def g_orb(d,c,acc):
    d.ellipse([8,8,24,24],fill=mul(c,0.7)+(255,),outline=mul(c,0.4)+(255,)); d.ellipse([11,11,21,21],fill=acc+(255,))
    d.ellipse([12,12,15,15],fill=(255,255,255,255)); d.rectangle([9,25,23,26],fill=mul(c,0.8)+(255,))
def g_tome(d,c,acc):
    d.rectangle([7,9,25,24],fill=c+(255,),outline=mul(c,0.45)+(255,)); d.rectangle([9,11,23,22],fill=(238,228,200,255)); d.line([16,11,16,22],fill=(120,100,70,255))
    sparkle(d,16,16,acc); d.rectangle([7,9,25,10],fill=mul(c,1.3)+(255,))
def g_pet(d,c,acc,kind):
    hi,lo=shade(c)
    if kind=='dragon':
        poly(d,[(5,16),(9,6),(12,14)],c); poly(d,[(27,16),(23,6),(20,14)],c)
        d.rectangle([10,12,22,24],fill=c+(255,),outline=lo+(255,)); d.rectangle([13,21,19,25],fill=hi+(255,)); d.point((14,23),fill=lo+(255,)); d.point((18,23),fill=lo+(255,))
        poly(d,[(11,12),(12,6),(14,12)],mul(c,0.7)); poly(d,[(21,12),(20,6),(18,12)],mul(c,0.7))
    elif kind=='fox':
        poly(d,[(7,6),(14,13),(8,16)],c); poly(d,[(25,6),(18,13),(24,16)],c)
        poly(d,[(7,14),(25,14),(16,27)],c); d.polygon([(11,21),(21,21),(16,27)],fill=(245,235,220,255))
    elif kind=='raven':
        d.ellipse([9,10,23,24],fill=c+(255,),outline=lo+(255,)); poly(d,[(22,15),(29,17),(22,19)],(240,190,60)); poly(d,[(9,16),(2,10),(5,20)],c)
    elif kind=='golem':
        d.rectangle([8,8,24,25],fill=c+(255,),outline=lo+(255,)); d.rectangle([8,8,24,10],fill=hi+(255,)); d.line([8,17,24,17],fill=lo+(255,)); d.line([16,8,16,17],fill=lo+(255,))
    elif kind=='owl':
        d.ellipse([8,8,24,26],fill=c+(255,),outline=lo+(255,)); poly(d,[(8,10),(11,5),(13,9)],c); poly(d,[(24,10),(21,5),(19,9)],c); poly(d,[(15,17),(17,17),(16,20)],(240,190,60))
        d.ellipse([10,12,14,16],fill=(255,240,200,255)); d.ellipse([18,12,22,16],fill=(255,240,200,255))
    elif kind=='phoenix':
        poly(d,[(3,18),(10,8),(14,18)],c); poly(d,[(29,18),(22,8),(18,18)],c); d.ellipse([11,11,21,24],fill=c+(255,),outline=lo+(255,)); poly(d,[(16,3),(13,9),(19,9)],acc)
    else:  # espiritu
        d.pieslice([8,6,24,22],180,360,fill=(c+(255,))); d.rectangle([8,14,24,24],fill=c+(255,))
        for x in (8,12,16,20): d.polygon([(x,24),(x+2,28),(x+4,24)],fill=c+(255,))
    eyes=acc if kind not in('golem',) else acc
    d.rectangle([12,16,13,17],fill=eyes+(255,)); d.rectangle([19,16,20,17],fill=eyes+(255,))
def g_wings(d,c,acc,style):
    hi,lo=shade(c)
    for sx in (-1,1):
        X=lambda x:16+sx*x
        if style=='angel':
            for i in range(4): poly(d,[(X(3),12+i*3),(X(13-i),6+i*5),(X(14-i),9+i*5),(X(5),15+i*3)],c if i%2==0 else mul(c,0.88))
        elif style=='demon':
            poly(d,[(X(3),22),(X(5),6),(X(14),9),(X(12),15),(X(10),14),(X(8),20),(X(6),19)],c); d.line([X(5),6,X(11),23],fill=lo+(255,)); d.line([X(5),6,X(14),9],fill=hi+(255,))
        elif style=='dragon':
            poly(d,[(X(3),22),(X(4),7),(X(15),12),(X(13),17),(X(10),16),(X(9),22)],c); d.line([X(4),7,X(10),22],fill=lo+(255,)); d.line([X(4),7,X(13),16],fill=lo+(255,))
        else: # hada
            d.ellipse([16+sx*3-(0 if sx>0 else 10),5,16+sx*3+(10 if sx>0 else 0),17],fill=c+(255,),outline=lo+(255,)); d.ellipse([16+sx*3-(0 if sx>0 else 7),15,16+sx*3+(7 if sx>0 else 0),24],fill=mul(c,0.9)+(255,),outline=lo+(255,))
    d.rectangle([15,10,16,26],fill=lo+(255,)); gem(d,15,13,acc,1)
def g_cape(d,c,acc):
    hi,lo=shade(c); poly(d,[(9,7),(23,7),(27,27),(5,27)],c); d.rectangle([9,6,23,9],fill=acc+(255,)); d.line([9,9,23,9],fill=lo+(255,))
    for x in (11,16,21): d.line([x,10,x+(x-16)//3,26],fill=lo+(255,))
    gem(d,15,7,(255,255,255),1)
def g_aura(d,c,acc,theme):
    hi,lo=shade(c)
    if theme=='fire':
        poly(d,[(16,3),(8,17),(10,26),(16,29),(22,26),(24,17),(19,10),(18,17),(16,11)],c); poly(d,[(16,14),(12,22),(16,27),(20,22)],mix(c,(255,240,160),0.6),False)
    elif theme=='ice':
        for a in range(0,180,60):
            r=math.radians(a); d.line([16-13*math.cos(r),16-13*math.sin(r),16+13*math.cos(r),16+13*math.sin(r)],fill=hi+(255,),width=2)
        d.ellipse([13,13,19,19],fill=(255,255,255,255))
    elif theme=='bolt':
        poly(d,[(19,3),(9,17),(15,17),(12,29),(24,13),(17,13)],c)
    elif theme=='nature':
        poly(d,[(16,4),(6,18),(16,28),(26,18)],c); d.line([16,6,16,27],fill=lo+(255,)); 
        for y in (11,16,21): d.line([16,y,10,y-3],fill=lo+(255,)); d.line([16,y,22,y-3],fill=lo+(255,))
    elif theme=='heart':
        d.ellipse([5,7,17,19],fill=c+(255,)); d.ellipse([15,7,27,19],fill=c+(255,)); poly(d,[(6,15),(26,15),(16,28)],c,False); d.ellipse([8,9,11,12],fill=mul(c,1.6)+(255,))
    elif theme=='star':
        pts=[]
        for i in range(10):
            r=14 if i%2==0 else 6; a=math.radians(-90+i*36); pts.append((16+r*math.cos(a),16+r*math.sin(a)))
        poly(d,pts,c)
    elif theme=='halo':
        d.ellipse([5,12,27,22],outline=hi+(255,),width=2); d.ellipse([7,14,25,20],outline=c+(255,)); sparkle(d,16,8,(255,255,255))
    else: # espiral/magia/runas
        for i in range(3):
            d.ellipse([16-12+i*3,16-12+i*3,16+12-i*3,16+12-i*3],outline=[hi,c,lo][i]+(255,),width=2)
        for a in range(0,360,60):
            r=math.radians(a); sparkle(d,int(16+11*math.cos(r)),int(16+11*math.sin(r)),acc)
def g_effect(d,c,acc,kind):
    hi,lo=shade(c)
    if kind=='WALK':
        for i,(x,y) in enumerate(((9,22),(19,15),(9,9))): d.ellipse([x,y,x+5,y+7],fill=c+(255,)); sparkle(d,x+11,y+2,acc)
    elif kind=='JUMP':
        for y in (22,15,8): d.polygon([(16,y-5),(8,y+2),(12,y+2),(16,y-2),(20,y+2),(24,y+2)],fill=mix(c,hi,0.3)+(255,))
    elif kind=='LAND':
        for y in (7,14): d.polygon([(16,y+7),(8,y),(12,y),(16,y+3),(20,y),(24,y)],fill=c+(255,))
        d.ellipse([4,22,28,28],outline=hi+(255,),width=2)
    elif kind=='KILL':
        d.ellipse([8,6,24,22],fill=(235,235,240,255),outline=lo+(255,)); d.rectangle([12,22,20,26],fill=(235,235,240,255)); d.ellipse([11,12,14,16],fill=c+(255,)); d.ellipse([18,12,21,16],fill=c+(255,))
    elif kind=='DAMAGE':
        poly(d,[(6,6),(26,6),(26,16),(16,28),(6,16)],c); d.line([16,6,16,26],fill=hi+(255,)); d.line([6,14,26,14],fill=hi+(255,))
    elif kind=='SPAWN':
        d.ellipse([6,4,26,28],outline=hi+(255,),width=3); d.ellipse([10,8,22,24],outline=c+(255,),width=2); sparkle(d,16,16,acc)
    else: # VICTORY
        d.polygon([(8,6),(24,6),(22,16),(18,19),(18,24),(22,26),(10,26),(14,24),(14,19),(10,16)],fill=c+(255,),outline=lo+(255,)); d.arc([3,7,11,15],90,270,fill=hi+(255,)); d.arc([21,7,29,15],-90,90,fill=hi+(255,))
def g_ring(d,c,acc):
    d.ellipse([5,10,27,26],outline=mul(c,0.6)+(255,),width=3); d.ellipse([5,8,27,24],outline=c+(255,),width=3); d.ellipse([6,9,26,23],outline=mul(c,1.3)+(255,)); gem(d,14,3,acc,3); sparkle(d,15,2,(255,255,255))
def g_orbit(d,c,acc,kind):
    for i in range(3):
        a=math.radians(i*120-90); x,y=int(16+9*math.cos(a)),int(16+9*math.sin(a))
        if kind=='swords': d.line([x,y-5,x,y+5],fill=(220,225,235,255),width=2); d.line([x-3,y+2,x+3,y+2],fill=c+(255,),width=2)
        elif kind=='runes': d.rectangle([x-2,y-4,x+2,y+4],fill=c+(255,),outline=mul(c,0.5)+(255,)); d.point((x,y),fill=acc+(255,))
        else: d.polygon([(x,y),(x-5,y-4),(x-4,y+3)],fill=c+(255,)); d.polygon([(x,y),(x+5,y-4),(x+4,y+3)],fill=acc+(255,))
def g_transform(d,c,acc):
    d.ellipse([11,4,21,14],fill=mul(c,0.9)+(200,)); poly(d,[(9,15),(23,15),(26,28),(6,28)],mul(c,0.9),False)
    for r in (13,10): d.ellipse([16-r,16-r,16+r,16+r],outline=acc+(150,))
    sparkle(d,16,9,(255,255,255))
def g_nova(d,c,acc):
    for a in range(0,360,45):
        r=math.radians(a); d.line([16,16,16+14*math.cos(r),16+14*math.sin(r)],fill=c+(255,),width=2 if a%90==0 else 1)
    d.ellipse([11,11,21,21],fill=acc+(255,)); d.ellipse([13,13,19,19],fill=(255,255,255,255))

def make_icon(cd):
    """cd: dict del catalogo."""
    c=hx(cd['color']); h_,s_,v_=hsv(c)
    if v_<0.4: c=mix(c,(170,160,210),0.4)
    acc=mix(c,(255,255,255),0.55)
    im=icon_base(cd['rareza'],cd['color']); d=ImageDraw.Draw(im); i=cd['id']; cat=cd['categoria']; slot=cd['slot']
    if cat=='SOMBREROS':
        if 'corona' in i: g_crown(d,c,acc)
        elif 'mascara' in i: g_mask(d,c,acc)
        elif 'casco' in i: g_helm(d,c,acc)
        elif 'mago' in i: g_wizard(d,c,acc)
        elif 'cuernos' in i: g_horns(d,c,acc)
        elif 'tomo' in i: g_tome(d,c,acc)
        else: g_orb(d,c,acc)
    elif cat=='MASCOTAS':
        k='dragon' if 'dragon' in i else 'fox' if 'zorro' in i else 'raven' if 'cuervo' in i else 'golem' if 'golem' in i else 'owl' if 'buho' in i else 'phoenix' if 'cupido' in i else 'spirit'
        g_pet(d,c,acc,k)
    elif cat=='ALAS_CAPAS':
        if i.startswith('cape'): g_cape(d,c,acc)
        else: g_wings(d,c,acc,'demon' if any(x in i for x in('demonio','abismo','infernal')) else 'dragon' if 'dragon' in i else 'fairy' if 'rosadas' in i else 'angel')
    elif cat=='AURAS':
        th='fire' if any(x in i for x in('llama','infierno')) else 'ice' if any(x in i for x in('escarcha','ventisca','nevada')) else 'bolt' if any(x in i for x in('chispa','tormenta')) else 'nature' if any(x in i for x in('bosque','hadas')) else 'heart' if 'corazones' in i else 'star' if any(x in i for x in('estrellas','nucleo')) else 'halo' if 'halo' in i else 'magic'
        g_aura(d,c,acc,th)
    elif cat=='EFECTOS': g_effect(d,c,acc,slot)
    else:
        if slot=='RING': g_ring(d,c,acc)
        elif slot=='ORBIT': g_orbit(d,c,acc,'swords' if 'espadas' in i else 'runes' if 'runas' in i else 'bf')
        elif slot=='TRANSFORM': g_transform(d,c,acc)
        else: g_nova(d,c,acc)
    return im

# ------------------------------------------------------------------ botones de GUI (32x32)
GOLD=hx('#e0aa28'); VIOLET=hx('#7a3cff')
def btn_base(accent=GOLD):
    im=Image.new('RGBA',(S,S),(0,0,0,0)); d=ImageDraw.Draw(im)
    for y in range(S): d.line([0,y,S-1,y],fill=mix((44,26,74),(14,8,26),y/S)+(255,))
    d.rectangle([0,0,S-1,S-1],outline=mul(accent,0.5)+(255,)); d.rectangle([1,1,S-2,S-2],outline=accent+(255,))
    d.line([2,2,S-3,2],fill=mul(accent,1.4)+(255,)); d.line([2,2,2,S-3],fill=mul(accent,1.4)+(255,))
    for x,y in((0,0),(S-2,0),(0,S-2),(S-2,S-2)): d.rectangle([x,y,x+1,y+1],fill=mul(accent,1.5)+(255,))
    return im,d
def make_button(kind):
    acc={'close':hx('#ff5a5a'),'claim':hx('#6ee7a0'),'search':hx('#4aa3ff')}.get(kind,GOLD); im,d=btn_base(acc); w=(255,246,214,255); sh=mul(acc,0.5)+(255,)
    if kind=='back':  d.polygon([(21,7),(8,16),(21,25),(21,20),(26,20),(26,12),(21,12)],fill=w,outline=sh)
    elif kind=='prev': d.polygon([(20,6),(9,16),(20,26),(20,20),(23,20),(23,12),(20,12)],fill=w,outline=sh)
    elif kind=='next': d.polygon([(12,6),(23,16),(12,26),(12,20),(9,20),(9,12),(12,12)],fill=w,outline=sh)
    elif kind=='close':
        d.line([9,9,23,23],fill=w,width=4); d.line([23,9,9,23],fill=w,width=4)
    elif kind=='search': d.ellipse([7,6,20,19],outline=w,width=3); d.line([19,18,26,25],fill=w,width=4)
    elif kind=='filter': d.polygon([(7,8),(25,8),(18,17),(18,25),(14,25),(14,17)],fill=w,outline=sh)
    elif kind=='claim':
        d.rectangle([7,14,25,26],fill=w,outline=sh); d.rectangle([7,10,25,14],fill=mul(acc,1.2)+(255,),outline=sh); d.rectangle([15,10,17,26],fill=mul(acc,0.8)+(255,)); d.ellipse([10,5,16,11],outline=w,width=2); d.ellipse([16,5,22,11],outline=w,width=2)
    elif kind=='star': 
        pts=[]
        for i in range(10):
            r=11 if i%2==0 else 5; a=math.radians(-90+i*36); pts.append((16+r*math.cos(a),16+r*math.sin(a)))
        d.polygon(pts,fill=w,outline=sh)
    return im
MAINICON={'cosmeticos':('#c77dff','gem'),'pase':('#f2c14e','shield'),'misiones':('#6ad8ff','scroll'),'adviento':('#2fae5e','tree'),'febrero':('#ff6fa5','heart')}
def make_main(kind):
    col,sym=MAINICON[kind]; c=hx(col); im,d=btn_base(c); hi,lo=shade(c); w=(255,246,214,255)
    if sym=='gem': d.polygon([(16,5),(25,12),(16,27),(7,12)],fill=c+(255,),outline=lo+(255,)); d.polygon([(7,12),(25,12),(16,5)],fill=hi+(255,)); d.line([16,12,16,27],fill=lo+(255,)); sparkle(d,22,7)
    elif sym=='shield': d.polygon([(7,6),(25,6),(25,17),(16,27),(7,17)],fill=c+(255,),outline=lo+(255,)); d.polygon([(16,9),(18,14),(23,14),(19,17),(21,22),(16,19),(11,22),(13,17),(9,14),(14,14)],fill=w)
    elif sym=='scroll': d.rectangle([8,6,24,25],fill=(238,228,200,255),outline=lo+(255,)); d.ellipse([6,4,12,9],fill=c+(255,)); d.ellipse([20,22,26,27],fill=c+(255,)); [d.line([11,y,21,y],fill=lo+(255,)) for y in (11,15,19)]
    elif sym=='tree':
        for k,(y,wd) in enumerate(((7,5),(12,8),(18,11))): d.polygon([(16,y-3),(16-wd,y+6),(16+wd,y+6)],fill=mul(c,1.0-0.1*k)+(255,),outline=lo+(255,))
        d.rectangle([14,24,18,27],fill=(110,70,40,255)); sparkle(d,16,5,(255,230,120)); d.point((11,16),fill=(255,80,80,255)); d.point((21,22),fill=(255,230,120,255))
    else: g_aura(d,c,hi,'heart')
    return im

# ------------------------------------------------------------------ paneles de GUI
THEMES={'main':('#1d1033','#0c0716','#e0aa28','#7a3cff','#0a0612','#c77dff'),
        'advent':('#2c0f18','#0b1a10','#e8e8f0','#c0302a','#08100a','#2fae5e'),
        'feb':('#33122a','#150a1e','#ffd6ea','#ff6fa5','#120610','#ff9bd0')}
def panel(theme,rows):
    top,bot,gold,edge,cellc,acc=[hx(x) for x in THEMES[theme]]
    W=176; H=114+18*rows; rnd=random.Random(rows*31+len(theme))
    im=Image.new('RGBA',(256,256),(0,0,0,0)); d=ImageDraw.Draw(im); px=im.load()
    for y in range(H):
        for x in range(W):
            base=mix(top,bot,y/H); vign=1-0.35*(((x-W/2)/(W/2))**2+((y-H/2)/(H/2))**2)/2
            n=rnd.uniform(-0.025,0.025); px[x,y]=mul(base,vign*(1+n))+(255,)
    # patron de runas/rombos en el fondo (muy sutil)
    for y in range(20,H-6,12):
        for x in range(8+((y//12)%2)*6,W-8,12):
            if px[x,y][:3]==px[x,y][:3]: d.point((x,y),fill=mix(px[x,y][:3],acc,0.22)+(255,)); d.point((x+1,y),fill=mix(px[x,y][:3],acc,0.12)+(255,))
    # marco triple
    d.rectangle([0,0,W-1,H-1],outline=mul(gold,0.45)+(255,)); d.rectangle([1,1,W-2,H-2],outline=gold+(255,)); d.rectangle([2,2,W-3,H-3],outline=mul(gold,0.55)+(255,))
    d.rectangle([3,3,W-4,H-4],outline=mul(edge,0.8)+(255,))
    d.line([2,1,W-3,1],fill=mul(gold,1.35)+(255,))
    # esquinas ornamentales
    for sx,x0 in((1,3),(-1,W-4)):
        for sy,y0 in((1,3),(-1,H-4)):
            for i in range(9): d.point((x0+sx*i,y0),fill=gold+(255,)); d.point((x0,y0+sy*i),fill=gold+(255,))
            for i in range(4): d.point((x0+sx*(2+i),y0+sy*2),fill=mul(gold,0.8)+(255,)); d.point((x0+sx*2,y0+sy*(2+i)),fill=mul(gold,0.8)+(255,))
            d.rectangle([x0+sx*5-(1 if sx<0 else 0),y0+sy*5-(1 if sy<0 else 0),x0+sx*5+(1 if sx>0 else 0),y0+sy*5+(1 if sy>0 else 0)],fill=acc+(255,))
    # cabecera
    d.rectangle([5,5,W-6,16],fill=mul(top,0.45)+(255,)); d.line([5,16,W-6,16],fill=gold+(255,)); d.line([5,17,W-6,17],fill=mul(edge,0.9)+(255,))
    d.line([5,5,W-6,5],fill=mul(gold,0.6)+(255,))
    for k,x in enumerate(range(W-46,W-8,9)):
        d.polygon([(x+3,7),(x+7,10),(x+3,14),(x-1,10)],outline=gold+(255,),fill=(acc if k%2==0 else mul(top,0.4))+(255,))
    # celdas: borde sutil con luz arriba/izq
    def cell(x,y,fill,e):
        d.rectangle([x,y,x+17,y+17],fill=fill+(255,)); d.rectangle([x,y,x+17,y+17],outline=e+(255,))
        d.line([x+1,y+1,x+16,y+1],fill=mul(e,1.5)+(255,)); d.line([x+1,y+1,x+1,y+16],fill=mul(e,1.3)+(255,))
        d.line([x+1,y+17,x+17,y+17],fill=mul(fill,0.6)+(255,))
    e1=mix(edge,(0,0,0),0.55)
    for r in range(rows):
        for c in range(9): cell(7+18*c,17+18*r,cellc,e1)
    sepy=18+18*rows+3
    d.line([8,sepy,W-9,sepy],fill=mul(gold,0.9)+(255,))
    for x in range(14,W-12,22): d.polygon([(x+1,sepy-2),(x+3,sepy),(x+1,sepy+2),(x-1,sepy)],fill=acc+(255,))
    invy=85+(rows-3)*18
    e2=mix(edge,(0,0,0),0.72); f2=mix(cellc,top,0.2)
    for r in range(3):
        for c in range(9): cell(7+18*c,invy-1+18*r,f2,e2)
    for c in range(9): cell(7+18*c,invy+58-1,f2,mix(gold,(0,0,0),0.62))
    return im

def make_cat(name):
    """Iconos de categoria (pestanas del catalogo)."""
    spec={'AURAS':('#c77dff',lambda d,c,a:g_aura(d,c,a,'magic')),'SOMBREROS':('#f2c14e',g_crown),'MASCOTAS':('#e07a2f',lambda d,c,a:g_pet(d,c,a,'fox')),
          'ALAS_CAPAS':('#d8d8f0',lambda d,c,a:g_wings(d,c,a,'angel')),'EFECTOS':('#ffe28a',g_nova),'OTROS':('#ff9bf5',g_ring)}[name]
    c=hx(spec[0]); im,d=btn_base(GOLD); spec[1](d,c,mix(c,(255,255,255),0.55)); return im
