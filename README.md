# CC:Tweaked Resource API

This project aims to add a simple but very flexible API to CC:Tweaked that allows access to resources via the Lua API.  
The API is "read-only"—you can only retrieve information, but you can't modify it.

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
