/*
 * Copyright 2014 Chris Cartland
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package processimage

import (
	"appengine"
	"appengine/urlfetch"
	"html/template"
	"net/http"
	"strconv"

	"github.com/cartland/go/imagic"
	"github.com/mjibson/appstats"
	"image"
	_ "image/gif"
	_ "image/jpeg"
	"image/png"
)

func init() {
	http.Handle("/", appstats.NewHandler(handler))
	http.Handle("/generate", appstats.NewHandler(generate))
}

func handler(c appengine.Context, w http.ResponseWriter, r *http.Request) {
	generateURL := "/generate"
	w.Header().Set("Content-Type", "text/html")
	err := rootTemplate.Execute(w, generateURL)
	if err != nil {
		c.Errorf("%v", err)
	}
}

var rootTemplate = template.Must(template.New("root").Parse(rootTemplateHTML))

const rootTemplateHTML = `
<html><head><title>Imagic Autostereogram Generator</title></head><body>
<form action="{{.}}" method="GET" enctype="multipart/form-data">
Background URL: <input type="textbox" name="background" value="http://www.chriscartland.com/static/Chefchaouen.jpg"><br>
Depth URL: <input type="textbox" name="depth" value="http://www.imsc.res.in/~kapil/geometry/borr/borrodepth.png"><br>
Invert depth: <input type="checkbox" name="invertDepth"><br>
Cross-eyed: <input type="checkbox" name="crossEyed"><br>
Separation Min: <input type="textbox" name="separationMin" value=""><br>
Separation Max: <input type="textbox" name="separationMax" value=""><br>
<input type="submit" name="submit" value="Generate">
</form>
</body></html>
`

// Upload two images and return a PNG autostereogram.
//
// "background" image contains a PNG/GIF/JPEG full of color and contrast.
// "depth" image contains a PNG/GIF/JPEG greyscale depth map.
//     White describes "high" points and black describes "low" points.
// "crossEyed" bool determines whether to optimize for cross-eyed viewing.
// "separationMin" int the minimum eye separation distance.
// "separationMax" int the maximum eye separation distance.
//
// returns a PNG image.
func generate(c appengine.Context, w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "image/png")
	bgc := make(chan image.Image)
	dmc := make(chan image.Image)

	go func() {
		bg, err := getImage(c, "background", r)
		if err != nil {
			bgc <- nil
		}
		bgc <- bg
	}()
	go func() {
		dm, err := getImage(c, "depth", r)
		if err != nil {
			dmc <- nil
		}
		dmc <- dm
	}()

	dm := <-dmc
	bg := <-bgc

	if dm == nil {
		http.Error(w, "No depth map found", http.StatusNotFound)
	}
	if bg == nil {
		http.Error(w, "No background image found", http.StatusNotFound)
	}

	var config imagic.Config
	width := dm.Bounds().Max.X - dm.Bounds().Min.X
	config = imagic.Config{width / 14, width / 10, false, false}

	config = updateConfig(&config, r)

	outputImage := imagic.Imagic(dm, bg, config)
	err := png.Encode(w, outputImage)
	if err != nil {
		panic(err)
	}
}

func getImage(c appengine.Context, name string, r *http.Request) (image.Image, error) {
	img, err := parseFirstImage(c, name, r)
	if err != nil {
		img, err := lookupImage(c, name, r)
		return img, err
	}
	return img, err
}

// Grab the first image from the multipart form that matches the supplied name.
func parseFirstImage(c appengine.Context, name string, r *http.Request) (image.Image, error) {
	err := r.ParseMultipartForm(10000000) // 10^7 bytes (10MB)  max payload
	if err != nil {
		return nil, err
	}
	mf := r.MultipartForm
	if mf == nil {
		return nil, appError{"Could not read multipart form"}
	}
	f := mf.File
	if f == nil {
		return nil, appError{"File not found in multipart form"}
	}

	images := f[name]
	if len(images) == 0 {
		return nil, appError{"Image not found"}
	}
	reader, err := images[0].Open()
	if err != nil {
		return nil, appError{"Image could not be read"}
	}
	im, _, err := image.Decode(reader)
	if err != nil {
		return nil, err
	}
	return im, nil
}

func lookupImage(c appengine.Context, name string, r *http.Request) (image.Image, error) {
	err := r.ParseForm()
	if err != nil {
		return nil, err
	}
	var vs []string
	vs = r.Form[name]
	for i := range vs {
		s := vs[i]
		client := urlfetch.Client(c)
		resp, err := client.Get(s)
		if err != nil {
			return nil, appError{"Could not fetch URL"}
		}
		reader := resp.Body
		im, _, err := image.Decode(reader)
		if err == nil {
			return im, nil
		}
	}
	return nil, appError{"Could not find image URL"}
}

// Update configuration based on URL parameters.
func updateConfig(config *imagic.Config, r *http.Request) imagic.Config {
	err := r.ParseForm()
	if err != nil {
		panic("Cannot parse form for parameters")
	}
	var vs []string
	vs = r.Form["separationMin"]
	if len(vs) > 0 {
		i, err := strconv.ParseInt(vs[0], 0, 0)
		if err == nil && i > 0 {
			config.SeparationMin = int(i)
		}
	}
	vs = r.Form["separationMax"]
	if len(vs) > 0 {
		i, err := strconv.ParseInt(vs[0], 0, 0)
		if err == nil && i > 0 {
			config.SeparationMax = int(i)
		}
	}
	vs = r.Form["crossEyed"]
	if len(vs) > 0 {
		s := vs[0]
		if len(s) > 0 {
			config.CrossEyed = true
		}
	}
	vs = r.Form["invertDepth"]
	if len(vs) > 0 {
		s := vs[0]
		if len(s) > 0 {
			config.InvertDepth = true
		}
	}
	return *config
}

type appError struct {
	err string
}

func (e appError) Error() string {
	return e.err
}
