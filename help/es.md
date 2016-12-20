# Ayuda online para Stringlate
- [Soy nuevo en GitHub. ¿Dónde empiezo?](#soy-nuevo-en-github-dónde-empiezo)
- [¿Cómo le digo a los desarrolladores que he traducido su app?](#cómo-le-digo-a-los-desarrolladores-que-he-traducido-su-app)
- [¿Necesito esta app para ayudar a los desarrolladores?](#necesito-esta-app-para-ayudar-a-los-desarrolladores)
- [¿Cómo cargo mi trabajo previo?](#cómo-cargo-mi-trabajo-previo)
- [¿Qué significa `%s` ó `%1$d/%2$d`?](#qué-significa-s-ó-1d2d)
- [¡Usaré esta app para causar el caos!](#usaré-esta-app-para-causar-el-caos)

## Soy nuevo en GitHub. ¿Dónde empiezo?
Si eres nuevo en GitHub y aún así quieres ayudar a la comunidad de software
libre, ¡eso es genial! Has tomado una gran decisión. El primer paso es conseguir
tu propia [cuenta en GitHub](https://github.com/join) (¡es gratis!). Necesitas
dar este paso para luego poder contarle a los desarrolladores que has traducido
su aplicación.

Una vez hayas hecho esto, descarga e instala *Stringlate*, **añade** el
repositorio de tu aplicación favorita y **empieza a traducir** todo el
texto (*strings*, cadenas en español) a tu propio idioma.

## ¿Cómo le digo a los desarrolladores que he traducido su app?
¿Has terminado? ¡Qué velocidad! Desde *Stringlate*, puedes exportar el resultado
de tu duro trabajo tocando en `Menú > Exportar…`. Puedes exportar el archivo
resultante a la *SD card*, a un *Gist en GitHub*, o simplemente compartiendo
el contenido del archivo de cualquier otra manera (incluso *email*).

Una vez hayas hecho esto, obtén el archivo o URL resultante de dónde lo hayas
exportado y dirígite a la página de *incidencias*
([issues](https://github.com/LonamiWebs/Stringlate/issues)) del repositorio
que hayas elegido. Haz clic en el botón verde de *Nueva incidencia*
(`New issue`, necesitas haber iniciado sesión), y titula tu nueva incidencia
algo como *"Añadida traducción para XXX"* (*"Added XXX translation"* en inglés).
Proporciona algún enlace u otra manera para que el autor consiga tu traducción,
¡y ya has terminado!

Ten en cuenta que es posible que el autor tenga otra manera de recibir las
traducciones para su aplicación (por ejemplo, es posible que usen otra
[plataforma online](https://www.transifex.com/) diferente). De todos modos,
no te rindas si cierran tu incidencia contándote esto. ¡Toda ayuda es poca!

## ¿Necesito esta app para ayudar a los desarrolladores?
Absolutamente **no**. Hice esta aplicación para que el prceso fuera más fácil,
pero no es la única manera de hacerlo. Puedes ir a cualquier repositorio,
por ejemplo, [este](https://github.com/LonamiWebs/Stringlate) repositorio,
pulsar la tecla <kbd>T</kbd> (para buscar un archivo) y teclear `strings.xml`.
Esto encontrará todas las *strings* en ese repositorio.

El archivo `res/values/strings.xml` es el **archivo original**, que contiene
todas las *cadenas*. Los archivos `res/values-xx/strings.xml` son
**otros idiomas** (por ejemplo, `es` para *Español*) que otra persona ya ha
traducido.

Haz clic en el *archivo original*, elige `Raw` y guárdalo en tu ordenador.

Una vez hayas hecho esto, renombra el archivo a, por ejemplo, `strings-xx.xml`.
Ábrelo en tu editor de texto preferido y empieza a traducir todas las cadenas
que *no* contengan `translatable="false"`. Si alguna *etiqueta* (*tag*)
contiene `translatable="false"`, bórrala. Esa no se puede traducir.

Una vez hayas terminado, dile a los desarrolladores que tienes una
nueva traducción disponible, tal y como he explicado más arriba.

## ¿Cómo cargo mi trabajo previo?
La primera vez que añades un repositorio, los archivos `strings.xml` que
contiene son descargados a la **memoria interna** de tu dispositivo, en el
directorio de la aplicación (a menos que seas un usuario root, no te darás
cuenta de esto).

Si **limpias los datos de la aplicación**, estos archivos **desaparecerán**.
¡Asegúrate de que no tenías ninguna traducción pendiente antes de hacer esto
o de desinstalar!

Cada vez que abres un repositorio que ya has guardado, estos archivos se
**cargan automáticamente**, sin necesidad de que hagas nada más.

Cuando editas una traducción, estos cambios se conserva en la memoria RAM del
teléfono. Para que **persistan**, asegúrate de que tocas en el botón de
**Guardar**. La próxima vez que abras la aplicación, verás los cambios.

## ¿Qué significa `%s` ó `%1$d/%2$d`?
`%s` se usa para "insertar" otra **s**tring en esa posición. Por ejemplo,
imagina que quieres darle la bienvenida a tus usuarios con  "*¡Hola Usuario!*".
*Usuario* sería un valor que puede cambiar, por lo que escribiríamos
"*¡Hola %s!*" y el desarrollador lo cambiará por el valor correcto.

La sintaxis `%1$d`, aunque algo más compleja, simplemente indica la posición
en la que insertar un número **d**ecimal. Por ejemplo, al mostrar el progreso
"*42 de 100*", escribirías "*`%1$d` de `%2$d`*", ya que en algunos idiomas
el orden puede cambiar, y por eso se necesita saber la posición.

## ¡Usaré esta app para causar el caos!
Por favor **no lo hagas**. Los desarrolladores de aplicaciones son gente como
tú, con buenas intenciones y a menudo vidas ocupadas. No les hagas perder el
tiempo (e incluso usuarios) con traducciones incorrectas, incompletas,
inválidas o incluso ofensivas.

Es imposible prevenir que este tipo de cosas pasen desde la aplicación, o
incluso si *Stringlate* no existiera. Los desarrolladores confían en el buen
hacer de la gente que les ayuda. Si eres un troll, por favor no malgastes
tu tiempo en esto. Hay miles de sitios web donde puedes ir a divertirte en
vez de aquí.
