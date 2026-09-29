# -*- coding: utf-8 -*-
"""Exports the KFGQPC Hafs text from a DailySeventy-style `quran.db` into the
library's compact asset `mushaf/src/main/assets/mushaf/hafs.tsv`.

One line per ayah, tab separated:

    id  sura  ayah  juz  page  line_start  line_end  text

`text` is the verse with the trailing ayah number removed (the renderer draws the
end-of-ayah rosette itself). The line numbers are only used by the fallback line
breaker; the real layout comes from `layout.txt`.

    python tools/export_quran_text.py path/to/quran.db
"""
import os
import sqlite3
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mushaf", "src", "main", "assets", "mushaf", "hafs.tsv")
DIGITS = set("0123456789٠١٢٣٤٥٦٧٨٩")


def clean(text: str) -> str:
    t = text.strip().replace(" ", " ")
    head, _, last = t.rpartition(" ")
    if head and last and all(c in DIGITS for c in last):
        return head.strip()
    return t


def main(db_path: str) -> None:
    con = sqlite3.connect(db_path)
    rows = con.execute(
        "SELECT id, sora, aya_no, jozz, page, line_start, line_end, aya_text "
        "FROM quran ORDER BY id"
    ).fetchall()
    assert len(rows) == 6236, len(rows)
    with open(OUT, "w", encoding="utf-8", newline="\n") as f:
        for r in rows:
            text = clean(r[7])
            assert "\t" not in text and "\n" not in text
            f.write("\t".join(map(str, r[:7])) + "\t" + text + "\n")
    print(f"wrote {len(rows)} ayat -> {OUT}")


if __name__ == "__main__":
    main(sys.argv[1])
