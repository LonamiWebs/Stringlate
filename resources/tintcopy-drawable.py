#!/usr/bin/env python3

import os
# Requires imagemagick's convert
#
# Tints the drawables under 'drawable' by changing their white color
# with the used accent color on the application, and outputs the result
# to the 'drawable' folder used by the application. Run this if you add
# new drawables on the folder. Icons from https://material.io/icons
import re
import subprocess

# http://www.imagemagick.org/Usage/color_mods/#diy_levels
command = 'convert "{}" xc:"{}" -fx "u*v.p{{0,0}}" "{}"'
outdir = '../src/app/src/main/res/'

# Find accent color
color = None
print('Looking for accent color...')
with open('../src/app/src/main/res/values/colors.xml', encoding='utf-8') as f:
    for line in f:
        m = re.search('name="colorAccent">(#[A-F\d]+)<', line)
        if m:
            color = m.group(1)
            break

if not color:
    raise ValueError('colorAccent not found')

# Convert the files
print('Tinting drawables...')
for dpi in os.listdir('drawable'):
    for png in os.listdir(os.path.join('drawable', dpi)):
        ipng = os.path.join('drawable', dpi, png)
        opng = os.path.join(outdir, dpi, png.replace('_white', ''))
        subprocess.run(command.format(ipng, color, opng), shell=True)
