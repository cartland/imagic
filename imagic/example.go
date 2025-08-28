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
	"github.com/cartland/imagic"
	flags "github.com/jessevdk/go-flags"
	"image"
	"log"
	"os"
	// _ "image/gif"
	_ "image/jpeg"
	"image/png"
)

var opts struct {
	DepthMapFile   string `short:"d" long:"depth" default:"borrodepth.png" description:"depth:depth map image"`
	BackgroundFile string `short:"b" long:"background" default:"Chefchaouen.jpg" description:"depth:background image texture"`
	CrossEyed      bool   `short:"c" long:"crosseyed" description:"crosseyed:create image designed for cross-eyed viewing"`
	InvertDepth    bool   `short:"i" long:"invertdepth" description:"invertdepth:invert depth map"`
	OutputFile     string `short:"o" long:"output" default:"output.png" description:"output:output file name"`
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
	reader, err := os.Open(opts.BackgroundFile)
	if err != nil {
		log.Fatal(err)
	}
	defer reader.Close()
	bg, _, err := image.Decode(reader)
	if err != nil {
		log.Fatal(err)
	}

	reader, err = os.Open(opts.DepthMapFile)
	if err != nil {
		log.Fatal(err)
	}
	defer reader.Close()
	dm, _, err := image.Decode(reader)
	if err != nil {
		log.Fatal(err)
	}

	var config imagic.Config
	width := dm.Bounds().Max.X - dm.Bounds().Min.X
	config = imagic.Config{width / 14, width / 10, opts.CrossEyed, opts.InvertDepth}

	outputImage := imagic.Imagic(dm, bg, config)
	writer, err := os.Create(opts.OutputFile)
	if err != nil {
		log.Fatal(err)
	}
	png.Encode(writer, outputImage)
}
