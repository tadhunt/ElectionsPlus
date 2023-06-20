/*
 * This file is part of ElectionsPlus, licensed under the MIT License.
 *
 * Copyright (c) Lorenzo0111
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
*/
package me.lorenzo0111.elections;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import me.filoghost.holographicdisplays.api.HolographicDisplaysAPI;
import me.filoghost.holographicdisplays.api.hologram.Hologram;
import me.filoghost.holographicdisplays.api.hologram.HologramLines;

public class ElectionsHologram {
    private Hologram hologram;
    private String name;

    ElectionsHologram(HolographicDisplaysAPI api, String name, Location location) {
        this.name = name;
        this.hologram = api.createHologram(location);
    }

    public String name() {
        return name;
    }

    public Location location() {
        return hologram.getPosition().toLocation();
    }

    public void set(List<String> contents) {
        HologramLines holoLines = hologram.getLines();
        for (String line : contents) {
            holoLines.appendText(line);
        }
    }
            
    public void set(String content) {
            ArrayList<String> contents = new ArrayList<String>();
            contents.add(content);

            set(contents);
    }

    public void clear() {
        hologram.getLines().clear();
    }

    public void delete() {
        hologram.delete();
    }
}
