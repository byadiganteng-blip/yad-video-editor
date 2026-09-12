#!/usr/bin/env python3
# ============================================================
#  AI STORY-TO-VIDEO GENERATOR
#  - MoviePy 1.x API
#  - Pecah cerita jadi banyak scene detail
#  - 1 gambar AI per scene
#  - Subtitle per scene
#  - Narasi TTS
# ============================================================

import os
import re
import asyncio
import textwrap
from pathlib import Path

import torch
from diffusers import StableDiffusionPipeline
from moviepy.editor import (
    ImageClip, AudioFileClip, CompositeVideoClip,
    TextClip, concatenate_videoclips
)

# ---------- ENV ----------
PROMPT         = os.environ["PROMPT"]
JOB_ID         = os.environ["JOB_ID"]
VOICE          = os.environ.get("VOICE", "male_id")
WATERMARK      = os.environ.get("WATERMARK", "")
SHOW_SUBTITLE  = os.environ.get("SHOW_SUBTITLE", "true") == "true"
SUBTITLE_STYLE = os.environ.get("SUBTITLE_STYLE", "neon")
SCENES_COUNT   = int(os.environ.get("SCENES_COUNT", "8"))
IMAGE_STYLE    = os.environ.get("IMAGE_STYLE", "cinematic")

OUTPUT_MP4 = f"{JOB_ID}.mp4"
SCENES_DIR = Path("scenes")
SCENES_DIR.mkdir(exist_ok=True)


def split_story_into_scenes(story, n):
    story = re.sub(r"\s+", " ", story.strip())
    sentences = re.split(r'(?<=[.!?…])\s+', story)
    sentences = [s.strip() for s in sentences if s.strip()]

    if len(sentences) < n:
        expanded = []
        for s in sentences:
            parts = re.split(r'(?<=[,;:])\s+', s)
            expanded.extend([p.strip() for p in parts if p.strip()])
        sentences = expanded

    if len(sentences) < n:
        words = story.split()
        chunk = max(1, len(words) // n)
        sentences = [" ".join(words[i:i+chunk])
                     for i in range(0, len(words), chunk)][:n]

    scenes = [[] for _ in range(n)]
    for i, s in enumerate(sentences):
        scenes[i % n].append(s)
    return [" ".join(sc).strip() for sc in scenes if sc]


STYLE_PROMPTS = {
    "anime":      "anime style, studio ghibli, detailed illustration, vibrant colors, "
                  "beautiful lighting, highly detailed, 4k, masterpiece",
    "realistic":  "photorealistic, cinematic, 8k, ultra detailed, natural lighting, "
                  "professional photography, depth of field",
    "cinematic":  "cinematic shot, dramatic lighting, movie still, anamorphic, "
                  "film grain, ultra detailed, epic composition, 8k",
    "watercolor": "watercolor painting, soft pastel colors, artistic, dreamy, "
                  "hand-painted, delicate details, beautiful composition",
}

NEGATIVE_PROMPT = (
    "low quality, blurry, deformed, ugly, bad anatomy, extra limbs, "
    "watermark, text, signature, cropped, worst quality, jpeg artifacts"
)


def build_image_prompt(scene_text, style):
    base = scene_text.strip()
    if len(base) > 200:
        base = base[:200] + "..."
    style_suffix = STYLE_PROMPTS.get(style, STYLE_PROMPTS["cinematic"])
    detail_boost = (
        "highly detailed, intricate details, sharp focus, "
        "professional composition, dramatic atmosphere, "
        "expressive characters, rich background"
    )
    return f"{base}, {detail_boost}, {style_suffix}"


print("=" * 70)
print("Loading Stable Diffusion model...")
print("=" * 70)

pipe = StableDiffusionPipeline.from_pretrained(
    "hakurei/waifu-diffusion",
    torch_dtype=torch.float32,
    safety_checker=None,
    requires_safety_checker=False,
).to("cpu")
pipe.enable_attention_slicing()


def generate_image(scene_text, idx, style):
    prompt = build_image_prompt(scene_text, style)
    print(f"\nScene {idx+1}: {scene_text[:80]}...")
    with torch.no_grad():
        image = pipe(
            prompt,
            negative_prompt=NEGATIVE_PROMPT,
            num_inference_steps=25,
            guidance_scale=7.5,
            width=512, height=512,
        ).images[0]
    path = SCENES_DIR / f"scene_{idx:02d}.png"
    image.save(path)
    print(f"   Saved: {path}")
    return str(path)


VOICE_MAP = {
    "male_id":   "id-ID-ArdiNeural",
    "female_id": "id-ID-GadisNeural",
    "child_id":  "id-ID-ArdiNeural",
    "male_en":   "en-US-GuyNeural",
    "female_en": "en-US-JennyNeural",
    "robot":     "en-US-DavisNeural",
}
VOICE_FILE = "voice.mp3"


async def _tts(text, voice):
    import edge_tts
    await edge_tts.Communicate(text, voice).save(VOICE_FILE)


def make_voice(text):
    if VOICE not in VOICE_MAP:
        return None
    print(f"\nGenerating voice-over ({VOICE})...")
    asyncio.run(_tts(text, VOICE_MAP[VOICE]))
    print(f"   Saved: {VOICE_FILE}")
    return VOICE_FILE


def get_subtitle_opts(style):
    opts = {
        "neon":    {"color": "cyan",  "stroke_color": "magenta", "stroke_width": 3},
        "outline": {"color": "white", "stroke_color": "black",   "stroke_width": 4},
        "classic": {"color": "white", "stroke_color": "black",   "stroke_width": 1},
        "bold":    {"color": "yellow","stroke_color": "black",   "stroke_width": 3},
    }
    return opts.get(style, opts["neon"])


def make_scene_clip(image_path, scene_text, duration, subtitle_style):
    img_clip = ImageClip(image_path).set_duration(duration)
    img_clip = img_clip.resize(lambda t: 1 + 0.04 * t)
    clips = [img_clip]

    if SHOW_SUBTITLE and scene_text.strip():
        wrapped = "\n".join(textwrap.wrap(scene_text, width=38))
        opts = get_subtitle_opts(subtitle_style)
        try:
            txt = (
                TextClip(
                    wrapped,
                    fontsize=32,
                    font="DejaVu-Sans-Bold",
                    color=opts["color"],
                    stroke_color=opts["stroke_color"],
                    stroke_width=opts["stroke_width"],
                    method="caption",
                    size=(img_clip.w - 60, None),
                )
                .set_position(("center", "bottom"))
                .set_duration(duration)
                .margin(bottom=40, opacity=0)
            )
            clips.append(txt)
        except Exception as e:
            print(f"Subtitle gagal: {e}")

    return CompositeVideoClip(clips, size=img_clip.size)


def main():
    print("=" * 70)
    print("AI STORY-TO-VIDEO GENERATOR")
    print("=" * 70)
    print(f"Story length : {len(PROMPT)} chars")
    print(f"Scenes count : {SCENES_COUNT}")
    print(f"Image style  : {IMAGE_STYLE}")
    print(f"Voice        : {VOICE}")
    print(f"Subtitle     : {SHOW_SUBTITLE} ({SUBTITLE_STYLE})")
    print(f"Watermark    : {WATERMARK or '(none)'}")
    print("=" * 70)

    scenes = split_story_into_scenes(PROMPT, SCENES_COUNT)
    print(f"\nCerita dipecah jadi {len(scenes)} scene:")
    for i, s in enumerate(scenes):
        print(f"   Scene {i+1}: {s[:80]}...")

    print("\n" + "=" * 70)
    print("GENERATE GAMBAR PER SCENE")
    print("=" * 70)
    image_paths = []
    for i, scene in enumerate(scenes):
        try:
            path = generate_image(scene, i, IMAGE_STYLE)
            image_paths.append(path)
        except Exception as e:
            print(f"Scene {i+1} gagal: {e}")

    if not image_paths:
        raise SystemExit("Tidak ada gambar yang berhasil di-generate.")

    voice_path = make_voice(PROMPT)

    if voice_path and os.path.exists(voice_path):
        audio = AudioFileClip(voice_path)
        total_duration = audio.duration + 0.5
    else:
        audio = None
        total_duration = 4.0 * len(image_paths)

    per_scene = total_duration / len(image_paths)
    print(f"\nDurasi total : {total_duration:.2f}s")
    print(f"Per scene    : {per_scene:.2f}s")

    print("\n" + "=" * 70)
    print("MEMBUAT CLIP PER SCENE")
    print("=" * 70)
    scene_clips = []
    for i, (img, scene) in enumerate(zip(image_paths, scenes)):
        print(f"   Scene {i+1}/{len(image_paths)}...")
        clip = make_scene_clip(img, scene, per_scene, SUBTITLE_STYLE)
        clip = clip.crossfadein(0.4).crossfadeout(0.4)
        scene_clips.append(clip)

    print("\n" + "=" * 70)
    print("MENGGABUNG SEMUA SCENE")
    print("=" * 70)
    final = concatenate_videoclips(scene_clips, method="compose")

    if WATERMARK:
        try:
            wm = (
                TextClip(WATERMARK, fontsize=24, font="DejaVu-Sans-Bold",
                         color="white", stroke_color="black", stroke_width=1)
                .set_position(("right", "top"))
                .set_duration(final.duration)
                .margin(right=20, top=20, opacity=0)
            )
            final = CompositeVideoClip([final, wm])
        except Exception as e:
            print(f"Watermark gagal: {e}")

    if audio is not None:
        audio = audio.set_duration(final.duration)
        final = final.set_audio(audio)

    print("\n" + "=" * 70)
    print(f"EXPORT KE {OUTPUT_MP4}")
    print("=" * 70)
    final.write_videofile(
        OUTPUT_MP4,
        fps=24,
        codec="libx264",
        audio_codec="aac",
        preset="medium",
        threads=4,
    )

    print("\n" + "=" * 70)
    print(f"SELESAI: {OUTPUT_MP4}")
    print(f"   Total scene : {len(image_paths)}")
    print(f"   Durasi      : {final.duration:.2f}s")
    print("=" * 70)


if __name__ == "__main__":
    main()
