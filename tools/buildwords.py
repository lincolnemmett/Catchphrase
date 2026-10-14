#!/usr/bin/env python3
"""Compile word sources into the bundled asset JSON.

Source layout:  tools/wordsrc/<category_id>/<subtopic_id>.txt
Line format:    Text|rating        e.g.  Spider-Man|5
                # comment          (ignored; used to record cuts, e.g. "# cut: Okapi|2")
Only entries rated >= 4 may be present as live lines (the review cuts the rest).

Output: app/src/main/assets/words/<category_id>.json

Usage: python3 tools/buildwords.py [category_id ...]
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "tools" / "wordsrc"
OUT = ROOT / "app" / "src" / "main" / "assets" / "words"

CATEGORIES = {
    "entertainment": ("Entertainment", {
        "movies": "Movies",
        "tv_shows": "TV Shows",
        "actors": "Actors",
        "musicians": "Musicians & Bands",
        "characters": "Famous Characters",
        "video_games": "Video Games",
        "games": "Board & Card Games",
        "books": "Books",
    }),
    "everyday": ("Everyday Life", {
        "jobs": "Jobs",
        "places": "Places",
        "travel": "Travel",
        "sports": "Sports",
        "weather": "Weather",
        "school": "School",
        "holidays": "Holidays & Events",
    }),
    "food": ("Food & Drink", {
        "dishes": "Dishes",
        "snacks": "Snacks",
        "desserts": "Desserts",
        "ingredients": "Ingredients",
        "drinks": "Drinks",
        "cuisines": "Cuisines",
    }),
    "things": ("Things", {
        "household": "Household Items",
        "tools": "Tools",
        "clothing": "Clothing",
        "gadgets": "Gadgets",
        "vehicles": "Vehicles",
        "toys": "Toys",
    }),
    "nature": ("Nature & Science", {
        "animals": "Animals",
        "plants": "Plants",
        "body": "Human Body",
        "space": "Space",
        "geography": "Geography",
        "technology": "Technology",
    }),
    "phrases": ("Phrases & Actions", {
        "idioms": "Idioms",
        "sayings": "Common Sayings",
        "verbs": "Actions",
        "emotions": "Emotions",
    }),
}


def build(cid: str) -> int:
    name, subs = CATEGORIES[cid]
    words = []
    for sid in subs:
        f = SRC / cid / f"{sid}.txt"
        if not f.exists():
            continue
        for n, raw in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
            line = raw.strip()
            if not line or line.startswith("#"):
                continue
            if "|" not in line:
                raise SystemExit(f"{f}:{n}: missing '|rating' in {line!r}")
            text, rating = line.rsplit("|", 1)
            words.append({"text": text.strip(), "subtopic": sid, "rating": int(rating)})
    OUT.mkdir(parents=True, exist_ok=True)
    doc = {"id": cid, "name": name, "subtopics": subs, "words": words}
    (OUT / f"{cid}.json").write_text(
        json.dumps(doc, ensure_ascii=False, indent=0, separators=(",", ":")) + "\n",
        encoding="utf-8",
    )
    return len(words)


if __name__ == "__main__":
    ids = sys.argv[1:] or list(CATEGORIES)
    for c in ids:
        print(f"{c}: {build(c)} entries")
