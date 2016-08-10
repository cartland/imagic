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
package imagic

import (
	"image"
	"image/color"
)

type Config struct {
	SeparationMin, SeparationMax int
	CrossEyed                    bool
	InvertDepth                  bool
}

/**
 * Given a depth map and background image, create an autostereogram.
 */
func Imagic(dm, bg image.Image, config Config) image.Image {
	bounds := dm.Bounds()
	min := bounds.Min
	max := bounds.Max
	result := newMutableImage(dm, bg)
	if result == nil {
		return nil
	}

	for y := min.Y; y < max.Y; y++ {
		r := magicInflateRow(dm, bg, config, y)
		result.imageRows[y] = r
	}
	return result
}

func magicInflateRow(dm, bg image.Image, config Config, y int) imageRow {
	dmWidth := boundsWidth(dm.Bounds())
	bgWidth := boundsWidth(bg.Bounds())
	dmHeight := boundsHeight(dm.Bounds())
	bgHeight := boundsHeight(bg.Bounds())
	bgY := y * bgHeight / dmHeight
	var sourceIndexes = make([]int, dmWidth)

	// Find desired index of pixel to the left.
	for x := 0; x < len(sourceIndexes); x++ {
		depth := depthAt(dm, x, y, config)
		offset := sourceOffset(depth, config)
		sourceIndexes[x] = x - int(offset)
	}

	// Skip initial consecutive places that reference negative-indexed pixels.
	initialWidth := 0
	for ; sourceIndexes[initialWidth] < 0; initialWidth++ {
	}
	bgIndexStep := bgWidth / initialWidth

	// Map background onto the first section on left.
	var bgIndexes = make([]int, dmWidth)
	for x := 0; x < initialWidth; x++ {
		bgIndexes[x] = x * bgIndexStep
	}

	// For the rest, copy pixel index from left to right.
	var usedBgIndexes = make([]bool, dmWidth)
	for x := initialWidth; x < len(bgIndexes); x++ {
		if si := sourceIndexes[x]; si < 0 {
			// If the source index is negative, just use the next bg pixel.
			bgIndexes[x] = bgIndexes[x-1] + 1
		} else if usedBgIndexes[si] && !config.CrossEyed {
			// This removes some phantom artifacts for wall-eyed viewing.
			// Previous pixels that have been used for the left eye previously
			// should not be used again here. That would make this right
			// eye and the previous right eye compete for the same left pixel.
			// This means the brain can interpret two different depths,
			// depending on which pixel the right eye chooses.
			bgIndexes[x] = bgIndexes[x-1] + 1 // bgIndexStep
		} else {
			bgIndexes[x] = bgIndexes[si]
			usedBgIndexes[si] = true
		}
	}

	row := imageRow{}
	row.colors = make([]color.Color, dmWidth)
	for x := 0; x < dmWidth; x++ {
		bgX := bgIndexes[x] // TODO(cartland): Check index bounds.
		row.colors[x] = bg.At(bgX, bgY)
	}
	return row
}

func boundsWidth(bounds image.Rectangle) int {
	return bounds.Max.X - bounds.Min.X
}

func boundsHeight(bounds image.Rectangle) int {
	return bounds.Max.Y - bounds.Min.Y
}

var depthMax = uint32(3000)

func depthAt(dm image.Image, x, y int, config Config) uint32 {
	color := dm.At(x, y)
	r, g, b, a := color.RGBA()
	rgb := (r + g + b)                // [0, 3 * 0xFFFF]
	rgb = rgb / 3                     // [0, 0xFFFF]
	rgba := rgb * a / 0xFFFF          // [0, 0xFFFF]
	depth := rgba * depthMax / 0xFFFF // [0, depthMax]
	if config.InvertDepth {
		return depthMax - depth
	}
	return depth
}

func sourceOffset(depth uint32, config Config) uint32 {
	maxWidth := config.SeparationMax - config.SeparationMin
	offsetWidth := depth * uint32(maxWidth) / depthMax
	var offset uint32
	if config.CrossEyed {
		offset = uint32(config.SeparationMin) + offsetWidth
	} else {
		offset = uint32(config.SeparationMax) - offsetWidth
	}
	return offset
}

func newMutableImage(dm, bg image.Image) *mutableImage {
	if dm == nil {
		return nil
	}
	if bg == nil {
		return nil
	}

	cm := bg.ColorModel()
	bounds := dm.Bounds()
	var imageRows = make([]imageRow, bounds.Max.Y)
	image := new(mutableImage)
	image.cm = cm
	image.bounds = bounds
	image.imageRows = imageRows
	return image
}

type mutableImage struct {
	cm        color.Model
	bounds    image.Rectangle
	imageRows []imageRow
}

func (i *mutableImage) ColorModel() color.Model {
	return i.cm
}

func (i *mutableImage) Bounds() image.Rectangle {
	return i.bounds
}

func (i *mutableImage) At(x, y int) color.Color {
	return i.imageRows[y].colors[x]
}

type imageRow struct {
	colors []color.Color
}
