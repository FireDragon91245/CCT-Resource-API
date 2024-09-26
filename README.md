# CC:Tweaked Resource API

This project aims to add a simple but very flexible API to CC:Tweaked that allows access to resources via the Lua API.  
The API is "read-only"—you can only retrieve information, but you can't modify it.

# Legal Notice
- The Minecraft EULA applies: [DE](https://www.minecraft.net/de-de/terms/r2) | [EN](https://www.minecraft.net/en-us/terms/r2).
- Any content under the folder [src/main/resources/bundled_resources/minecraft](https://github.com/FireDragon91245/CCT-Resource-API/tree/master/src/main/resources/bundled_resources/minecraft) belongs to Microsoft (Mojang) and is used under the constraints of section 1, "ONE MAJOR RULE," and section 4, "MINECRAFT: JAVA EDITION" of the EULA.
- The license of this project ONLY applies to content outside of the `src/main/resources/bundled_resources/minecraft` directory.  
  
> [!CAUTION]
> v1.1 1.18.2 & 1.19.4 releases hava a major flaw  
> **These releases have a major flaw and do not have the correct version vanilla resourcepack bundled  
> Content added newer then 1.16.5 will not be accessable trough the API**

### v1.0
- Initial release with support for accessing textures, models, blockstate models, and crafting recipes.

### v1.1
- Introduced item textures and models.
- Improved texture handling APIs, allowing extraction of pixel and color data directly from raw image bytes.

### v1.2
- Added APIs for mass information retrieval, providing two variants for every method. For example, `getItemInfo` retrieves details for a single item, while `getItemInfos` can handle multiple items at once.

### v1.3
- Introduced new APIs for handling enchantments and sounds, expanding functionality in both areas.

> [!NOTE]
> For syntax and examples, please refer to the [wiki](https://github.com/FireDragon91245/CCT-Resource-API/wiki).
