#!/usr/bin/env python3
"""
Skript Chart to JSON Converter for CubeRhythm
Converts .sk format charts to the new JSON format
"""

import re
import json
import os
from pathlib import Path

def parse_properties(properties_file):
    """Parse the properties file to extract metadata"""
    metadata = {
        "id": "",
        "title": "",
        "artist": "",
        "charter": "",
        "difficulty": {"name": "", "level": 1, "color": "WHITE"},
        "audio": "",
        "duration": 60,
        "offset": 0,
        "bpm": 120
    }

    with open(properties_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract options
    chart_match = re.search(r'谱面:\s*(\S+)', content)
    title_match = re.search(r'曲名:\s*(.+)', content)
    artist_match = re.search(r'曲师:\s*(.+)', content)
    charter_match = re.search(r'谱师:\s*(.+)', content)
    difficulty_match = re.search(r'难度:\s*(.+)', content)
    duration_match = re.search(r'时长:\s*(\d+)', content)
    offset_match = re.search(r'偏移:\s*(-?\d+)', content)
    bpm_match = re.search(r'BPM:\s*(\d+)', content)

    if chart_match:
        metadata["id"] = chart_match.group(1).strip()
    if title_match:
        metadata["title"] = title_match.group(1).strip()
    if artist_match:
        metadata["artist"] = artist_match.group(1).strip()
    if charter_match:
        metadata["charter"] = charter_match.group(1).strip()
    if difficulty_match:
        diff_text = difficulty_match.group(1).strip()
        # Parse color code and name
        if '&' in diff_text:
            parts = diff_text.split()
            metadata["difficulty"]["name"] = ' '.join(parts[1:]) if len(parts) > 1 else "Normal"
            color_code = parts[0] if parts else "&f"
            color_map = {
                '&b': 'AQUA', '&a': 'GREEN', '&e': 'YELLOW',
                '&6': 'GOLD', '&c': 'RED', '&d': 'LIGHT_PURPLE',
                '&f': 'WHITE', '&7': 'GRAY'
            }
            metadata["difficulty"]["color"] = color_map.get(color_code, 'WHITE')
        else:
            metadata["difficulty"]["name"] = diff_text
    if duration_match:
        metadata["duration"] = int(duration_match.group(1))
    if offset_match:
        metadata["offset"] = int(offset_match.group(1))
    if bpm_match:
        metadata["bpm"] = int(bpm_match.group(1))

    return metadata

def parse_chart_file(chart_file):
    """Parse the chart file to extract notes"""
    notes = []

    with open(chart_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # Extract audio from options
    audio_match = re.search(r'音频:\s*(\S+)', content)
    audio = audio_match.group(1).strip() if audio_match else ""

    # Find all note function calls
    # tap(time, face, x, y, glowing, tag)
    tap_pattern = r'tap\(([\d.]+),\s*"(\w+)",\s*([-\d.]+),\s*([-\d.]+),\s*(\w+),\s*"([^"]*)"\)'
    for match in re.finditer(tap_pattern, content):
        time, face, x, y, glowing, tag = match.groups()
        notes.append({
            "type": "tap",
            "time": float(time),
            "face": face,
            "position": {"x": float(x), "y": float(y)},
            "glowing": glowing.lower() == 'true',
            "tag": tag
        })

    # hold(time, face, x, y, glowing, tag)
    hold_pattern = r'hold\(([\d.]+),\s*"(\w+)",\s*([-\d.]+),\s*([-\d.]+),\s*(\w+),\s*"([^"]*)"\)'
    for match in re.finditer(hold_pattern, content):
        time, face, x, y, glowing, tag = match.groups()
        notes.append({
            "type": "hold",
            "time": float(time),
            "face": face,
            "position": {"x": float(x), "y": float(y)},
            "glowing": glowing.lower() == 'true',
            "tag": tag
        })

    # drag(time, face, x, y, glowing, tag)
    drag_pattern = r'drag\(([\d.]+),\s*"(\w+)",\s*([-\d.]+),\s*([-\d.]+),\s*(\w+),\s*"([^"]*)"\)'
    for match in re.finditer(drag_pattern, content):
        time, face, x, y, glowing, tag = match.groups()
        notes.append({
            "type": "drag",
            "time": float(time),
            "face": face,
            "position": {"x": float(x), "y": float(y)},
            "glowing": glowing.lower() == 'true',
            "tag": tag
        })

    # flick(time, face, turn, glowing, tag)
    flick_pattern = r'flick\(([\d.]+),\s*"(\w+)",\s*"(\w+)",\s*(\w+),\s*"([^"]*)"\)'
    for match in re.finditer(flick_pattern, content):
        time, face, turn, glowing, tag = match.groups()
        notes.append({
            "type": "flick",
            "time": float(time),
            "face": face,
            "turn": turn,
            "glowing": glowing.lower() == 'true',
            "tag": tag
        })

    # double(time, face, x1, y1, x2, y2, glowing, tag)
    double_pattern = r'double\(([\d.]+),\s*"(\w+)",\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*([-\d.]+),\s*(\w+),\s*"([^"]*)"\)'
    for match in re.finditer(double_pattern, content):
        time, face, x1, y1, x2, y2, glowing, tag = match.groups()
        notes.append({
            "type": "double",
            "time": float(time),
            "face": face,
            "positions": [
                {"x": float(x1), "y": float(y1)},
                {"x": float(x2), "y": float(y2)}
            ],
            "glowing": glowing.lower() == 'true',
            "tag": tag
        })

    # Sort notes by time
    notes.sort(key=lambda n: n["time"])

    return notes, audio

def convert_chart(chart_name, scripts_dir, output_dir):
    """Convert a single chart from Skript to JSON"""
    properties_file = scripts_dir / f"{chart_name}_properties.sk"
    chart_file = scripts_dir / f"-{chart_name}.sk"

    if not properties_file.exists() or not chart_file.exists():
        print(f"Skipping {chart_name}: missing files")
        return False

    print(f"Converting {chart_name}...")

    # Parse metadata
    metadata = parse_properties(properties_file)

    # Parse notes
    notes, audio = parse_chart_file(chart_file)

    # Update audio if found
    if audio:
        metadata["audio"] = audio

    # Create JSON structure
    chart_json = {
        "version": "1.0.0",
        "metadata": metadata,
        "notes": notes
    }

    # Write to file
    output_file = output_dir / f"{chart_name}.json"
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(chart_json, f, indent=2, ensure_ascii=False)

    print(f"✓ Converted {chart_name} ({len(notes)} notes)")
    return True

def main():
    # Paths
    project_root = Path(__file__).parent
    scripts_dir = project_root / "scripts" / "charts"
    output_dir = project_root / "plugins" / "CubeRhythm" / "charts"

    # Create output directory if it doesn't exist
    output_dir.mkdir(parents=True, exist_ok=True)

    # Find all chart names
    chart_names = set()
    for file in scripts_dir.glob("*_properties.sk"):
        chart_name = file.stem.replace("_properties", "")
        chart_names.add(chart_name)

    print(f"Found {len(chart_names)} charts to convert")
    print("-" * 50)

    # Convert each chart
    success_count = 0
    for chart_name in sorted(chart_names):
        if convert_chart(chart_name, scripts_dir, output_dir):
            success_count += 1

    print("-" * 50)
    print(f"Conversion complete: {success_count}/{len(chart_names)} charts converted")

if __name__ == "__main__":
    main()
