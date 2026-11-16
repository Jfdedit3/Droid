from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter
import math

width,height=1600,900
image=Image.new('RGB',(width,height))
pixels=image.load()
for y in range(height):
    for x in range(width):
        u,v=x/width,y/height
        glow=math.exp(-((u-.73)**2/.19+(v-.47)**2/.27))
        pixels[x,y]=(int(6+9*glow+2*u),int(24+60*glow+10*u),int(57+126*glow+18*u))
beam=Image.new('RGBA',image.size)
draw=ImageDraw.Draw(beam)
draw.polygon([(0,130),(1020,305),(1020,595),(0,880)],fill=(30,126,240,30))
draw.polygon([(0,405),(1020,414),(1020,538),(0,626)],fill=(60,157,255,30))
image=Image.alpha_composite(image.convert('RGBA'),beam.filter(ImageFilter.GaussianBlur(28)))
polygons=[[(985,315),(1166,293),(1166,440),(985,438)],[(1186,291),(1417,264),(1417,444),(1186,440)],
          [(985,459),(1166,462),(1166,611),(985,580)],[(1186,463),(1417,469),(1417,657),(1186,615)]]
glow=Image.new('RGBA',image.size);d=ImageDraw.Draw(glow)
for points in polygons:d.polygon(points,fill=(57,162,255,150))
image=Image.alpha_composite(image,glow.filter(ImageFilter.GaussianBlur(35)))
panes=Image.new('RGBA',image.size);d=ImageDraw.Draw(panes)
for points in polygons:
    d.polygon(points,fill=(67,158,245,245));d.line(points+[points[0]],fill=(139,213,255,210),width=2)
image=Image.alpha_composite(image,panes)
output=Path(__file__).resolve().parents[1]/'linuxfs/overlay/usr/local/share/droiddeck/windows-wallpaper.png'
image.convert('RGB').save(output,optimize=True)
print(output)
