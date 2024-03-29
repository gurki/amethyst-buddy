package de.gurki.buddy.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.StructureBlockBlockEntity;
import net.minecraft.block.enums.StructureBlockMode;
import net.minecraft.block.StructureBlock;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

import static net.minecraft.block.Block.NOTIFY_LISTENERS;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;


public class Utility
{
    public static final String MOD_ID = "de.gurki.buddy";
	public static final Logger LOGGER = LogManager.getLogger( MOD_ID );


    ////////////////////////////////////////////////////////////////////////////////
    public static void drawClustersXYZ( List<ArrayList<HashSet<BlockPos>>> clustersXYZ, int[] offs, World world ) {
        for ( int dim = 0; dim < 3; dim++ ) {
            drawClusters( clustersXYZ.get( dim ), offs, world, dim );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void drawClusters( ArrayList<HashSet<BlockPos>> clusters, int[] offs, World world, int dim )
    {
        List<BlockState> fillerStates = Constants.kFillerBlocks.stream().map( Block::getDefaultState ).toList();

        for ( var i = 0; i < clusters.size(); i++ ) {
            BlockState state = fillerStates.get( i % fillerStates.size() );
            clusters.get( i ).forEach( p -> world.setBlockState( Utility.unpack( p, dim, offs[ dim ] ), state ) );
        }
    }


    ////////////////////////////////////////////////////////////////////////////////
    public static void drawValidity( List<ArrayList<HashSet<BlockPos>>> clustersXYZ, int[] offs, World world )
    {
        HashMap<Clusters.Validity, BlockState> states = new HashMap<>();
        states.put( Clusters.Validity.Valid, Blocks.LIME_CONCRETE.getDefaultState() );
        states.put( Clusters.Validity.TooSmall, Blocks.PINK_CONCRETE.getDefaultState() );
        states.put( Clusters.Validity.TooLarge, Blocks.MAGENTA_CONCRETE.getDefaultState() );
        states.put( Clusters.Validity.NoStraight, Blocks.PURPLE_CONCRETE.getDefaultState() );

        for ( int dim = 0; dim < 3; dim++ ) {
            final int currDim = dim;
            clustersXYZ.get( dim ).forEach( cluster -> {
                final Clusters.Validity validity = Clusters.validate( cluster );
                cluster.forEach( p -> world.setBlockState( Utility.unpack( p, currDim, offs[ currDim ] ), states.get( validity ) ) );
            });
        }
    }


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
        final BlockState stone = Blocks.STONE.getDefaultState();
        final int m = Constants.kMargin;
        final int d = Constants.kDepth;

        //  build stone walls to keep water out

        BlockPos
            .stream( boxMax.getX() + m, boxMin.getY() - m - 1, boxMin.getZ() - m - 1, boxMax.getX() + m + d + 1, boxMax.getY() + m + 1, boxMax.getZ() + m + 1 )
            .forEach( p -> world.setBlockState( p, stone ) );

        BlockPos
            .stream( boxMin.getX() - m - 1, boxMax.getY() + m, boxMin.getZ() - m - 1, boxMax.getX() + m + 1, boxMax.getY() + m + d + 1, boxMax.getZ() + m + 1 )
            .forEach( p -> world.setBlockState( p, stone ) );

        BlockPos
            .stream( boxMin.getX() - m - 1, boxMin.getY() - m - 1, boxMax.getZ() + m, boxMax.getX() + m + 1, boxMax.getY() + m + 1, boxMax.getZ() + m + d + 1 )
            .forEach( p -> world.setBlockState( p, stone ) );

        //  clear out all non-amethyst blocks in the area

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
    public static StructureBlockBlockEntity highlightBox( BlockBox box, World world )
    {
        int commandBlockOffset = 1;
        BlockPos structureBlockPos = new BlockPos( box.getMinX() - commandBlockOffset, box.getMinY() - commandBlockOffset, box.getMinZ() - commandBlockOffset );
        BlockState structureBlockState = Blocks.STRUCTURE_BLOCK.getDefaultState().with( StructureBlock.MODE, StructureBlockMode.SAVE );
        world.setBlockState( structureBlockPos, structureBlockState, NOTIFY_LISTENERS );
        StructureBlockBlockEntity structure = (StructureBlockBlockEntity) world.getBlockEntity(structureBlockPos);

        structure.setOffset( new BlockPos( commandBlockOffset, commandBlockOffset, commandBlockOffset ) );
        structure.setSize( box.getDimensions().add( 1, 1, 1 ) );
        structure.setShowBoundingBox( true );
        structure.markDirty();

        return structure;
    }
}
