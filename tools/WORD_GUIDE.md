# Word-list authoring guide (Pass the Phrase)

This guide is the contract for every category's word list. The game is a
Catchphrase-style party game: one person sees the entry and gives clues (they
can't say the word, rhymes, "sounds like", or first letters) until a teammate
shouts it out. Speed matters: a word that makes the room go "huh?" kills the fun.

## Files

- Source: `tools/wordsrc/<category_id>/<subtopic_id>.txt`
- One entry per line: `Text|rating` (e.g. `Spider-Man|5`).
- Lines starting with `#` are comments. Record every cut as a comment so the
  review is auditable, e.g. `# cut: Okapi|2 (too obscure)`.
- Compile: `python3 tools/buildwords.py <category_id>`
- Review: `~/.cache/tools/venv/bin/python tools/wordreview.py <category_id>`
  (this venv has `wordfreq` installed). The review must print
  "All hard checks passed." for your category before you're done
  (cross-category duplicate errors involving other categories still in progress
  are resolved by the coordinator; but avoid them using the boundary rules below).

## Size

- At least 500 entries per category; aim for 750+.
- At least 40 entries per sub-topic. Spread entries so no sub-topic dominates
  (roughly balanced; no sub-topic over ~25% of a category unless the category
  has only 4 sub-topics).
- Quality beats quantity. Never pad.

## The party test (what counts as valid)

In a mixed room of adults of different ages, most people recognize it the
moment they hear it and could shout it out from a few clues.

- Popular, instantly guessable, concrete, easy to clue or act out.
- Avoid vague abstractions (Concept, Aspect, Notion).
- Entertainment: mainstream, lasting: big titles, household-name people,
  characters known across age groups. No deep cuts, niche fandom, short-lived
  trends.
- Keep-style: Pizza, Giraffe, Toothbrush, Firefighter, Spider-Man, Monopoly,
  Thunderstorm, Break a leg.
- Cut-style: Okapi, Kohlrabi, Cartographer, Tachometer, Hoist by your own petard.
- No invented words, made-up compound phrases, technical jargon, misspellings.
- 1–4 words (hyphen parts count as words: "Jack-in-the-box" = 4).
- Natural capitalization (Title case for names/titles, sentence case for
  ordinary things: "Ice cream", "Break a leg", "The Lion King").
- Only letters (accented letters OK, e.g. Pokémon, Beyoncé), spaces, hyphens,
  apostrophes. No digits, commas, periods, colons, ampersands, "!" or "?".
  Rewrite titles naturally when possible ("Mission: Impossible" ->
  "Mission Impossible"; "Ocean's Eleven" and "WALL-E" are fine as-is);
  drop titles that can't be written without digits ("Apollo 13", "Se7en").
- No duplicates within the category (case-insensitive; singular/plural and
  hyphen/space variants count as the same entry).
- No near-duplicates: from Dog / Puppy / Doggy keep one. Same franchise only
  once (don't list "Shrek" in movies and characters; "Harry Potter" once).
- Family-friendly, nothing offensive (no gore, sex, drugs, slurs, divisive
  politics or religion jokes). Mild things like "Wine" or "Beer" are OK.

## Popularity review (mandatory)

1. Draft generously.
2. Rate EVERY entry 1–5 for how instantly a typical adult would get it:
   5 = everyone instantly; 4 = nearly everyone quickly; 3 = many would hesitate;
   2 = niche; 1 = obscure. Be honest and strict.
3. Cut everything below 4 (keep it as a `# cut:` comment with its rating).
4. Run the review. For single words with Zipf < 2.5 it prints flags: give each
   a second look. Keep it only if it's genuinely common in everyday speech
   (Colander, Flip-flops); otherwise cut it. Add a comment line
   `# zipf-reviewed: <word> kept (reason)` for each flagged word you keep.
5. If cuts drop a sub-topic below 40 or the category below 500, refill with
   MORE POPULAR entries. Never lower the bar.

## Category boundaries (no entry may appear in two categories)

- Entertainment: movies, tv_shows, actors, musicians (incl. bands),
  characters (fictional characters, mascots, fairy-tale/cartoon/superhero
  characters), video_games (games, franchises, consoles like PlayStation),
  games (board games, card games, party games, classic playground/parlor
  games like Hide and seek, Chess, Uno, Poker), books (famous books, fairy
  tales/nursery rhymes, household-name authors). A franchise appears ONCE in
  the whole category.
- Everyday Life: jobs (occupations), places (man-made venues/buildings/rooms:
  Library, Hospital, Zoo, Gym, Kitchen), travel (Passport, Suitcase, Airport,
  Hotel, Road trip, Cruise, Jet lag, Souvenir), sports (sports, athletic
  activities, sports equipment and sports terms: Basketball, Touchdown,
  Skateboarding, Tennis racket), weather (weather & seasons: Rainbow,
  Hurricane, Snowflake, Summer), school (school life, subjects, school
  supplies: Homework, Backpack, Pencil, Recess, Math), holidays (holidays,
  celebrations, life events: Christmas, Birthday party, Wedding, Graduation).
  NOT jobs-as-characters, NOT natural features.
- Food & Drink: dishes, snacks, desserts, ingredients (incl. all fruits,
  vegetables, herbs, spices, cheeses), drinks, cuisines (cuisines & food
  styles: Italian food, Sushi bar no; use e.g. "Mexican food", "Fast food",
  "Barbecue", "Buffet", "Brunch", "Takeout", "Street food"). Do NOT use bare
  animal names (Chicken, Lobster, Salmon are Animals); use food forms instead
  ("Fried chicken", "Lobster roll", "Fish and chips"). No kitchen utensils.
- Things: household (furniture, kitchenware, bathroom & cleaning items,
  everyday objects), tools (hand/power tools, garden tools, office tools),
  clothing (clothes, shoes, accessories, jewelry), gadgets (physical electronic
  devices: Smartphone, Laptop, Headphones, Remote control; household
  appliances like Microwave and Toaster go in household), vehicles (land/sea/air vehicles incl.
  Bicycle), toys (Teddy bear, Yo-yo, Lego, Kite, Slinky). No sports equipment,
  no school supplies, no board/card games, no video game consoles.
- Nature & Science: animals (all creatures incl. sea life, insects, birds,
  dinosaurs), plants (non-food plants, flowers, trees: Sunflower, Cactus, Oak
  tree, Poison ivy), body (body parts, organs, bodily actions as nouns like
  Hiccups, Heartbeat), space (Moon, Astronaut, Black hole, planets, Rocket),
  geography (countries, famous cities, continents, natural features:
  Volcano, Beach, Waterfall, Desert; famous landmarks: Eiffel Tower; natural
  disasters like Earthquake), technology (digital & scientific concepts and
  inventions that aren't consumer gadgets: Internet, Wi-Fi, Email, Password,
  Robot, Emoji, Electricity, Magnet, Microscope, Solar panel, Barcode).
- Phrases & Actions: idioms (Break a leg, Piece of cake, Spill the beans),
  sayings (common sayings, proverbs, catchphrases and everyday expressions,
  1–4 words: Practice makes perfect, Long time no see, Bless you, Game over),
  verbs (actable everyday actions, -ing form: Sneezing, Whispering, Juggling,
  Brushing teeth; NOT sports, NOT dance styles if they're performances),
  emotions (Happy, Jealous, Embarrassed, Homesick, Hangry).
