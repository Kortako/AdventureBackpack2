package com.darkona.adventurebackpack.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Created on 10.04.2017
 *
 * @author Ugachaga
 */
@SuppressWarnings("WeakerAccess")
abstract class ContainerAdventureBackpack extends Container
{
    protected static final int PLAYER_HOT_START = 0;
    protected static final int PLAYER_HOT_END = PLAYER_HOT_START + 8;
    protected static final int PLAYER_INV_START = PLAYER_HOT_END + 1;
    protected static final int PLAYER_INV_END = PLAYER_INV_START + 26;
    protected static final int PLAYER_INV_LENGTH = PLAYER_INV_END + 1;

    protected void bindPlayerInventory(InventoryPlayer invPlayer, int startX, int startY)
    {
        for (int x = 0; x < 9; x++) // hotbar - 9 slots
        {
            addSlotToContainer(new Slot(invPlayer, x, (startX + 18 * x), (58 + startY)));
        }

        for (int y = 0; y < 3; y++) // inventory - 9*3, 27 slots
        {
            for (int x = 0; x < 9; x++)
            {
                addSlotToContainer(new Slot(invPlayer, (x + y * 9 + 9), (startX + 18 * x), (startY + y * 18)));
            }
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int fromSlot)
    {
        Slot slot = getSlot(fromSlot);

        if (slot == null || slot.getStack() == null )
            return null;

        ItemStack stack = slot.getStack();
        ItemStack result = stack.copy();

        if (fromSlot >= PLAYER_INV_LENGTH)
        {
            if (!mergePlayerInv(stack))
                return null;
        }

        if (fromSlot < PLAYER_INV_LENGTH)
        {
            if (!transferStackToPack(stack))
                return null;
        }

        if (stack.stackSize == 0)
        {
            slot.putStack(null);
        } else
        {
            slot.onSlotChanged();
        }

        if (stack.stackSize == result.stackSize)
            return null;

        slot.onPickupFromSlot(player, stack);
        return result;
    }

    protected boolean mergePlayerInv(ItemStack stack)
    {
        return mergeItemStack(stack, PLAYER_HOT_START, PLAYER_INV_END + 1, false);
    }

    abstract protected boolean transferStackToPack(ItemStack stack);

    @Override
    protected boolean mergeItemStack(ItemStack initStack, int minIndex, int maxIndex, boolean backward)
    {
        boolean changesMade = false;
        int activeIndex = (backward ? maxIndex - 1 : minIndex);
        Slot activeSlot;
        ItemStack activeStack;

        if (initStack.isStackable())
        {
            while (initStack.stackSize > 0 && (!backward && activeIndex < maxIndex || backward && activeIndex >= minIndex))
            {
                activeSlot = (Slot) this.inventorySlots.get(activeIndex);
                activeStack = activeSlot.getStack();

                if (activeStack != null && activeStack.getItem() == initStack.getItem()
                        && (!initStack.getHasSubtypes() || initStack.getItemDamage() == activeStack.getItemDamage())
                        && ItemStack.areItemStackTagsEqual(initStack, activeStack))
                {
                    int mergedSize = activeStack.stackSize + initStack.stackSize;
                    int maxStackSize = Math.min(initStack.getMaxStackSize(), activeSlot.getSlotStackLimit());

                    if (mergedSize <= maxStackSize)
                    {
                        initStack.stackSize = 0;
                        activeStack.stackSize = mergedSize;
                        activeSlot.onSlotChanged();
                        changesMade = true;
                    } else if (activeStack.stackSize < maxStackSize && !(activeSlot instanceof SlotFluid))
                    {
                        initStack.stackSize -= maxStackSize - activeStack.stackSize;
                        activeStack.stackSize = maxStackSize;
                        activeSlot.onSlotChanged();
                        changesMade = true;
                    }
                }
                activeIndex += (backward ? -1 : 1);
            }
        }

        if (initStack.stackSize > 0)
        {
            activeIndex = (backward ? maxIndex - 1 : minIndex);

            while (!backward && activeIndex < maxIndex || backward && activeIndex >= minIndex)
            {
                activeSlot = (Slot) this.inventorySlots.get(activeIndex);
                activeStack = activeSlot.getStack();

                if (activeStack == null /*&& activeSlot.isItemValid(initStack)*/)
                {
                    ItemStack copyStack = initStack.copy();
                    int mergedSize = copyStack.stackSize = Math.min(copyStack.stackSize, activeSlot.getSlotStackLimit());

                    activeSlot.putStack(copyStack);
                    if (mergedSize >= initStack.stackSize)
                    {
                        initStack.stackSize = 0;
                    } else
                    {
                        initStack.stackSize -= mergedSize;
                    }
                    changesMade = true;
                    break;
                }
                activeIndex += (backward ? -1 : 1);
            }
        }

        return changesMade;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player)
    {
        return true;
    }
}
