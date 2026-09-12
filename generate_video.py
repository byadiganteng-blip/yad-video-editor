#!/usr/bin/env python3
# ============================================================
#  AI STORY-TO-VIDEO GENERATOR
#  - TTS: Piper (offline, tanpa API)
#  - Fallback: tanpa audio jika model tidak ada
# ============================================================

import os, re, subprocess, wave, textwrap, time
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


# ============================================================
#  PIPER TTS — OFFLINE, TANPA API
# ============================================================
PIPER_MODEL_MAP = {
    # Indonesia
    "male_id":   "id_ID-male",
    "female_id": "id_ID-female",
    "child_id":  "id_ID-male",
    # English
    "male_en":   "en_US-male",
    "female_en": "en_US-female",
    "robot":     "en_US-male",
}

PIPER_BASE_URL = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
PIPER_MODEL_DIR = Path("piper_models")
PIPER_MODEL_DIR.mkdir(exist_ok=True)
VOICE_FILE = "voice.wav"


def download_piper_model(voice_name):
    """Download model Piper jika belum ada."""
    PIPER_MODEL_DIR.mkdir(exist_ok=True)

    # Contoh: id_ID-male → id/id_ID/male/id_ID-male-medium.onnx
    parts = voice_name.split("-")
    lang_full = parts[0]   # id_ID
    gender = parts[1]      # male
    lang_short = lang_full.split("_")[0]  # id

    # Path HuggingFace
    base = f"{PIPER_BASE_URL}/{lang_short}/{lang_full}/{gender}/{voice_name}"
    onnx_url = f"{base}-medium.onnx"
    json_url = f"{base}-medium.onnx.json"

    onnx_path = PIPER_MODEL_DIR / f"{voice_name}.onnx"
    json_path = PIPER_MODEL_DIR / f"{voice_name}.onnx.json"

    import urllib.request
    for url, path in [(onnx_url, onnx_path), (json_url, json_path)]:
        if not path.exists():
            print(f"  📥 Download: {url}")
            try:
                urllib.request.urlretrieve(url, path)
                print(f"  ✅ Saved: {path}")
            except Exception as e:
                print(f"  ❌ Gagal download {url}: {e}")
                return None, None

    return str(onnx_path), str(json_path)


def make_voice_piper(text):
    """Generate voice dengan Piper TTS."""
    if VOICE not in PIPER_MODEL_MAP:
        print(f"⚠️  Voice '{VOICE}' tidak ada di Piper map")
        return None

    voice_name = PIPER_MODEL_MAP[VOICE]
    print(f"\n🎤 Generating voice-over (Piper: {voice_name})...")

    onnx_path, json_path = download_piper_model(voice_name)
    if not onnx_path or not os.path.exists(onnx_path):
        print("  ❌ Model Piper tidak tersedia")
        return None

    # Pakai piper via command line (lebih reliable)
    try:
        import shutil
        piper_bin = shutil.which("piper")
        if not piper_bin:
            # Coba lewat python module
            cmd = [
                "python", "-m", "piper",
                "--model", onnx_path,
                "--config", json_path,
                "--output_file", VOICE_FILE,
            ]
        else:
            cmd = [
                piper_bin,
                "--model", onnx_path,
                "--config", json_path,
                "--output_file", VOICE_FILE,
            ]

        result = subprocess.run(
            cmd, input=text.encode("utf-8"),
            capture_output=True, timeout=300
        )

        if result.returncode == 0 and os.path.exists(VOICE_FILE):
            size = os.path.getsize(VOICE_FILE)
            if size > 1000:
                print(f"  ✅ Saved: {VOICE_FILE} ({size} bytes)")
                return VOICE_FILE
            else:
                print(f"  ⚠️  File terlalu kecil: {size} bytes")
                return None
        else:
            print(f"  ❌ Piper error: {result.stderr.decode()[:200]}")
            return None

    except Exception as e:
        print(f"  ❌ Exception: {type(e).__name__}: {e}")
        return None


def make_voice(text):
    """Wrapper — coba Piper, fallback ke tanpa audio."""
    return make_voice_piper(text)


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
    print(f"TTS: Piper (offline) | VOICE: {VOICE}")
    print("=" * 70)

    scenes = split_story_into_scenes(PROMPT, SCENES_COUNT)
    print(f"Cerita dipecah jadi {len(scenes)} scene")

    image_paths = []
    for i, scene in enumerate(scenes):
        try: image_paths.append(generate_image(scene, i, IMAGE_STYLE))
        except Exception as e: print(f"Scene {i+1} gagal: {e}")

    if not image_paths: raise SystemExit("Tidak ada gambar di-generate.")

    # TTS Piper
    voice_path = make_voice(PROMPT)

    if voice_path and os.path.exists(voice_path):
        try:
            audio = AudioFileClip(voice_path)
            total_duration = audio.duration + 0.5
            print(f"✅ Audio: {audio.duration:.1f}s")
        except Exception as e:
            print(f"⚠️  Audio error: {e}")
            audio = None
            total_duration = 4.0 * len(image_paths)
    else:
        audio = None
        total_duration = 4.0 * len(image_paths)
        print(f"⚠️  Tanpa audio — durasi: {total_duration}s")

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
        try:
            audio = audio.set_duration(final.duration)
            final = final.set_audio(audio)
        except Exception as e: print(f"Set audio error: {e}")

    final.write_videofile(OUTPUT_MP4, fps=24, codec="libx264",
                          audio_codec="aac", preset="medium", threads=4)
    print(f"SELESAI: {OUTPUT_MP4}")


if __name__ == "__main__":
    main()
