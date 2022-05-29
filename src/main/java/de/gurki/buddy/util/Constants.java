package de.gurki.buddy.util;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;


public class Constants
{
    public static final int kSearchRadius = 16;
    public static final int kOffset = 3;
    public static final int kMargin = 2;
    public static final int kDepth = 12;

    public static final List<Block> kFillerBlocks = List.of(
        Blocks.RED_CONCRETE,
        Blocks.GREEN_CONCRETE,
        Blocks.BLUE_CONCRETE,
        Blocks.YELLOW_CONCRETE,
        Blocks.LIME_CONCRETE,
        Blocks.CYAN_CONCRETE,
        Blocks.ORANGE_CONCRETE,
        Blocks.MAGENTA_CONCRETE,
        Blocks.PINK_CONCRETE,
        Blocks.PURPLE_CONCRETE,
        Blocks.LIGHT_BLUE_CONCRETE,
        Blocks.LIGHT_GRAY_CONCRETE,
        Blocks.BROWN_CONCRETE,
        Blocks.GRAY_CONCRETE,
        Blocks.WHITE_CONCRETE,
        // "black_concrete",

        Blocks.RED_WOOL,
        Blocks.GREEN_WOOL,
        Blocks.BLUE_WOOL,
        Blocks.YELLOW_WOOL,
        Blocks.LIME_WOOL,
        Blocks.CYAN_WOOL,
        Blocks.ORANGE_WOOL,
        Blocks.MAGENTA_WOOL,
        Blocks.PINK_WOOL,
        Blocks.PURPLE_WOOL,
        Blocks.LIGHT_BLUE_WOOL,
        Blocks.LIGHT_GRAY_WOOL,
        Blocks.BROWN_WOOL,
        Blocks.GRAY_WOOL,
        Blocks.WHITE_WOOL
        // "black_wool"
    );
}
