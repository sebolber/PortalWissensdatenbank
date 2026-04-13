#!/usr/bin/env python3
"""
Parst das Aenderungsdialog Benutzerhandbuch PDF und erzeugt eine JSON-Importdatei
fuer die Wissensdatenbank (KnowledgeItemCreateRequest-Format).

Verwendung:
    python3 tools/parse_handbuch.py <pdf-pfad> [<output-pfad>]

Beispiel:
    python3 tools/parse_handbuch.py ~/Desktop/Aenderungsdialog\ Benutzerhandbuch.pdf
"""

import json
import re
import sys
from dataclasses import dataclass, field
from pypdf import PdfReader


# --- Kapitelstruktur-Definition ---
# Abschnittsnummer-Pattern: Nur Nummern mit mindestens einem Punkt (z.B. "2.1", "3.3.14")
# Einstellige Nummern ("3", "10") werden nur als Kapitel-Ueberschriften akzeptiert
SECTION_PATTERN = re.compile(
    r'^(\d+\.\d+(?:\.\d+)*)\s+'  # Abschnittsnummer mit mind. einem Punkt
    r'(.+?)$',                    # Titel (Rest der Zeile)
    re.MULTILINE
)

# Seitenzahlen und Header/Footer entfernen
HEADER_FOOTER = re.compile(
    r'(?:© adesso health solutions GmbH.*?$|'
    r'Inhaltsverzeichnis\s+\d{2}\.\d{2}\.\d{4}|'
    r'Seite\s+[ivxlcdm\d]+)',
    re.MULTILINE | re.IGNORECASE
)

# Hauptkapitel (einstellige Nummer) mit ihren Startseitenzahlen
CHAPTER_PAGES = {
    "1":  30,
    "2":  31,
    "3":  34,
    "4":  271,
    "5":  288,
    "6":  292,
    "7":  329,
    "8":  338,
    "9":  370,
    "10": 376,
    "11": 404,
    "12": 430,
    "13": 474,
    "14": 483,
    "15": 489,
    "16": 491,
    "17": 497,
    "18": 499,
}

# Kapitel-Endseiten (naechstes Kapitel - 1)
CHAPTER_END_PAGES = {}
chapters_sorted = sorted(CHAPTER_PAGES.items(), key=lambda x: int(x[0]))
for i, (ch, start) in enumerate(chapters_sorted):
    if i + 1 < len(chapters_sorted):
        CHAPTER_END_PAGES[ch] = chapters_sorted[i + 1][1] - 1
    else:
        CHAPTER_END_PAGES[ch] = 502  # letzte Seite

CHAPTER_TITLES = {
    "1":  "Ziel der Dokumentation",
    "2":  "Starten und Beenden des Abrechnungs-Aenderungsdialogs",
    "3":  "Aufbau der Oberflaeche",
    "4":  "Verwaltung von Teilpaketen",
    "5":  "Dialogtrennwerk / Trennen",
    "6":  "Dialogregeln",
    "7":  "Umhaengeoperationen",
    "8":  "Gruppenkorrekturen",
    "9":  "Fertigstellen von Faellen",
    "10": "Korrekturdialog",
    "11": "Fehler der Klammerwerke korrigieren - Der Patienteneditor",
    "12": "Manuelles Erfassen von Scheinen",
    "13": "Einstellungendialog",
    "14": "Protokollierung",
    "15": "Arbeitskoordinierungsliste (AKL) im Aenderungsdialog",
    "16": "Besonderheiten fuer die ASV-Abrechnung",
    "17": "Aufnahme / Wiedergabe",
    "18": "Anhaenge",
}


@dataclass
class Section:
    """Repraesentiert einen Abschnitt im Handbuch."""
    number: str
    title: str
    content: str = ""
    children: list = field(default_factory=list)

    @property
    def depth(self):
        return self.number.count('.')

    def to_dict(self, order=0):
        result = {
            "heading": self.title,
            "content": self.content.strip() if self.content else None,
            "sectionNumber": self.number,
            "orderIndex": order,
        }
        if self.children:
            result["children"] = [
                child.to_dict(i) for i, child in enumerate(self.children)
            ]
        return result


def extract_text_range(reader, start_page, end_page):
    """Extrahiert Text aus einem Seitenbereich (1-basiert)."""
    text = ""
    for i in range(start_page - 1, min(end_page, len(reader.pages))):
        page_text = reader.pages[i].extract_text()
        if page_text:
            # Header/Footer bereinigen
            page_text = HEADER_FOOTER.sub('', page_text)
            text += page_text + "\n"
    return text


def find_sections_in_text(text, parent_number=""):
    """Findet alle Abschnitts-Ueberschriften im Text."""
    sections = []
    for match in SECTION_PATTERN.finditer(text):
        sec_num = match.group(1)
        sec_title = match.group(2).strip()

        # Nur Abschnitte die zum Parent-Kapitel gehoeren
        if parent_number and not sec_num.startswith(parent_number + "."):
            continue

        # Seitenzahl-Referenzen aus dem Titel entfernen
        sec_title = re.sub(r'\s*\.{3,}\s*\d+\s*$', '', sec_title)
        sec_title = re.sub(r'\s+\d+\s*$', '', sec_title)

        if sec_title and len(sec_title) > 2:
            sections.append((sec_num, sec_title, match.start()))

    return sections


def extract_section_content(text, sections, idx):
    """Extrahiert den Inhalt zwischen zwei aufeinanderfolgenden Abschnitten."""
    start = sections[idx][2]
    # Zum Ende der Ueberschriftszeile springen
    line_end = text.find('\n', start)
    if line_end == -1:
        line_end = start + len(sections[idx][1]) + len(sections[idx][0]) + 2

    if idx + 1 < len(sections):
        end = sections[idx + 1][2]
    else:
        end = len(text)

    content = text[line_end:end].strip()

    # Ueberschriften der Kind-Abschnitte aus dem Content entfernen
    # (die werden als eigene SubArticles angelegt)
    child_prefix = sections[idx][0] + "."
    for sec_num, sec_title, _ in sections:
        if sec_num.startswith(child_prefix):
            # Entferne die Ueberschriftszeile aus dem Content
            content = re.sub(
                re.escape(sec_num) + r'\s+' + re.escape(sec_title) + r'\s*\n?',
                '', content, count=1
            )

    # Content kuerzen wenn sehr lang (max ~2000 Zeichen pro Abschnitt)
    if len(content) > 3000:
        content = content[:2900] + "\n\n[... weiterer Inhalt gekuerzt ...]"

    return content


def build_section_tree(sections_flat):
    """Baut aus einer flachen Liste von Sections einen Baum."""
    if not sections_flat:
        return []

    root_sections = []
    section_map = {}

    for sec in sections_flat:
        section_map[sec.number] = sec

    for sec in sections_flat:
        parts = sec.number.rsplit('.', 1)
        if len(parts) == 1:
            # Top-Level innerhalb des Kapitels
            root_sections.append(sec)
        else:
            parent_num = parts[0]
            if parent_num in section_map:
                section_map[parent_num].children.append(sec)
            else:
                root_sections.append(sec)

    return root_sections


def parse_chapter(reader, chapter_num, chapter_title):
    """Parst ein einzelnes Kapitel und gibt eine Liste von Sections zurueck."""
    start_page = CHAPTER_PAGES[chapter_num]
    end_page = CHAPTER_END_PAGES[chapter_num]

    print(f"  Kapitel {chapter_num}: {chapter_title} (Seiten {start_page}-{end_page})")

    text = extract_text_range(reader, start_page, end_page)

    if not text.strip():
        return []

    # Alle Abschnitte in diesem Kapitel finden
    all_sections = find_sections_in_text(text)

    # Nur Abschnitte die zu diesem Kapitel gehoeren
    chapter_sections = [
        (num, title, pos) for num, title, pos in all_sections
        if num == chapter_num or num.startswith(chapter_num + ".")
    ]

    if not chapter_sections:
        return []

    # Content fuer jeden Abschnitt extrahieren
    sections_flat = []
    for i, (sec_num, sec_title, _) in enumerate(chapter_sections):
        content = extract_section_content(text, chapter_sections, i)
        sections_flat.append(Section(
            number=sec_num,
            title=sec_title,
            content=content
        ))

    # Baum aufbauen
    tree = build_section_tree(sections_flat)

    # Das Kapitel selbst entfernen wenn es als eigene Section existiert
    # (nur die Kinder zurueckgeben)
    if len(tree) == 1 and tree[0].number == chapter_num:
        return tree[0].children if tree[0].children else [tree[0]]

    return tree


def create_import_data(reader):
    """Erstellt die vollstaendige Import-Datenstruktur."""
    print("Erstelle Importdaten...")

    # Softwareprodukt und Version
    product = {
        "name": "Abrechnungs-Aenderungsdialog",
        "executableName": "a1dlg.exe",
        "publisher": "adesso health solutions GmbH",
        "description": "Abrechnungs- und Informationssystem fuer Kassenaerztliche Vereinigungen"
    }

    product_version = {
        "versionLabel": "152.0",
        "releaseDate": "2025-03-21",
        "changeSummary": "Produktivlieferung Patientensystem"
    }

    # Pro Hauptkapitel ein KnowledgeItem erstellen
    knowledge_items = []

    for chapter_num in sorted(CHAPTER_PAGES.keys(), key=lambda x: int(x)):
        chapter_title = CHAPTER_TITLES.get(chapter_num, f"Kapitel {chapter_num}")

        sub_articles = parse_chapter(reader, chapter_num, chapter_title)

        # Kapitel-Summary aus dem ersten Absatz erstellen
        start_page = CHAPTER_PAGES[chapter_num]
        first_page_text = extract_text_range(reader, start_page, start_page + 1)
        summary = first_page_text[:500].strip() if first_page_text else None
        if summary and len(summary) > 400:
            summary = summary[:400] + "..."

        item = {
            "title": f"Kapitel {chapter_num}: {chapter_title}",
            "summary": summary,
            "knowledgeType": "HANDBUCH",
            "bindingLevel": "INFORMATIV",
            "keywords": f"Aenderungsdialog, a1dlg, {chapter_title}, Benutzerhandbuch",
            "sourceReference": "Aenderungsdialog Benutzerhandbuch v152.0",
            "tags": ["Aenderungsdialog", "Benutzerhandbuch", "KVAI"],
            "subArticles": [sa.to_dict(i) for i, sa in enumerate(sub_articles)]
        }

        knowledge_items.append(item)
        print(f"    -> {len(sub_articles)} Top-Level-Abschnitte")

    return {
        "_meta": {
            "format": "wissensdatenbank-import",
            "version": "1.0",
            "description": "Import des Aenderungsdialog Benutzerhandbuchs v152.0",
            "generatedFrom": "Aenderungsdialog Benutzerhandbuch.pdf"
        },
        "softwareProduct": product,
        "productVersion": product_version,
        "knowledgeItems": knowledge_items
    }


def count_articles(items):
    """Zaehlt die Gesamtanzahl aller SubArticles rekursiv."""
    total = 0
    for item in items:
        for sa in item.get("subArticles", []):
            total += count_recursive(sa)
    return total


def count_recursive(sa):
    count = 1
    for child in sa.get("children", []):
        count += count_recursive(child)
    return count


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    pdf_path = sys.argv[1]
    output_path = sys.argv[2] if len(sys.argv) > 2 else "import-handbuch-aenderungsdialog.json"

    print(f"Lese PDF: {pdf_path}")
    reader = PdfReader(pdf_path)
    print(f"  {len(reader.pages)} Seiten")

    data = create_import_data(reader)

    # Statistik
    n_items = len(data["knowledgeItems"])
    n_articles = count_articles(data["knowledgeItems"])
    print(f"\nErgebnis:")
    print(f"  {n_items} KnowledgeItems (Kapitel)")
    print(f"  {n_articles} SubArticles (Abschnitte) gesamt")

    # Schreiben
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    print(f"\nImportdatei geschrieben: {output_path}")


if __name__ == "__main__":
    main()
