local box = require("pixelbox_lite").new(term.current())

local pick = resourceapi.getItemInfo("minecraft:stone_pickaxe", "m")
local texture = pick.model.textures["minecraft:item/stone_pickaxe"]
local pixels = resourceapi.imageBytesToPixels(texture, "decimal")

for x, row in pairs(pixels) do
  for y, color in pairs(row) do
    box.canvas[x][y] = color
  end
end
box:render()
