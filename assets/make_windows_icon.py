"""Render the original mask as a large, proportional Windows launcher icon."""
from pathlib import Path
from PIL import Image, ImageOps

root = Path(__file__).resolve().parents[1]
original = Image.open(root / '桌面特效/icon.ico').convert('RGBA')
if original.size != (154, 256):
    raise ValueError('The source crop is defined for the original 154 x 256 artwork.')
# Remove the hanging thread, small body and transparent lower canvas. Keep
# the mask's original proportions and a small margin at every output size.
mask = original.crop((24, 64, 154, 184))
mask = mask.crop(mask.getbbox())
mask = ImageOps.contain(mask, (244, 244), Image.Resampling.LANCZOS)
icon = Image.new('RGBA', (256, 256))
icon.alpha_composite(mask, ((256-mask.width)//2, (256-mask.height)//2))
icon.save(root / 'assets/app.ico', format='ICO',
          sizes=[(16,16),(24,24),(32,32),(48,48),(64,64),(128,128),(256,256)])
print('Windows mask icon:', icon.getbbox())
