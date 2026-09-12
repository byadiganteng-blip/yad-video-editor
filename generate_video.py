#!/usr/bin/env python3
# AI STORY-TO-VIDEO GENERATOR — Multi-Model Support
import os, re, asyncio, textwrap
from pathlib import Path
import torch
from diffusers import StableDiffusionPipeline
from moviepy.editor import (ImageClip, AudioFileClip, CompositeVideoClip,
                             TextClip, concatenate_videoclips)

PROMPT         = os.environ["PROMPT"]
JOB_ID         = os.environ["JOB_ID"]
VOICE          = os.environ.get("VOICE", "male_id")
WATERMARK      = os.environ.get("WATERMARK", "")
SHOW_SUBTITLE  = os.environ.get("SHOW_SUBTITLE", "true") == "true"
SUBTITLE_STYLE = os.environ.get("SUBTITLE_STYLE", "neon")
SCENES_COUNT   = int(os.environ.get("SCENES_COUNT", "8"))
IMAGE_STYLE    = os.environ.get("IMAGE_STYLE", "cinematic")
MODEL_ID       = os.environ.get("MODEL_ID", "waifu")

# Mapping model ID → HuggingFace path
MODEL_MAP = {
    "waifu":       "hakurei/waifu-diffusion",
    "sd15":        "runwayml/stable-diffusion-v1-5",
    "anything":    "andite/anything-v4.0",
    "dreamshaper": "Lykon/DreamShaper",
    "sdxl":        "stabilityai/stable-diffusion-xl-base-1.0",
    "openjourney": "prompthero/openjourney",
    "dreamlike":   "dreamlike-art/dreamlike-diffusion-1.0",
    "trinart":     "naclbit/trinart_stable_diffusion_v2",
    "realistic":   "SG161222/Realistic_Vision_V5.1_noVAE",
    "majicmix":    "digiplay/majicMIX_realistic_v7",
}

MODEL_PATH = MODEL_MAP.get(MODEL_ID, MODEL_MAP["waifu"])
print(f"Using model: {MODEL_ID} → {MODEL_PATH}")

OUTPUT_MP4 = f"{JOB_ID}.mp4"
SCENES_DIR = Path("scenes"); SCENES_DIR.mkdir(exist_ok=True)


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
    "anime":      "anime style, key visual, detailed, vibrant colors, studio quality, masterpiece",
    "realistic":  "photorealistic, 8k, ultra detailed, sharp focus, professional photo, DSLR",
    "cinematic":  "cinematic shot, dramatic lighting, movie still, anamorphic, epic composition, 8k",
    "artistic":   "artistic painting, surreal, dreamy, detailed, masterpiece",
    "watercolor": "watercolor painting, soft colors, artistic, delicate",
}
NEGATIVE_PROMPT = ("low quality, blurry, deformed, ugly, bad anatomy, extra limbs, "
                    "watermark, text, signature, cropped, worst quality, jpeg artifacts")


def build_image_prompt(scene_text, style):
    base = scene_text.strip()
    if len(base) > 200: base = base[:200] + "..."
    style_suffix = STYLE_PROMPTS.get(style, STYLE_PROMPTS["cinematic"])
    detail = ("highly detailed, intricate details, sharp focus, professional "
              "composition, dramatic atmosphere, rich background")
    return f"{base}, {detail}, {style_suffix}"


print("Loading model...")
pipe = StableDiffusionPipeline.from_pretrained(
    MODEL_PATH, torch_dtype=torch.float32,
    safety_checker=None, requires_safety_checker=False,
).to("cpu")
pipe.enable_attention_slicing()


def generate_image(scene_text, idx, style):
    prompt = build_image_prompt(scene_text, style)
    print(f"\nScene {idx+1}: {scene_text[:80]}...")
    with torch.no_grad():
        image = pipe(prompt, negative_prompt=NEGATIVE_PROMPT,
                     num_inference_steps=25, guidance_scale=7.5,
                     width=512, height=512).images[0]
    path = SCENES_DIR / f"scene_{idx:02d}.png"
    image.save(path); print(f"   Saved: {path}"); return str(path)


VOICE_MAP = {"male_id": "id-ID-ArdiNeural", "female_id": "id-ID-GadisNeural",
             "child_id": "id-ID-ArdiNeural", "male_en": "en-US-GuyNeural",
             "female_en": "en-US-JennyNeural", "robot": "en-US-DavisNeural"}
VOICE_FILE = "voice.mp3"

async def _tts(text, voice):
    import edge_tts
    await edge_tts.Communicate(text, voice).save(VOICE_FILE)

def make_voice(text):
    if VOICE not in VOICE_MAP: return None
    print(f"Generating voice-over ({VOICE})...")
    asyncio.run(_tts(text, VOICE_MAP[VOICE]))
    return VOICE_FILE


def get_subtitle_opts(style):
    return {
        "neon": {"color": "cyan", "stroke_color": "magenta", "stroke_width": 3},
        "outline": {"color": "white", "stroke_color": "black", "stroke_width": 4},
        "classic": {"color": "white", "stroke_color": "black", "stroke_width": 1},
        "bold": {"color": "yellow", "stroke_color": "black", "stroke_width": 3},
    }.get(style, {"color": "cyan", "stroke_color": "magenta", "stroke_width": 3})


def make_scene_clip(image_path, scene_text, duration, subtitle_style):
    img_clip = ImageClip(image_path).set_duration(duration)
    img_clip = img_clip.resize(lambda t: 1 + 0.04 * t)
    clips = [img_clip]
    if SHOW_SUBTITLE and scene_text.strip():
        wrapped = "\n".join(textwrap.wrap(scene_text, width=38))
        opts = get_subtitle_opts(subtitle_style)
        try:
            txt = (TextClip(wrapped, fontsize=32, font="DejaVu-Sans-Bold",
                            color=opts["color"], stroke_color=opts["stroke_color"],
                            stroke_width=opts["stroke_width"], method="caption",
                            size=(img_clip.w - 60, None))
                   .set_position(("center", "bottom")).set_duration(duration)
                   .margin(bottom=40, opacity=0))
            clips.append(txt)
        except Exception as e: print(f"Subtitle gagal: {e}")
    return CompositeVideoClip(clips, size=img_clip.size)


def main():
    print("=" * 70)
    print(f"MODEL: {MODEL_ID} | STYLE: {IMAGE_STYLE} | SCENES: {SCENES_COUNT}")
    print("=" * 70)

    scenes = split_story_into_scenes(PROMPT, SCENES_COUNT)
    print(f"Cerita dipecah jadi {len(scenes)} scene")

    image_paths = []
    for i, scene in enumerate(scenes):
        try: image_paths.append(generate_image(scene, i, IMAGE_STYLE))
        except Exception as e: print(f"Scene {i+1} gagal: {e}")

    if not image_paths: raise SystemExit("Tidak ada gambar di-generate.")

    voice_path = make_voice(PROMPT)
    if voice_path and os.path.exists(voice_path):
        audio = AudioFileClip(voice_path); total_duration = audio.duration + 0.5
    else:
        audio = None; total_duration = 4.0 * len(image_paths)

    per_scene = total_duration / len(image_paths)
    scene_clips = []
    for i, (img, scene) in enumerate(zip(image_paths, scenes)):
        clip = make_scene_clip(img, scene, per_scene, SUBTITLE_STYLE)
        clip = clip.crossfadein(0.4).crossfadeout(0.4)
        scene_clips.append(clip)

    final = concatenate_videoclips(scene_clips, method="compose")
    if WATERMARK:
        try:
            wm = (TextClip(WATERMARK, fontsize=24, font="DejaVu-Sans-Bold",
                           color="white", stroke_color="black", stroke_width=1)
                  .set_position(("right", "top")).set_duration(final.duration)
                  .margin(right=20, top=20, opacity=0))
            final = CompositeVideoClip([final, wm])
        except Exception as e: print(f"Watermark gagal: {e}")

    if audio is not None:
        audio = audio.set_duration(final.duration)
        final = final.set_audio(audio)

    final.write_videofile(OUTPUT_MP4, fps=24, codec="libx264",
                          audio_codec="aac", preset="medium", threads=4)
    print(f"SELESAI: {OUTPUT_MP4}")


if __name__ == "__main__":
    main()
