imagic
======

"Image Magic" autostereogram generator.

Go Import
=========

```
import (
    "github.com/cartland/imagic"
)
```

Hosted Service
==============

Try it online at https://imagic-golang.appspot.com.

Source in `/appengine`.

Command Line
============

```
go get github.com/cartland/imagic/imagic
imagic -d borrodepth.png -b Chefchaouen.jpg -o output.png [-c] [-i]
```

Source in `/imagic`.

![Output Autostereogram](imagic/output.png "Autostereogram")
![Input Depth Map](imagic/borrodepth.png "Depth Map")
![Input Background](imagic/Chefchaouen.jpg "Chefchaouen")

Depth map image borrowed from http://www.imsc.res.in/~kapil/geometry/borr/borrodepth.png.
