# FancyShop
Create chest shops that trade in physical currencies.

## Features
* Create chest shops with a graphical interface.
* Use in-game items as currency.
* Buy and sell any item - enchanted items and books, potions, even items with lore or custom NBT data.
* Buy and sell up to 27 different items in a single shop.
* Shops are protected against damage and against stealing with hoppers.

## Why another shop plugin?
I'm not a fan of economy plugins. While they might make things simpler on large servers, they don't seem very Minecraft-y to me. They add a layer of abstraction that I feel takes away from the survival aspects. However, I like having shops - they give players more choice where to spend their time. If you don't want to spend time mining cobble or building a big farm, you can buy them from someone who does. So I wanted a shop plugin that can use in-game valuables rather than creating an artificial currency.

### Why not use GoldIsMoney, Gringotts, etc.?
Because I don't want to inflate any one item (gold, emeralds, diamonds, etc.) by making it the sole currency. On my small whitelisted server, I don't need world protection or other plugins that need Vault compatibility.

### Why not use PhysicalShop?
I have been for a while, but PhysicalShop can't handle enchanted items, dyed armor, heads, custom potions, or other complex items, and it can't sell different types of items from the same chest. In addition, the sign-based interface can be a bit tricky - item names on the server don't always match the displayed names on the client, and dealing with data values is annoying.

## Commands
/fancyshop: Create and manage shops. (Aliases: /fs, /shop)
    /fancyshop create - Create a new shop.
    /fancyshop remove - Remove a shop.

## Permissions
    fancyshop.create:
        description: Create a shop.
        default: true
    fancyshop.use:
        description: Use other people's shops.
        default: true
    fancyshop.remove:
        description: Remove someone else's shop (you can always remove your own)
        default: op
    fancyshop.open:
        description: Open chests for other people's shops.
        default: op

## GitHub
[https://github.com/DeltaWhy/bukkit-fancyshop](https://github.com/DeltaWhy/bukkit-fancyshop)
