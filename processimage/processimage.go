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
	"html/template"
	"net/http"
	"strconv"

	"github.com/cartland/go/imagic"
	"image"
	_ "image/gif"
	_ "image/jpeg"
	"image/png"
)

func init() {
	http.HandleFunc("/", handler)
	http.HandleFunc("/uploads", upload)
}

func handler(w http.ResponseWriter, r *http.Request) {
	c := appengine.NewContext(r)
	uploadURL := "/uploads"
	w.Header().Set("Content-Type", "text/html")
	err := rootTemplate.Execute(w, uploadURL)
	if err != nil {
		c.Errorf("%v", err)
	}
}

var rootTemplate = template.Must(template.New("root").Parse(rootTemplateHTML))

const rootTemplateHTML = `
<html><head><title>Imagic Autostereogram Generator</title></head><body>
<form action="{{.}}" method="POST" enctype="multipart/form-data">
Background File: <input type="file" name="background"><br>
Depth File: <input type="file" name="depth"><br>
Cross-eyed: <input type="checkbox" name="crossEyed"><br>
Invert depth: <input type="checkbox" name="invertDepth"><br>
Separation Min: <input type="textbox" name="separationMin" value=""><br>
Separation Max: <input type="textbox" name="separationMax" value=""><br>
<input type="submit" name="submit" value="Submit">
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
func upload(w http.ResponseWriter, r *http.Request) {
	bg := parseFirstImage("background", r)
	dm := parseFirstImage("depth", r)

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

// Grab the first image from the multipart form that matches the supplied name.
func parseFirstImage(name string, r *http.Request) image.Image {
	err := r.ParseMultipartForm(10000000) // 10^7 bytes (10MB)  max payload
	if err != nil {
		panic(err)
	}
	mf := r.MultipartForm
	if mf == nil {
		panic("Could not read multipart form")
	}
	f := mf.File
	if f == nil {
		panic("File not found in multipart form")
	}

	images := f[name]
	if len(images) == 0 {
		panic("Image not found")
	}
	reader, err := images[0].Open()
	if err != nil {
		panic("Image could not be read")
	}
	bg, _, err := image.Decode(reader)
	if err != nil {
		panic("Background image could not be decoded")
	}
	return bg
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
