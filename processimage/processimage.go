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
<html><body>
<form action="{{.}}" method="POST" enctype="multipart/form-data">
Background File: <input type="file" name="background"><br>
Depth File: <input type="file" name="depth"><br>
Cross-eyed: <input type="checkbox" name="crossEyed"><br>
Separation Min: <input type="textbox" name="separationMin" value="100"><br>
Separation Max: <input type="textbox" name="separationMax" value="160"><br>
<input type="submit" name="submit" value="Submit">
</form>
</body></html>
`

func upload(w http.ResponseWriter, r *http.Request) {
	err := r.ParseMultipartForm(500000)
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

	backgrounds := f["background"]
	if len(backgrounds) == 0 {
		panic("Background not found")
	}
	background := backgrounds[0]
	reader, err := background.Open()
	if err != nil {
		panic("Background could not be read")
	}
	bg, _, err := image.Decode(reader)
	if err != nil {
		panic("Background image could not be decoded")
	}

	depths := f["depth"]
	if len(depths) == 0 {
		panic("Depth map not found")
	}
	depth := depths[0]
	reader, err = depth.Open()
	if err != nil {
		panic("Depth map could not be read")
	}
	dm, _, err := image.Decode(reader)
	if err != nil {
		panic("Depth map image could not be decoded")
	}

	var config imagic.Config
	config = imagic.Config{60, 100, true}

	err = r.ParseForm() // Ignore error and use defaults.
	if err == nil {
		var vs []string
		vs = r.Form["separationMin"]
		if len(vs) > 0 {
			i, err := strconv.ParseInt(vs[0], 0, 0)
			if err == nil {
				config.SeparationMin = int(i)
			}
		}
		vs = r.Form["separationMax"]
		if len(vs) > 0 {
			i, err := strconv.ParseInt(vs[0], 0, 0)
			if err == nil {
				config.SeparationMax = int(i)
			}
		}
		vs = r.Form["crossEyed"]
		if len(vs) > 0 {
			b, err := strconv.ParseBool(vs[0])
			if err == nil {
				config.CrossEyed = b
			}
		}
	}

	outputImage := imagic.Imagic(dm, bg, config)
	err = png.Encode(w, outputImage)
	if err != nil {
		panic(err)
	}
}
