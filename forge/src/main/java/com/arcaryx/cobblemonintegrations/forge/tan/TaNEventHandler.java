package com.arcaryx.cobblemonintegrations.forge.tan;


import com.arcaryx.cobblemonintegrations.forge.CobblemonIntegrationsForge;
import com.cobblemon.mod.common.api.types.ElementalTypes;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import toughasnails.api.item.TANItems;
import toughasnails.api.thirst.WaterType;
import toughasnails.item.EmptyCanteenItem;

import java.util.Objects;

public class TaNEventHandler {
    @SubscribeEvent
    public static void onRightClickPokemon(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof PokemonEntity pokemon)) {
            return;
        }

        var player = event.getEntity();
        if (CobblemonIntegrationsForge.CONFIG.pokemonGiveWater.get() && pokemon.isOwnedBy(player) &&
                (Objects.equals(pokemon.getPokemon().getPrimaryType(), ElementalTypes.INSTANCE.getWATER()) ||
                        (!CobblemonIntegrationsForge.CONFIG.requirePrimaryType.get() &&
                                Objects.equals(pokemon.getPokemon().getSecondaryType(), ElementalTypes.INSTANCE.getWATER())))) {
            ItemStack itemstack = player.getItemInHand(event.getHand());

            var level = pokemon.getPokemon().getLevel();
            WaterType water = null;

            if (level >= CobblemonIntegrationsForge.CONFIG.minLevelForPurified.get()) {
                water = WaterType.PURIFIED;
            }
            else if (level >= CobblemonIntegrationsForge.CONFIG.minLevelForNormal.get()) {
                water = WaterType.NORMAL;
            }
            else if (level >= CobblemonIntegrationsForge.CONFIG.minLevelForDirty.get()) {
                water = WaterType.DIRTY;
            }

            if (CobblemonIntegrationsForge.CONFIG.fillBottle.get() && water != null && itemstack.is(Items.GLASS_BOTTLE)) {
                player.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
                ItemStack filledStack = switch (water) {
                    case PURIFIED -> new ItemStack(TANItems.PURIFIED_WATER_BOTTLE);
                    case DIRTY -> new ItemStack(TANItems.DIRTY_WATER_BOTTLE);
                    default -> PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
                };
                fillItem(event, player, itemstack, filledStack);
                return;
            }

            if (CobblemonIntegrationsForge.CONFIG.fillCanteen.get() && water != null &&
                    itemstack.getItem() instanceof EmptyCanteenItem canteen) {
                player.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
                ItemStack filledStack = switch (water) {
                    case PURIFIED -> new ItemStack(canteen.getPurifiedWaterCanteen());
                    case DIRTY -> new ItemStack(canteen.getDirtyWaterCanteen());
                    default -> PotionUtils.setPotion(new ItemStack(canteen.getWaterCanteen()), Potions.WATER);
                };
                fillItem(event, player, itemstack, filledStack);
            }
        }
    }

    private static void fillItem(PlayerInteractEvent.EntityInteract event, Player player, ItemStack itemstack, ItemStack filledStack) {
        ItemStack replacementStack = ItemUtils.createFilledResult(itemstack, player, filledStack);
        if (itemstack != replacementStack) {
            player.setItemInHand(event.getHand(), replacementStack);
            if (replacementStack.isEmpty()) {
                ForgeEventFactory.onPlayerDestroyItem(player, itemstack, event.getHand());
            }
        }
        player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
        event.setCanceled(true);
        event.setCancellationResult(InteractionResultHolder.sidedSuccess(replacementStack, event.getSide().isClient()).getResult());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
            PokemonTemperatureModifier.registerModifier();
    }
}
