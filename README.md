<p align="center"><img src="mc-26.1/src/main/resources/assets/vulkanmotionblur/icon.png" width="128" alt="Vulkan Blur icon"></p>

# Vulkan Blur

**A lightweight motion blur mod for Minecraft (Fabric) that works with VulkanMod and Minecraft's own Vulkan renderer.**

- Works with **VulkanMod**, **Minecraft's built-in Vulkan mode (26.2+)**, Sodium and vanilla
- **Same blur at any FPS** (frame-time weighted)
- **HUD stays sharp**: only the world is blurred
- About **3% FPS**; zero cost when off or when nothing moves
- Optional **Only While Turning** mode and a toggle key
- Settings in **Mod Menu**

Download: https://modrinth.com/mod/vulkan-blur

## Versions

| Folder | Minecraft | Java | Tested with |
|---|---|---|---|
| `mc-26.3` | 26.3 | 25 | Built-in Vulkan + Mod Menu |
| `mc-26.2` | 26.2 | 25 | Built-in Vulkan + Mod Menu |
| `mc-26.1` | 26.1.x | 25 | VulkanMod + Mod Menu |
| `mc-1.21.11` | 1.21.11 | 21 | VulkanMod + Mod Menu |

## Building

Requires a JDK 25 (it can build every version).

```
cd mc-26.3        # or mc-26.2, mc-26.1, mc-1.21.11
gradle build
```

The jar is written to `build/libs/`.

## How it works

The mod adds a small vanilla post-effect chain (`assets/vulkanmotionblur/post_effect/motion_blur.json`):

1. Blend the current frame with a persistent history buffer.
2. Copy the result back into the history buffer.
3. Copy the result to the screen.

The blend weight is `exp(-frameTime / blurLength)`, written to the pass's uniform buffer every frame, so the blur looks the same at any frame rate. It runs before the GUI is drawn, so the HUD is never blurred. Because it uses Minecraft's own post-effect system, any renderer that supports vanilla effects (VulkanMod, or Minecraft's own Vulkan/OpenGL backends) can run it.

## License

MIT
