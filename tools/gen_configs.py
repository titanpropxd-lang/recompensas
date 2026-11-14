#!/usr/bin/env python3
"""Genera crates.yml, battlepass.yml, advent.yml, february.yml y quests.yml iniciales (editables)."""
import yaml, os, random
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__))); RES=os.path.join(ROOT,'src/main/resources')
cat=yaml.safe_load(open(os.path.join(RES,'catalog.yml'),encoding='utf-8'))['cosmetics']
def cos(i): assert i in cat,i; return dict(type='COSMETIC',cosmetic=i,amount=1)
def coin(n): return dict(type='CORONA',amount=n)
def key(c,n=1): return dict(type='KEY',crate=c,amount=n)
def xp(n): return dict(type='XP',amount=n)
def dump(name,obj,header=''):
    open(os.path.join(RES,name),'w',encoding='utf-8').write(header+yaml.safe_dump(obj,allow_unicode=True,sort_keys=False,width=200))
byrar=lambda r,pred=lambda c:True:[i for i,c in cat.items() if c['rareza']==r and c['tier']=='PREMIUM' and not c.get('evento') and pred(c)]
R={r:byrar(r) for r in ('RARE','EPIC','LEGENDARY','MYTHIC')}
random.seed(3)
def ent(w,rar,reward): return dict(weight=w,rarity=rar,reward=reward)
CR={
 'madera':('§6Cofre de Madera','COMMON','Llave de Madera','BARREL',[ent(60,'COMMON',coin(5)),ent(25,'COMMON',coin(15)),ent(10,'RARE',key('hierro')),ent(4,'RARE',cos(random.choice(R['RARE']))),ent(1,'EPIC',cos(random.choice(R['EPIC'])))]),
 'hierro':('§fCofre de Hierro','RARE','Llave de Hierro','IRON_BLOCK',[ent(50,'COMMON',coin(20)),ent(25,'RARE',coin(50)),ent(12,'RARE',key('dorado')),ent(10,'RARE',cos(random.choice(R['RARE']))),ent(3,'EPIC',cos(random.choice(R['EPIC'])))]),
 'dorado':('§eCofre Dorado','EPIC','Llave Dorada','GOLD_BLOCK',[ent(45,'RARE',coin(50)),ent(25,'EPIC',coin(120)),ent(12,'EPIC',key('magico')),ent(12,'EPIC',cos(random.choice(R['EPIC']))),ent(6,'LEGENDARY',cos(random.choice(R['LEGENDARY'])))]),
 'magico':('§5Cofre Mágico','EPIC','Llave Mágica','AMETHYST_BLOCK',[ent(40,'EPIC',coin(150)),ent(25,'EPIC',cos(random.choice(R['EPIC']))),ent(15,'EPIC',cos(random.choice(R['EPIC']))),ent(12,'LEGENDARY',cos(random.choice(R['LEGENDARY']))),ent(8,'LEGENDARY',key('legendario'))]),
 'legendario':('§6§lCofre Legendario','LEGENDARY','Llave Legendaria','BEACON',[ent(40,'EPIC',coin(300)),ent(30,'LEGENDARY',cos(random.choice(R['LEGENDARY']))),ent(15,'LEGENDARY',cos(random.choice(R['LEGENDARY']))),ent(10,'MYTHIC',cos(random.choice(R['MYTHIC']))),ent(5,'MYTHIC',coin(1000))]),
 'oscuro':('§8Cofre Oscuro','LEGENDARY','Llave Oscura','OBSIDIAN',[ent(35,'EPIC',coin(200)),ent(25,'EPIC',cos('hat_mascara_sombra')),ent(20,'LEGENDARY',cos('wing_abismo')),ent(12,'LEGENDARY',cos('aura_vacio')),ent(8,'MYTHIC',cos('wing_infernal'))]),
 'evento':('§cCofre de Evento','EPIC','Llave de Evento','RED_SHULKER_BOX',[ent(50,'RARE',coin(60)),ent(20,'EPIC',cos('aura_nevada')),ent(15,'EPIC',cos('hat_mago_invierno')),ent(10,'LEGENDARY',cos('pet_zorro_reno')),ent(5,'LEGENDARY',cos('hat_corona_hielo'))]),
}
keycmd={'madera':3001,'hierro':3002,'dorado':3003,'magico':3004,'legendario':3005,'oscuro':3006,'evento':3007}
crates={}
for k,(name,rar,kn,fb,ents) in CR.items():
    crates[k]=dict(name=name,rarity=rar,model='crate_'+k,scale=1.0,**{'fallback-block':fb},key=dict(name=kn,cmd=keycmd[k],material='AMETHYST_SHARD'),
                   rewards={f'r{i}':e for i,e in enumerate(ents)})
dump('crates.yml',dict(crates=crates),"# Crates. Edita aqui o desde /aeadmin menu. weight = peso relativo (probabilidad = weight / suma).\n")
# ---- battle pass
lv={}
pool_free=[i for i,c in cat.items() if c['tier']=='FREE' and c['categoria'] in('EFECTOS','AURAS','SOMBREROS','MASCOTAS','ALAS_CAPAS')]
prem=R['RARE']+R['EPIC']+R['LEGENDARY']; random.shuffle(prem)
for l in range(1,31):
    f=coin(10+l*2)
    if l%5==0: f=key('madera',1)
    if l==10: f=cos('fx_land_impacto') if cat['fx_land_impacto']['tier']=='FREE' else f
    p=coin(40+l*5) if l%3 else key(['hierro','dorado','magico'][(l//3)%3],1)
    if l%5==0 and prem: p=cos(prem.pop())
    if l==30: p=cos(R['MYTHIC'][0])
    lv[str(l)]=dict(free=f,premium=p)
dump('battlepass.yml',{'premium-permission':'rewards.premium','season':{'id':'s1','name':'Temporada 1: El Reino Despierta','start':'2026-12-01T00:00','end':'2027-03-01T00:00'},
  'max-level':30,'xp-base':500,'xp-increment':100,'levels':lv},"# Pase de batalla. Fechas en la zona horaria de config.yml. Premium = permiso 'premium-permission'.\n")
# ---- advent
days={}
adv_cos=['aura_nevada','hat_mago_invierno','pet_zorro_reno','hat_corona_hielo']
special={6:cos('hat_mago_invierno'),12:cos('aura_nevada'),18:cos('pet_zorro_reno'),24:cos('hat_corona_hielo'),
         8:key('evento'),16:key('evento'),20:key('evento',2),22:key('magico'),
         3:coin(25),9:coin(40),14:coin(75),21:coin(150)}
for d in range(1,25):
    days[str(d)]=special.get(d, coin(10+d) if d%2 else xp(100+d*5))
dump('advent.yml',{'year':2026,'claim-past-days':True,'end-claim':'2026-12-31T23:59','days':days},
 "# Calendario de adviento (1-24 diciembre). claim-past-days: si true se pueden reclamar dias anteriores.\n# Cada dia admite: CORONA, XP, KEY, COSMETIC, ITEM, COMMAND.\n")
# ---- february
tiers={'1':coin(50),'2':cos('fx_walk_corazones'),'3':key('evento'),'5':cos('aura_corazones'),'7':cos('wing_rosadas'),'9':cos('pet_cupido'),'12':cos('hat_corona_amor')}
dump('february.yml',{'start':'2027-02-01T00:00','end':'2027-03-01T00:00','tiers':tiers},"# Evento de febrero 2027. tiers: puntos -> recompensa. Puntos = misiones FEBRERO reclamadas.\n")
# ---- quests
Q={}
def q(i,scope,typ,amt,name,desc,rw): Q[i]=dict(scope=scope,type=typ,name=name,desc=desc,amount=amt,reward=rw)
q('d_mobs','DAILY','KILL_MOB',30,'Cazador del día','Derrota 30 monstruos.',xp(150))
q('d_mine','DAILY','BREAK_BLOCK',200,'Minero diligente','Rompe 200 bloques.',xp(100))
q('d_fish','DAILY','FISH',5,'Pesca tranquila','Pesca 5 peces.',xp(100))
q('w_mobs','WEEKLY','KILL_MOB',250,'Azote de monstruos','Derrota 250 monstruos.',xp(600))
q('w_crate','WEEKLY','OPEN_CRATE',3,'Cazatesoros','Abre 3 cofres.',dict(type='KEY',crate='hierro',amount=1))
q('w_chest','WEEKLY','FIND_CHEST',1,'Explorador','Descubre un cofre oculto.',xp(400))
q('s_mobs','SEASON','KILL_MOB',2000,'Leyenda de la temporada','Derrota 2000 monstruos.',dict(type='COSMETIC',cosmetic=R['EPIC'][0],amount=1))
q('s_crate','SEASON','OPEN_CRATE',25,'Maestro de cofres','Abre 25 cofres.',key('legendario'))
q('f_hearts','FEBRERO','KILL_MOB',50,'Valor y amor','Derrota 50 monstruos.',coin(30))
q('f_fish','FEBRERO','FISH',10,'Pesca romántica','Pesca 10 peces.',coin(30))
q('f_chest','FEBRERO','FIND_CHEST',1,'Tesoro del corazón','Encuentra un cofre oculto.',coin(40))
q('f_crate','FEBRERO','OPEN_CRATE',2,'Regalos de amistad','Abre 2 cofres.',coin(40))
for i,v in Q.items():
    v['reward']=v['reward']
dump('quests.yml',{'quests':Q},"# Misiones. scope: DAILY, WEEKLY, SEASON, FEBRERO. type: KILL_MOB KILL_PLAYER BREAK_BLOCK FISH OPEN_CRATE FIND_CHEST\n")
print('ok')
