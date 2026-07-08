import { readFileSync, writeFileSync, mkdirSync, existsSync, rmSync } from "node:fs";
import { execFileSync } from "node:child_process";
import { parse } from "yaml";
import { join, resolve } from "node:path";

const BOT = resolve("../../indonesian/content/quiz");
const OUT = resolve("../composeApp/src/commonMain/composeResources/files");
const CONTENT_OUT = join(OUT, "content");
const AUDIO_OUT = join(OUT, "audio");

interface BotSentence { text: string; blank: string; en: string }
interface BotCard {
  id: string; indonesian: string; english: string;
  audio?: string; note?: { en?: string }; sentences?: BotSentence[];
}
interface BotModule { id: string; title: { en: string }; cards: BotCard[] }

interface OutCard {
  id: string; indonesian: string; english: string;
  note?: string; audio?: string; sentences: BotSentence[];
}

function transcode(oggName: string): string {
  const src = join(BOT, "audio", oggName);
  const m4aName = oggName.replace(/\.ogg$/, ".m4a");
  const dst = join(AUDIO_OUT, m4aName);
  if (!existsSync(dst)) {
    execFileSync("/opt/homebrew/bin/ffmpeg", ["-y", "-i", src, "-c:a", "aac", "-b:a", "64k", dst],
      { stdio: "ignore" });
  }
  return m4aName;
}

function main() {
  rmSync(CONTENT_OUT, { recursive: true, force: true });
  mkdirSync(CONTENT_OUT, { recursive: true });
  mkdirSync(AUDIO_OUT, { recursive: true });

  const manifest: { modules: { id: string; title: string; cardCount: number }[] } = { modules: [] };

  for (let n = 1; n <= 8; n++) {
    const mod = parse(readFileSync(join(BOT, `module-${n}.yaml`), "utf8")) as BotModule;
    const cards: OutCard[] = mod.cards.map((c) => ({
      id: c.id,
      indonesian: c.indonesian,
      english: c.english,
      note: c.note?.en,
      audio: c.audio ? transcode(c.audio) : undefined,
      sentences: (c.sentences ?? []).map((s) => ({ text: s.text, blank: s.blank, en: s.en })),
    }));
    writeFileSync(join(CONTENT_OUT, `${mod.id}.json`), JSON.stringify(cards));
    manifest.modules.push({ id: mod.id, title: mod.title.en, cardCount: cards.length });
    console.log(`${mod.id}: ${cards.length} cards`);
  }
  writeFileSync(join(CONTENT_OUT, "manifest.json"), JSON.stringify(manifest, null, 2));
  console.log("done");
}

main();
