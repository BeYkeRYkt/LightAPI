/*
 * The MIT License (MIT)
 *
 * Copyright 2021 Qveshn
 * Copyright 2021 Vladimir Mikhailov <beykerykt@gmail.com>
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
package ru.beykerykt.minecraft.lightapi.bukkit.internal.utils;

import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtil {

    public static String serverVersion() {
        return Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
    }

    public static String serverName() {
        return Bukkit.getVersion().split("-")[1];
    }

    public static String bukkitName() {
        return Bukkit.getName();
    }

    public static String bukkitVersion() {
        return (Bukkit.getBukkitVersion() + "-").split("-")[0];
    }

    private static String paddedVersion(String value) {
        return VersionUtil.leftPad(value, "\\d+", '0', 8);
    }

    private static String paddedBukkitVersion() {
        return paddedVersion(bukkitVersion());
    }

    public static int compareBukkitVersionTo(String version) {
        return paddedBukkitVersion().compareTo(paddedVersion(version));
    }

    private static String leftPad(String text, String regex, char padCharacter, int width) {
        StringBuilder sb = new StringBuilder();
        Matcher m = Pattern.compile(regex).matcher(text);
        String chars = String.format("%" + width + "s", "").replace(' ', padCharacter);
        int last = 0;
        while (m.find()) {
            int start = m.start();
            int n = m.end() - start;
            if (n < width) {
                sb.append(text, last, start);
                sb.append(chars, n, width);
                last = start;
            }
        }
        if (last == 0) {
            return text;
        }
        if (last < text.length()) {
            sb.append(text, last, text.length());
        }
        return sb.toString();
    }
}
