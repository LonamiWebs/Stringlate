#!/usr/bin/env python3

# Requires markdown (http://daringfireball.net/projects/markdown/)
#
# Converts the help markdown to HTML and injects the help/style.css
# Used to provide offline help on the application
import os
import subprocess

template = \
'''
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <style>{}</style>
</head>
<body>
    <article>{}</article>
</body>
</html>
'''
outdir = './src/app/src/main/res/raw/'


# Load the `help/style.css` to a string
print('Loading style...')
with open('help/style.css', encoding='utf-8') as f:
    style = f.read()


# Convert the .md files to HTML
print('Converting .md to .html...')
name_html = []
for md in os.listdir('help'):
    if 'index' in md or not md.endswith('.md'):
        continue
    
    imd = os.path.join('help', md)
    html = subprocess.check_output('markdown {}'.format(imd), shell=True)
    name_html.append((os.path.splitext(md)[0]+'.html',
                      template.format(style, str(html, encoding='utf-8'))))


# Save the HTML files to the .../res/raw/ directory
print('Saving files...')
os.makedirs(outdir, exist_ok=True)
for name, html in name_html:
    with open(os.path.join(outdir, name), mode='w') as f:
        f.write(html)
