#!/usr/bin/env python3
"""Genera los fondos ornamentales de las GUIs (resource pack, fuente 'aetheria:gui')."""
import os, json, sys
sys.path.insert(0,os.path.dirname(os.path.abspath(__file__)))
import art
ROOT=os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACK=os.path.join(ROOT,'resourcepack/assets/aetheria'); os.makedirs(PACK+'/font',exist_ok=True); os.makedirs(PACK+'/textures/font',exist_ok=True)
providers=[{"type":"space","advances":{"\uf808":-8,"\uf8f9":-248}}]
for ti,t in enumerate(['main','advent','feb']):
    for rows in (3,4,5,6):
        name=f'menu_{t}_{rows}'
        art.panel(t,rows).save(f'{PACK}/textures/font/{name}.png')
        providers.append({"type":"bitmap","file":f"aetheria:font/{name}.png","ascent":13,"height":256,"chars":[chr(0xE100+ti*4+(rows-3))]})
json.dump({"providers":providers},open(PACK+'/font/gui.json','w'),indent=1)
print('paneles generados:',len(providers)-1)
