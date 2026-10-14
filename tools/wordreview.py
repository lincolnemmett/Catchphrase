#!/usr/bin/env python3
"""Word-list review tool for Pass the Phrase.

Checks every category file in app/src/main/assets/words/ against the same rules
as the JVM validation test (WordListValidationTest), and additionally runs a
word-frequency popularity check using the `wordfreq` package (Zipf scale).

Single-word entries with a Zipf frequency below 2.5 are FLAGGED for a second
human look (they are not cut automatically: e.g. "Colander" is a fine party word
that is rare in writing).

Usage:
    python3 tools/wordreview.py              # all categories
    python3 tools/wordreview.py food         # one category id
    python3 tools/wordreview.py --stats      # counts only

Exit code is non-zero if any hard rule fails.
"""
import json
import re
import sys
import unicodedata
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
WORDS_DIR = ROOT / "app" / "src" / "main" / "assets" / "words"
MIN_PER_CATEGORY = 500
MIN_PER_SUBTOPIC = 40
MIN_RATING = 4
ZIPF_FLAG = 2.5

ALLOWED = re.compile(r"^[^\W\d_]+(?:[ '\-]+[^\W\d_]+)*'?$", re.UNICODE)


def fold(s: str) -> str:
    return "".join(c for c in unicodedata.normalize("NFD", s) if unicodedata.category(c) != "Mn")


def singular(tok: str) -> str:
    if len(tok) > 4 and tok.endswith("ies"):
        return tok[:-3] + "y"
    if len(tok) > 3 and tok.endswith("es") and re.search(r"(s|x|z|ch|sh)es$", tok):
        return tok[:-2]
    if len(tok) > 3 and tok.endswith("s") and not tok.endswith("ss") and not tok.endswith("us"):
        return tok[:-1]
    return tok


def dup_key(text: str) -> str:
    """Case-insensitive key where singular/plural and hyphen/space variants collide."""
    t = fold(text).lower().replace("'", "")
    toks = [x for x in re.split(r"[\s\-]+", t) if x]
    if toks:
        toks[-1] = singular(toks[-1])
    return "".join(toks)


def word_count(text: str) -> int:
    return len([x for x in re.split(r"[\s\-]+", text) if x])


def load(path: Path):
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def main(argv):
    only = [a for a in argv if not a.startswith("--")]
    stats_only = "--stats" in argv
    files = sorted(WORDS_DIR.glob("*.json"))
    if not files:
        print(f"No word files in {WORDS_DIR}")
        return 1
    errors = []
    global_keys = defaultdict(list)  # key -> [(cat, text)]
    data = {}
    for f in files:
        try:
            d = load(f)
        except Exception as e:  # noqa: BLE001
            errors.append(f"{f.name}: invalid JSON: {e}")
            continue
        data[f.stem] = d
        for w in d.get("words", []):
            global_keys[dup_key(w.get("text", ""))].append((f.stem, w.get("text")))

    try:
        from wordfreq import zipf_frequency
    except ImportError:
        zipf_frequency = None
        print("(wordfreq not installed: skipping frequency flags)")

    for cid, d in data.items():
        if only and cid not in only:
            continue
        subs = d.get("subtopics", {})
        words = d.get("words", [])
        print(f"\n=== {d.get('name', cid)} ({cid}): {len(words)} entries")
        cnt = Counter(w.get("subtopic") for w in words)
        for sid, sname in subs.items():
            n = cnt.get(sid, 0)
            mark = "" if n >= MIN_PER_SUBTOPIC else "   <-- BELOW MIN"
            print(f"   {sname:<28} {n:>4}  ({100 * n / max(1, len(words)):.0f}%){mark}")
            if n < MIN_PER_SUBTOPIC:
                errors.append(f"{cid}: subtopic {sid} has {n} < {MIN_PER_SUBTOPIC}")
        if len(words) < MIN_PER_CATEGORY:
            errors.append(f"{cid}: {len(words)} entries < {MIN_PER_CATEGORY}")
        if d.get("id") != cid:
            errors.append(f"{cid}: id field '{d.get('id')}' does not match file name")
        if stats_only:
            continue
        seen = {}
        flags = []
        for w in words:
            t = w.get("text")
            if not isinstance(t, str) or t != t.strip() or "  " in t:
                errors.append(f"{cid}: bad text {t!r}")
                continue
            if w.get("subtopic") not in subs:
                errors.append(f"{cid}: '{t}' unknown subtopic {w.get('subtopic')!r}")
            r = w.get("rating")
            if not isinstance(r, int) or r < MIN_RATING or r > 5:
                errors.append(f"{cid}: '{t}' rating {r!r} (need int {MIN_RATING}..5)")
            if not ALLOWED.match(t):
                errors.append(f"{cid}: '{t}' has disallowed characters")
            wc = word_count(t)
            if wc < 1 or wc > 4:
                errors.append(f"{cid}: '{t}' has {wc} words (1-4 allowed)")
            k = dup_key(t)
            if k in seen:
                errors.append(f"{cid}: duplicate '{t}' ~ '{seen[k]}'")
            seen[k] = t
            others = {c for c, _ in global_keys[k] if c != cid}
            if others:
                errors.append(f"{cid}: '{t}' also appears in {sorted(others)}")
            if zipf_frequency and wc == 1:
                z = zipf_frequency(t, "en")
                if z < ZIPF_FLAG:
                    flags.append((z, t))
        if flags:
            flags.sort()
            print(f"   Zipf<{ZIPF_FLAG} flags for second look ({len(flags)}):")
            print("     " + ", ".join(f"{t} ({z:.2f})" for z, t in flags))

    if errors:
        print(f"\n{len(errors)} ERROR(S):")
        for e in errors:
            print("  - " + e)
        return 1
    print("\nAll hard checks passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
