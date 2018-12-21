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
package main

import (
	flags "github.com/jessevdk/go-flags"
  "github.com/cartland/imagic"
	"image"
	"image/color"
	"log"
  "math/rand"
	"os"
  "time"
	// _ "image/gif"
	_ "image/jpeg"
	"image/png"
)

// $GOBIN/palette -i Chefchaouen.jpg -o hello.png -s 50

var opts struct {
  ImageFile   string `short:"i" long:"image" default:"Chefchaouen.jpg" description:"image: input file"`
	OutputFile     string `short:"o" long:"output" default:"output.png" description:"output: output file name"`
	PaletteSize      uint `short:"s" long:"size" default:"100" description:"size: palette size"`
}

func main() {
	_, err := flags.Parse(&opts)
	if err != nil {
		if e, ok := err.(*flags.Error); ok {
			if e.Type == flags.ErrHelp {
				os.Exit(0)
			}
		}
		os.Exit(1)
	}
	reader, err := os.Open(opts.ImageFile)
	if err != nil {
		log.Fatal(err)
	}
	defer reader.Close()
	input, _, err := image.Decode(reader)
	if err != nil {
		log.Fatal(err)
	}

  // palette := make([]color.Color, 5)
  // palette[0] = color.Black
  // palette[1] = color.RGBA{255, 0, 0, 255}
  // palette[2] = color.RGBA{0, 255, 0, 255}
  // palette[3] = color.RGBA{0, 0, 255, 255}
  // palette[4] = color.RGBA{255, 255, 255, 255}

  size := opts.PaletteSize
  palette := make([]color.Color, size)
  rand.Seed(time.Now().UnixNano())
  for i := 0; i < int(size); i++ {
	  r := uint8(rand.Intn(255))
	  g := uint8(rand.Intn(255))
	  b := uint8(rand.Intn(255))
    a := uint8(255)
	  palette[i] = color.RGBA{r, g, b, a}
  }

  outputImage := imagic.ApplyPalette(input, palette)
	writer, err := os.Create(opts.OutputFile)
	png.Encode(writer, outputImage)
}
