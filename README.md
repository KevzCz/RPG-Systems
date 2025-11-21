# RPG SYSTEMS

## How to make a title

```data/namespace/rpgsystems-title/*.json```

Titles will have:
```
{
  "name": "A required name",
  "description": "An optional description",
  // Default is true, doesn't load the title if false (Optional)
  "enabled": false
  // Default is false, hides from the list if true (Optional)
  "hidden": true
  // Optional bonuses section - only active when title is equipped
  "bonuses": [
    {
    // Optional
      "attribute": [
        { "id": "minecraft:generic.attack_damage", "amount": 0.10, "operation": "multiply_total" },
        { "id": "minecraft:generic.max_health",     "amount": 2.0,  "operation": "add_value" }
      ],
    // Optional
      "power": [
        "rpg-systems:illuminate"
      ],
    // Optional
      "spell": [
        "paladins:heal"
      ],
    // Optional. Operation: added or multiplied
      "damage_bonus": [
        { "id": "minecraft:ender_dragon", "amount": 0.15, "operation": "multiplied" },
        { "tag": "#minecraft:undead", "amount": 0.15, "operation": "multiplied" }
      ]
    }
  ],
  // Optional permanent bonuses section - always active once title is unlocked
  "perma_bonuses": [
    {
    // Optional
      "attribute": [
        { "id": "minecraft:generic.max_health", "amount": 4.0, "operation": "add_value" },
        { "id": "minecraft:generic.armor", "amount": 0.10, "operation": "multiply_total" }
      ],
    // Optional
      "power": [
        "rpg-systems:illuminate"
      ],
    // Optional
      "spell": [
        "paladins:heal"
      ],
    // Optional. Operation: added or multiplied
      "damage_bonus": [
        { "id": "minecraft:zombie", "amount": 5.0, "operation": "added" },
        { "tag": "#minecraft:undead", "amount": 0.20, "operation": "multiplied" }
      ]
    }
  ],
  // Optional conditions section
  "conditions": [
    { "type": "advancement", "advancement": "minecraft:end/kill_dragon" }
  ]
}
```
### Name tokens

| Token           | Syntax                        | Effect                             | Params (optional → default)       | Close With                 |
| --------------- | ----------------------------- | ---------------------------------- | --------------------------------- | -------------------------- |
| Color           | `{color:#RRGGBB}`             | Sets a solid color                 | `#RRGGBB` (hex)                   | `{/color}` or `{reset}`    |
| Gradient        | `{gradient:#c1,#c2[,#c3...]}` | Smooth color blend across the text | Comma-separated hex colors        | `{/gradient}` or `{reset}` |
| Rainbow         | `{rainbow[:speed]}`           | Cycling rainbow                    | `speed` float → `0.18`            | `{/rainbow}` or `{reset}`  |
| Pulse           | `{pulse[:speed]}`             | Brightness pulsing                 | `speed` float → `1.0`             | `{/pulse}` or `{reset}`    |
| Wiggle          | `{wiggle:amp=A,speed=S}`      | Vertical jiggle per character      | `amp` px → `1.0`, `speed` → `5.6` | `{/wiggle}` or `{reset}`   |
| Shake           | `{shake[:amp]}`               | Horizontal jitter                  | `amp` px → `1.0`                  | `{/shake}` or `{reset}`    |
| Bounce          | `{bounce:amp=A,speed=S}`      | Up-and-down bounce (abs(sin))      | `amp` px → `1.0`, `speed` → `4.5` | `{/bounce}` or `{reset}`   |
| Wave            | `{wave:amp=A,speed=S}`        | Horizontal sine wave               | `amp` px → `1.0`, `speed` → `3.0` | `{/wave}` or `{reset}`     |
| Glitch          | `{glitch:intensity=I}`        | Occasional jitter + tint           | `intensity` 0..1 → `0.5`          | `{/glitch}` or `{reset}`   |
| Clear/Reset     | `{clear}` or `{reset}`        | Clears all active styles           | None                              | —                          |
| Legacy MC codes | `&0..&f`, `&r`                | Vanilla color codes and reset      | Standard `&` codes                | `&r`                       |

### Conditions

| Type                     | What it tracks                                  | required/optional json fields                                                                                       | Examples                                                                            |
| ------------------------ | ----------------------------------------------- |---------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------|
| `obtain_item`            | Peak amount of an item you’ve ever held at once | `item` (id), `count` (int, default 1)                                                                               | `{ "type":"obtain_item", "item":"minecraft:crafting_table", "count":1 }`            |
| `kill_mobs`              | Number of kills of certain mobs                 | `entity_type` (id)  or `entity_tag` **or** `entity` (`"any"`/`"ns:id"`/`"ns:*"`), `count` (int, default 1), `nbt` (string, optional) | `{ "type":"kill_mobs", "entity_type":"minecraft:zombie", "count":100 }`             |
| `deal_damage_total`      | Total damage dealt (sum of hits)                | `entity_type` or `entity_tag`  or `entity`, `count` (damage threshold), `nbt` (optional)                                             | `{ "type":"deal_damage_total", "entity":"any", "count":500 }`                       |
| `deal_damage_max`        | Highest single-hit damage                       | `entity_type` or `entity_tag`  or `entity`, `count` (min single-hit), `nbt` (optional)                                               | `{ "type":"deal_damage_max", "entity":"any", "count":20 }`                          |
| `interact_block`         | Right-click interactions on a block             | `block` (id), `count` (int, default 1)                                                                              | `{ "type":"interact_block", "block":"minecraft:crafting_table", "count":5 }`        |
| `interact_entity`        | Right-click interactions on mobs                | `entity_type` or `entity_tag` or `entity`, `count` (int, default 1), `nbt` (optional)                               | `{ "type":"interact_entity", "entity_type":"minecraft:villager", "count":1 }`       |
| `advancement`            | Completion of a specific advancement            | `advancement` (id)                                                                                                  | `{ "type":"advancement", "advancement":"minecraft:adventure/hero_of_the_village" }` |
| `reach_level_xp`         | Player XP level                         | `level` (int)                                                                                                       | `{ "type":"reach_level_xp", "level":20 }`                                           |
| `reach_level_pufferfish` | Total Puffish Skills level                      | `level` (int)                                                                                                       | `{ "type":"reach_level_pufferfish", "level":10 }`                                   |
| `walk_blocks`            | Distance walked (on ground)                     | `distance` (int, blocks)                                                                                            | `{ "type":"walk_blocks", "distance":5000 }`                                         |
| `craft_item`             | Lifetime crafted count of an item               | `item` (id), `count` (int, default 1)                                                                               | `{ "type":"craft_item", "item":"minecraft:diamond_pickaxe", "count":1 }`            |
| `mine_blocks`            | Lifetime mined count of a block                 | `block` (id), `count` (int, default 1)                                                                              | `{ "type":"mine_blocks", "block":"minecraft:stone", "count":256 }`                  |
| `visit_biome`            | Visiting a biome                                | `biome` (id)                                                                                                        | `{ "type":"visit_biome", "biome":"minecraft:jungle" }`                              |
| `enter_dimension`        | Entering a dimension                            | `dimension` (id)                                                                                                    | `{ "type":"enter_dimension", "dimension":"minecraft:the_nether" }`                  |
| `check_attribute`        | Current value of a player attribute             | `attribute` (id), `min` (number)                                                                                    | `{ "type":"check_attribute", "attribute":"minecraft:generic.armor", "min":8.0 }`    |
| `find_structure`         | Being inside a structure                        | `structure` (id)                                                                                                    | `{ "type":"find_structure", "structure":"minecraft:fortress" }`                     |

| Field         | What it accepts                  | Examples                                                             | Notes                                                                                                                             |
| ------------- | -------------------------------- | -------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------- |
| `entity_type` | Exact entity id (fastest)        | `minecraft:zombie`, `minecraft:blaze`, `modid:custom_mob`            | Use this when you know the exact id. No wildcards.                                                                                |
| `entity`      | Flexible string spec             | `any` • `minecraft:*` • `modid:*` • `minecraft:zombie`               | Supports `any`, exact id, or **namespace-wide wildcard** (`namespace:*`). No partial name wildcards like `zomb*`.                 |
| `nbt`         | Extra filter on the target’s NBT | `tag:Boss` • `CustomName:"Zombie King"` • `NoAI:1b` • `Health:20.0f` | Two modes: `tag:<CommandTag>` checks command tags; otherwise it does a **substring match** on full SNBT. Match is case-sensitive. |


### Title sprite
- Depending on the title's id: ``` rpg-systems:free_the_end ``` = ```free the end```. 
- It will look inside ```textures/block/title/free_the_end.png``` and also supports ```free_the_end.png.mcemeta```

### Title power
- Create a class that implements ```TitlePower```
- Create an ID for the power:

```public static final Identifier ID = Identifier.of("rpg-systems", "illuminate");```

- For more information, check ```IlluminatePower```