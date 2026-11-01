"""Enlarge the complete original artwork, removing transparent border only."""
from pathlib import Path
from PIL import Image, ImageOps

root = Path(__file__).resolve().parents[1]
original = Image.open(root / '桌面特效/icon.ico').convert('RGBA')
bounds = original.getchannel('A').getbbox()
if not bounds:
    raise ValueError('Original icon contains no visible artwork.')
# Preserve every visible part: thread, body and mask. Never crop the subject.
artwork = original.crop(bounds)
artwork = ImageOps.contain(artwork, (248, 248), Image.Resampling.LANCZOS)
icon = Image.new('RGBA', (256, 256))
icon.alpha_composite(artwork, ((256-artwork.width)//2, (256-artwork.height)//2))
icon.save(root / 'assets/app.ico', format='ICO',
          sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
print('Complete original bounds:', bounds, 'scaled artwork:', artwork.size)
