#!/usr/bin/env python3

import markdown
# Requires markdown (https://python-markdown.github.io/)
#
# Converts the help markdown to HTML and injects the help/style.css
# Used to provide offline help on the application
import os

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

OUT_DIR = './src/app/src/main/res/raw/'

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

    with open(os.path.join('help', md), encoding='utf-8') as f:
        html = markdown.markdown(f.read())
    name_html.append(
        (os.path.splitext(md)[0] + '.html', template.format(style, html))
    )

# Save the HTML files to the .../res/raw/ directory
print('Saving files...')
os.makedirs(OUT_DIR, exist_ok=True)
for name, html in name_html:
    with open(os.path.join(OUT_DIR, name), 'w', encoding='utf-8') as f:
        f.write(html)
