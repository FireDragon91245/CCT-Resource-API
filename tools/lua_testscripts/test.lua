local a = resourceapi.getBlockInfo("minecraft:grass_block", "m")
for k, _ in pairs(a.model.textures) do print(k) end
local texture = a.model.textures["block/grass_block_side"]
local str = resourceapi.imageBytesToCCFormat(texture)
print(str)
local img = paintutils.parseImage(str)
paintutils.drawImage(img, 1, 1)
