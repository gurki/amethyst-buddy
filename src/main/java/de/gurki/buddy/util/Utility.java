package de.gurki.buddy.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.StructureBlock;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.util.math.Direction;
import net.minecraft.state.property.Properties;
import static net.minecraft.block.Block.NOTIFY_LISTENERS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;



public class Utility
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );


    ////////////////////////////////////////////////////////////////////////////////
    public static List<BlockPos> computeBoundingBox( BlockPos center, World world )
    {
        LOGGER.info( "computing bounding box..." );

        final int searchRadius = Constants.kSearchRadius;

        final List<BlockPos> budList = BlockPos
            .streamOutwards( center, searchRadius, searchRadius, searchRadius )
            .filter( i -> world.getBlockState( i ).isOf( Blocks.BUDDING_AMETHYST ) )
            .map( i -> i.toImmutable() )
            .toList();

        final BlockBox box = BlockBox.encompassPositions( budList ).get();
        // highlightBox( box, world );

        List<BlockPos> minMax = new ArrayList<BlockPos>();
        minMax.add( new BlockPos( box.getMinX(), box.getMinY(), box.getMinZ() ));
        minMax.add( new BlockPos( box.getMaxX(), box.getMaxY(), box.getMaxZ() ));
        return minMax;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void clearArea( BlockPos boxMin, BlockPos boxMax, World world )
    {
        //  clear area, and carve out the three flying machine tunnels

        LOGGER.info( "clearing area..." );

        final Block bud = Blocks.BUDDING_AMETHYST;
        final BlockState air = Blocks.AIR.getDefaultState();
        final int m = Constants.kMargin;
        final int d = Constants.kDepth;

        BlockPos
            .stream( boxMin.getX() - m, boxMin.getY() - m, boxMin.getZ() - m, boxMax.getX() + m + d, boxMax.getY() + m, boxMax.getZ() + m )
            .filter( p -> ! world.getBlockState( p ).isOf( bud ) )
            .forEach( p -> world.setBlockState( p, air ));

        BlockPos
            .stream( boxMin.getX() - m, boxMin.getY() - m, boxMin.getZ() - m, boxMax.getX() + m, boxMax.getY() + m + d, boxMax.getZ() + m )
            .filter( p -> ! world.getBlockState( p ).isOf( bud ) )
            .forEach( p -> world.setBlockState( p, air ));

        BlockPos
            .stream( boxMin.getX() - m, boxMin.getY() - m, boxMin.getZ() - m, boxMax.getX() + m, boxMax.getY() + m, boxMax.getZ() + m + d )
            .filter( p -> ! world.getBlockState( p ).isOf( bud ) )
            .forEach( p -> world.setBlockState( p, air ));
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void generateBackside( BlockPos boxMin, BlockPos boxMax, World world )
    {
        //  create stopper walls

        LOGGER.info( "creating stopper walls..." );

        final BlockState obsidian = Blocks.OBSIDIAN.getDefaultState();
        final int m = Constants.kMargin;

        BlockPos
            .stream( boxMin.getX() - m - 1, boxMin.getY() - m, boxMin.getZ() - m, boxMin.getX() - m - 1, boxMax.getY() + m, boxMax.getZ() + m )
            .forEach( p -> world.setBlockState( p, obsidian ));

        BlockPos
            .stream( boxMin.getX() - m, boxMin.getY() - m - 1, boxMin.getZ() - m, boxMax.getX() + m, boxMin.getY() - m - 1, boxMax.getZ() + m )
            .forEach( p -> world.setBlockState( p, obsidian ));

        BlockPos
            .stream( boxMin.getX() - m, boxMin.getY() - m, boxMin.getZ() - m - 1, boxMax.getX() + m, boxMax.getY() + m, boxMin.getZ() - m - 1 )
            .forEach( p -> world.setBlockState( p, obsidian ));
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static List<HashSet<BlockPos>> projectBuds( BlockPos boxMin, BlockPos boxMax, World world )
    {
        //  project onto cardinal planes

        LOGGER.info( "projecting budding amethysts..." );

        final Block bud = Registry.BLOCK.get( new Identifier( "minecraft", "budding_amethyst" ) );

        final List<BlockPos> budList = BlockPos
            .stream( boxMin, boxMax )
            .filter( i -> world.getBlockState( i ).isOf( bud ) )
            .map( i -> i.toImmutable() )
            .toList();

        List<HashSet<BlockPos>> projs = Arrays.asList( new HashSet<BlockPos>(), new HashSet<BlockPos>(), new HashSet<BlockPos>() );

        for ( BlockPos p : budList ) {
            projs.get( 0 ).add( p.east( boxMax.getX() + Constants.kOffset - p.getX() ) );
            projs.get( 1 ).add( p.up( boxMax.getY() + Constants.kOffset - p.getY() ) );
            projs.get( 2 ).add( p.south( boxMax.getZ() + Constants.kOffset - p.getZ() ) );
        }

        return projs;
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static BlockPos pack( BlockPos pos, int dimension ) {
        switch ( dimension ) {
            case 0: return new BlockPos( pos.getY(), pos.getZ(), 0 );
            case 1: return new BlockPos( pos.getX(), pos.getZ(), 0 );
            default: return new BlockPos( pos.getX(), pos.getY(), 0 );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////
    public static BlockPos unpack( BlockPos pos, int dimension, int coord ) {
        switch ( dimension ) {
            case 0: return new BlockPos( coord, pos.getX(), pos.getY() );
            case 1: return new BlockPos( pos.getX(), coord, pos.getY() );
            default: return new BlockPos( pos.getX(), pos.getY(), coord );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void highlightBox( BlockBox box, World world ) {
        // Highlight the geode area.
        int commandBlockOffset = 2+1;
        BlockPos structureBlockPos = new BlockPos( box.getMinX()-commandBlockOffset, box.getMinY()-commandBlockOffset, box.getMinZ()-commandBlockOffset);
        BlockState structureBlockState = Blocks.STRUCTURE_BLOCK.getDefaultState().with(StructureBlock.MODE, StructureBlockMode.SAVE);
        world.setBlockState(structureBlockPos, structureBlockState, NOTIFY_LISTENERS);
        StructureBlockBlockEntity structure = (StructureBlockBlockEntity) world.getBlockEntity(structureBlockPos);
        if (structure == null) {
            LOGGER.error("StructureBlock tile entity is missing... this should never happen????");
            return;
        }
        structure.setStructureName("box");
        structure.setOffset(new BlockPos(commandBlockOffset, commandBlockOffset, commandBlockOffset));
        structure.setSize(box.getDimensions().add(1, 1, 1));
        structure.setShowBoundingBox(true);
        structure.markDirty();
    }


    ////////////////////////////////////////////////////////////////////////////////
    //  with thanks adapted from kosma [1]
    //  [1] https://github.com/kosma/geodesy-fabric/blob/master/src/main/java/pl/kosma/geodesy/GeodesyCore.java#L363
    public static void buildMachine( BlockPos pos, Direction directionAlong, Direction directionUp, Block stickyBlock, World world ) {
        /*
         * It looks like this:
         * S HHH
         * S HVHH[<N
         * SB[L>]SSSB
         */
        // Blocker block.
        // world.setBlockState(blockerPos, Blocks.OBSIDIAN.getDefaultState(), NOTIFY_LISTENERS );
        // Clear out the machine marker blocks.
        world.setBlockState(pos.offset(directionUp, 0), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), Blocks.AIR.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);
        // First layer: piston, 2 slime
        world.setBlockState(pos.offset(directionUp, 0), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);
        // Second layer: redstone lamp, observer, slime (order is important)
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionUp), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 0), Blocks.REDSTONE_LAMP.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);
        // Third layer: observer, slime, slime
        world.setBlockState(pos.offset(directionUp, 0), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 2), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);
        // Fourth layer: piston, slime
        world.setBlockState(pos.offset(directionUp, 0), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 1);
        // Fifth layer: slime, piston
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.STICKY_PISTON.getDefaultState().with(Properties.FACING, directionAlong.getOpposite()), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 2);
        // [SKIP!] Seventh layer: slime, note block
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.NOTE_BLOCK.getDefaultState(), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, -1);
        // [GO BACK!] Sixth layer: slime, observer
        // This one is tricky, we initially set the observer in a wrong direction
        // so the note block tune change is not triggered.
        world.setBlockState(pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionUp), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 0), stickyBlock.getDefaultState(), NOTIFY_LISTENERS );
        world.setBlockState(pos.offset(directionUp, 1), Blocks.OBSERVER.getDefaultState().with(Properties.FACING, directionAlong), NOTIFY_LISTENERS );
        pos = pos.offset(directionAlong, 2);
        // [SKIP AGAIN!] Eighth layer: blocker
        world.setBlockState(pos.offset(directionUp, 0), Blocks.CRYING_OBSIDIAN.getDefaultState(), NOTIFY_LISTENERS );
    }
}
