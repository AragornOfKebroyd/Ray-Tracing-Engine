import math
import numpy as np

def sph_to_c(r,t,p):
    r = r -0.005
    x = r * math.sin(t) * math.cos(p)
    y = r * math.cos(t)
    z = r * math.sin(t) * math.sin(p)
    return (x,y,z)

def phi_to_theta(p):
    pi = math.pi
    return (((p - pi*(3/2))**2) * (-3)/pi)+(8/12)*pi

def smile():
    phi = math.pi * np.arange(4/3,5/3,1/18)
    theta = phi_to_theta(phi)
    coords = [sph_to_c(0.16,theta[i],phi[i]) for i in range(len(phi))]
    for coord in coords:
        print(f'<sphere x="{round(coord[0]-0.45,5)}" y="{round(coord[1]+0.3,5)}" z="{round(coord[2]+1.45,5)}" colour="#090909" reflectivity="0.02" radius="0.014" alphaS="30"/>')

smile()