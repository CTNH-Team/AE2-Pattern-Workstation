package com.ctnh.ae2pw.utils;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public interface Utils {

    public static boolean quickInsert(
            InternalInventory inventory,
            ItemStack stack
    ){
        return quickInsert(inventory, stack, false);
    }

    public static boolean quickInsert(
            InternalInventory inventory,
            ItemStack stack,
            boolean simulate
    ) {
        if (!stack.isEmpty() && !inventory.isItemValid(0, stack)) {
            return false;
        }

        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                if (!simulate) {
                    ItemStack toInsert = stack.copy();
                    toInsert.setCount(1);
                    inventory.setItemDirect(i, toInsert);
                }
                return true;
            }
        }

        return false;
    }

    public static ItemStack quickExtract(
            InternalInventory inventory,
            boolean simulate
    ) {
        for (int i = inventory.size() - 1; i >= 0; i--) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack result = stack.copy();
                result.setCount(1);

                if (!simulate) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        inventory.setItemDirect(i, ItemStack.EMPTY);
                    }
                }

                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean quickTransfer(
            InternalInventory from,
            InternalInventory to,
            boolean simulate
    ) {
        // 从后往前找可提取的物品
        for (int i = from.size() - 1; i >= 0; i--) {
            ItemStack stack = from.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            // 校验目标库存是否接受
            if (!to.isItemValid(0, stack)) {
                break;
            }

            // 查找目标空槽
            for (int j = 0; j < to.size(); j++) {
                if (to.getStackInSlot(j).isEmpty()) {

                    if (!simulate) {
                        // 插入 1 个
                        ItemStack one = stack.copy();
                        one.setCount(1);
                        to.setItemDirect(j, one);

                        // 源库存减 1
                        stack.shrink(1);
                        if (stack.isEmpty()) {
                            from.setItemDirect(i, ItemStack.EMPTY);
                        }
                    }

                    return true;
                }
            }
        }

        return false;
    }

    public static boolean quickTransferAll(
            InternalInventory from,
            InternalInventory to
    ) {
        boolean moved = false;

        // 指向目标库存当前可用的空槽
        int toIndex = 0;

        // 先把 toIndex 移到第一个空槽
        while (toIndex < to.size() && !to.getStackInSlot(toIndex).isEmpty()) {
            toIndex++;
        }

        // 从后往前遍历源库存
        for (int i = from.size() - 1; i >= 0 && toIndex < to.size(); i--) {
            ItemStack stack = from.getStackInSlot(i);
            if (stack.isEmpty()) {
                continue;
            }

            // 校验目标库存是否接受
            if (!to.isItemValid(0, stack)) {
                continue;
            }

            // 当前源槽能放多少放多少，但不回头找空槽
            while (!stack.isEmpty() && toIndex < to.size()) {
                // toIndex 必然指向空槽
                ItemStack one = stack.copy();
                one.setCount(1);
                to.setItemDirect(toIndex, one);

                stack.shrink(1);
                moved = true;

                // toIndex 向后推进到下一个空槽
                do {
                    toIndex++;
                } while (toIndex < to.size()
                        && !to.getStackInSlot(toIndex).isEmpty());
            }

            if (stack.isEmpty()) {
                from.setItemDirect(i, ItemStack.EMPTY);
            }
        }

        return moved;
    }



    public static BlockPos findSafeTeleportPos(ServerLevel level, BlockPos origin) {
        BlockPos start = origin.above();

        // 第一优先：正上方
        if (isSafe(level, start)) {
            return start;
        }

        // 最大搜索半径
        int maxRadius = 4;
        // 垂直搜索范围
        int maxVertical = 3;

        // 按“距离”逐层扩散
        for (int r = 1; r <= maxRadius; r++) {
            // 同一半径下，优先接近起始高度
            for (int dy = 0; dy <= maxVertical; dy++) {
                for (int sy = -1; sy <= 1; sy += 2) {
                    int yOffset = dy * sy;
                    if (dy == 0 && sy == -1) {
                        continue;
                    }

                    int y = start.getY() + yOffset;

                    // 在这一层高度，按“环”遍历
                    for (int dx = -r; dx <= r; dx++) {
                        int dz = r - Math.abs(dx);

                        // (+dz)
                        BlockPos p1 = new BlockPos(
                                start.getX() + dx,
                                y,
                                start.getZ() + dz
                        );
                        if (isSafe(level, p1)) {
                            return p1;
                        }

                        // (-dz)，避免 dz = 0 时重复
                        if (dz != 0) {
                            BlockPos p2 = new BlockPos(
                                    start.getX() + dx,
                                    y,
                                    start.getZ() - dz
                            );
                            if (isSafe(level, p2)) {
                                return p2;
                            }
                        }
                    }
                }
            }
        }

        // 兜底：强制抬高
        return origin.above(5);
    }

    private static boolean isSafe(ServerLevel level, BlockPos pos) {
        // 脚下必须是空气
        if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
            return false;
        }

        // 头部必须是空气
        BlockPos head = pos.above();
        if (!level.getBlockState(head).getCollisionShape(level, head).isEmpty()) {
            return false;
        }

        // 必须有可站立的地面
        BlockPos below = pos.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    public static float[] getLookAtRotation(ServerPlayer player, Vec3 feetPos, Vec3 target) {
        double eyeY = feetPos.y + player.getEyeHeight(player.getPose());

        double dx = target.x - feetPos.x;
        double dy = target.y - eyeY;
        double dz = target.z - feetPos.z;

        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));

        return new float[]{yaw, pitch};
    }

    static void drawSlotBorder(GuiGraphics gg, int x, int y, int color){
        gg.fill(x - 1, y - 1, x + 17, y, color);
        // 下
        gg.fill(x - 1, y + 16, x + 17, y + 17, color);
        // 左
        gg.fill(x - 1, y, x, y + 16, color);
        // 右
        gg.fill(x + 16, y, x + 17, y + 16, color);
    }

    Set<Component> craftingMachines = new HashSet<>();


    static boolean isCraftingMachine(PatternContainerGroup group){
        return craftingMachines.contains(group.name());
    }

    Set<Component> processingMachines = new HashSet<>();

    static boolean isProcessingMachine(PatternContainerGroup group){
        if(processingMachines.contains(group.name())) return true;
        else return !craftingMachines.contains(group.name());
    }

    static GenericStack[] multiply(ConfigInventory inv, int data) {
        boolean flag = data > 0;
        if (!flag) {
            data = -data;
        }
        GenericStack[] result = new GenericStack[inv.size()];
        for (int slot = 0; slot < inv.size(); ++slot) {
            GenericStack stack = inv.getStack(slot);
            if (stack != null) {
                if (flag) {
                    if (data * stack.amount() > Integer.MAX_VALUE) {
                        return null;
                    } else {
                        result[slot] = new GenericStack(stack.what(), data * stack.amount());
                    }
                } else {
                    if (stack.amount() % data != 0) {
                        return null;
                    } else {
                        result[slot] = new GenericStack(stack.what(), stack.amount() / data);
                    }
                }
            }
        }
        return result;
    }
}
