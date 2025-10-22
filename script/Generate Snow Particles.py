import numpy as np
R = 1.5
(x0,y0,z0)=(0,-0.2,1.45)
def generateRandomCoordThatIsValid():
    while True:
        (x,y,z) = np.random.uniform(-1,1,3)
        if (x**2 + y**2 + z**2 < 1) and (y > -0.2):
            break

    (x,y,z) = (R*x+x0,R*y+y0,R*z+z0)
    
    return (x, y, z)


N=75
for i in range(N):
    (x,y,z) = generateRandomCoordThatIsValid()
    radius=np.random.uniform(0.02,0.04)
    print(f'<sphere x="{round(x,5)}" y="{round(y,5)}" z="{round(z,5)}" radius="{round(radius,5)}" colour="#fffafa" reflectivity="0.2" tr="0.82" tg="0.82" tb="0.82" refractive_index="1.31"/>')